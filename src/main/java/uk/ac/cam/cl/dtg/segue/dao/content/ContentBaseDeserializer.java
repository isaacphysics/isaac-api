/**
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dao.content;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.cam.cl.dtg.segue.dos.content.Content;

/**
 * Content deserializer will try and use the map built up in the ContentMapper class to determine what subtype of
 * content needs to be created.
 * 
 * Currently this is dependent on the register map key being the exact same text as the json 'type' property value
 * stored in the database.
 * 
 * All content objects MUST have a type property set for this to work - the default behaviour is to try and create a
 * plan content object if we are in doubt.
 * 
 */
public class ContentBaseDeserializer extends JsonDeserializer<Content> {
    private Map<String, Class<? extends Content>> typeMap = null;
    //private static final Logger log = LoggerFactory.getLogger(ContentBaseDeserializer.class);
    
    /**
     * Register the map of json types that this deserializer should be interested in.
     * 
     * @param jsonTypes
     *            - mapping a string type to a DO / DTO class.
     */
    public final void registerTypeMap(final Map<String, Class<? extends Content>> jsonTypes) {
        this.typeMap = jsonTypes;
    }
    
    @Override
    public final Content deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
            throws IOException {

        if (null == typeMap) {
            throw new IllegalStateException("No Map provided for Content Type deserialization.");
        }

        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        ObjectNode root = (ObjectNode) mapper.readTree(jsonParser);
        Class<? extends Content> contentClass;

        if (null == root.get("type")) {
            throw new JsonMappingException("Error: unable to parse content as there "
                    + "is no type property within the json input. Json Fragment: " + root.toString());
        }

        String contentType = root.get("type").textValue();
        if (typeMap.containsKey(contentType)) {
            contentClass = typeMap.get(contentType);

            return mapper.readValue(root.toString(), contentClass);
        }

        return mapper.readValue(root.toString(), Content.class);
    }
}