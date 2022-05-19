/*
 * Copyright 2022 Chris Purdy
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
import uk.ac.cam.cl.dtg.isaac.dos.IsaacReorderQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validator that provides functionality to validate reorder questions. It is essentially a copy of the cloze question
 * validator.
 */
public class IsaacReorderValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacReorderValidator.class);

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacReorderQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with IsaacReorderQuestion (%s is not ReorderQuestion)", question.getId()));
        }

        if (!(answer instanceof ItemChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected ItemChoice for IsaacReorderQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        IsaacReorderQuestion reorderQuestion = (IsaacReorderQuestion) question;
        ItemChoice submittedChoice = (ItemChoice) answer;

        // STEP 0: Is it even possible to answer this question?

        if (null == reorderQuestion.getChoices() || reorderQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());
            feedback = new Content("This question does not have any correct answers");
        }

        if (null == reorderQuestion.getItems() || reorderQuestion.getItems().isEmpty()) {
            log.error("ReorderQuestion does not have any items. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());
            feedback = new Content("This question does not have any items to choose from!");
        }

        // STEP 1: Did they provide a valid answer?

        if (null == feedback && (null == submittedChoice.getItems() || submittedChoice.getItems().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        Set<String> submittedItemIdSet = null;
        Set<String> allowedItemIds;
        if (null != submittedChoice.getItems() && null != reorderQuestion.getItems()) {
            submittedItemIdSet = submittedChoice.getItems().stream().map(Item::getId).collect(Collectors.toSet());
            allowedItemIds = reorderQuestion.getItems().stream().map(Item::getId).collect(Collectors.toSet());
            if (!allowedItemIds.containsAll(submittedItemIdSet)) {
                feedback = new Content("You did not provide a valid answer; it contained unrecognised items");
            }
        }

        // STEP 2: If they did, does their answer match a known answer?

        if (null == feedback && null != submittedItemIdSet) {
            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(reorderQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are ItemChoices ...
                if (!(c instanceof ItemChoice)) {
                    log.error(String.format(
                            "Validator for question (%s) expected there to be an ItemChoice. Instead it found a %s.",
                            reorderQuestion.getId(), c.getClass().toString()));
                    continue;
                }

                ItemChoice itemChoice = (ItemChoice) c;

                // ... and that have items ...
                if (null == itemChoice.getItems() || itemChoice.getItems().isEmpty()) {
                    log.error("Expected list of Items, but none found in choice for question id: "
                            + reorderQuestion.getId());
                    continue;
                }

                // ... look for a match to the submitted answer.

                // Skip choices with different numbers of items, they definitely won't match
                if (itemChoice.getItems().size() != submittedChoice.getItems().size()) {
                    continue;
                }

                List<String> submittedItemIds = submittedChoice.getItems().stream().map(ContentBase::getId).collect(Collectors.toList());
                List<String> choiceItemIds = itemChoice.getItems().stream().map(ContentBase::getId).collect(Collectors.toList());

                if (choiceItemIds.equals(submittedItemIds)) {
                    responseCorrect = itemChoice.isCorrect();
                    feedback = (Content) itemChoice.getExplanation();
                    break;
                }
            }
        }

        // STEP 3: If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != reorderQuestion.getDefaultFeedback()) {
            feedback = reorderQuestion.getDefaultFeedback();
        }

        return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
    }

    @Override
    public List<Choice> getOrderedChoices(final List<Choice> choices) {
        return IsaacItemQuestionValidator.getOrderedChoicesWithSubsets(choices);
    }

}
