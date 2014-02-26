package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.mongojack.internal.MongoJackModule;

import uk.ac.cam.cl.dtg.rspp.models.JsonType;
import uk.ac.cam.cl.dtg.segue.dto.Content;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBObject;

/**
 * High level Class responsible for mapping Content objects (or contentBase objects) to their respective subclass. 
 *
 */
public class ContentMapper {
	// Used for serialization into the correct POJO as well as deserialization. Currently depends on the string key being the same text value as the type field.
	private Map<String, Class<? extends Content>> jsonTypes = new HashMap<String, Class<? extends Content>>();
	
	public void registerJsonType(Class<? extends Content> cls) {
		JsonType jt = cls.getAnnotation(JsonType.class);
		if (jt != null)
			jsonTypes.put(jt.value(), cls);
	}
	
	public ContentMapper(Map<String, Class<? extends Content>> additionalTypes) {
		jsonTypes.putAll(additionalTypes);
	}

	/**
	 * This method will accept a json string and will return a Content object (or one of its subtypes)
	 * @param json
	 * @return A Content object or one of its registered sub classes
	 * @throws JsonParseException
	 * @throws JsonMappingException
	 * @throws IOException
	 */
	public Content load(String docJson) throws JsonParseException, JsonMappingException, IOException {
		Content c = JsonLoader.load(docJson, Content.class, true);

		Class<? extends Content> cls = jsonTypes.get(c.getType());
		
		if (cls != null)
			return JsonLoader.load(docJson, cls);
		else
			return JsonLoader.load(docJson, Content.class);
	}
	
	/**
	 * Map a DBObject into the appropriate Content DTO, without having to know  what type it is.
	 * 
	 * It so happens that RestEasy will correctly serialize Content or any of its subtypes when it is provided with an object from this method (without having to do instanceof checks or anything).
	 * 
	 * @param reference to the DBObject obj
	 * @return A content object or any subclass of Content or Null if the obj param is not provided.
	 * @throws IllegalArgumentException if the database item retrieved fails to map into a content object.
	 */
	public Content mapDBOjectToContentDTO(DBObject obj) throws IllegalArgumentException {
		if(null == obj){
			return null;
		}
		
		// Create an ObjectMapper capable of deserializing mongo ObjectIDs
		ObjectMapper contentMapper = MongoJackModule.configure(new ObjectMapper());
		
		// Find out what type label the JSON object has 
		String labelledType = (String)obj.get("type");

		// Lookup the matching POJO class
		Class<? extends Content> contentClass = jsonTypes.get(labelledType); // Returns null if no entry for this type

		if (null == contentClass) {
			// We haven't registered this type. Deserialize into the Content base class.

			return contentMapper.convertValue(obj, Content.class); 
		} else {
			
			// We have a registered POJO class. Deserialize into it.
			// TODO: Work out whether we should configure the contentMapper to ignore missing fields in this case. 
			return contentMapper.convertValue(obj, contentClass);  
		}
	}
	
	/**
	 * This is a utility method that will return the HashMap that contains all possible type name to class mappings that we know about. 
	 * @return
	 */
	public Map<String, Class<? extends Content>> getJsonTypes(){
		return jsonTypes;
	}
}
