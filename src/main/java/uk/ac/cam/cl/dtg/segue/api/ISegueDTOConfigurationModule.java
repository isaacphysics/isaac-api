package uk.ac.cam.cl.dtg.segue.api;

import java.util.Map;

import uk.ac.cam.cl.dtg.segue.dto.content.Content;

public interface ISegueDTOConfigurationModule {

	/**
	 * This method should provide a map of 'type' identifiers to Classes which
	 * extend the Segue Content DTO.
	 * 
	 * The DTOs registered using this method should match the content objects
	 * stored in the content object datastore.
	 * 
	 * Note: It is expected that the 'type' key should be exactly the same as
	 * any type declared in json files that might need to be deserialized.
	 * 
	 * @return
	 */
	public Map<String, Class<? extends Content>> getContentDataTransferObjectMap();

}
