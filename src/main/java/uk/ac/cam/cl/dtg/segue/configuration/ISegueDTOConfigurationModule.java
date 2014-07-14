package uk.ac.cam.cl.dtg.segue.configuration;

import java.util.Map;

import uk.ac.cam.cl.dtg.segue.dos.content.Content;

/**
 * Interface for configuration modules that will work nicely with Segue.
 *
 */
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
	 * @return a map of string type identifiers to classes that extend Content.
	 */
	Map<String, Class<? extends Content>> getContentDataTransferObjectMap();

}
