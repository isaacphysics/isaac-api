/*
 * Copyright 2016 Alistair Stead, James Sharkey, Ian Davies
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
package uk.ac.cam.cl.dtg.isaac.quiz;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.segue.dos.FormulaValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Formula;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatorUnavailableException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Validator that provides functionality to validate symbolic questions.
 *
 */
public class IsaacSymbolicValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacSymbolicValidator.class);

    private enum MatchType {
        NONE,
        NUMERIC,
        SYMBOLIC,
        EXACT
    }

    private final String hostname;
    private final String port;
    private final String externalValidatorUrl;

    public IsaacSymbolicValidator(final String hostname, final String port) {
        this.hostname = hostname;
        this.port = port;
        this.externalValidatorUrl = "http://" + this.hostname + ":" + this.port + "/check";
    }

    @Override
    public QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer)
            throws ValidatorUnavailableException {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacSymbolicQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac Symbolic Questions... (%s is not symbolic)",
                    question.getId()));
        }
        
        if (!(answer instanceof Formula)) {
            throw new IllegalArgumentException(String.format(
                    "Expected Formula for IsaacSymbolicQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        IsaacSymbolicQuestion symbolicQuestion = (IsaacSymbolicQuestion) question;
        Formula submittedFormula = (Formula) answer;

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        MatchType responseMatchType = MatchType.NONE;   // The match type we found
        boolean responseCorrect = false;                // Whether we're right or wrong


        // There are several specific responses the user can receive. Each of them will set feedback content, so
        // use that to decide whether to proceed to the next check in each case.

        // STEP 0: Do we even have any answers for this question? Always do this check, because we know we
        //         won't have feedback yet.

        if (null == symbolicQuestion.getChoices() || symbolicQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            feedback = new Content("This question does not have any correct answers");
        }

        // STEP 1: Did they provide an answer?

        if (null == feedback && (null == submittedFormula.getPythonExpression() || submittedFormula.getPythonExpression().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        // STEP 2: Otherwise, Does their answer match a choice exactly?

        if (null == feedback) {

            // For all the choices on this question...
            for (Choice c : symbolicQuestion.getChoices()) {

                // ... that are of the Formula type, ...
                if (!(c instanceof Formula)) {
                    log.error("Validator for questionId: " + symbolicQuestion.getId()
                            + " expected there to be a Formula. Instead it found a Choice.");
                    continue;
                }

                Formula formulaChoice = (Formula) c;

                // ... and that have a python expression ...
                if (null == formulaChoice.getPythonExpression() || formulaChoice.getPythonExpression().isEmpty()) {
                    log.error("Expected python expression, but none found in choice for question id: "
                            + symbolicQuestion.getId());
                    continue;
                }

                // ... look for an exact string match to the submitted answer.
                if (formulaChoice.getPythonExpression().equals(submittedFormula.getPythonExpression())) {
                    feedback = (Content) formulaChoice.getExplanation();
                    responseMatchType = MatchType.EXACT;
                    responseCorrect = formulaChoice.isCorrect();
                }
            }
        }

        // STEP 3: Otherwise, use the symbolic checker to analyse their answer

        if (null == feedback) {

            // Go through all choices, keeping track of the best match we've seen so far. A symbolic match terminates
            // this loop immediately. A numeric match may later be replaced with a symbolic match, but otherwise will suffice.

            Formula closestMatch = null;
            MatchType closestMatchType = MatchType.NONE;

            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(symbolicQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are of the Formula type, ...
                if (!(c instanceof Formula)) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                Formula formulaChoice = (Formula) c;

                // ... and that have a python expression ...
                if (null == formulaChoice.getPythonExpression() || formulaChoice.getPythonExpression().isEmpty()) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                // ... test their answer against this choice with the symbolic checker.

                // We don't do any sanitisation of user input here, we'll leave that to the python.

                MatchType matchType = MatchType.NONE;

                try {
                    HashMap<String, String> req = Maps.newHashMap();
                    req.put("target", formulaChoice.getPythonExpression());
                    req.put("test", submittedFormula.getPythonExpression());
                    req.put("description", symbolicQuestion.getId());
                    if (symbolicQuestion.getAvailableSymbols() != null) {
                        req.put("symbols", String.join(",", symbolicQuestion.getAvailableSymbols()));
                    }

                    HashMap<String, Object> response = getResponseFromExternalValidator(req);

                    if (response.containsKey("error")) {
                        if (response.containsKey("code")) {
                            log.error("Failed to check formula \"" + submittedFormula.getPythonExpression()
                                    + "\" against \"" + formulaChoice.getPythonExpression() + "\": " + response.get("error"));
                        } else if (response.containsKey("syntax_error")) {
                            // There's a syntax error in the "test" expression, no use checking it further:
                            closestMatch = null;
                            feedback = new Content("Your answer does not seem to be valid maths.<br>"
                                        + "Check for things like mismatched brackets or misplaced symbols.");
                            feedback.setTags(new HashSet<>(Collections.singletonList("syntax_error")));
                            responseCorrect = false;
                            break;
                        } else {
                            log.warn("Problem checking formula \"" + submittedFormula.getPythonExpression()
                                    + "\" for (" + symbolicQuestion.getId() + ") with symbolic checker: " + response.get("error"));
                        }
                    } else {
                        if (response.get("equal").equals("true")) {
                            matchType = MatchType.valueOf(((String) response.get("equality_type")).toUpperCase());
                        }
                    }

                } catch (IOException e) {
                    log.error("Failed to check formula with symbolic checker. Is the server running? Not trying again.");
                    throw new ValidatorUnavailableException("We are having problems marking Symbolic Questions."
                            + " Please try again later!");
                }

                if (matchType == MatchType.EXACT) {
                    closestMatch = formulaChoice;
                    closestMatchType = MatchType.EXACT;
                    break;
                } else if (matchType.compareTo(closestMatchType) > 0) {
                    if (formulaChoice.getRequiresExactMatch() && formulaChoice.isCorrect()) {
                        closestMatch = formulaChoice;
                        closestMatchType = matchType;
                    } else {
                        if (closestMatch == null || !closestMatch.getRequiresExactMatch()) {
                            closestMatch = formulaChoice;
                            closestMatchType = matchType;
                        } else {
                            // This is not as good a match as the one we already have.
                        }
                    }
                }
            }

            if (null != closestMatch) {
                // We found a decent match. Of course, it still might be wrong.

                if (closestMatchType != MatchType.EXACT && closestMatch.getRequiresExactMatch()) {
                    if (closestMatch.isCorrect()) {
                        feedback = new Content("Your answer is not in the form we expected. Can you rearrange or simplify it?");
                        feedback.setTags(new HashSet<>(Collections.singletonList("required_exact")));
                        responseCorrect = false;
                        responseMatchType = closestMatchType;

                        log.info("User submitted an answer that was close to an exact match, but not exact "
                                + "for question " + symbolicQuestion.getId() + ". Choice: "
                                + closestMatch.getPythonExpression() + ", submitted: "
                                + submittedFormula.getPythonExpression());
                    } else {
                        // This is weak match to a wrong answer; we can't use the feedback for the choice.
                    }
                } else {
                    feedback = (Content) closestMatch.getExplanation();
                    responseCorrect = closestMatch.isCorrect();
                    responseMatchType = closestMatchType;
                }

                if (closestMatchType == MatchType.NUMERIC) {
                    log.info("User submitted an answer that was only numerically equivalent to one of our choices "
                            + "for question " + symbolicQuestion.getId() + ". Choice: "
                            + closestMatch.getPythonExpression() + ", submitted: "
                            + submittedFormula.getPythonExpression());
                }

            }
        }

        // If we got this far and feedback is still null, they were wrong. There's no useful feedback we can give at this point.

        return new FormulaValidationResponse(symbolicQuestion.getId(), answer, feedback, responseCorrect, responseMatchType.toString(), new Date());
    }

    /**
     * Create a new list of the Choice objects, sorted into correct-first order for checking.
     *
     * @param choices - the Choices from a Question
     * @return the ordered list of Choices
     */
    private List<Choice> getOrderedChoices(final List<Choice> choices) {
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
     * @param requestBody - the JSON request body as a Map
     * @return the response JSON, as a HashMap
     * @throws IOException - on failure to communicate with the external validator
     */
    private HashMap<String, Object> getResponseFromExternalValidator(final Map<String, String> requestBody) throws IOException {
        // This is ridiculous. All we want to do is pass some JSON to a REST endpoint and get some JSON back.
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        JsonGenerator g = new JsonFactory().createGenerator(sw);
        mapper.writeValue(g, requestBody);
        g.close();
        String requestString = sw.toString();

        HttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(externalValidatorUrl);

        httpPost.setEntity(new StringEntity(requestString, "UTF-8"));
        httpPost.addHeader("Content-Type", "application/json");

        HttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity responseEntity = httpResponse.getEntity();
        String responseString = EntityUtils.toString(responseEntity);
        HashMap<String, Object> response = mapper.readValue(responseString, HashMap.class);

        return response;
    }
}
