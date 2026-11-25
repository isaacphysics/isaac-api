/*
 * Copyright 2021 Chris Purdy, 2022 James Sharkey
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.DndValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacDndQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;

/** Validator that only provides functionality to validate Drag and drop questions. */
public class IsaacDndValidator implements IValidator {
    public static final String FEEDBACK_QUESTION_INVALID_ANS = "This choice is invalid.";
    public static final String FEEDBACK_QUESTION_MISSING_ITEMS = "This question is missing items.";
    public static final String FEEDBACK_ANSWER_NOT_ENOUGH = "Your answer does not contain an item for each gap.";

    private static final Logger log = LoggerFactory.getLogger(IsaacDndValidator.class);

    @Override
    public final DndValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        List<String> problems = getProblemsWithQuestionOrChoice(question, answer);
        if (problems.isEmpty()) {
            return getMarkedResponse((IsaacDndQuestion) question, (DndChoice) answer);
        } else {
            return new DndValidationResponse(
                question.getId(), answer, false, null, new Content(String.join(" ", problems)), new Date());
        }
    }

    private DndValidationResponse getMarkedResponse(final IsaacDndQuestion question, final DndChoice answer) {
        List<Choice> orderedChoices = getOrderedChoices(question.getChoices());
        boolean detailedItemFeedback = question.getDetailedItemFeedback() != null && question.getDetailedItemFeedback();

        // These variables store the important features of the response we'll send.
        boolean isCorrect = false;
        Content feedback = null;

        // A question can have several correct choices, e.g. one per ordering of interchangeable items. 
        // This saves the one with the most correct items, rather than the one on the last place on the list 
        // in case it's not 100% correct.
        Map<String, Boolean> mostDropZonesCorrect = null;
        int bestMatchCount = -1;

        // For all the choices on this question...
        for (Choice choice : orderedChoices) {
            boolean submissionMatches = true;
            int matchCount = 0;
            DndChoice dndChoice = (DndChoice) choice;
            List<DndItem> expectedItems = dndChoice.getItems();
            List<DndItem> submittedItems = answer.getItems();

            if (submittedItems.size() < expectedItems.size()) {
                feedback = new Content(FEEDBACK_ANSWER_NOT_ENOUGH);
            }

            // ...decide if the provided answer matched it
            Map<String, Boolean> itemMatches = new HashMap<>();
            for (DndItem expectedItem : expectedItems) {
                // fetch the corresponding user answer by drop zone id
                DndItem submittedItem = answer.getItems().stream()
                    .filter(i -> i.getDropZoneId().equals(expectedItem.getDropZoneId()))
                    .findFirst().orElse(null);
                boolean itemMatch = submittedItem != null && submittedItem.getId().equals(expectedItem.getId());
                itemMatches.put(expectedItem.getDropZoneId(), itemMatch);
                if (itemMatch) {
                    matchCount++;
                } else {
                    submissionMatches = false;
                }
            }

            // only when the match count is more than the current max
            if (detailedItemFeedback && dndChoice.isCorrect() && matchCount > bestMatchCount) {
                bestMatchCount = matchCount;
                mostDropZonesCorrect = submittedItems.stream().collect(
                        Collectors.toMap(DndItem::getDropZoneId, i -> itemMatches.getOrDefault(i.getDropZoneId(), false)));
            }

            if (submissionMatches) {
                isCorrect = dndChoice.isCorrect();
                feedback = (Content) dndChoice.getExplanation();
                break;
            }
        }

        if (feedbackIsNullOrEmpty(feedback)) {
            feedback = question.getDefaultFeedback();
        }

        return new DndValidationResponse(question.getId(), answer, isCorrect, mostDropZonesCorrect, feedback, new Date());
    }

    private List<String> getProblemsWithQuestionOrChoice(final Question question, final Choice choice) {
        Objects.requireNonNull(question);
        Objects.requireNonNull(choice);

        if (!(choice instanceof DndChoice)) {
            throw new IllegalArgumentException(format("This validator only works with DndChoices (%s is not a DndChoice)", choice.getId()));
        }

        if (!(question instanceof IsaacDndQuestion)) {
            throw new IllegalArgumentException(format("This validator only works with IsaacDndQuestions (%s is not an IsaacDndQuestion)", question.getId()));
        }

        List<String> problems = getProblemsWithQuestion((IsaacDndQuestion) question);
        if (!problems.isEmpty()) {
            problems.forEach(log::error);
        }

        List<String> answerProblems = getProblemsWithChoice((IsaacDndQuestion) question, (DndChoice) choice);
        problems.addAll(answerProblems);
        return problems;
    }

    /**
     * Check for problems with a DnD question, including problems with its choices.
     *
     * @param question the question to validate
     * @return a list of problems, empty if no problems were found
     */
    public static List<String> getProblemsWithQuestion(final IsaacDndQuestion question) {
        List<String> problems = new java.util.ArrayList<>();

        if (question.getItems() == null || question.getItems().isEmpty()) {
            problems.add(String.format("Question '%s': %s", question.getId(), FEEDBACK_QUESTION_MISSING_ITEMS));
        }

        if (question.getChoices() == null || question.getChoices().isEmpty()) {
            problems.add(String.format("Question '%s': %s", question.getId(), FEEDBACK_NO_ANSWERS));
            return problems;
        }

        question.getChoices().stream()
            .filter(c -> !DndChoice.class.equals(c.getClass()))
            .forEach(c -> problems.add(String.format("Question '%s', Choice '%s': %s", question.getId(), c.getId(), FEEDBACK_QUESTION_INVALID_ANS)));

        if (question.getChoices().stream().noneMatch(Choice::isCorrect)) {
            problems.add(String.format("Question '%s': %s", question.getId(), FEEDBACK_NO_CORRECT_ANSWERS));
        }

        question.getChoices().stream().filter(DndChoice.class::isInstance).map(DndChoice.class::cast)
            .forEach(c -> problems.addAll(getProblemsWithChoice(question, c)));

        return problems;
    }

    private static List<String> getProblemsWithChoice(final IsaacDndQuestion question, final DndChoice choice) {
        List<String> problems = new java.util.ArrayList<>();

        if (choice.getItems() == null || choice.getItems().isEmpty()) {
            problems.add(String.format("DnD question (%s) contains a choice (%s) with no items", question.getId(), choice.getId()));
            return problems;
        }
        if (choice.getItems().stream().anyMatch(i -> i.getClass() != DndItem.class)) {
            problems.add(String.format("DnD question (%s) contains a choice (%s) with an invalid item", question.getId(), choice.getId()));
        }
        if (choice.getItems().stream().anyMatch(i -> i.getId() == null || i.getDropZoneId() == null
                || Objects.equals(i.getId(), "") || Objects.equals(i.getDropZoneId(), ""))) {
            problems.add(String.format("DnD question (%s) contains a choice (%s) with an item with an empty id or dropzone id", question.getId(), choice.getId()));
        }
        if (question.getItems() != null && choice.getItems().stream().anyMatch(i -> !question.getItems().contains(i))) {
            problems.add(String.format("DnD question (%s) contains a choice (%s) with an item that is not in the question's items", question.getId(), choice.getId()));
        }
        Optional<Integer> correctNumberOfGaps = question.getChoices().stream().filter(Choice::isCorrect).findFirst().map(c -> ((DndChoice) c).getItems().size());
        if (correctNumberOfGaps.isPresent() && choice.getItems().size() > correctNumberOfGaps.get()) {
            problems.add(String.format("DnD question (%s) contains a choice (%s) with more items than there are gaps", question.getId(), choice.getId()));
        }
        List<String> dropZoneIds = choice.getItems().stream().map(DndItem::getDropZoneId).toList();
        if (dropZoneIds.size() != new HashSet<>(dropZoneIds).size()) {
            problems.add(String.format("DnD question (%s) contains a choice (%s) with duplicate dropzone IDs", question.getId(), choice.getId()));
        }
        return problems;
    }
}
