package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.util.HashMap;

import org.mongojack.internal.MongoJackModule;

import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.teaching.models.JsonType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DBObject;

public class ContentMapper {
	// Used for serialization into the correct POJO as well as deserialization. Currently depends on the string key being the same text value as the type field.
	private static HashMap<String, Class<? extends Content>> jsonTypes = new HashMap<String, Class<? extends Content>>();
	
	public static void registerJsonType(Class<? extends Content> cls) {
		JsonType jt = cls.getAnnotation(JsonType.class);
		if (jt != null)
			jsonTypes.put(jt.value(), cls);
	}
	
	public static Content load(String docJson) throws JsonParseException, JsonMappingException, IOException {
		Content c = JsonLoader.load(docJson, Content.class, true);

		Class<? extends Content> cls = jsonTypes.get(c.getType());
		
		if (cls != null)
			return JsonLoader.load(docJson, cls);
		else
			return JsonLoader.load(docJson, Content.class);
	}
	
	
	
	@SuppressWarnings("unchecked")
	public static <T extends Content> T contentFromDb(DBObject obj, Class<T> type) {
		
		// Create an ObjectMapper capable of deserializing mongo ObjectIDs
		ObjectMapper contentMapper = MongoJackModule.configure(new ObjectMapper());
		
		// Find out what type label the JSON object has 
		String labelledType = (String)obj.get("type");

		// Lookup the matching POJO class
		Class<? extends Content> contentClass = jsonTypes.get(labelledType); // Returns null if no entry for this type

		if (contentClass == null) {
			// We have a registered POJO class. Deserialize into it.
			return (T) contentMapper.convertValue(obj, Content.class); 
		} else {
			// We haven't registered this type. Deserialize into the Content base class.
			
			// TODO: Work out whether we should configure the contentMapper to ignore missing fields in this case. 
			
			return (T) contentMapper.convertValue(obj, contentClass);  
		}


	}
	

}
