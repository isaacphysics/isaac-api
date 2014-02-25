package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.cam.cl.dtg.segue.dto.Content;

/**
 * Content deserializer will try and use the map built up in the ContentMapper class to determine what subtype of content needs to be created.
 * 
 * Currently this is dependent on the register map key being the exact same text as the json type property value stored in the database.
 * 
 */
public class ContentBaseDeserializer extends JsonDeserializer<Content> {

	private Map<String, Class<? extends Content>> typeMap = null;

	public void registerTypeMap(Map<String, Class<? extends Content>> jsonTypes){
		this.typeMap = jsonTypes;
	}

	@Override
	public Content deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
			throws IOException, JsonProcessingException{

		if(null == typeMap){
			throw new IllegalStateException("No Map provided for Content Type deserialization.");
		}

		ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
		ObjectNode root = (ObjectNode) mapper.readTree(jsonParser);  
		Class<? extends Content> contentClass = null;  

		String contentType = root.get("type").textValue();

		if (typeMap.containsKey(contentType))
		{  
			contentClass = typeMap.get(contentType);
			return mapper.readValue(root.toString(), contentClass);  
		}  

		return mapper.readValue(root.toString(), Content.class);
	}

}