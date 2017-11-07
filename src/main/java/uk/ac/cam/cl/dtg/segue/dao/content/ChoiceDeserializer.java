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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;

import uk.ac.cam.cl.dtg.segue.dos.content.*;

/**
 * Choice deserializer
 * 
 * This class requires the primary content base deserializer as a constructor
 * arguement.
 * 
 * It is to allow subclasses of the choices object to be detected correctly.
 */
public class ChoiceDeserializer extends JsonDeserializer<Choice> {
    private ContentBaseDeserializer contentDeserializer;
    
    private static ObjectMapper choiceMapper;
    
    /**
     * Creates a Choice deserializer that is used by jackson to handle polymorphic types.
     * 
     * @param contentDeserializer
     *            - Instance of a contentBase deserializer needed to deserialize nested content.
     */
    public ChoiceDeserializer(final ContentBaseDeserializer contentDeserializer) {
        this.contentDeserializer = contentDeserializer;
    }

    @Override
    public Choice deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
            throws IOException {

        ObjectNode root = (ObjectNode) getSingletonChoiceMapper().readTree(jsonParser);

        if (null == root.get("type")) {
            throw new JsonMappingException(
                    "Error: unable to parse content as there is no type property within the json input.");
        }

        String contentType = root.get("type").textValue();

        switch (contentType) {
            case "quantity":
                return getSingletonChoiceMapper().readValue(root.toString(), Quantity.class);
            case "formula":
                return getSingletonChoiceMapper().readValue(root.toString(), Formula.class);
            case "chemicalFormula":
                return getSingletonChoiceMapper().readValue(root.toString(), ChemicalFormula.class);
            case "stringChoice":
                return getSingletonChoiceMapper().readValue(root.toString(), StringChoice.class);
            default:
                return getSingletonChoiceMapper().readValue(root.toString(), Choice.class);
        }
    }
    
    /**
     * This is to reduce overhead in creating object mappers.
     * @return a preconfigured object mapper.
     */
    private ObjectMapper getSingletonChoiceMapper() {
        if (null == choiceMapper) {
            SimpleModule contentDeserializerModule = new SimpleModule("ContentDeserializerModule");
            contentDeserializerModule.addDeserializer(ContentBase.class, contentDeserializer);
            
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(contentDeserializerModule);
            choiceMapper = mapper;
        }
        
        return choiceMapper;
    }
}