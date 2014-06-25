package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Content deserializer will try and use the map built up in the ContentMapper
 * class to determine what subtype of content needs to be created.
 * 
 * Currently this is dependent on the register map key being the exact same text
 * as the json type property value stored in the database.
 * 
 */
public class TrimWhitespaceDeserializer extends JsonDeserializer<String> {

	@Override
	public String deserialize(JsonParser jsonParser,
			DeserializationContext deserializationContext) throws IOException,
			JsonProcessingException, JsonMappingException {
		return jsonParser.getText().trim();
	}
}