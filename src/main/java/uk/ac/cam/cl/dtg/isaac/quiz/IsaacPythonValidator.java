/*
 * Copyright 2021 Giorgio Cavicchioli
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

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacPythonQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dos.content.StringChoice;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

import java.util.Date;
import java.util.List;

/**
 * Validator that only provides functionality to validate Python questions.
 */
public class IsaacPythonValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacPythonValidator.class);
    
    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacPythonQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac String Match Questions... (%s is not string match)",
                    question.getId()));
        }

        /*if (!(answer instanceof StringChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected StringChoice for IsaacPythonQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        //StringChoice userAnswer = (StringChoice) answer;*/

        IsaacPythonQuestion PythonQuestion = (IsaacPythonQuestion) question;

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        if (null == PythonQuestion.getChoices() || PythonQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            feedback = new Content("This question does not have any correct answers");
        }

        // STEP 1: Did they provide an answer at all?

        if (null == feedback && (null == answer.getValue() || answer.getValue().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        // STEP 2: If they did, does their answer match a known answer?

        if (null == feedback) {

            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(PythonQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are of the StringChoice type, ...
                if (!(c instanceof StringChoice)) {
                    log.error("Isaac Python Validator for questionId: " + PythonQuestion.getId()
                            + " expected there to be a StringChoice. Instead it found a Choice.");
                    continue;
                }
                StringChoice stringChoice = (StringChoice) c;

                if (null == stringChoice.getValue() || stringChoice.getValue().isEmpty()) {
                    log.error("Expected a string to match, but none found in choice for question id: "
                            + PythonQuestion.getId());
                    continue;
                }

                // ... check if they match the choice, ...
                if (valuesMatch(stringChoice.getValue(), answer.getValue())) {
                    feedback = (Content) stringChoice.getExplanation();
                    responseCorrect = stringChoice.isCorrect();
                    // ... and taking exact case-sensitive matches to be the best possible and stopping if found.
                    break;
                }
            }
        }

        // STEP 3: If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != PythonQuestion.getDefaultFeedback()) {
            feedback = PythonQuestion.getDefaultFeedback();
        }

        return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
    }

    private boolean valuesMatch(String trustedValue, String userValue) {
        if (null == trustedValue || null == userValue) {
            return false;
        }

        return trustedValue.equals(userValue);
    }
}
