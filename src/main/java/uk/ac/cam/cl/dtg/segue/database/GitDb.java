package uk.ac.cam.cl.dtg.segue.database;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a representation of the Git Database and provides some helper methods to allow file access.
 * 
 * It is responsible for providing basic functionality to search a specified Git Repository and find files based on a given SHA.
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
	 * This method will access the git repository given a particular SHA and will attempt to locate a unique file and return a bytearrayoutputstream of the files contents.
	 * 
	 * @param SHA to search in.
	 * @param Full file path to search for e.g. /src/filename.json
	 * @return the ByteArrayOutputStream - which you can extract the file contents via the toString method.
	 * @throws IOException
	 * @throws UnsupportedOperationException - This method is intended to only locate one file at a time. If your search matches multiple files then this exception will be thrown.
	 */
	public ByteArrayOutputStream getFileByCommitSHA(String sha, String fullFilePath) throws IOException, UnsupportedOperationException{
		if(null == sha || null == fullFilePath)
			return null;
		
		ObjectId objectId = this.findGitObject(sha, fullFilePath);
		
		if(null == objectId){
			return null;
		}
		
		Repository repository = gitHandle.getRepository();
	    ObjectLoader loader = repository.open(objectId);
	 
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    loader.copyTo(out);
	    
	    repository.close();
	    return out;
	}	

	/**
	 * This method will configure a treewalk object that can be used to navigate the git repository.
	 * 
	 * @param sha - the version that the treewalk should be configured to search within.
	 * @param searchString - the search string which can be a full path or simply a file extension.
	 * @return A preconfigured treewalk object.
	 * @throws IOException
	 * @throws UnsupportedOperationException
	 */
	public TreeWalk getTreeWalk(String sha, String searchString) throws IOException, UnsupportedOperationException{
		if(null == sha || null == searchString){
			return null;
		}
		
		ObjectId commitId = gitHandle.getRepository().resolve(sha);
		if(null == commitId){
			log.error("Failed to buildGitIndex - Unable to locate resource with SHA: " + sha);
		}else{
			RevWalk revWalk = new RevWalk(gitHandle.getRepository());
		    RevCommit commit = revWalk.parseCommit(commitId);
			
		    RevTree tree = commit.getTree();

		    TreeWalk treeWalk = new TreeWalk(gitHandle.getRepository());
		    treeWalk.addTree(tree);
		    treeWalk.setRecursive(true);
		    treeWalk.setFilter(PathSuffixFilter.create(searchString));
		    
		    return treeWalk;
		}
		return null;
	}
	
	/**
	 * Get the git handle for the database 
	 * @return
	 */
	public Repository getGitRepository(){
		return gitHandle.getRepository();
	}
	
	/**
	 * Attempt to verify if an object exists in the git repository for a given sha and full path.
	 * 
	 * @param sha
	 * @param fullfilePath
	 * @return True if we can successfully find the object, false if not. False if we encounter an exception.
	 */
	public boolean verifyGitObject(String sha, String fullfilePath){
		try {
			if(findGitObject(sha, fullfilePath)!= null){
				return true;
			}
		} catch (UnsupportedOperationException | IOException e) {
			return false;
		}
		return false;
	}
	
	/**
	 * Check that a commit sha exists within the git repository.
	 * 
	 * @param sha
	 * @return True if we have found the git sha false if not.
	 */
	public boolean verifyCommitExists(String sha){
		try {
			Iterable<RevCommit> logs = gitHandle.log().all().call();
			
			for(RevCommit rev : logs){
				if(rev.getName().equals(sha))
					return true;
			}
			
		} catch (NoHeadException e) {
			log.error("Git returned a no head exception. Unable to list all commits.");
			e.printStackTrace();
		} catch (GitAPIException e) {
			log.error("Git returned an API exception. Unable to list all commits.");
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Git returned an IO exception. Unable to list all commits.");
			e.printStackTrace();
		}
		log.warn("Commit " + sha + " does not exist");
		return false;
	}
	
	/**
	 * gets a complete list of commits with the most recent commit first.
	 * 
	 * @return List of the commit shas we have found in the git repository.
	 */
	public List<RevCommit> listCommits(){
		List<RevCommit> logList = null;
		try {
			Iterable<RevCommit> logs = gitHandle.log().all().call();
			logList = new ArrayList<RevCommit>();
			
			for(RevCommit rev : logs){
				logList.add(rev);
			}
			
		} catch (NoHeadException e) {
			log.error("Git returned a no head exception. Unable to list all commits.");
		} catch (GitAPIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("Git returned an API exception. Unable to list all commits.");			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("Git returned an IO exception. Unable to list all commits.");
		}	
		return logList;
	}
	
	/**
	 * Retrieve the SHA that is the head of the repository
	 * 
	 * @return String of sha id
	 */
	public String getHeadSha(){
		String result = null;
		
		try {
			result =  gitHandle.getRepository().resolve(Constants.HEAD).getName();
		} catch (RevisionSyntaxException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("Error getting the head from the repository.");
		}
		return result;
	}
	
	/**
	 * Will find an object from the git repository if given a sha and a full git path.
	 * 
	 * @param sha
	 * @param filename
	 * @return ObjectId which will allow you to access information about the node.
	 */
	private ObjectId findGitObject(String sha, String filename) throws IOException, UnsupportedOperationException{
		if(null == sha || null == filename){
			return null;
		}
		
		Repository repository = gitHandle.getRepository();
		
		ObjectId commitId = repository.resolve(sha);
		 
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
	    
	    revWalk.dispose();
	    log.info("Retrieved Commit Id: " + commitId.getName() + " Searching for: "+ filename + " found: " + path);
	    return objectId;
	}

}
