package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonLoader {
	private static ObjectMapper mapper = new ObjectMapper();

	public static <T> T load(String json, Class<T> c, boolean ignoreUnknown)
			throws JsonParseException, JsonMappingException, IOException {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
				!ignoreUnknown);
		return mapper.readValue(json, c);
	}

	public static <T> T load(String json, Class<T> c)
			throws JsonParseException, JsonMappingException, IOException {
		return load(json, c, false);
	}
}
