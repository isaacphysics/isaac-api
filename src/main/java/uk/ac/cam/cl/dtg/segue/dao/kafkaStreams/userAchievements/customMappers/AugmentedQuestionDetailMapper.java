/**
 * Copyright 2017 Dan Underwood
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

package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.customMappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.util.Lists;
import org.apache.commons.lang3.Validate;
import org.apache.kafka.streams.kstream.ValueMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuickQuestionDTO;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;

import java.util.Iterator;
import java.util.List;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;



/**
 *  JsonNode -> JsonNode ValueMapper for augmenting question part attempt details
 *  @author Dan Underwood
 */
public class AugmentedQuestionDetailMapper implements ValueMapper<JsonNode, JsonNode> {

    private static final Logger log = LoggerFactory.getLogger(AugmentedQuestionDetailMapper.class);

    private JsonNodeFactory nodeFactory;
    private ObjectMapper objectMapper;
    private IContentManager contentManager;
    private final String contentIndex;

    public AugmentedQuestionDetailMapper(final IContentManager contentManager,
                                         final String contentIndex) {

        nodeFactory = JsonNodeFactory.instance;
        objectMapper = new ObjectMapper();

        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
    }



    /**
     * Overridden ValueMapper method to map a question detail json object to an augmented json object with more info
     *
     * @param questionDetails
     *            - question detail json object
     */
    public JsonNode apply(final JsonNode questionDetails) {

        String questionId = questionDetails.path("question_id").asText();

        ArrayNode questionPartAttempts = (ArrayNode) questionDetails.path("part_attempts_correct");
        ObjectNode refinedQuestionObjects = nodeFactory.objectNode()
                .put("question_id", questionId)
                .put("event", "ignore");


        // get extra question part details
        JsonNode extraDetails = this.getExtraDetails(questionId);

        if (questionDetails.path("latest_attempt").path("new_part_attempt").asBoolean())
            refinedQuestionObjects.put("event", "new_part_attempt");

        Integer numberOfParts = extraDetails.path("question_part_count").asInt();
        if (numberOfParts == questionPartAttempts.size() &&
                questionDetails.path("latest_attempt").path("first_time_correct").asBoolean()) {

            Iterator<JsonNode> elements = questionPartAttempts.elements();
            Boolean entireQuestionComplete = true;

            while (elements.hasNext()) {

                JsonNode node = elements.next();

                // if any part is incorrect, the whole question is incorrect
                if (node.path("correct_count").asInt() < 1) {
                    entireQuestionComplete = false;
                    break;
                }
            }

            if (entireQuestionComplete)
                refinedQuestionObjects.put("event", "question_completed");
        }


        refinedQuestionObjects.put("level", extraDetails.path("level"));
        refinedQuestionObjects.put("tags", extraDetails.path("tags"));
        refinedQuestionObjects.put("latest_attempt", questionDetails.path("latest_attempt").path("timestamp"));

        return refinedQuestionObjects;
    }






    /** External call to extract additional info for an Isaac question page, returned as a json object.
     *
     * @param questionId
     *            - id of the question for which we require info
     */
    private JsonNode getExtraDetails(final String questionId) {

        ObjectNode jsonResponse = nodeFactory.objectNode()
                .put("question_id", questionId)
                .put("level", "")
                .put("question_part_count", "")
                .put("tags", "");

        Validate.notBlank(questionId);

        try {
            // go through each question in the question page
            ResultsWrapper<ContentDTO> listOfQuestions = this.contentManager.getByIdPrefix(
                    contentIndex, questionId + ID_SEPARATOR, 0, NO_SEARCH_LIMIT);

            List<QuestionDTO> questionParts = Lists.newArrayList();

            for (ContentDTO content: listOfQuestions.getResults()) {
                if (!(content instanceof QuestionDTO) || content instanceof IsaacQuickQuestionDTO) {
                    // we are not interested if this is not a question or if it is a quick question.
                    continue;
                }
                QuestionDTO question = (QuestionDTO) content;
                questionParts.add(question);
            }

            ContentDTO questionPage = contentManager.getContentById(this.contentIndex, questionId);
            SeguePageDTO questionContent = (SeguePageDTO) questionPage;


            ArrayNode tags = nodeFactory.arrayNode();
            for (String tag: questionContent.getTags())
                tags.add(tag);

            jsonResponse
                    .put("question_part_count", questionParts.size())
                    .put("type", questionContent.getType())
                    .put("level", questionContent.getLevel())
                    .put("tags", tags);


        } catch (ContentManagerException e) {
            log.debug("ContentManagerException " + e.getMessage());
        }

        return jsonResponse;

    }
}
