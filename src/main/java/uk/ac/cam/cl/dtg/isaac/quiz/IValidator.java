/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.quiz;


import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface that allows the quiz engine to validate questions and answers.
 * 
 * Note: It is expected that the classes implementing this interface can be automatically instantiated using the default
 * constructor.
 * 
 * @author Stephen Cummins
 *
 */
public interface IValidator {
    
    /**
     * validateQuestionResponse This method is specifically for single field questions.
     * 
     * i.e. when a question expects a single answer from the user.
     * 
     * @param question
     *            - question to check against.
     * @param answer
     *            - answer from the user.
     *
     * @throws ValidatorUnavailableException
     *            - If the checking server/code is not working.
     *
     * @return a QuestionValidationResponseDTO
     */
    QuestionValidationResponse validateQuestionResponse(Question question, Choice answer)
            throws ValidatorUnavailableException;

    /**
     * Create a new list of Choice objects, sorted into correct-first order for checking.
     *
     * This is usually desired by all validators, but could be overridden if necessary for a specific case.
     *
     * @param choices - the Choices from a Question
     * @return the ordered list of Choices
     */
    default List<Choice> getOrderedChoices(final List<Choice> choices) {
        List<Choice> orderedChoices = Lists.newArrayList(choices);

        orderedChoices.sort((o1, o2) -> {
            int o1Val = o1.isCorrect() ? 0 : 1;
            int o2Val = o2.isCorrect() ? 0 : 1;
            return o1Val - o2Val;
        });

        return orderedChoices;
    }


    /**
     * Make a JSON HTTP POST request to an external validator, and provide the response JSON as a HashMap.
     *
     * @param externalValidatorUrl - the URL of an external validator to POST to.
     * @param requestBody - the JSON request body as a Map
     * @return the response JSON, as a HashMap
     * @throws IOException - on failure to communicate with the external validator
     */
    default HashMap<String, Object> getResponseFromExternalValidator(final String externalValidatorUrl,
                                                                     final Map<String, String> requestBody) throws IOException {
        // This is ridiculous. All we want to do is pass some JSON to a REST endpoint and get some JSON back.
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        JsonGenerator g = new JsonFactory().createGenerator(sw);
        mapper.writeValue(g, requestBody);
        g.close();
        String requestString = sw.toString();

        try {
            HttpClient httpClient = HttpClient.newHttpClient();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(externalValidatorUrl))
                    .timeout(Duration.ofMillis(1000))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestString))
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")  // JSON _will_ be String -> Object.
            HashMap<String, Object> response = mapper.readValue(httpResponse.body(), HashMap.class);

            return response;
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    /**
     *  Check if a feedback content object contains no meaningful feedback.
     *
     * @param feedback - content object to test
     * @return whether the content object is empty
     */
    default boolean feedbackIsNullOrEmpty(final Content feedback) {
        if (null == feedback) {
            return true;
        }
        boolean valueEmpty = null == feedback.getValue() || feedback.getValue().isEmpty();
        boolean childrenEmpty = null == feedback.getChildren() || feedback.getChildren().isEmpty();
        return valueEmpty && childrenEmpty;
    }
}
