package uk.ac.cam.cl.dtg.segue.dao;

import uk.ac.cam.cl.dtg.segue.dto.Content;

public interface IContentPersistenceManager {

	public String save(Content objectToSave);
	
	public Content getById(String id);
	
}
