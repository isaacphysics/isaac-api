/*
 * Copyright 2019 Meurig Thomas
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacItemQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacParsonsQuestion;
import uk.ac.cam.cl.dtg.segue.dos.ItemQuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Item;
import uk.ac.cam.cl.dtg.segue.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.ParsonsChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.ParsonsItem;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class IsaacParsonsQuestionValidator extends IsaacItemQuestionValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacParsonsQuestionValidator.class);

    @Override
    protected void checkQuestionAndAnswerTypes(final Question question, final Choice answer) {
        if (!(question instanceof IsaacParsonsQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with IsaacParsonsQuestions (%s is not IsaacParsosnsQuestion)", question.getId()));
        }

        if (!(answer instanceof ParsonsChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected ParsonsChoice for IsaacParsonsQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }
    }

    @Override
    protected ItemQuestionValidationResponse checkIfAnswerMatchesAKnownAnswer(IsaacItemQuestion itemQuestion, ItemChoice submittedChoice) {
        // Submitted Item IDs stored in order
        List<String> submittedItemIds = submittedChoice.getItems().stream().map(Item::getId).collect(Collectors.toList());

        // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
        List<Choice> orderedChoices = getOrderedChoices(itemQuestion.getChoices());

        // For all the choices on this question...
        for (Choice c : orderedChoices) {
            ItemQuestionValidationResponse defaultResponse =
                    ItemQuestionValidationResponse.createDefaultFailedResponse(itemQuestion.getId(), submittedChoice);

            // ... that are the right type of ParsonsChoice, ...
            if (!(c instanceof ParsonsChoice)) {
                log.error(String.format(
                        "Validator for question (%s) expected there to be an ParsonsChoice. Instead it found a %s.",
                        itemQuestion.getId(), c.getClass().toString()));
                continue;
            }

            ParsonsChoice parsonsChoice = (ParsonsChoice) c;

            // ... and that have items ...
            if (null == parsonsChoice.getItems() || parsonsChoice.getItems().isEmpty()) {
                log.error(String.format("Missing Item list in choice for question id (%s)!", itemQuestion.getId()));
                continue;
            }

            // ... and those items are of the correct type ...
            if (parsonsChoice.getItems().stream().anyMatch(choiceItem -> !(choiceItem instanceof ParsonsItem))) {
                log.error("ParsonsChoice contained non-ParsonsItem. Skipping!");
                continue;
            }

            // ... look for a match to the submitted answer:
            if (parsonsChoice.getItems().size() != submittedChoice.getItems().size()) {
                continue;
            }

            List<Integer> submittedItemIndentations = new ArrayList<>();
            Collection<String> choiceItemIds = new ArrayList<>();
            List<Integer> choiceItemIndentations = new ArrayList<>();
            for (int i = 0; i < parsonsChoice.getItems().size(); i++) {
                Item choiceItem = parsonsChoice.getItems().get(i);
                Item submittedItem = submittedChoice.getItems().get(i);

                choiceItemIds.add(choiceItem.getId());
                if (!(submittedItem instanceof ParsonsItem)) {
                    throw new IllegalArgumentException("Expected ParsonsChoice to contain ParsonsItems!");
                }

                Integer submittedItemIndentation = ((ParsonsItem) submittedItem).getIndentation();
                Integer choiceItemIndentation = ((ParsonsItem) choiceItem).getIndentation();
                submittedItemIndentations.add(submittedItemIndentation);
                choiceItemIndentations.add(choiceItemIndentation);

                if (parsonsChoice.isCorrect() && null == defaultResponse.getIncorrectItemIds()) {
                    if (!choiceItem.getId().equals(submittedItem.getId()) || !choiceItemIndentation.equals(submittedItemIndentation)) {
                        defaultResponse.setIncorrectItemIds(Lists.newArrayList(submittedItem.getId()));
                    }
                }
            }

            if (choiceItemIds.equals(submittedItemIds) && choiceItemIndentations.equals(submittedItemIndentations)) {
                defaultResponse.setCorrect(c.isCorrect());
                defaultResponse.setIncorrectItemIds(new ArrayList<>());
                defaultResponse.setExplanation((Content) c.getExplanation());
                return defaultResponse;
            }
        }
        return ItemQuestionValidationResponse.createDefaultFailedResponse(itemQuestion.getId(), submittedChoice);
    }
}
