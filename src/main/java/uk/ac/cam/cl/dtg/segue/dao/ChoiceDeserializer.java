package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.cam.cl.dtg.segue.dto.content.Choice;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dto.content.Quantity;

/**
 * Choice deserializer will try and use the map built up in the ContentMapper class to determine what subtype of content needs to be created.
 * 
 * Currently this is dependent on the register map key being the exact same text as the json 'type' property value stored in the database.
 * 
 * All content objects MUST have a type property set for this to work - the default behaviour is to try and create a plan content object if we are in doubt.
 * 
 */
public class ChoiceDeserializer extends JsonDeserializer<Choice> {
	private ContentBaseDeserializer contentDeserializer;
	
	public ChoiceDeserializer(ContentBaseDeserializer contentDeserializer){
		this.contentDeserializer = contentDeserializer;
	}
	
	@Override
	public Choice deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
			throws IOException, JsonProcessingException, JsonMappingException{
	    
		
	    SimpleModule contentDeserializerModule = new SimpleModule("ContentDeserializerModule");
	    contentDeserializerModule.addDeserializer(ContentBase.class, contentDeserializer);
	    
	    ObjectMapper mapper = new ObjectMapper();
	    mapper.registerModule(contentDeserializerModule);
		
		ObjectNode root = (ObjectNode) mapper.readTree(jsonParser);  

		if(null == root.get("type"))
			throw new JsonMappingException("Error: unable to parse content as there is no type property within the json input.");
		
		String contentType = root.get("type").textValue();

		if(contentType.equals("quantity")){
			return (Quantity) mapper.readValue(root.toString(), Quantity.class);
		}
		else
		{
			return (Choice) mapper.readValue(root.toString(), Choice.class);
		}
	}
}