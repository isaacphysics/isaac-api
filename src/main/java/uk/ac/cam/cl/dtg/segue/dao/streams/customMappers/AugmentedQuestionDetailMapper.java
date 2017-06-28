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

package uk.ac.cam.cl.dtg.isaac.kafka.customMappers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;


/**
 *  Concrete implementation of a JsonNode -> JsonNode ValueMapper for augmenting question part attempt details
 *  @author Dan Underwood
 */
public class AugmentedQuestionDetailMapper extends AbstractJsonToJsonMapper {

    private JsonNodeFactory nodeFactory;
    private ObjectMapper objectMapper;


    public AugmentedQuestionDetailMapper() {
        nodeFactory = JsonNodeFactory.instance;
        objectMapper = new ObjectMapper();
    }



    /**
     * Overridden ValueMapper method to map a question detail json object to an augmented json object with more info
     *
     * @param questionDetails
     *            - question detail json object
     */
    public JsonNode apply(JsonNode questionDetails) {

        String questionId = questionDetails.path("questionId").asText();
        Boolean questionCompletelyCorrect = true;

        ArrayNode questionPartAttempts = (ArrayNode)questionDetails.path("partAttemptsCorrect");

        // this call obtains details of the question page pertaining to the current question part attempt
        // retrieves details of tags, level, other question parts etc
        JsonNode extraDetails = this.GetExtraDetails(questionId);

        // has the user answered all question parts?
        if (extraDetails.path("questionPartCount").asLong() != questionPartAttempts.size()) {
            questionCompletelyCorrect = false;
        }
        else {

            Long attemptCount = questionPartAttempts.get(questionPartAttempts.size() - 1).path("correctAttemptCount").asLong();

            if (attemptCount != 1) {
                questionCompletelyCorrect = false;
            }
        }

        ObjectNode refinedQuestionObjects = nodeFactory.objectNode();

        refinedQuestionObjects.put("questionId", questionId);
        refinedQuestionObjects.put("level", extraDetails.path("level"));
        refinedQuestionObjects.put("tags", extraDetails.path("tags"));
        refinedQuestionObjects.put("correct", questionCompletelyCorrect);
        refinedQuestionObjects.put("dateAttempted", questionDetails.path("latestAttempt"));

        return refinedQuestionObjects;
    }




    /**
     * External call to extract additional info for an Isaac question page, returned as a json object
     *
     * @param questionId
     *            - id of the question for which we require info
     */
    private JsonNode GetExtraDetails(String questionId) {

        JsonNode jsonResponse = nodeFactory.objectNode();

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
        /*} catch (MalformedURLException e) {
            e.printStackTrace();
            return jsonResponse;*/
        } catch (IOException e) {
            //e.printStackTrace();
            return jsonResponse;
        }
    }
}
