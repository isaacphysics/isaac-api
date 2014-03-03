package uk.ac.cam.cl.dtg.segue.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.PersistenceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.ContentBase;

/**
 * Implementation that specifically works with Content objects
 *
 */
public class GitContentManager implements IContentManager {
	private final GitDb database;
	private final ContentMapper mapper;
	
	private static final Logger log = LoggerFactory.getLogger(GitContentManager.class);

	// TODO: should we make this a weak cache?
	private static Map<String, Map<String,Content>> gitCache = new HashMap<String,Map<String,Content>>();
	
	@Inject
	public GitContentManager(GitDb database) {
		this.database = database;
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		this.mapper = injector.getInstance(ContentMapper.class);
	}	
	
	@Override
	public <T extends Content> String save(T objectToSave) throws IllegalArgumentException {
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}
	
	@Override
	public Content getById(String id, String version) throws IllegalArgumentException{
		if(null == id)
			return null;
		
		if(this.ensureCache(version)){
			log.info("Loading content from cache: " + id);
			return gitCache.get(version).get(id);			
		}
		else{
			log.error("Unable to access Content with ID "+ id + " from git commit: " + version + " from cache.");
			return null;
		}
		
	}
	
	@Override
	public List<Content> findAllByType(String type, String version, Integer limit){
		ArrayList<Content> result = new ArrayList<Content>();
		if(this.ensureCache(version)){
			for(Content c : gitCache.get(version).values()){
				if(result.size() == limit && limit > 0)
					break;
				
				if(c.getType().equals(type)){
					result.add(c);
				}
			}			
		}
		
		return result;
	}
	
	/**
	 * Will build cache if necessary. 
	 * 
	 * @param version - SHA
	 * @return True if version exists in cache, false if not
	 */
	private boolean ensureCache(String version){
		if(!gitCache.containsKey(version)){
			log.info("Rebuilding cache as sha does not exist in hashmap");
			buildGitIndex(version);
			validateReferentialIntegrity(version);
		}
		
		return gitCache.containsKey(version);
	}
	
	/**
	 * This method will populate the gitCache based on the content object files found for a given SHA 
	 * 
	 * @param sha
	 */
	private void buildGitIndex(String sha){
		// This set of code only needs to happen if we have to read from git again.
		if(null != sha && gitCache.get(sha) == null){
			
			// find all json files for a given git commit
			Git gitHandle = database.getGitHandle();
			
			// iterate through them to create content objects
			Repository repository = gitHandle.getRepository();
			
			try{
				ObjectId commitId = null;
				if(sha.toLowerCase() == "head")
			    	commitId = repository.resolve(Constants.HEAD);
				else
					commitId = repository.resolve(sha);
				
				if(null == commitId){
					log.error("Failed to buildGitIndex - Unable to locate resource with SHA: " + sha);
				
				}else{
					
					RevWalk revWalk = new RevWalk(repository);
				    RevCommit commit = revWalk.parseCommit(commitId);
					
				    RevTree tree = commit.getTree();

				    TreeWalk treeWalk = new TreeWalk(repository);
				    treeWalk.addTree(tree);
				    treeWalk.setRecursive(true);
				    treeWalk.setFilter(PathSuffixFilter.create(".json"));
				    Map<String,Content> shaCache = new HashMap<String,Content>();

				    // Traverse the git repository looking for everything that matches the PathSuffixFilter
				    while(treeWalk.next()){
				    	
			    		ObjectId objectId = treeWalk.getObjectId(0);
			    	    ObjectLoader loader = repository.open(objectId);
			    		 
			    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
			    	    loader.copyTo(out);

			    	    ContentBaseDeserializer contentDeserializer = new ContentBaseDeserializer();
			    	    contentDeserializer.registerTypeMap(mapper.getJsonTypes());
			    	    		
			    	    SimpleModule simpleModule = new SimpleModule("ContentDeserializerModule");
			    	    simpleModule.addDeserializer(ContentBase.class, contentDeserializer);
			    	    		
			    	    ObjectMapper objectMapper = new ObjectMapper();
			    	    objectMapper.registerModule(simpleModule);
			    	    Content c = null;
			    	    try{
				    	    c = (Content) objectMapper.readValue(out.toString(), ContentBase.class);
				    	    c = this.augmentChildContent(c, treeWalk.getPathString());
			    	    }
			    	    catch(JsonMappingException e){
			    	    	log.warn("Unable to parse the json file found " + treeWalk.getPathString() +" as a content object. Skipping file...");
			    	    }
			    	    
			    	    if (null != c){
				    	    log.info("Loading into cache: " + c.getId() + " from " + treeWalk.getPathString());
					    	// throw a fit if we find that there are duplicate ids.
				    	    if(shaCache.containsKey(c.getId()))
				    	    	log.error("Resource with duplicate ID (" + c.getId() +") detected in cache. Skipping " + treeWalk.getPathString());
				    	    else
				    	    	shaCache.put(c.getId(), c);		    	    	
			    	    }
				    }
				    
				    gitCache.put(sha, shaCache);					
				}
			}
			catch(IOException exception){
				log.error("IOException while trying to access git repository. " + exception.getMessage());
				exception.printStackTrace();
			}
		}
	}
	
	/**
	 * Augments all child objects recursively to include additional information.
	 * @param content
	 * @param canonicalSourceFile
	 * @return Content object with new reference
	 */
	private Content augmentChildContent(Content content, String canonicalSourceFile){
		if(null == content){
			return null;
		}
		
		if(!content.getChildren().isEmpty()){
			for(ContentBase cb : content.getChildren()){
				if(cb instanceof Content){
					Content c = (Content) cb;
					this.augmentChildContent(c, canonicalSourceFile);
				} 
			}
		}
		
		content.setCanonicalSourceFile(canonicalSourceFile);
		return content;		
	}
	
	private boolean validateReferentialIntegrity(String versionToCheck){
		Set<String> expectedIds = new HashSet<String>();
		Set<String> definedIds = new HashSet<String>();

		// Currently check relatedContent Only. Children of Content objects are a different matter still to be defined.
		for(Content c : gitCache.get(versionToCheck).values()){
			definedIds.add(c.getId());
			
			expectedIds.addAll(c.getRelatedContent());
		}
		
		if(expectedIds.equals(definedIds)){
			return true;
		}
		else
		{
			expectedIds.removeAll(definedIds);
			log.error("Referential integrity broken for related Content. The following ids are referenced but do not exist: " + expectedIds.toString());
			return false;
		}	
	}
}
