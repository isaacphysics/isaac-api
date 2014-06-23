package uk.ac.cam.cl.dtg.segue.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.Content;

public interface IContentManager {

	public <T extends Content> String save(T objectToSave);
	
	/**
	 * Goes to the configured Database and attempts to find a content item with the specified ID
	 * 
	 * @param unique id to search for in preconfigured data source
	 * @return Will return a Content object (or subclass of Content) or Null if no content object is found
	 * @throws Throws IllegalArgumentException if a mapping error occurs  
	 */
	public Content getById(String id, String version);
	
	/**
	 * Method to allow bulk search of content based on the type field
	 * 
	 * @param type - should match whatever is stored in the database
	 * @param limit - limit the number of results returned - if null or 0 is provided no limit will be applied. 
	 * @return List of Content Objects or an empty list if none are found
	 */
	public ResultsWrapper<Content> findByFieldNames(String version, final Map<String,List<String>> fieldsToMatch, Integer startIndex, Integer limit);
	
	/**
	 * The same as findByFieldNames but the results list is returned in a randomised order.
	 * 
	 * @see findByFieldNames
	 * @param version
	 * @param fieldsToMatch
	 * @param startIndex
	 * @param limit
	 * @return Results wrapper containing the results or an empty list.
	 */
	public ResultsWrapper<Content> findByFieldNamesRandomOrder(String version,	Map<String, List<String>> fieldsToMatch, Integer startIndex, Integer limit);
	
	/**
	 * Allows fullText search using the internal search provider.
	 * 
	 * @param version
	 * @param searchString
	 * @return list of results ordered by relevance.
	 */
	public ResultsWrapper<Content> searchForContent(String version, String searchString);
	
	/**
	 * Search for content by providing a set of tags
	 * 
	 * @param version
	 * @param tags
	 * @return Content objects that are associated with any of the tags specified.
	 */
	public ResultsWrapper<Content> getContentByTags(String version, Set<String> tags);
	
	/**
	 * Method allows raw output to be retrieved for given files in the git repository. This is mainly so we can retrieve image files.
	 * 
	 * @param version - The version of the content to retrieve
	 * @param filename - The full path of the file you wish to retrieve. 
	 * @return The outputstream of the file contents
	 */
	public ByteArrayOutputStream getFileBytes(String version, String filename) throws IOException;
	
	/**
	 * Provide a list of all possible versions from the underlying database
	 * 
	 * This method will only work on IContentManagers that store snapshots with attached version numbers.
	 * 
	 * @return
	 */
	public List<String> listAvailableVersions() throws UnsupportedOperationException;
	
	/**
	 * Get the latest version id from the data source
	 * 
	 * @return
	 */
	public String getLatestVersionId() throws UnsupportedOperationException;
	
	/**
	 * A utility method to instruct the content manager to evict ALL of its cached data including data held within its search providers.
	 * 
	 * WARNING: this is a nuclear method. Re-indexing will definitely have to occur if you do this.
	 * 
	 */
	public void clearCache();
	
	/**
	 * A utility method to instruct a content manager to evict a particular version of the content from its caches. This includes data held within its search providers.
	 * 
	 * @param version
	 */
	public void clearCache(String version);

	/**
	 * A method that will return an unordered set of tags registered for a particular version of the content 
	 *  
	 * @param version
	 * @return A set of tags that have been already used in a particular version of the content
	 */
	public Set<String> getTagsList(String version);

	/**
	 * Provides a Set of currently indexed and cached versions. 
	 * 
	 * @return A set of all of the version id's which are currently available without reindexing.
	 */
	public Set<String> getCachedVersionList();
	
	/**
	 * Utility method that will check whether a version number supplied validates.
	 * 
	 * @param version
	 * @return true if the version specified is valid and can potentially be indexed, false if it cannot.
	 */
	public boolean isValidVersion(String version);
	
	/**
	 * Will build the cache and search index, if necessary
	 * 
	 * Note: it is the responsibility of the caller to manage the cache size.
	 * 
	 * @param version - version
	 * @return True if version exists in cache, false if not
	 */
	public boolean ensureCache(String version);

	/**
	 * This method will compare two versions to determine which is the newer.
	 * 
	 * @param version1
	 * @param version2
	 * 
	 * @return a positive number if version1 is newer, zero if they are the same, and a negative number if version 2 is newer.
	 */
	public int compareTo(String version1, String version2);

	/**
	 * Get the problem map for a particular version 
	 * 
	 * @param version
	 * @return the map containing the content objects with problems and associated list of problem messages. Or null if there is no problem index.
	 */
	public Map<Content, List<String>> getProblemMap(String version);
}
