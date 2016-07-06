/**
 * Copyright 2016 Ian Davies
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicChemistryQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.*;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Validator that only provides functionality to validate symbolic chemistry questions.
 *
 * @author Ian Davies
 *
 */
public class IsaacSymbolicChemistryValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacSymbolicChemistryValidator.class);

    private enum MatchType {
        NONE,
        WEAK0,
        WEAK1,
        WEAK2,
        WEAK3,
        EXACT
    }

    @Override
    public QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacSymbolicChemistryQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac Symbolic Chemistry Questions... (%s is not symbolic chemistry)",
                    question.getId()));
        }
        
        if (!(answer instanceof ChemicalFormula)) {
            throw new IllegalArgumentException(String.format(
                    "Expected ChemicalFormula for IsaacSymbolicQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        IsaacSymbolicChemistryQuestion symbolicQuestion = (IsaacSymbolicChemistryQuestion) question;
        ChemicalFormula submittedFormula = (ChemicalFormula) answer;

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        MatchType responseMatchType = MatchType.NONE;   // The match type we found
        boolean responseCorrect = false;                // Whether we're right or wrong

        // STEP 0: Do we even have any answers for this question? Always do this check, because we know we
        //         won't have feedback yet.

        if (null == symbolicQuestion.getChoices() || symbolicQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            feedback = new Content("This question does not have any correct answers");
        }

        // STEP 1: Did they provide an answer?

        if (null == feedback && (null == submittedFormula.getMhchemExpression() || submittedFormula.getMhchemExpression().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        // STEP 2: Otherwise, Does their answer match a choice exactly?

        if (null == feedback) {

            // For all the choices on this question...
            for (Choice c : symbolicQuestion.getChoices()) {

                // ... that are of the ChemicalFormula type, ...
                if (!(c instanceof ChemicalFormula)) {
                    log.error("Isaac Symbolic Chemistry Validator for questionId: " + symbolicQuestion.getId()
                            + " expected there to be a ChemicalFormula. Instead it found a Choice.");
                    continue;
                }

                ChemicalFormula formulaChoice = (ChemicalFormula) c;

                // ... and that have a mhchem expression ...
                if (null == formulaChoice.getMhchemExpression() || formulaChoice.getMhchemExpression().isEmpty()) {
                    log.error("Expected python expression, but none found in choice for question id: "
                            + symbolicQuestion.getId());
                    continue;
                }

                // ... look for an exact string match to the submitted answer (lazy).
                if (formulaChoice.getMhchemExpression().equals(submittedFormula.getMhchemExpression())) {
                    feedback = (Content) formulaChoice.getExplanation();
                    responseMatchType = IsaacSymbolicChemistryValidator.MatchType.EXACT;
                    responseCorrect = formulaChoice.isCorrect();
                }
            }
        }

        // STEP 3: Otherwise, use the symbolic checker to analyse their answer

        if (null == feedback) {

            // Go through all choices, keeping track of the best match we've seen so far. A symbolic match terminates
            // this loop immediately. A numeric match may later be replaced with a symbolic match, but otherwise will suffice.

            ChemicalFormula closestMatch = null;
            HashMap<String, Object> closestResponse = null;
            IsaacSymbolicChemistryValidator.MatchType closestMatchType = IsaacSymbolicChemistryValidator.MatchType.NONE;

            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = Lists.newArrayList(symbolicQuestion.getChoices());

            Collections.sort(orderedChoices, new Comparator<Choice>() {
                @Override
                public int compare(Choice o1, Choice o2) {
                    int o1Val = o1.isCorrect() ? 0 : 1;
                    int o2Val = o2.isCorrect() ? 0 : 1;
                    return o1Val - o2Val;
                }
            });

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are of the ChemicalFormula type, ...
                if (!(c instanceof ChemicalFormula)) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                ChemicalFormula formulaChoice = (ChemicalFormula) c;

                // ... and that have a mhchem expression ...
                if (null == formulaChoice.getMhchemExpression() || formulaChoice.getMhchemExpression().isEmpty()) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                // ... test their answer against this choice with the symbolic checker.

                IsaacSymbolicChemistryValidator.MatchType matchType;
                HashMap<String, Object> response;

                try {
                    // This is ridiculous. All I want to do is pass some JSON to a REST endpoint and get some JSON back.

                    ObjectMapper mapper = new ObjectMapper();

                    HashMap<String, String> req = Maps.newHashMap();
                    req.put("target", formulaChoice.getMhchemExpression());
                    req.put("test", submittedFormula.getMhchemExpression());
                    req.put("description", symbolicQuestion.getId());

                    StringWriter sw = new StringWriter();
                    JsonGenerator g = new JsonFactory().createGenerator(sw);
                    mapper.writeValue(g, req);
                    g.close();
                    String requestString = sw.toString();

                    // TODO: Do some real checking through HTTP
                    /*HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost("http://equality-checker:5000/check");

                    httpPost.setEntity(new StringEntity(requestString));
                    httpPost.addHeader("Content-Type", "application/json");

                    HttpResponse httpResponse = httpClient.execute(httpPost);
                    HttpEntity responseEntity = httpResponse.getEntity();
                    String responseString = EntityUtils.toString(responseEntity);*/
                    response = new HashMap<>();

                    response.put("testString",       "H2+O2->H2O");
                    response.put("targetString",     "2H2+O2->2H2O");
                    response.put("test",             "H2 + O2 -> H2O");
                    response.put("target",           "2H2 + O2 -> 2H2O");
                    response.put("error",            false);
                    response.put("equal",            false);
                    response.put("typeMismatch",     false);
                    response.put("expectedType",     "equation");
                    response.put("receivedType",     "equation");
                    response.put("weaklyEquivalent", true);
                    response.put("sameCoefficient",  false);
                    response.put("sameState",        true);
                    response.put("sameArrow",        true);
                    response.put("isBalanced",       false);
                    response.put("balancedAtoms",    false);
                    response.put("balancedCharge",   true);
                    response.put("wrongTerms", "[ \"H2\", \"H2O\" ]");

                    if (response.get("error").equals(true)) {

                        // If it doesn't contain a code, it wasn't a fatal error in the checker; probably only a
                        // problem with the submitted answer.
                        log.warn("Problem checking formula \"" + submittedFormula.getMhchemExpression()
                                + "\" with symbolic chemistry checker: " + response.get("error"));
                        break;

                    } else if (response.get("equal").equals(true)) {

                        matchType = MatchType.EXACT;

                    } else if (response.get("typeMismatch").equals(true)) {

                        // Choice is not a good match: Skip to next choice.
                        continue;

                    } else if (response.get("expectedType").equals("equation")) {

                        // Response & Answer have type Equation.
                        if (response.get("isBalanced").equals(false)) {

                            if (response.get("balancedAtoms").equals(false))
                                feedback = new Content("Atoms are unbalanced in equation!");
                            else
                                feedback = new Content("Charges are unbalanced in equation!");

                            break; // user input has unbalanced equation: better stop comparing.

                        }

                        if (response.get("weaklyEquivalent").equals(false)) {

                            // current choice is not a good match.
                            continue;

                        }

                        // Measure the 'weakness' level. (0 is the weakest)
                        int counter = 0;

                        if (response.get("sameState").equals(true)) counter++;

                        if (response.get("sameCoefficient").equals(true)) counter++;

                        if (response.get("sameArrow").equals(true))
                            counter++;

                        matchType = MatchType.valueOf("WEAK" + counter);

                    } else {

                        // Response & Answer have type Expression.
                        if (response.get("weaklyEquivalent").equals(false)) {

                            // current choice is not a good match.
                            continue;

                        }

                        // Measure the 'weakness' level. (0 is the weakest)
                        int counter = 0;

                        if (response.get("sameState").equals(true)) counter++;

                        if (response.get("sameCoefficient").equals(true)) counter++;

                        matchType = MatchType.valueOf("WEAK" + counter);

                    }

                } catch (IOException e) {
                    log.error("Failed to check formula with symbolic chemistry checker. Is the server running? Not trying again.");

                    // warn the user it's not working rather than just say Incorrect
                    feedback = new Content("Server for checking chemistry equations is not working. Please try again later.");
                    break;
                }

                if (matchType == IsaacSymbolicChemistryValidator.MatchType.EXACT) {

                    // Found an exact match with one of the choices!

                    closestMatch = formulaChoice;
                    closestMatchType = IsaacSymbolicChemistryValidator.MatchType.EXACT;
                    break;

                } else if (matchType.compareTo(closestMatchType) > 0) {

                    // Found a partial match, with matchType > current closest matchType.

                    if (formulaChoice.isCorrect() || closestMatch == null) {

                        // We have no current closest match, or this choice is actually correct.
                        // Have no other choice than accepting this as closest match right now.

                        closestMatch = formulaChoice;
                        closestResponse = response;
                        closestMatchType = matchType;

                    } else {

                       // Input partially matches a wrong choice, or closestMatch is assigned already.
                       // The best thing to do here is to do nothing.

                    }
                }
            }

            // End of second choice matching

            if (null != closestMatch) {

                // We found a decent match. Of course, it still might be wrong.

                if (closestMatchType != IsaacSymbolicChemistryValidator.MatchType.EXACT) {
                    if (closestMatch.isCorrect()) {

                        // Weak match to a correct answer: Give suitable advices to user.
                        String contentString = "Your answer is close to the correct answer.\n";

                        if (closestResponse.get("sameState").equals("false"))
                            contentString += "Some term(s) have wrong state symbols.\n";

                        if (closestResponse.get("sameCoefficient").equals("false"))
                            contentString += "Some term(s) have wrong coefficients.\n";

                        if (closestResponse.get("expectedType").equals("equation") &&
                                closestResponse.get("sameArrow").equals("false"))
                            contentString += "The equation has wrong arrow.\n";

                        // Supply all wrong terms in user input.
                        contentString += "Wrong terms: " + closestResponse.get("wrongTerms");

                        responseCorrect = false;

                        feedback = new Content(contentString);

                        log.info("User submitted an answer that was close to an exact match, but not exact "
                                + "for question " + symbolicQuestion.getId() + ". Choice: "
                                + closestMatch.getMhchemExpression() + ", submitted: "
                                + submittedFormula.getMhchemExpression());
                    } else {
                        // This is weak match to a wrong answer; we can't use the feedback for the choice.
                    }
                } else {

                    // Exact match to some choice (not necessarily correct answer).
                    feedback = (Content) closestMatch.getExplanation();
                    responseCorrect = closestMatch.isCorrect();
                }

                if (closestMatchType.compareTo(IsaacSymbolicChemistryValidator.MatchType.WEAK0) >= 0 &&
                        closestMatchType.compareTo(IsaacSymbolicChemistryValidator.MatchType.WEAK3) <= 0) {

                    // Inform log about the weakly equivalent choice.
                    log.info("User submitted an answer that was close to to one of our choices "
                            + "for question " + symbolicQuestion.getId() + ". Choice: "
                            + closestMatch.getMhchemExpression() + ", submitted: "
                            + submittedFormula.getMhchemExpression());

                    /* TODO: Decide whether we want to add something to the explanation along the lines of "you got it
                           right, but only numerically. */
                }

            }
        }

        // There's no useful feedback we can give at this point.

        return new QuestionValidationResponse(symbolicQuestion.getId(), answer, responseCorrect, feedback, new Date());
    }
}
