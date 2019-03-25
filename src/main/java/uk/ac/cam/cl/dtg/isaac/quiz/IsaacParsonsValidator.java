/*
 * Copyright 2019 James Sharkey
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
import uk.ac.cam.cl.dtg.isaac.dos.IsaacParsonsQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.ParsonsChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.ParsonsItem;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validator that only provides functionality to validate Parsons questions.
 */
public class IsaacParsonsValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacParsonsValidator.class);

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacParsonsQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac Parsons Questions... (%s is not Parsons)",
                    question.getId()));
        }

        if (!(answer instanceof ParsonsChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected ParsonsChoice for IsaacParsonsQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        IsaacParsonsQuestion parsonsQuestion = (IsaacParsonsQuestion) question;
        ParsonsChoice submittedChoice = (ParsonsChoice) answer;

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        if (null == parsonsQuestion.getChoices() || parsonsQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            feedback = new Content("This question does not have any correct answers");
        }

        // STEP 1: Did they provide an answer at all?

        if (null == feedback && (null == submittedChoice.getItems() || submittedChoice.getItems().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        // STEP 2: If they did, does their answer match a known answer?

        if (null == feedback) {
            // For all the choices on this question...
            for (Choice c : parsonsQuestion.getChoices()) {

                // ... that are of the Formula type, ...
                if (!(c instanceof ParsonsChoice)) {
                    log.error("Validator for questionId: " + parsonsQuestion.getId()
                            + " expected there to be a Formula. Instead it found a Choice.");
                    continue;
                }

                ParsonsChoice parsonsChoice = (ParsonsChoice) c;

                // ... and that have a python expression ...
                if (null == parsonsChoice.getItems() || parsonsChoice.getItems().isEmpty()) {
                    log.error("Expected list of ParsonsItems, but none found in choice for question id: "
                            + parsonsQuestion.getId());
                    continue;
                }

                // ... look for a match to the submitted answer.
                if (parsonsChoice.getItems().size() != submittedChoice.getItems().size()) {
                    continue;
                }
                List<String> choiceItemIds = parsonsChoice.getItems().stream().map(ParsonsItem::getId).collect(Collectors.toList());
                List<String> submittedItemIds = submittedChoice.getItems().stream().map(ParsonsItem::getId).collect(Collectors.toList());

                List<Integer> choiceItemIndentations = parsonsChoice.getItems().stream().map(ParsonsItem::getIndentation).collect(Collectors.toList());
                List<Integer> submittedItemIndentations = submittedChoice.getItems().stream().map(ParsonsItem::getIndentation).collect(Collectors.toList());

                if (choiceItemIds.equals(submittedItemIds) && choiceItemIndentations.equals(submittedItemIndentations)) {
                    responseCorrect = parsonsChoice.isCorrect();
                    feedback = (Content) parsonsChoice.getExplanation();
                    break;
                }
            }
        }

        return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
    }

}
