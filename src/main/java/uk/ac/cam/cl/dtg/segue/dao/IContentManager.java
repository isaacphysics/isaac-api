package uk.ac.cam.cl.dtg.segue.dao;

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dto.Content;

public interface IContentManager {

	public <T extends Content> String save(T objectToSave);
	
	/**
	 * Goes to the configured Database and attempts to find a content item with the specified ID
	 * @param unique id to search for in preconfigured data source
	 * @return Will return a Content object (or subclass of Content) or Null if no content object is found
	 * @throws Throws IllegalArgumentException if a mapping error occurs  
	 */
	public Content getById(String id);
	
	/**
	 * Method to allow bulk search of content based on the type field
	 * @param type - should match whatever is stored in the database
	 * @param limit - limit the number of results returned - if null or 0 is provided no limit will be applied. 
	 * @return List of Content Objects
	 */
	public List<Content> findAllByType(String type, Integer limit);
}
