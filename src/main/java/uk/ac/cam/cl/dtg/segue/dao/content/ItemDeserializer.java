/*
 * Copyright 2019 James Sharkey
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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.ac.cam.cl.dtg.segue.dos.content.ClozeItem;
import uk.ac.cam.cl.dtg.segue.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dos.content.Item;
import uk.ac.cam.cl.dtg.segue.dos.content.ParsonsItem;

import java.io.IOException;

/**
 * Item deserializer
 *
 * This class requires the primary content base deserializer as a constructor argument.
 *
 * It is to allow subclasses of the Item object to be detected correctly.
 */
public class ItemDeserializer extends JsonDeserializer<Item> {
    private ContentBaseDeserializer contentDeserializer;

    private static ObjectMapper itemMapper;

    /**
     * Creates an Item deserializer that is used by jackson to handle polymorphic types.
     *
     * @param contentDeserializer
     *            - Instance of a contentBase deserializer needed to deserialize nested content.
     */
    public ItemDeserializer(final ContentBaseDeserializer contentDeserializer) {
        this.contentDeserializer = contentDeserializer;
    }

    @Override
    public Item deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
            throws IOException {

        ObjectNode root = getSingletonItemMapper().readTree(jsonParser);

        if (null == root.get("type")) {
            throw new JsonMappingException("Error: JSON missing 'type' property!");
        }

        String contentType = root.get("type").textValue();

        switch (contentType) {
            case "parsonsItem":
                return getSingletonItemMapper().readValue(root.toString(), ParsonsItem.class);
            case "clozeItem":
                return getSingletonItemMapper().readValue(root.toString(), ClozeItem.class);
            default:
                return getSingletonItemMapper().readValue(root.toString(), Item.class);
        }
    }

    /**
     * This is to reduce overhead in creating object mappers.
     * @return a preconfigured object mapper.
     */
    private ObjectMapper getSingletonItemMapper() {
        if (null == itemMapper) {
            SimpleModule contentDeserializerModule = new SimpleModule("ContentDeserializerModule");
            contentDeserializerModule.addDeserializer(ContentBase.class, contentDeserializer);

            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(contentDeserializerModule);
            itemMapper = mapper;
        }

        return itemMapper;
    }
}