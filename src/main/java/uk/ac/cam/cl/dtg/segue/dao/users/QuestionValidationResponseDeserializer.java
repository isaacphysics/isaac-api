/*
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
package uk.ac.cam.cl.dtg.segue.dao.users;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.ac.cam.cl.dtg.isaac.dos.ItemValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.MultiPartValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.segue.dao.content.ChoiceDeserializer;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentBaseDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * QuestionValidationResponse deserializer
 * 
 * This class requires the primary content base deserializer as a constructor argument.
 *
 * It is to allow subclasses of the choices object to be detected correctly.
 */
public class QuestionValidationResponseDeserializer extends JsonDeserializer<QuestionValidationResponse> {
    private static ObjectMapper mapper;

    /**
     * Create a QuestionValidationResponse deserializer.
     * 
     * @param contentDeserializer
     *            - 
     * @param choiceDeserializer
     *            -
     */
    public QuestionValidationResponseDeserializer(final ContentBaseDeserializer contentDeserializer,
            final ChoiceDeserializer choiceDeserializer) {
        
        // only do this once as it is quite expensive.
        if (null == mapper) {
            SimpleModule contentDeserializerModule = new SimpleModule("ContentDeserializerModule");
            contentDeserializerModule.addDeserializer(ContentBase.class, contentDeserializer);
            contentDeserializerModule.addDeserializer(Choice.class, choiceDeserializer);
            
            mapper = new ObjectMapper();
            mapper.registerModule(contentDeserializerModule);
        }
    }

    @Override
    public QuestionValidationResponse deserialize(final JsonParser jsonParser,
            final DeserializationContext deserializationContext) throws IOException {

        ObjectNode root = (ObjectNode) mapper.readTree(jsonParser);

        if (null == root.get("answer")) {
            throw new JsonMappingException(
                    "Error: unable to parse content as there is no answer property within the json input.");
        }

        // Have to get the raw json out otherwise we dates do not serialize properly.
        String jsonString = new ObjectMapper().writeValueAsString(root);
        String questionResponseType = root.get("answer").get("type").textValue();
        if (questionResponseType.equals("quantity")) {
            return mapper.readValue(jsonString, QuantityValidationResponse.class);
        } else if (questionResponseType.equals("multiPartChoice")) {
            // FIXME homemade deserializer for multi-part question validation responses
            MultiPartValidationResponse mpvr = new MultiPartValidationResponse();
            mpvr.setAnswer(mapper.readValue(root.get("answer").toString(), Choice.class));
            mpvr.setExplanation(mapper.readValue(root.get("explanation").toString(), Content.class));
            mpvr.setQuestionId(root.get("questionId").textValue());
            mpvr.setCorrect(root.get("correct").booleanValue());
            mpvr.setDateAttempted(new Date(root.get("dateAttempted").longValue()));
            JsonNode jsonValidationResponses = root.get("validationResponses");
            List<QuestionValidationResponse> validationResponses = new ArrayList<>();
            if (jsonValidationResponses.isArray()) {
                for (JsonNode validationResponseNode : jsonValidationResponses) {
                    String innerResponseType = validationResponseNode.get("answer").get("type").textValue();
                    String innerJsonString = new ObjectMapper().writeValueAsString(validationResponseNode);
                    if (innerResponseType.equals("quantity")) {
                        validationResponses.add(mapper.readValue(innerJsonString, QuantityValidationResponse.class));
                    } else if (innerResponseType.equals("itemChoice")) {
                        validationResponses.add(mapper.readValue(innerJsonString, ItemValidationResponse.class));
                    } else {
                        validationResponses.add(mapper.readValue(innerJsonString, QuestionValidationResponse.class));
                    }
                }
            }
            mpvr.setValidationResponses(validationResponses);
            return mpvr;
        } else if (questionResponseType.equals("itemChoice")) {
            // We don't actually use this validation response type for all ItemChoices, but it should
            // be safe to use regardless of the "true" type because the null values will be excluded.
            return mapper.readValue(jsonString, ItemValidationResponse.class);
        } else {
            return mapper.readValue(jsonString, QuestionValidationResponse.class);
        }
    }
}