package uk.ac.cam.cl.dtg.isaac.configuration;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

/**
 * This class modifies the RestEasyJackson Configuration globally for all end points.
 * @author Stephen Cummins
 *
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class RestEasyJacksonConfiguration extends JacksonJaxbJsonProvider {
	
	/**
	 * Constructor.
	 */
	public RestEasyJacksonConfiguration() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		super.setMapper(mapper);
	}
}
