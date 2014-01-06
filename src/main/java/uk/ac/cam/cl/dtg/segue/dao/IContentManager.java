package uk.ac.cam.cl.dtg.segue.dao;

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
	 * Method to support the expansion of the recursive datastructure.
	 * @param content
	 * @return Augmented Content object
	 */
	public Content expandReferencedContent(Content content);
}
