package uk.ac.cam.cl.dtg.segue.database;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * This class is a representation of the Git Database
 * 
 * It is responsible for providing basic functionality to search a specified Git Repository and find files based on a given SHA
 * 
 */
public class GitDb { 
	private static final Logger log = LoggerFactory.getLogger(GitDb.class);
	
	private String repositoryLocation;
	private Git gitHandle;
	
	/**
	 * Create a new instance of a GitDb object
	 * 
	 * This will immediately try and connect to the Git folder specified to check its validity.
	 *
	 * @param repoLocation - location of the git repository
	 * @throws IOException
	 */
	public GitDb(String repoLocation) throws IOException{
		this.repositoryLocation = repoLocation;
		gitHandle = Git.open(new File(this.repositoryLocation));
	}

	/**
	 * getFileByCommitSHA
	 * 
	 * This method will search the git repository for a particular SHA and will attempt to locate a unique file within the repository at the time of the SHA provided.
	 * 
	 * @param SHA to search for
	 * @param Filename to search for
	 * @return the ByteArrayOutputStream - which you can extract the file contents via the toString method.
	 * @throws IOException
	 * @throws UnsupportedOperationException - This method is intended to only locate one file at a time. If your search matches multiple files then this exception will be thrown.
	 */
	public ByteArrayOutputStream getFileByCommitSHA(String sha, String filename) throws IOException, UnsupportedOperationException{
		
		if(null == sha || null == filename)
			return null;
		
		Repository repository = gitHandle.getRepository();
		
		// TODO refactor this
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
	    treeWalk.setFilter(PathFilter.create(filename));
	    
	    int count = 0;
	    ObjectId objectId = null;
	    String path = null;
	    while(treeWalk.next()){
	    	count++;
	    	if (null == objectId){
	    		objectId = treeWalk.getObjectId(0);
	    		path = treeWalk.getPathString();
	    	}
	    	// throw exception if we find that there is more than one that matches the search.
	    	else if(count > 1){
	    		StringBuilder sb = new StringBuilder();
	    		sb.append("Multiple results have been found in the git repository for the following search: ");
	    		sb.append(filename + ".");
	    		sb.append(" in ");
	    		sb.append(sha);
	    		sb.append(" Unable to decide which one to return.");
	    		throw new UnsupportedOperationException(sb.toString());
	    	}
	    }
	    
	    if (null == objectId){
		    log.warn("No objects found matching the search criteria ("+ sha + "," +filename  +") in Git");  
		    return null;
	    }
	   
	    ObjectLoader loader = repository.open(objectId);
	 
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    loader.copyTo(out);
	    
	    log.info("Retrieved Commit Id: " + commitId.getName() + " Searching for: "+ filename + " found: " + path);
	    
	    revWalk.dispose();
	    repository.close();
	    return out;
	}	

	public Git getGitHandle(){
		return gitHandle;
	}
}
