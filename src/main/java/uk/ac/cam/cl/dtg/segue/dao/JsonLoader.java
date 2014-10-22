/**
 * Copyright 2014 Ian Davies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class to help simplify deserialization of json classes.
 * 
 */
public final class JsonLoader {
	private static ObjectMapper mapper = new ObjectMapper();

	/**
	 * Prevent this class from being instantiated.
	 */
	private JsonLoader() {

	}

	/**
	 * Deserialize json into a class of a given type.
	 * 
	 * @param <T>
	 *            type to return
	 * @param json
	 *            - as a string
	 * @param c
	 *            - class to create
	 * @param ignoreUnknown
	 *            - boolean - if true it will ignore unknown properties if false
	 *            it will throw an exception.
	 * @return an instance of c
	 * @throws JsonParseException
	 *             - if we cannot read the json
	 * @throws JsonMappingException
	 *             - if we cannot map the json into the specified object.
	 * @throws IOException
	 *             - failure during IO.
	 */
	public static <T> T load(final String json, final Class<T> c, final boolean ignoreUnknown)
		throws JsonParseException, JsonMappingException, IOException {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, !ignoreUnknown);
		return mapper.readValue(json, c);
	}

	/**
	 * Deserialize json into a class of a given type.
	 * 
	 * @param <T>
	 *            type to return
	 * @param json
	 *            - as a string
	 * @param c
	 *            - class to create
	 * @return an instance of c
	 * @throws JsonParseException
	 *             - if we cannot read the json
	 * @throws JsonMappingException
	 *             - if we cannot map the json into the specified object.
	 * @throws IOException
	 *             - failure during IO.
	 */
	public static <T> T load(final String json, final Class<T> c) throws JsonParseException, JsonMappingException,
			IOException {
		return load(json, c, false);
	}
}
