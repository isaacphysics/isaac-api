package uk.ac.cam.cl.dtg.segue.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
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

	@Override
	public ByteArrayOutputStream getFileBytes(String version, String filename) throws IOException{
		return database.getFileByCommitSHA(version, filename);
	}
	
	@Override
	public List<String> listAvailableVersions()
			throws UnsupportedOperationException {
		
		List<String> result = new ArrayList<String>();
		for(RevCommit rc : database.listCommits()){
			result.add(rc.getName());
		}
		
		return result;
	}
	
	/**
	 * Will build cache if necessary. 
	 * 
	 * @param version - version
	 * @return True if version exists in cache, false if not
	 */
	private boolean ensureCache(String version){
		if(!gitCache.containsKey(version)){
			if(database.verifyCommitExists(version)){
				log.info("Rebuilding cache as sha does not exist in hashmap");
				buildGitIndex(version);
				validateReferentialIntegrity(version);				
			}else{
				// we can't find the commit in git.
				return false;
			}
		}
		
		return gitCache.containsKey(version);
	}
	
	/**
	 * This method will populate the gitCache based on the content object files found for a given SHA.
	 * 
	 * Currently it only looks for json files in the repository.
	 * 
	 * @param sha
	 */
	private void buildGitIndex(String sha){
		// This set of code only needs to happen if we have to read from git again.
		if(null != sha && gitCache.get(sha) == null){
			
			// iterate through them to create content objects
			Repository repository = database.getGitRepository();
			
			try{
				ObjectId commitId = repository.resolve(sha);
				
				if(null == commitId){
					log.error("Failed to buildGitIndex - Unable to locate resource with SHA: " + sha);
				}else{										
				    Map<String,Content> shaCache = new HashMap<String,Content>();
					TreeWalk treeWalk = database.getTreeWalk(sha, ".json");

				    // Traverse the git repository looking for the .json files
				    while(treeWalk.next()){
			    		ObjectId objectId = treeWalk.getObjectId(0);
			    	    ObjectLoader loader = repository.open(objectId);
			    		 
			    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
			    	    loader.copyTo(out);

			    	    // setup object mapper to use preconfigured deserializer module. Required to deal with type polymorphism
			    	    ObjectMapper objectMapper = new ObjectMapper();
			    	    objectMapper.registerModule(getContentDeserializerModule());
			    	    
			    	    Content content = null;
			    	    try{
				    	    content = (Content) objectMapper.readValue(out.toString(), ContentBase.class);
				    	    content = this.augmentChildContent(content, treeWalk.getPathString());
				    	    
				    	    if (null != content){
				    	    	// add children (and parent) from flattened Set to cache if they have ids
					    	    for(Content flattenedContent : this.flattenContentObjects(content)){
					    	    	if(flattenedContent.getId() != null){

					    	    		// check if we have seen this key before if we have then we don't want to add it again
					    	    		if(shaCache.containsKey(flattenedContent.getId())){

					    	    			// if the key is the same but the content is different then something has gone wrong - log an error
					    	    			if(!shaCache.get(flattenedContent.getId()).equals(flattenedContent)){
								    	    	// log an error if we find that there are duplicate ids and the content is different.
								    	    	log.error("Resource with duplicate ID (" + content.getId() +") detected in cache. Skipping " + treeWalk.getPathString());
							    	    	}
					    	    			// if the content is the same then it is just reuse of a content object so that is fine.
					    	    			else{
								    	    	log.debug("Resource (" + content.getId() +") already seen in cache. Skipping " + treeWalk.getPathString());
							    	    	}
							    	    }
					    	    		// It must be new so we can add it
							    	    else{
							    	    	log.info("Loading into cache: " + flattenedContent.getId() + "(" +flattenedContent.getType() + ")" + " from " + treeWalk.getPathString());
							    	    	shaCache.put(flattenedContent.getId(), flattenedContent);
							    	    }
					    	    	}
					    	    }
					    	    
				    	    }		    	    
			    	    }
			    	    catch(JsonMappingException e){
			    	    	log.warn("Unable to parse the json file found " + treeWalk.getPathString() +" as a content object. Skipping file...");
			    	    }
				    }
				    
				    gitCache.put(sha, shaCache);
				    repository.close();
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
	 * 
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

		// Hack to convert image source into something that the api can use to locate the specific image in the repository.
		if(content.getType().equals("image")){
			String newPath = FilenameUtils.normalize(FilenameUtils.getPath(canonicalSourceFile) + content.getSrc(),true);
			content.setSrc(newPath);
		}

		return content;		
	}
	
	/**
	 * Provides a preconfigured module that can be added to an object mapper so that contentBase objects can be deseerialized using the custom deserializer.
	 * @return
	 */
	private SimpleModule getContentDeserializerModule(){ 
	    ContentBaseDeserializer contentDeserializer = new ContentBaseDeserializer();
	    contentDeserializer.registerTypeMap(mapper.getJsonTypes());
	    		
	    SimpleModule simpleModule = new SimpleModule("ContentDeserializerModule");
	    simpleModule.addDeserializer(ContentBase.class, contentDeserializer);
	    return simpleModule;
	}
	
	/**
	 * This method will attempt to traverse the cache to ensure that all content references are valid.
	 * @param versionToCheck
	 * @return
	 */
	private boolean validateReferentialIntegrity(String versionToCheck){
		Set<Content> allObjectsSeen = new HashSet<Content>();
		
		Set<String> expectedIds = new HashSet<String>();
		Set<String> definedIds = new HashSet<String>();
		Set<String> missingContent = new HashSet<String>();
		
		// Build up a set of all content (and content fragments for validation)
		for(Content c : gitCache.get(versionToCheck).values()){			
			allObjectsSeen.addAll(this.flattenContentObjects(c));
		}
		
		// Start looking for issues in the flattened content data
		for(Content c : allObjectsSeen){
			// add the id to the list of defined ids if one is set for this content object
			if(c.getId() != null)
				definedIds.add(c.getId());

			// add the ids to the list of expected ids if we see a list of referenced content  
			if(c.getRelatedContent() != null)
				expectedIds.addAll(c.getRelatedContent());
			
			// content type specific checks
			if(c.getType().equals("image")){
				if(c.getSrc() != null && !c.getSrc().startsWith("http") && !database.verifyGitObject(versionToCheck, c.getSrc())){
					log.warn("Unable to find Image: " + c.getSrc() + " in Git. Could the reference be incorrect? SourceFile is " + c.getCanonicalSourceFile());
					missingContent.add("Image: " + c.getSrc());
				}					
				else
					log.debug("Verified image " + c.getSrc() + " exists in git.");
			}
		}
		
		if(expectedIds.equals(definedIds) && missingContent.isEmpty()){
			return true;
		}
		else
		{
			expectedIds.removeAll(definedIds);
			missingContent.addAll(expectedIds);
			log.error("Referential integrity broken for related Content. The following ids are referenced but do not exist: " + expectedIds.toString());
			return false;
		}
	}
	
	/**
	 * Unpack the content objects into one big set. Useful for validation but will produce a very large set
	 * 
	 * @param c
	 * @return
	 */
	private Set<Content> flattenContentObjects(Content c){
		Set<Content> setOfContentObjects = new HashSet<Content>();
		
		if(!c.getChildren().isEmpty()){

			List<ContentBase> children = c.getChildren();
			
			for(ContentBase child : children){
				setOfContentObjects.add((Content) child);
				setOfContentObjects.addAll(flattenContentObjects((Content) child));
			}
		}

		setOfContentObjects.add(c);
		
		return setOfContentObjects;
	}

}
