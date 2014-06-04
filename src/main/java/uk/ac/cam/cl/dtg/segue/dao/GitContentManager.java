package uk.ac.cam.cl.dtg.segue.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Validate;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Sets;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.ContentBase;
import uk.ac.cam.cl.dtg.segue.dto.Figure;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;

/**
 * Implementation that specifically works with Content objects
 *
 */
public class GitContentManager implements IContentManager {
	private static final Logger log = LoggerFactory.getLogger(GitContentManager.class);
	
	private static final String CONTENT_TYPE = "content";

	private static final Map<String, Map<String,Content>> gitCache = new ConcurrentHashMap<String,Map<String,Content>>();
	private static final Map<String, Set<String>> tagsList = new ConcurrentHashMap<String,Set<String>>();
	
	private final GitDb database;
	private final ContentMapper mapper;
	private final ISearchProvider searchProvider;
	
	@Inject
	public GitContentManager(GitDb database, ISearchProvider searchProvider, ContentMapper contentMapper) {
		this.database = database;
		this.mapper = contentMapper;
		this.searchProvider = searchProvider;
	}	
	
	@Override
	public <T extends Content> String save(T objectToSave) throws IllegalArgumentException {
		throw new UnsupportedOperationException("This method is not implemented yet - Git is a readonly data store at the moment.");
	}

	@Override
	public Content getById(String id, String version) throws IllegalArgumentException{
		if(null == id){
			return null;
		}
			
		if(this.ensureCache(version)){
			Content result = gitCache.get(version).get(id);
			if(null == result){
				log.info("Failed to locate the content "+ id + " in the cache for version " + version);
			}
			else{
				log.info("Loading content from cache: " + id);
			}
			return result;			
		}
		else{
			return null;
		}
	}

	@Override
	public List<Content> searchForContent(String version, String searchString){
		if(this.ensureCache(version)){
			List<String> searchHits = searchProvider.fuzzySearch(version, CONTENT_TYPE, searchString, Constants.ID_FIELDNAME, Constants.TITLE_FIELDNAME, Constants.TAGS_FIELDNAME, Constants.VALUE_FIELDNAME, Constants.CHILDREN_FIELDNAME);
		    
			// setup object mapper to use preconfigured deserializer module. Required to deal with type polymorphism
		    ObjectMapper objectMapper = mapper.getContentObjectMapper();
		    
		    List<Content> searchResults = new ArrayList<Content>();
		    for(String hit : searchHits){
		    	try {
					searchResults.add((Content) objectMapper.readValue(hit, ContentBase.class));
				} catch (JsonParseException e) {
					e.printStackTrace();
				} catch (JsonMappingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		    }
			return searchResults;
		}
		else{
			log.error("Unable to ensure cache for requested version" + version);
			return null;
		}
		
	}
	
	@Override
	public List<Content> findByFieldNames(String version, final Map<String,List<String>> fieldsToMatch, Integer startIndex, Integer limit){
		List<Content> result = new ArrayList<Content>();
		if(this.ensureCache(version)){

			Map<String, Constants.SortOrder> sortInstructions = new HashMap<String, Constants.SortOrder>();
			sortInstructions.put(Constants.TITLE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX, Constants.SortOrder.ASC);
			
			//TODO: fix tag search
			if(fieldsToMatch.containsKey(Constants.TAGS_FIELDNAME)){
				List<String> tagList = Arrays.asList(fieldsToMatch.get(Constants.TAGS_FIELDNAME).get(0).split(","));
				fieldsToMatch.put(Constants.TAGS_FIELDNAME, tagList);
			}
			
			List<String> searchHits = searchProvider.paginatedMatchSearch(version, CONTENT_TYPE, fieldsToMatch, startIndex, limit, sortInstructions);

			// setup object mapper to use preconfigured deserializer module. Required to deal with type polymorphism
			result = mapper.mapFromStringListToContentList(searchHits);
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
	
	@Override
	public boolean isValidVersion(String version){
		if(null == version || version.isEmpty()){
			return false;
		}
			
		return this.database.verifyCommitExists(version);
	}
	
	@Override
	public int compareTo(String version1, String version2){
		Validate.notBlank(version1);
		Validate.notBlank(version2);
		
		int version1Epoch = this.database.getCommitTime(version1);
		int version2Epoch = this.database.getCommitTime(version2);
		
		return version1Epoch - version2Epoch;
	}
	
	@Override
	public String getLatestVersionId() throws UnsupportedOperationException {
		return database.pullLatestFromRemote();
	}

	@Override
	public Set<String> getCachedVersionList(){		
		return gitCache.keySet();
	}

	@Override
	public void clearCache() {
		log.info("Clearing Git content cache.");
		gitCache.clear();
		searchProvider.expungeEntireSearchCache();
	}

	@Override
	public void clearCache(String version){
		Validate.notBlank(version);
		
		if(gitCache.containsKey(version)){
			gitCache.remove(version);
			searchProvider.expungeIndexFromSearchCache(version);	
		}
	}

	@Override
	public List<Content> getContentByTags(String version, Set<String> tags){
		if(null==version || null == tags){
			return null;
		}
		
		if(this.ensureCache(version)){
			List<String> searchResults = this.searchProvider.termSearch(version, CONTENT_TYPE, tags, "tags");
			
			List<Content> contentResults = mapper.mapFromStringListToContentList(searchResults);

			return contentResults;
		}
		else{
			log.error("Cache not found. Failed to build cache with version: " + version);
			return null;
		}
	}
	
	@Override
	public Set<String> getTagsList(String version){
		Validate.notBlank(version);
		
		this.ensureCache(version);
		
		if(!tagsList.containsKey(version)){
			log.warn("The version requested does not exist in the tag list.");
			return null;
		}

		return tagsList.get(version);
	}
	

	@Override
	public boolean ensureCache(String version){
		if(!gitCache.containsKey(version)){
			if(database.verifyCommitExists(version)){
				log.info("Rebuilding cache as sha does not exist in hashmap");
				buildGitContentIndex(version);
				buildSearchIndexFromLocalGitIndex(version);
				validateReferentialIntegrity(version);
			}else{
				log.warn("Unable find the commit (" + version + ") in git to ensure the cache");
				return false;
			}
		}
		
		boolean searchIndexed = searchProvider.hasIndex(version);
		
		if(!searchIndexed){
			log.warn("Search does not have a valid index for the "+ version + " version of the content");
			this.buildSearchIndexFromLocalGitIndex(version);
		}
		
		return gitCache.containsKey(version) && searchIndexed;
	}
	
	/**
	 * This method will send off the information in the git cache to the search provider for indexing.
	 * 
	 * @param sha - the version in the git cache to send to the search provider.
	 */
	private synchronized void buildSearchIndexFromLocalGitIndex(String sha){
		if(!gitCache.containsKey(sha)){
			log.error("Unable to create search index as git cache does not exist locally");
			return;
		}
		
		if(this.searchProvider.hasIndex(sha)){
			log.info("Search index has already been updated by another thread. No need to reindex. Aborting...");
			return;
		}
		
		log.info("Building search index for: " + sha);
		for(Content content : gitCache.get(sha).values()){
    	    // setup object mapper to use pre-configured deserializer module. Required to deal with type polymorphism
    	    ObjectMapper objectMapper = mapper.getContentObjectMapper();
			
			try {
				this.searchProvider.indexObject(sha, CONTENT_TYPE, objectMapper.writeValueAsString(content), content.getId());
			} catch (JsonProcessingException e) {
				log.error("Unable to serialize content object for indexing with the search provider." );
				e.printStackTrace();
			}
		}
		log.info("Search index built for: " + sha);
	}
	
	/**
	 * This method will populate the gitCache based on the content object files found for a given SHA.
	 * 
	 * Currently it only looks for json files in the repository.
	 * 
	 * @param sha
	 */
	private synchronized void buildGitContentIndex(String sha){
		// This set of code only needs to happen if we have to read from git again.
		if(null != sha && gitCache.get(sha) == null){
			
			// iterate through them to create content objects
			Repository repository = database.getGitRepository();
			
			try{
				ObjectId commitId = repository.resolve(sha);
				
				if(null == commitId){
					log.error("Failed to buildGitIndex - Unable to locate resource with SHA: " + sha);
					return;
				}
				
			    Map<String,Content> shaCache = new HashMap<String,Content>();
				TreeWalk treeWalk = database.getTreeWalk(sha, ".json");
				log.info("Populating git content cache based on sha " + sha + " ...");
				
			    // Traverse the git repository looking for the .json files
			    while(treeWalk.next()){
		    		ObjectId objectId = treeWalk.getObjectId(0);
		    	    ObjectLoader loader = repository.open(objectId);
		    		 
		    	    ByteArrayOutputStream out = new ByteArrayOutputStream();
		    	    loader.copyTo(out);

		    	    // setup object mapper to use preconfigured deserializer module. Required to deal with type polymorphism		    	    
		    	    ObjectMapper objectMapper = mapper.getContentObjectMapper();
		    	    
		    	    Content content = null;
		    	    try{
			    	    String rawJson = out.toString();
		    	    	content = (Content) objectMapper.readValue(rawJson, ContentBase.class);
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
							    	    	log.warn("Resource with duplicate ID (" + content.getId() +") detected in cache. Skipping " + treeWalk.getPathString());
						    	    	}
				    	    			// if the content is the same then it is just reuse of a content object so that is fine.
				    	    			else{
							    	    	log.info("Resource (" + content.getId() +") already seen in cache. Skipping " + treeWalk.getPathString());
						    	    	}
						    	    }
				    	    		// It must be new so we can add it
						    	    else{
						    	    	log.debug("Loading into cache: " + flattenedContent.getId() + "(" +flattenedContent.getType() + ")" + " from " + treeWalk.getPathString());
						    	    	shaCache.put(flattenedContent.getId(), flattenedContent);
						    	    	registerTagsWithVersion(sha, flattenedContent.getTags());
						    	    }
				    	    	}
				    	    }
				    	    
			    	    }		    	    
		    	    }
		    	    catch(JsonMappingException e){
		    	    	log.warn("Unable to parse the json file found " + treeWalk.getPathString() +" as a content object. Skipping file...");
		    	    	e.printStackTrace();
		    	    }
			    }

			    // add all of the work we have done to the git cache.
			    gitCache.put(sha, shaCache);
			    repository.close();
			    log.info("Tags available " + tagsList);
				log.info("Git content cache population for " + sha + " completed!");
				
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
	 * This should be done before saving to the local gitCache in memory storage.
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

		// TODO Improve Hack to convert image source into something that the api can use to locate the specific image in the repository.
		if(content.getType().equals("image") || content.getType().equals("figure")){
			Figure figure = (Figure) content;
			
			String newPath = FilenameUtils.normalize(FilenameUtils.getPath(canonicalSourceFile) + figure.getSrc(),true);
			figure.setSrc(newPath);
		}

		return content;		
	}
	
	/**
	 * This method will attempt to traverse the cache to ensure that all content references are valid.
	 * TODO: Make this more efficient or only call it on demand?
	 * 
	 * @param sha
	 * @return True if we are happy with the integrity of the git repository, False if there is something wrong.
	 */
	private boolean validateReferentialIntegrity(String sha){
		Set<Content> allObjectsSeen = new HashSet<Content>();
		
		Set<String> expectedIds = new HashSet<String>();
		Set<String> definedIds = new HashSet<String>();
		Set<String> missingContent = new HashSet<String>();
		
		// Build up a set of all content (and content fragments for validation)
		for(Content c : gitCache.get(sha).values()){			
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
				Figure f = (Figure) c;
				
				if(f.getSrc() != null && !f.getSrc().startsWith("http") && !database.verifyGitObject(sha, f.getSrc())){
					log.warn("Unable to find Image: " + f.getSrc() + " in Git. Could the reference be incorrect? SourceFile is " + c.getCanonicalSourceFile());
					missingContent.add("Image: " + f.getSrc());
				}					
				else
					log.debug("Verified image " + f.getSrc() + " exists in git.");
			}
		}
		
		if(expectedIds.equals(definedIds) && missingContent.isEmpty()){
			return true;
		}
		else
		{
			expectedIds.removeAll(definedIds);
			missingContent.addAll(expectedIds);
			log.error("Referential integrity broken for (" + expectedIds.size() + ") related Content items. The following ids are referenced but do not exist: " + expectedIds.toString());
			return false;
		}
	}
	
	/**
	 * Unpack the content objects into one big set. Useful for validation but could produce a very large set
	 * 
	 * @param content content object to flatten
	 * @return Set of content objects comprised of all children and the parent.
	 */
	private Set<Content> flattenContentObjects(Content content){
		Set<Content> setOfContentObjects = new HashSet<Content>();
		
		if(!content.getChildren().isEmpty()){

			List<ContentBase> children = content.getChildren();
			
			for(ContentBase child : children){
				setOfContentObjects.add((Content) child);
				setOfContentObjects.addAll(flattenContentObjects((Content) child));
			}
		}

		setOfContentObjects.add(content);
		
		return setOfContentObjects;
	}
	
	private void registerTagsWithVersion(String version, Set<String> tags){
		Validate.notBlank(version);
		
		if(null == tags || tags.isEmpty()){
			// don't do anything.
			return;
		}
		
		if(!tagsList.containsKey(version)){
			tagsList.put(version, new HashSet<String>());
		}
		Set<String> newTagSet = Sets.newHashSet();
		
		// sanity check that tags are trimmed.
		for(String tag : tags){
			newTagSet.add(tag.trim());
		}
		
		tagsList.get(version).addAll(newTagSet);
	}
}
