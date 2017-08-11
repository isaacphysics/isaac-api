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

package uk.ac.cam.cl.dtg.segue.dao.streams.customMappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.util.Lists;
import org.apache.commons.lang3.Validate;
import org.apache.kafka.streams.kstream.ValueMapper;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuickQuestionDTO;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;



/**
 *  JsonNode -> JsonNode ValueMapper for augmenting question part attempt details
 *  @author Dan Underwood
 */
public class AugmentedQuestionDetailMapper implements ValueMapper<JsonNode, JsonNode> {

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
        Boolean questionCompletelyCorrect = true;

        ArrayNode questionPartAttempts = (ArrayNode) questionDetails.path("part_attempts_correct");
        ObjectNode refinedQuestionObjects = nodeFactory.objectNode();
        try {

            // this call obtains details of the question page pertaining to the current question part attempt
            // retrieves details of tags, level, other question parts etc
            JsonNode extraDetails = this.getExtraDetails(questionId);

            // has the user answered all question parts?
            if (extraDetails.path("question_part_count").asLong() != questionPartAttempts.size()) {
                questionCompletelyCorrect = false;

            } else {

                Long attemptCount = questionPartAttempts.get(questionPartAttempts.size() - 1).path("correct_attempt_count").asLong();

                if (attemptCount != 1) {
                    questionCompletelyCorrect = false;
                }
            }

            refinedQuestionObjects.put("question_id", questionId);
            refinedQuestionObjects.put("level", extraDetails.path("level"));
            refinedQuestionObjects.put("tags", extraDetails.path("tags"));
            refinedQuestionObjects.put("correct", questionCompletelyCorrect);
            refinedQuestionObjects.put("date_attempted", questionDetails.path("latest_attempt"));

            return refinedQuestionObjects;

        } catch (ContentManagerException e) {
            refinedQuestionObjects.put("question_id", questionId);
            refinedQuestionObjects.put("level", "invalid");
            refinedQuestionObjects.put("tags", "invalid");
            refinedQuestionObjects.put("correct", "invalid");
            refinedQuestionObjects.put("date_attempted", "invalid");

            return refinedQuestionObjects;
        }
    }




    /** External call to extract additional info for an Isaac question page, returned as a json object.
     *
     * @param questionId
     *            - id of the question for which we require info
     */
    private JsonNode getExtraDetails(final String questionId) throws ContentManagerException {

        ObjectNode jsonResponse = nodeFactory.objectNode();

        Validate.notBlank(questionId);

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

        jsonResponse.put("question_id", questionId)
                .put("level", questionContent.getLevel())
                .put("question_part_count", questionParts.size())
                .put("tags", tags);

        return jsonResponse;

        /*JsonNode jsonResponse = nodeFactory.objectNode();

        try {

            // Isaac api call to question_detail endpoint
            URL url = new URL("http://localhost:8080/isaac-api/api/pages/question_details/" + questionId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()
            ));

            String strResponse;
            while ((strResponse = br.readLine()) != null) {
                jsonResponse = objectMapper.readTree(strResponse);
            }

            connection.disconnect();
            return jsonResponse;

        } catch (FileNotFoundException e) {
            //e.printStackTrace();
            return jsonResponse;
        } catch (IOException e) {
            //e.printStackTrace();
            return jsonResponse;
        }*/
    }
}
