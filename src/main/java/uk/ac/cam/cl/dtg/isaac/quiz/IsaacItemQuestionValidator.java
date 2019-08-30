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

import com.google.common.collect.Lists;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacItemQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacParsonsQuestion;
import uk.ac.cam.cl.dtg.segue.dos.ItemQuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Item;
import uk.ac.cam.cl.dtg.segue.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.ParsonsChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.ParsonsItem;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
                    "This validator only works with IsaacItemQuestions (%s is not ItemQuestion)", question.getId()));
        }

        if (!(answer instanceof ItemChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected ItemChoice for IsaacItemQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }

        // Check that question type and choice type match: Don't allow child choice types:
        if (question instanceof IsaacParsonsQuestion && !(answer instanceof ParsonsChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected ParsonsChoice for IsaacParsonsQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        } else if (!(question instanceof IsaacParsonsQuestion) && answer instanceof ParsonsChoice) {
            throw new IllegalArgumentException(String.format(
                    "Expected ItemChoice for IsaacItemQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }

        // These variables store the important features of the response we'll send.
        Content feedback = null;                // The feedback we send the user
        List<String> incorrectItemIds = null;   // Additional info about incorrect items
        boolean responseCorrect = false;        // Whether we're right or wrong

        IsaacItemQuestion itemQuestion = (IsaacItemQuestion) question;
        ItemChoice submittedChoice = (ItemChoice) answer;
        boolean isParsonQuestion = question instanceof IsaacParsonsQuestion;

        // STEP 0: Is it even possible to answer this question?

        if (null == itemQuestion.getChoices() || itemQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());
            feedback = new Content("This question does not have any correct answers!");
        }

        if (null == itemQuestion.getItems() || itemQuestion.getItems().isEmpty()) {
            log.error("ItemQuestion does not have any items. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());
            feedback = new Content("This question does not have any items to choose from!");
        }

        // STEP 1: Did they provide a valid answer?

        if (null == feedback && (null == submittedChoice.getItems() || submittedChoice.getItems().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        Collection<String> submittedItemIds = null;
        Set<String> allowedItemIds;
        if (null != submittedChoice.getItems() && null != itemQuestion.getItems()) {
            // Item order doesn't matter for ItemQuestions so use set not a list:
            if (isParsonQuestion) {
                submittedItemIds = submittedChoice.getItems().stream().map(Item::getId).collect(Collectors.toList());
            } else {
                submittedItemIds = submittedChoice.getItems().stream().map(Item::getId).collect(Collectors.toSet());
            }
            allowedItemIds = itemQuestion.getItems().stream().map(Item::getId).collect(Collectors.toSet());
            if (!allowedItemIds.containsAll(submittedItemIds)) {
                feedback = new Content("You did not provide a valid answer; it contained unrecognised items");
            }
        }

        // STEP 2: If they did, does their answer match a known answer?

        if (null == feedback && null != submittedItemIds) {
            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = getOrderedChoices(itemQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are the right type of ItemChoices, ...
                if (!(c instanceof ItemChoice)) {
                    log.error(String.format(
                            "Validator for question (%s) expected there to be an ItemChoice. Instead it found a %s.",
                            itemQuestion.getId(), c.getClass().toString()));
                    continue;
                } else if (!(c instanceof ParsonsChoice) && isParsonQuestion) {
                    log.error(String.format(
                            "Validator for question (%s) expected there to be an ParsonsChoice. Instead it found a %s.",
                            itemQuestion.getId(), c.getClass().toString()));
                    continue;
                }

                ItemChoice itemChoice = (ItemChoice) c;

                // ... and that have items ...
                if (null == itemChoice.getItems() || itemChoice.getItems().isEmpty()) {
                    log.error(String.format("Missing Item list in choice for question id (%s)!", itemQuestion.getId()));
                    continue;
                }

                // ... look for a match to the submitted answer:
                if (itemChoice.getItems().size() != submittedChoice.getItems().size()) {
                    continue;
                }

                boolean itemTypeMismatch = false;
                List<Integer> submittedItemIndentations = new ArrayList<>();
                Collection<String> choiceItemIds = isParsonQuestion ? new ArrayList<>() : new HashSet<>();
                List<Integer> choiceItemIndentations = new ArrayList<>();
                for (int i = 0; i < itemChoice.getItems().size(); i++) {
                    Item choiceItem = itemChoice.getItems().get(i);
                    Item submittedItem = submittedChoice.getItems().get(i);

                    choiceItemIds.add(choiceItem.getId());
                    if (isParsonQuestion) {
                        if (!(submittedItem instanceof ParsonsItem)) {
                            throw new IllegalArgumentException("Expected ParsonsChoice to contain ParsonsItems!");
                        }
                        if (!(choiceItem instanceof ParsonsItem)) {
                            log.error("ParsonsChoice contained non-ParsonsItem. Skipping!");
                            itemTypeMismatch = true;
                            break;
                        }

                        Integer submittedItemIndentation = ((ParsonsItem) submittedItem).getIndentation();
                        Integer choiceItemIndentation = ((ParsonsItem) choiceItem).getIndentation();
                        submittedItemIndentations.add(submittedItemIndentation);
                        choiceItemIndentations.add(choiceItemIndentation);

                        if (itemChoice.isCorrect() && null == incorrectItemIds) {
                            if (!choiceItem.getId().equals(submittedItem.getId()) || !choiceItemIndentation.equals(submittedItemIndentation)) {
                                incorrectItemIds = Lists.newArrayList(submittedItem.getId());
                            }
                        }
                    }
                }

                if (itemTypeMismatch) {
                    // A problem with the choice itself. Maybe another one will be a better match?
                    continue;
                }

                if (choiceItemIds.equals(submittedItemIds) && choiceItemIndentations.equals(submittedItemIndentations)) {
                    responseCorrect = c.isCorrect();
                    incorrectItemIds = new ArrayList<>();
                    feedback = (Content) c.getExplanation();
                    break;
                }
            }
        }

        return new ItemQuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, incorrectItemIds, new Date());
    }

}
