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
import uk.ac.cam.cl.dtg.isaac.dos.IsaacItemQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Item;
import uk.ac.cam.cl.dtg.segue.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator that only provides functionality to validate Item questions.
 */
public class IsaacItemQuestionValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacItemQuestionValidator.class);

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacItemQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with IsaacItemQuestions... (%s is not ItemQuestion)", question.getId()));
        }

        if (!(answer instanceof ItemChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected ItemChoice for IsaacItemQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        IsaacItemQuestion itemQuestion = (IsaacItemQuestion) question;
        ItemChoice submittedChoice = (ItemChoice) answer;

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        if (null == itemQuestion.getChoices() || itemQuestion.getChoices().isEmpty()) {
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
            for (Choice c : itemQuestion.getChoices()) {

                // ... that are ItemChoices, ...
                if (!(c instanceof ItemChoice)) {
                    log.error("Validator for questionId: " + itemQuestion.getId()
                            + " expected there to be an ItemChoice. Instead it found a Choice.");
                    continue;
                }

                ItemChoice itemChoice = (ItemChoice) c;

                // ... and that have items ...
                if (null == itemChoice.getItems() || itemChoice.getItems().isEmpty()) {
                    log.error("Missing Item list in choice for question id: " + itemQuestion.getId());
                    continue;
                }

                // ... look for a match to the submitted answer.
                if (itemChoice.getItems().size() != submittedChoice.getItems().size()) {
                    continue;
                }
                Set<String> choiceItemIds = itemChoice.getItems().stream().map(Item::getId).collect(Collectors.toSet());
                Set<String> submittedItemIds = submittedChoice.getItems().stream().map(Item::getId).collect(Collectors.toSet());

                if (choiceItemIds.equals(submittedItemIds)) {
                    responseCorrect = itemChoice.isCorrect();
                    feedback = (Content) itemChoice.getExplanation();
                    break;
                }
            }
        }

        return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
    }

}
