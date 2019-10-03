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
import uk.ac.cam.cl.dtg.segue.dos.ItemQuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Item;
import uk.ac.cam.cl.dtg.segue.dos.content.ItemChoice;
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

    protected void checkQuestionAndAnswerTypes(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question.getClass() == IsaacItemQuestion.class)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with IsaacItemQuestions (%s is not ItemQuestion)", question.getId()));
        }

        if (!(answer.getClass() == ItemChoice.class)) {
            throw new IllegalArgumentException(String.format(
                    "Expected ItemChoice for IsaacItemQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }
    }

    private ItemQuestionValidationResponse checkQuestionIsAnswerable(
            IsaacItemQuestion itemQuestion, ItemChoice submittedChoice) {

        ItemQuestionValidationResponse defaultFailedResponse =
                ItemQuestionValidationResponse.createDefaultFailedResponse(itemQuestion.getId(), submittedChoice);

        if (null == itemQuestion.getChoices() || itemQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + itemQuestion.getId() + " src: "
                    + itemQuestion.getCanonicalSourceFile());
            defaultFailedResponse.setExplanation(new Content("This question does not have any correct answers!"));
            return defaultFailedResponse;
        }

        if (null == itemQuestion.getItems() || itemQuestion.getItems().isEmpty()) {
            log.error("ItemQuestion does not have any items. " + itemQuestion.getId() + " src: "
                    + itemQuestion.getCanonicalSourceFile());
            defaultFailedResponse.setExplanation(new Content("This question does not have any items to choose from!"));
            return defaultFailedResponse;
        }

        return null;
    }

    private ItemQuestionValidationResponse checkAnswerIsValid(IsaacItemQuestion itemQuestion, ItemChoice submittedChoice) {
        ItemQuestionValidationResponse defaultFailedResponse =
                ItemQuestionValidationResponse.createDefaultFailedResponse(itemQuestion.getId(), submittedChoice);

        if (null == submittedChoice.getItems() || submittedChoice.getItems().isEmpty()) {
            defaultFailedResponse.setExplanation(new Content("You did not provide an answer"));
            return defaultFailedResponse;
        }

        Set<String> allowedItemIds;
        if (null != submittedChoice.getItems() && null != itemQuestion.getItems()) {
            Collection<String> submittedItemIds = submittedChoice.getItems().stream().map(Item::getId).collect(Collectors.toSet());
            allowedItemIds = itemQuestion.getItems().stream().map(Item::getId).collect(Collectors.toSet());
            if (!allowedItemIds.containsAll(submittedItemIds)) {
                defaultFailedResponse.setExplanation(
                        new Content("You did not provide a valid answer; it contained unrecognised items"));
                return defaultFailedResponse;
            }
        }
        return null;
    }

    protected ItemQuestionValidationResponse checkIfAnswerMatchesAKnownAnswer(
            IsaacItemQuestion itemQuestion, ItemChoice submittedChoice) {

        // Order does not matter for Item Questions
        Set<String> submittedItemIds = submittedChoice.getItems().stream().map(Item::getId).collect(Collectors.toSet());

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

            Collection<String> choiceItemIds = new HashSet<>();
            for (Item choiceItem : itemChoice.getItems()) {
                choiceItemIds.add(choiceItem.getId());
            }

            if (choiceItemIds.equals(submittedItemIds)) {
                return new ItemQuestionValidationResponse(
                        itemQuestion.getId(),
                        submittedChoice,
                        c.isCorrect(),
                        (Content) c.getExplanation(),
                        new ArrayList<>(),
                        new Date()
                );
            }
        }
        return new ItemQuestionValidationResponse(
                itemQuestion.getId(), submittedChoice, false, null, null, new Date());
    }

    @Override
    public QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        ItemQuestionValidationResponse response = null;
        checkQuestionAndAnswerTypes(question, answer);

        IsaacItemQuestion itemQuestion = (IsaacItemQuestion) question;
        ItemChoice submittedChoice = (ItemChoice) answer;

        response = checkQuestionIsAnswerable(itemQuestion, submittedChoice);
        if (null != response) {return response;}

        response = checkAnswerIsValid(itemQuestion, submittedChoice);
        if (null != response) {return response;}

        return checkIfAnswerMatchesAKnownAnswer(itemQuestion, submittedChoice);
    }

}
