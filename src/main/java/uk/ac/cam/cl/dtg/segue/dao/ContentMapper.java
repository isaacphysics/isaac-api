package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.util.HashMap;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;

import uk.ac.cam.cl.dtg.segue.dto.Choice;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.ContentBase;
import uk.ac.cam.cl.dtg.teaching.models.JsonType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

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
	
	/**
	 * This method can be used to get a specialized object mapper that is set to look for ContentBase Classes and do some magic deserialization, so that we can get
	 * a subclass at runtime.
	 * 
	 * @return ObjectMapper that has been preconfigured to be able to deserialize content from mongodb.
	 */
	public static ObjectMapper getContentObjectMapper(){
		ContentMapper.registerJsonType(Choice.class);
		ContentBaseDeserializer contentBaseDeserializer = new ContentBaseDeserializer();
		contentBaseDeserializer.registerTypeMap(jsonTypes);
		
		SimpleModule simpleModule = new SimpleModule("ContentDeserializerModule", new Version(1,0,0,null));
		simpleModule.addDeserializer(ContentBase.class, contentBaseDeserializer);
		
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(simpleModule);
		return objectMapper;
	}

}
