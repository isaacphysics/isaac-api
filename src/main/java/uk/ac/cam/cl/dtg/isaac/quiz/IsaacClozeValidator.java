/*
 * Copyright 2021 Chris Purdy
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

import com.google.common.collect.Sets;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacClozeQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Validator that only provides functionality to validate Cloze questions.
 */
public class IsaacClozeValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacClozeValidator.class);
    private static final String NULL_CLOZE_ITEM_ID = "NULL_CLOZE_ITEM";

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacClozeQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with IsaacClozeQuestions (%s is not ClozeQuestion)", question.getId()));
        }

        if (!(answer instanceof ItemChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected ItemChoice for IsaacClozeQuestions: %s. Received (%s) ", question.getId(), answer.getClass()));
        }

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        IsaacClozeQuestion clozeQuestion = (IsaacClozeQuestion) question;
        ItemChoice submittedChoice = (ItemChoice) answer;

        // STEP 0: Is it even possible to answer this question?

        if (null == clozeQuestion.getChoices() || clozeQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());
            feedback = new Content("This question does not have any correct answers!");
        }

        if (null == clozeQuestion.getItems() || clozeQuestion.getItems().isEmpty()) {
            log.error("ItemQuestion does not have any items. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());
            feedback = new Content("This question does not have any items to choose from!");
        }

        // STEP 1: Did they provide a valid answer?

        if (null == feedback && (null == submittedChoice.getItems() || submittedChoice.getItems().isEmpty())) {
            feedback = new Content("You did not provide an answer.");
        }

        Set<String> submittedItemIdSet = null;
        Set<String> allowedItemIds;
        if (null != submittedChoice.getItems() && null != clozeQuestion.getItems()) {
            submittedItemIdSet = submittedChoice.getItems().stream().map(Item::getId).collect(Collectors.toSet());
            allowedItemIds = clozeQuestion.getItems().stream().map(Item::getId).collect(Collectors.toSet());
            allowedItemIds.add(NULL_CLOZE_ITEM_ID);
            if (!allowedItemIds.containsAll(submittedItemIdSet)) {
                feedback = new Content("You did not provide a valid answer; it contained unrecognised items");
            }
        }

        // STEP 2: If they did, does their answer match a known answer?

        if (null == feedback && null != submittedItemIdSet) {
            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(clozeQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are ItemChoices ...
                if (!(c instanceof ItemChoice)) {
                    log.error(String.format(
                            "Validator for question (%s) expected there to be an ItemChoice. Instead it found a %s.",
                            clozeQuestion.getId(), c.getClass().toString()));
                    continue;
                }

                ItemChoice itemChoice = (ItemChoice) c;

                // ... and that have items ...
                if (null == itemChoice.getItems() || itemChoice.getItems().isEmpty()) {
                    log.error("Expected list of Items, but none found in choice for question id: "
                            + clozeQuestion.getId());
                    continue;
                }

                // ... look for a match to the submitted answer.
                if (itemChoice.getItems().size() != submittedChoice.getItems().size()) {
                    continue;
                }

                List<String> submittedItemIds = new ArrayList<>();
                List<String> choiceItemIds = new ArrayList<>();

                // Run through the submitted items:
                for (Item item : submittedChoice.getItems()) {
                    submittedItemIds.add(item.getId());
                }
                // Run through the items in the question:
                for (Item item : itemChoice.getItems()) {
                    choiceItemIds.add(item.getId());
                }

                // Do not allow subset matching by default, and only allow if the cloze question does not have replacement/item duplication enabled
                boolean allowSubsetMatch = (null != itemChoice.isAllowSubsetMatch() && itemChoice.isAllowSubsetMatch()) && (null != clozeQuestion.getWithReplacement() && !clozeQuestion.getWithReplacement());

                Set<String> choiceItemIdSet = new HashSet<>(choiceItemIds);
                /* If the intersection of the submitted and choice ids is equal to the choice ones, then
                   this means that:
                    - choiceItemIdSet.size() <= submittedItemIds.size()
                    - All choice ids are within the set of submitted ids
                 */
                if (allowSubsetMatch && Sets.intersection(submittedItemIdSet, choiceItemIdSet).equals(choiceItemIdSet)) {
                    responseCorrect = itemChoice.isCorrect();
                    feedback = (Content) itemChoice.getExplanation();
                    break;
                } else if (choiceItemIds.equals(submittedItemIds)) {
                    responseCorrect = itemChoice.isCorrect();
                    feedback = (Content) itemChoice.getExplanation();
                    break;
                }
            }
        }

        // STEP 3: If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != clozeQuestion.getDefaultFeedback()) {
            feedback = clozeQuestion.getDefaultFeedback();
        }

        return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
    }

    @Override
    public List<Choice> getOrderedChoices(final List<Choice> choices) {
        return IsaacItemQuestionValidator.getOrderedChoicesWithSubsets(choices);
    }

}
