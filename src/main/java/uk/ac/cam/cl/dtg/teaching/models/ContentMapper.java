package uk.ac.cam.cl.dtg.teaching.models;

import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class ContentMapper {
	private static HashMap<String, Class<? extends Content>> jsonTypes = new HashMap<>();
	
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

}
