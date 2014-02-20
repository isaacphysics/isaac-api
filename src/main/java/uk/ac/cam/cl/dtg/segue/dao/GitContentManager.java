package uk.ac.cam.cl.dtg.segue.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

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

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.PersistenceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.Choice;
import uk.ac.cam.cl.dtg.segue.dto.Content;

/**
 * Implementation that specifically works with MongoDB Content objects
 *
 */
public class GitContentManager implements IContentManager {

	private final GitDb database;
	private final ContentMapper mapper;
	
	private final String defaultSha = "35d0b0440010d9742a0cd558d184739a83a4ef70";
	
	private static final Logger log = LoggerFactory.getLogger(GitContentManager.class);
	
	private HashMap<String, HashMap<String,Content>> gitCache;
	
	@Inject
	public GitContentManager(GitDb database) {
		this.database = database;
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		this.mapper = injector.getInstance(ContentMapper.class);
		this.gitCache = new HashMap<String,HashMap<String,Content>>();
	}
	
	public void buildGitIndex(String sha){
		if(null != sha && gitCache.get(sha) == null){
			
			// find all json files for a given git commit
			Git gitHandle = GitDb.getGitHandle();
			
			// iterate through them to create content objects
			Repository repository = gitHandle.getRepository();
			
			try{
				ObjectId commitId = null;
				if(sha.toLowerCase() == "head")
			    	commitId = repository.resolve(Constants.HEAD);
				else
					commitId = repository.resolve(sha);
				 
			    RevWalk revWalk = new RevWalk(repository);
			    RevCommit commit = revWalk.parseCommit(commitId);
				
			    RevTree tree = commit.getTree();	    

			    TreeWalk treeWalk = new TreeWalk(repository);
			    treeWalk.addTree(tree);
			    treeWalk.setRecursive(true);
			    treeWalk.setFilter(PathSuffixFilter.create(".json"));
			    HashMap<String,Content> shaCache = new HashMap<String,Content>();
			    
			    while(treeWalk.next()){
			    	// throw exception if we find that there is more than one that matches the search.
			    	
		    		ObjectId objectId = treeWalk.getObjectId(0);
		    	    ObjectLoader loader = repository.open(objectId);
		    		 
		    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
		    	    loader.copyTo(out);
		    	    
		    	    Content c = mapper.mapJsonStringToContentDTO(out.toString());
		    	    
		    	    log.warn(c.getTitle() + " " + c.getType());
		    	    shaCache.put(c.getId(), c);
		    	    log.debug("Loaded object: "+c.getId()+" from git.");System.out.println();
			    }
			    
			    gitCache.put(sha, shaCache);
			    
			}
			catch(IOException exception){
				exception.printStackTrace();
				log.error("Exception whil trying to access git repository. " + exception.getMessage());
			}

			// populate the gitCache map
		}
	}

	
	
	@Override
	public <T extends Content> String save(T objectToSave) throws IllegalArgumentException {
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}
	
	@Override
	public Content getById(String id) throws IllegalArgumentException{
		
		if(null == id)
			return null;
		
		if(!gitCache.containsKey(defaultSha)){
			buildGitIndex(defaultSha);
		}
		
		return gitCache.get(defaultSha).get(id);
	}
	
	@Override
	public List<Content> findAllByType(String type, Integer limit){
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}
}
