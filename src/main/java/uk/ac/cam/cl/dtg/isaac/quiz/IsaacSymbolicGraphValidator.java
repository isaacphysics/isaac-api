/*
 * Copyright 2016 Alistair Stead, James Sharkey, Ian Davies
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.FormulaValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicGraphQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.GraphFormula;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.io.IOException;
import java.util.*;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.FEEDBACK_NO_ANSWER_PROVIDED;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.FEEDBACK_NO_CORRECT_ANSWERS;

/**
 * Validator that provides functionality to validate symbolic graph questions.
 *
 */
public class IsaacSymbolicGraphValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacSymbolicGraphValidator.class);

    private enum MatchType {
        NONE,
        SYMBOLIC,
        EXACT
    }

    public IsaacSymbolicGraphValidator() {
    }

    @Override
    public QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer)
            throws ValidatorUnavailableException {
        Objects.requireNonNull(question);
        Objects.requireNonNull(answer);

        if (!(question instanceof IsaacSymbolicGraphQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac Symbolic Questions... (%s is not symbolic)",
                    question.getId()));
        }
        
        if (!(answer instanceof GraphFormula)) {
            throw new IllegalArgumentException(String.format(
                    "Expected GraphFormula for IsaacSymbolicGraphQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        IsaacSymbolicGraphQuestion symbolicGraphQuestion = (IsaacSymbolicGraphQuestion) question;
        GraphFormula submittedGraphFormula = (GraphFormula) answer;

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        MatchType responseMatchType = MatchType.NONE;   // The match type we found
        boolean responseCorrect = false;                // Whether we're right or wrong


        // There are several specific responses the user can receive. Each of them will set feedback content, so
        // use that to decide whether to proceed to the next check in each case.

        // STEP 0: Do we even have any answers for this question? Always do this check, because we know we
        //         won't have feedback yet.

        if (null == symbolicGraphQuestion.getChoices() || symbolicGraphQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. {} src: {}", question.getId(), question.getCanonicalSourceFile());

            feedback = new Content(FEEDBACK_NO_CORRECT_ANSWERS);
        }

        // STEP 1: Did they provide an answer?

        if (null == feedback && (null == submittedGraphFormula.getPythonExpression() || submittedGraphFormula.getPythonExpression().isEmpty())) {
            feedback = new Content(FEEDBACK_NO_ANSWER_PROVIDED);
        }

        // STEP 2: Otherwise, Does their answer match a choice exactly?

        if (null == feedback) {

            // For all the choices on this question...
            for (Choice c : symbolicGraphQuestion.getChoices()) {

                // ... that are of the GraphFormula type, ...
                if (!(c instanceof GraphFormula)) {
                    log.error("Validator for questionId: {} expected a GraphFormula. Instead it found a Choice.", symbolicGraphQuestion.getId());
                    continue;
                }

                GraphFormula graphFormulaChoice = (GraphFormula) c;

                // ... and that have a python expression ...
                if (null == graphFormulaChoice.getPythonExpression() || graphFormulaChoice.getPythonExpression().isEmpty()) {
                    log.error("Expected python expression, but none found in choice for question id: {}", symbolicGraphQuestion.getId());
                    continue;
                }

                // ... look for an exact string match to the submitted answer.
                if (graphFormulaChoice.getPythonExpression().equals(submittedGraphFormula.getPythonExpression())) {
                    feedback = (Content) graphFormulaChoice.getExplanation();
                    responseMatchType = MatchType.EXACT;
                    responseCorrect = graphFormulaChoice.isCorrect();
                }
            }
        }

        // STEP 3: Otherwise, use the symbolic checker to analyse their answer

        if (null == feedback) {

            // Go through all choices, keeping track of the best match we've seen so far. A symbolic match terminates
            // this loop immediately. A numeric match may later be replaced with a symbolic match, but otherwise will suffice.

            GraphFormula closestMatch = null;
            MatchType closestMatchType = MatchType.NONE;

            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(symbolicGraphQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are of the GraphFormula type, ...
                if (!(c instanceof GraphFormula)) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                GraphFormula graphFormulaChoice = (GraphFormula) c;

                // ... and that have a python expression ...
                if (null == graphFormulaChoice.getPythonExpression() || graphFormulaChoice.getPythonExpression().isEmpty()) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                // ... test their answer against this choice with the symbolic checker.

                // We don't do any sanitisation of user input here, we'll leave that to the python.

                MatchType matchType = MatchType.NONE;

                try {
                    String json = submittedGraphFormula.getPythonExpression();
                    ObjectMapper mapper = new ObjectMapper();
                    HashMap<String, Object> map = mapper.readValue(json, HashMap.class);

                    String jsonAnswer = graphFormulaChoice.getPythonExpression();
                    ObjectMapper mapperAnswer = new ObjectMapper();
                    HashMap<String, Object> mapAnswer = mapperAnswer.readValue(jsonAnswer, HashMap.class);

                    if (map.equals(mapAnswer)) {
                        matchType = MatchType.EXACT;
                    }

                } catch (Exception e) {
                    log.error("Failed to check formula with symbolic checker. Is the server running? Not trying again.");
                    throw new ValidatorUnavailableException("We are having problems marking graph questions."
                            + " Please try again later!");
                }

                if (matchType == MatchType.EXACT) {
                    closestMatch = graphFormulaChoice;
                    closestMatchType = MatchType.EXACT;
                    break;
                } else if (matchType == MatchType.SYMBOLIC && !graphFormulaChoice.getRequiresExactMatch()) {
                    closestMatch = graphFormulaChoice;
                    closestMatchType = MatchType.SYMBOLIC;
                } else if (matchType.compareTo(closestMatchType) > 0) {
                    if (graphFormulaChoice.getRequiresExactMatch() && graphFormulaChoice.isCorrect()) {
                        closestMatch = graphFormulaChoice;
                        closestMatchType = matchType;
                    } else {
                        if (closestMatch == null || !closestMatch.getRequiresExactMatch()) {
                            closestMatch = graphFormulaChoice;
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

                        log.debug("User submitted an answer that was close to an exact match, but not exact for question {}. Choice: {}, submitted: {}",
                                symbolicGraphQuestion.getId(), closestMatch.getPythonExpression(), submittedGraphFormula.getPythonExpression());
                    } else {
                        // This is weak match to a wrong answer; we can't use the feedback for the choice.
                    }
                } else {
                    feedback = (Content) closestMatch.getExplanation();
                    responseCorrect = closestMatch.isCorrect();
                    responseMatchType = closestMatchType;
                }

            }
        }

        // STEP 4: If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != symbolicGraphQuestion.getDefaultFeedback()) {
            feedback = symbolicGraphQuestion.getDefaultFeedback();
        }

        // If we got this far and feedback is still null, they were wrong. There's no useful feedback we can give at this point.

        return new FormulaValidationResponse(symbolicGraphQuestion.getId(), answer, feedback, responseCorrect, responseMatchType.toString(), new Date());
    }

}
