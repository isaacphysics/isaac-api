package uk.ac.cam.cl.dtg.segue.dao;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import uk.ac.cam.cl.dtg.segue.dto.Content;

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
	public List<Content> findAllByType(String type, String version, Integer limit);
	
	/**
	 * Allows fullText search using the internal search provider.
	 * 
	 * @param version
	 * @param searchString
	 * @return list of results ordered by relevance.
	 */
	public List<Content> searchForContent(String version, String searchString);
	
	/**
	 * Search for content by providing a set of tags
	 * 
	 * @param version
	 * @param tags
	 * @return Content objects that are associated with any of the tags specified.
	 */
	public Set<Content> getContentByTags(String version, Set<String> tags);
	
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
	 * A utility method to instruct the content manager to evict all of its cached data.
	 */
	public void clearCache();
}
