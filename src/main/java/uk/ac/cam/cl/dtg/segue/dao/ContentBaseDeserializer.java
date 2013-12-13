package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.deser.StdDeserializer;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.type.JavaType;

import uk.ac.cam.cl.dtg.segue.dto.Content;

/**
 * Content deserializer will try and use the map built up in the ContentMapper class to determine what subtype of content needs to be created.
 * 
 * Currently this is dependent on the register map key being the exact same text as the json type property value stored in the database.
 * 
 * @author sac92
 */
public class ContentBaseDeserializer extends StdDeserializer<Content> {

	private Map<String, Class<? extends Content>> typeMap = null;
	
	public ContentBaseDeserializer() {
		super(Content.class);
	}

	public ContentBaseDeserializer(JavaType valueType) {
		super(valueType);
	}
	
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

		String contentType = root.get("type").getTextValue();

		// remove db ID as we don't really need to expose this - and it breaks the custom deserializer
		root.remove("_id");
		
		if (typeMap.containsKey(contentType))  
		{  
			contentClass = typeMap.get(contentType);  
		}  

		if (contentClass == null) return mapper.readValue(root, Content.class);  
		return mapper.readValue(root, contentClass);  
	}  
}
