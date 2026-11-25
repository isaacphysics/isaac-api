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
import uk.ac.cam.cl.dtg.isaac.api.Constants;
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

/** Validator that only provides functionality to validate Drag and drop questions. */
public class IsaacDndValidator implements IValidator {
    public static final String FEEDBACK_QUESTION_NO_ANSWERS = "This question does not have any answers.";
    public static final String FEEDBACK_QUESTION_INVALID_ANS = "This question contains at least one invalid answer.";
    public static final String FEEDBACK_QUESTION_MISSING_ITEMS = "This question is missing items.";
    public static final String FEEDBACK_ANSWER_NOT_ENOUGH = "You did not provide a valid answer; it does not contain "
                                                          + "an item for each gap.";

    private static final Logger log = LoggerFactory.getLogger(IsaacDndValidator.class);

    @Override
    public final DndValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        DndProblem problem = getProblemsWithQuestionOrChoice(question, answer);
        if (problem == null) {
            return getMarkedResponse((IsaacDndQuestion) question, (DndChoice) answer);
        } else {
            return new DndValidationResponse(
                question.getId(), answer, false, null, new Content(problem.message), new Date());
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

        return new DndValidationResponse(
            question.getId(), answer, isCorrect, mostDropZonesCorrect, feedback, new Date());
    }

    private DndProblem getProblemsWithQuestionOrChoice(final Question question, final Choice choice) {
        Objects.requireNonNull(question);
        Objects.requireNonNull(choice);

        if (!(choice instanceof DndChoice)) {
            throw new IllegalArgumentException(format(
                "This validator only works with DndChoices (%s is not DndChoice)", question.getId()));
        }

        if (!(question instanceof IsaacDndQuestion)) {
            throw new IllegalArgumentException(format(
                "This validator only works with IsaacDndQuestions (%s is not IsaacDndQuestion)", question.getId()));
        }

        DndProblem questionProblem = getProblemsWithQuestion((IsaacDndQuestion) question);
        if (questionProblem != null) {
            DndLogger.log(questionProblem);
            return questionProblem;
        }
        DndProblem choiceProblem = getProblemsWithChoice((IsaacDndQuestion) question, (DndChoice) choice);
        if (choiceProblem != null) {
            choiceProblem.message = "You provided " + choiceProblem.message;
            return choiceProblem;
        }
        return null;
    }

    /**
     * Check for problems with a DnD question, including problems with its choices.
     *
     * @param question the question to validate
     * @return a {@link DndProblem} describing the first problem found, or {@code null} if valid
     */
    public static DndProblem getProblemsWithQuestion(final IsaacDndQuestion question) {
        // items
        if (question.getItems() == null || question.getItems().isEmpty()) {
            return new DndProblem(FEEDBACK_QUESTION_MISSING_ITEMS, question);
        }

        // answers
        if (question.getChoices() == null || question.getChoices().isEmpty()) {
            return new DndProblem(FEEDBACK_QUESTION_NO_ANSWERS, question);
        }

        Optional<Choice> nonDndChoice = question.getChoices().stream()
            .filter(c -> !DndChoice.class.equals(c.getClass()))
            .findFirst();
        if (nonDndChoice.isPresent()) {
            return new DndProblem(FEEDBACK_QUESTION_INVALID_ANS, question, nonDndChoice.get());
        }

        if (question.getChoices().stream().noneMatch(Choice::isCorrect)) {
            return new DndProblem(Constants.FEEDBACK_NO_CORRECT_ANSWERS, question);
        }

        Optional<String> choiceProblems = question.getChoices().stream()
            .map(c -> getProblemsWithChoice(question, (DndChoice) c))
            .filter(Objects::nonNull)
            .map(m -> "The question is invalid, because it has " + m.message)
            .findFirst();
        if (choiceProblems.isPresent()) {
            return new DndProblem(choiceProblems.get(), question);
        }

        return null;
    }

    private static DndProblem getProblemsWithChoice(final IsaacDndQuestion question, final DndChoice choice) {
        if (choice.getItems() == null || choice.getItems().isEmpty()) {
            return new DndProblem("an empty answer.", question);
        }
        if (choice.getItems().stream().anyMatch(i -> i.getClass() != DndItem.class)) {
            return new DndProblem("an invalid answer.", question);
        }
        if (choice.getItems().stream().anyMatch(i -> i.getId() == null || i.getDropZoneId() == null
                || Objects.equals(i.getId(), "") || Objects.equals(i.getDropZoneId(), ""))) {
            return new DndProblem("an answer in an unrecognised format.", question);
        }
        if (choice.getItems().stream().anyMatch(i -> !question.getItems().contains(i))) {
            return new DndProblem("an answer with unrecognised items.", question);
        }
        if (choice.getItems().size() > question.getChoices().stream().filter(Choice::isCorrect).findFirst()
                .map(c -> ((DndChoice) c).getItems().size()).orElse(0)) {
            return new DndProblem("an answer with more items than we have gaps.", question);
        }
        List<String> dropZoneIds = choice.getItems().stream().map(DndItem::getDropZoneId).toList();
        if (dropZoneIds.size() != new HashSet<>(dropZoneIds).size()) {
            return new DndProblem("an answer with duplicate drop zones.", question);
        }
        return null;
    }

    /** Utility class to represent the result of checking whether a question or answer was valid. */
    public static class DndProblem {
        public String message;
        public Question question;
        public Choice choice = null;

        /**
         * Construct a problem associated with a question.
         *
         * @param message a description of the problem
         * @param question the question where the problem was found
         */
        public DndProblem(final String message, final Question question) {
            this.message = message;
            this.question = question;
        }

        /**
         * Construct a problem associated with a question and a specific choice.
         *
         * @param message a description of the problem
         * @param question the question where the problem was found
         * @param choice the specific choice where the problem was found
         */
        public DndProblem(final String message, final Question question, final Choice choice) {
            this.message = message;
            this.question = question;
            this.choice = choice;
        }
    }

    private static class DndLogger {
        private static void log(final DndProblem result) {
            log.error(messageFor(result));
        }

        private static String messageFor(final DndProblem message) {
            String id = message.question.getId();
            String sourceFile = message.question.getCanonicalSourceFile();
            return switch (message.message) {
                // questions
                case FEEDBACK_QUESTION_MISSING_ITEMS -> String.format(
                        "Expected items in question (%s), but didn't find any!", id);
                case "The question is invalid, because it has an answer with duplicate drop zones." ->
                        String.format("Question contains duplicate drop zones. %s src %s", id, sourceFile);
                case FEEDBACK_QUESTION_NO_ANSWERS -> String.format(
                        "Question does not have any answers. %s src: %s", id, sourceFile);
                case Constants.FEEDBACK_NO_CORRECT_ANSWERS -> String.format(
                        "Question does not have any correct answers. %s src: %s", id, sourceFile
                );
                case FEEDBACK_QUESTION_INVALID_ANS -> String.format(
                        "Expected DndItem in question (%s), instead found %s!", id, message.choice.getClass());
                // answers
                case "The question is invalid, because it has an empty answer." -> String.format(
                        "Expected list of DndItems, but none found in choice for question id (%s)!", id);
                case "The question is invalid, because it has an invalid answer." -> String.format(
                        "Expected list of DndItems, but something else found in choice for question id (%s)!", id);
                case "The question is invalid, because it has an answer in an unrecognised format." ->
                        String.format("Found item with missing id or drop zone id in answer for question id (%s)!", id);
                case "The question is invalid, because it has an answer with unrecognised items." -> String.format(
                        "Question contains invalid item ref. %s src %s", id, sourceFile);
                case "The question is invalid, because it has an answer with more items than we have gaps." ->
                        String.format("Question has answer with more items than we have gaps. %s src %s", id, sourceFile);
                default -> null;
            };
        }
    }
}
