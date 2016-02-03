/**
 * Copyright 2016 Alistair Stead
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

import java.io.*;
import java.util.*;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
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

/**
 * Validator that only provides functionality to validate symbolic questions.
 *
 * @author Alistair Stead
 *
 */
public class IsaacSymbolicValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacSymbolicValidator.class);

    @Override
    public QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
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
        Content feedback = null;          // The feedback we send the user
        boolean symbolicCorrect = false;  // Whether their answer was symbolically equivalent to one of ours
        boolean numericCorrect = false;   // Whether their answer was numerically equivalent to one of ours


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
                    log.error("Isaac Symbolic Validator for questionId: " + symbolicQuestion.getId()
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

                // ... look for an exact match to the submitted answer.
                if (formulaChoice.getPythonExpression().equals(submittedFormula.getPythonExpression())) {
                    feedback = (Content)formulaChoice.getExplanation();
                    symbolicCorrect = formulaChoice.isCorrect();
                    numericCorrect = formulaChoice.isCorrect();
                }
            }
        }

        // STEP 3: Otherwise, use the symbolic checker to analyse their answer

        if (null == feedback) {

            // Go through all choices, keeping track of the best match we've seen so far. A symbolic match terminates
            // this loop immediately. A numeric match may later be replaced with a symbolic match, but otherwise will suffice.

            Formula closestMatch = null;
            boolean closestMatchSymbolicCorrect = false;

            // For all the choices on this question...
            for (Choice c : symbolicQuestion.getChoices()) {

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

                boolean symbolicMatch = false;
                boolean numericMatch = false;

                try {
                    // This is ridiculous. All I want to do is pass some JSON to a REST endpoint and get some JSON back.

                    ObjectMapper mapper = new ObjectMapper();

                    HashMap<String, String> req = Maps.newHashMap();
                    req.put("target", formulaChoice.getPythonExpression());
                    req.put("test", submittedFormula.getPythonExpression());

                    StringWriter sw = new StringWriter();
                    JsonGenerator g = new JsonFactory().createGenerator(sw);
                    mapper.writeValue(g, req);
                    g.close();
                    String requestString = sw.toString();

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost("http://localhost:5000/check");

                    httpPost.setEntity(new StringEntity(requestString));
                    httpPost.addHeader("Content-Type", "application/json");

                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    HttpEntity responseEntity = httpResponse.getEntity();
                    String responseString = EntityUtils.toString(responseEntity);
                    HashMap<String, Object> response = mapper.readValue(responseString, HashMap.class);

                    if (response.containsKey("error")) {
                        log.error("Failed to check formula with symbolic checker: " + response.get("error"));
                    } else {
                        if ((boolean)response.get("equal")) {
                            symbolicMatch =  response.get("equality_type") == "symbolic";
                            numericMatch = response.get("equality_type") == "numeric";
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    log.error("Failed to check formula with symbolic checker.", e);
                }


                if (symbolicMatch) {
                    // This is the best kind of match. No need to continue checking.
                    closestMatch = formulaChoice;
                    closestMatchSymbolicCorrect = true;
                    break;
                } else if (numericMatch && null == closestMatch) {
                    // This is an acceptable match, but we may yet find a better one. Continue checking.
                    closestMatch = formulaChoice;
                }
            }

            if (null != closestMatch) {
                // We found a decent match.

                feedback = (Content) closestMatch.getExplanation();
                symbolicCorrect = closestMatchSymbolicCorrect;
                numericCorrect = true;

                if (!symbolicCorrect) {
                    log.info("User submitted an answer that was only numerically equivalent to one of our choices "
                            + "for question " + symbolicQuestion.getId() + ". Choice: "
                            + closestMatch.getPythonExpression() + ", submitted: "
                            + submittedFormula.getPythonExpression());

                    // TODO: Decide whether we want to add something to the explanation along the lines of "you got it
                    //       right, but only numerically.
                }

            }
        }

        // If we got this far and feedback is still null, they were wrong. There's no useful feedback we can give at this point.

        return new FormulaValidationResponse(symbolicQuestion.getId(), answer, feedback, symbolicCorrect, numericCorrect, new Date());
    }
}
