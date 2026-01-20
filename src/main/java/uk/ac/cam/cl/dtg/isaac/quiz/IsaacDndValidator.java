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
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.Figure;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.util.FigureRegion;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

/** Validator that only provides functionality to validate Drag and drop questions. */
public class IsaacDndValidator implements IValidator {
    public static final String FEEDBACK_QUESTION_NO_ANSWERS = "This question does not have any answers.";
    public static final String FEEDBACK_QUESTION_INVALID_ANS = "This question contains at least one invalid answer.";
    public static final String FEEDBACK_QUESTION_MISSING_ITEMS = "This question is missing items.";
    public static final String FEEDBACK_QUESTION_NO_DZ = "This question doesn't have any drop zones.";
    public static final String FEEDBACK_QUESTION_DUP_DZ = "This question contains duplicate drop zones.";
    public static final String FEEDBACK_QUESTION_UNUSED_DZ = "This question contains a correct answer that doesn't use "
                                                           + "all drop zones.";
    public static final String FEEDBACK_ANSWER_NOT_ENOUGH = "You did not provide a valid answer; it does not contain "
                                                          + "an item for each gap.";

    private static final Logger log = LoggerFactory.getLogger(IsaacDndValidator.class);

    @Override
    public final DndValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        return getProblemWithQuestionOrAnswer(question, answer)
            .map(problem ->
                new DndValidationResponse(question.getId(), answer, false, null, new Content(problem), new Date()))
            .orElseGet(() -> getMarkedResponse((IsaacDndQuestion) question, (DndChoice) answer));
    }

    private DndValidationResponse getMarkedResponse(final IsaacDndQuestion question, final DndChoice answer) {
        List<Choice> orderedChoices = getOrderedChoices(question.getChoices());
        boolean detailedItemFeedback = question.getDetailedItemFeedback() != null && question.getDetailedItemFeedback();

        // These variables store the important features of the response we'll send.
        boolean isCorrect = false;
        Content feedback = null;
        Map<String, Boolean> dropZonesCorrect = null;

        // For all the choices on this question...
        for (Choice choice : orderedChoices) {
            boolean submissionMatches = true;
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
                if (!itemMatch) {
                    submissionMatches = false;
                }
            }

            if (detailedItemFeedback && dndChoice.isCorrect()) {
                dropZonesCorrect = submittedItems.stream().collect(
                    Collectors.toMap(DndItem::getDropZoneId, i -> itemMatches.get(i.getDropZoneId())));
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

        return new DndValidationResponse(question.getId(), answer, isCorrect, dropZonesCorrect, feedback, new Date());
    }

    private Optional<String> getProblemWithQuestionOrAnswer(final Question question, final Choice answer) {
        Objects.requireNonNull(question);
        Objects.requireNonNull(answer);

        if (!(answer instanceof DndChoice)) {
            throw new IllegalArgumentException(format(
                "This validator only works with DndChoices (%s is not DndChoice)", question.getId()));
        }

        if (!(question instanceof IsaacDndQuestion)) {
            throw new IllegalArgumentException(format(
                "This validator only works with IsaacDndQuestions (%s is not IsaacDndQuestion)", question.getId()));
        }

        var questionValidationResult = validateQuestion((IsaacDndQuestion) question);
        if (questionValidationResult != null) {
            DndLogger.log(questionValidationResult);
            return Optional.of(questionValidationResult.problem);
        }
        var answerValidationResult = validateAnswer((IsaacDndQuestion) question, (DndChoice) answer);
        if (answerValidationResult != null) {
            return Optional.of("You provided " + answerValidationResult.problem);
        }
        return Optional.empty();
    }

    public static DndValidationResult validateQuestion(final IsaacDndQuestion q) {
        // items
        if (q.getItems() == null || q.getItems().isEmpty()) {
            return new DndValidationResult(FEEDBACK_QUESTION_MISSING_ITEMS, q);
        }
        // dropZones
        if (DropZones.getFromQuestion(q).isEmpty()) {
            return new DndValidationResult(FEEDBACK_QUESTION_NO_DZ, q);
        }
        if (DropZones.getFromQuestion(q).size() != new HashSet<>(DropZones.getFromQuestion(q)).size()) {
            return new DndValidationResult(FEEDBACK_QUESTION_DUP_DZ, q);
        }
        // answers
        if (q.getChoices() == null || q.getChoices().isEmpty()) {
            return new DndValidationResult(FEEDBACK_QUESTION_NO_ANSWERS, q);
        }

        Optional<Choice> nonDndChoice = q.getChoices().stream()
            .filter(c -> !DndChoice.class.equals(c.getClass()))
            .findFirst();
        if (nonDndChoice.isPresent()) {
            return new DndValidationResult(FEEDBACK_QUESTION_INVALID_ANS, q, nonDndChoice.get());
        }

        Optional<String> answerProblems = q.getChoices().stream()
            .map(c -> validateAnswer(q, (DndChoice) c))
            .filter(Objects::nonNull)
            .map(m -> "The question is invalid, because it has " + m.problem)
            .findFirst();
        if (answerProblems.isPresent()) {
            return new DndValidationResult(answerProblems.get(), q);
        }

        if (q.getChoices().stream().noneMatch(Choice::isCorrect)) {
            return new DndValidationResult(Constants.FEEDBACK_NO_CORRECT_ANSWERS, q);
        }
        if (q.getChoices().stream().anyMatch(c -> c.isCorrect()
                && DropZones.getFromQuestion(q).size() != ((DndChoice) c).getItems().size())) {
            return new DndValidationResult(FEEDBACK_QUESTION_UNUSED_DZ, q);
        }
        return null;
    }

    private static DndValidationResult validateAnswer(final IsaacDndQuestion q, final DndChoice a) {
        if (a.getItems() == null || a.getItems().isEmpty()) {
            return new DndValidationResult("an empty answer.", q);
        }
        if (a.getItems().stream().anyMatch(i -> i.getClass() != DndItem.class)) {
            return new DndValidationResult("an invalid answer.", q);
        }
        if (a.getItems().stream().anyMatch(i -> i.getId() == null || i.getDropZoneId() == null
                || Objects.equals(i.getId(), "") || Objects.equals(i.getDropZoneId(), ""))) {
            return new DndValidationResult("an answer in an unrecognised format.", q);
        }
        if (a.getItems().stream().anyMatch(i -> !q.getItems().contains(i))) {
            return new DndValidationResult("an answer with unrecognised items.", q);
        }
        if (a.getItems().stream().anyMatch(i -> !DropZones.getFromQuestion(q).contains(i.getDropZoneId()))) {
            return new DndValidationResult("an answer with unrecognised drop zones.", q);
        }
        if (a.getItems().size() > DropZones.getFromQuestion(q).size()) {
            return new DndValidationResult("an answer with more items than we have gaps.", q);
        }
        var dropZoneIds = a.getItems().stream().map(DndItem::getDropZoneId).collect(Collectors.toList());
        if (dropZoneIds.size() != new HashSet<>(dropZoneIds).size()) {
            return new DndValidationResult("an answer with duplicate drop zones.", q);
        }
        return null;
    }

    /**
     * Helper class to collect methods related to drop zone parsing.
     * */
    public static class DropZones {
        /**
         * Recursively collects the drop zone ids from any content within the question.
         */
        public static List<String> getFromQuestion(final IsaacDndQuestion question) {
            if (question.getChildren() == null) {
                return List.of();
            }
            return question.getChildren().stream()
                .flatMap(DropZones::getFromContent)
                .collect(Collectors.toList());
        }

        private static Stream<String> getFromContent(final ContentBase content) {
            if (content instanceof Figure && ((Figure) content).getFigureRegions() != null) {
                var figure = (Figure) content;
                return figure.getFigureRegions().stream().map(FigureRegion::getId);
            }
            if (content instanceof Content && ((Content) content).getValue() != null) {
                var textContent = (Content) content;
                String expr = "\\[drop-zone:(?<id>[a-zA-Z0-9_-]+)(?<params>\\|(?<width>w-\\d+?)?(?<height>h-\\d+?)?)?]";
                Pattern dndDropZoneRegex = Pattern.compile(expr);
                return dndDropZoneRegex.matcher(textContent.getValue()).results().map(mr -> mr.group(1));
            }
            if (content instanceof Content && ((Content) content).getChildren() != null) {
                return ((Content) content).getChildren().stream().flatMap(DropZones::getFromContent);
            }
            return Stream.of();
        }
    }

    public static class DndValidationResult {
        public String problem;
        public Question question;
        public Choice choice = null;

        public DndValidationResult(final String problem, final Question question) {
            this.problem = problem;
            this.question = question;
        }

        public DndValidationResult(final String problem, final Question question, final Choice choice) {
            this.problem = problem;
            this.question = question;
            this.choice = choice;
        }
    }

    private static class DndLogger {
        private static void log(final DndValidationResult result) {
            log.error(messageFor(result));
        }

        private static String messageFor(final DndValidationResult message) {
            var id = message.question.getId();
            var sourceFile = message.question.getCanonicalSourceFile();
            switch (message.problem) {
                // questions
                case FEEDBACK_QUESTION_MISSING_ITEMS: return String.format(
                    "Expected items in question (%s), but didn't find any!", id);
                case FEEDBACK_QUESTION_NO_DZ: return String.format(
                    "Question does not have any drop zones. %s src %s", id, sourceFile);
                case FEEDBACK_QUESTION_DUP_DZ:
                case "The question is invalid, because it has an answer with duplicate drop zones.": return String.format(
                    "Question contains duplicate drop zones. %s src %s", id, sourceFile);
                case FEEDBACK_QUESTION_NO_ANSWERS: return String.format(
                    "Question does not have any answers. %s src: %s", id, sourceFile);
                case Constants.FEEDBACK_NO_CORRECT_ANSWERS: return String.format(
                    "Question does not have any correct answers. %s src: %s", id, sourceFile
                );
                case FEEDBACK_QUESTION_UNUSED_DZ: return String.format(
                    "Question contains correct answer that doesn't use all drop zones. %s src %s", id, sourceFile);
                case FEEDBACK_QUESTION_INVALID_ANS: return String.format(
                    "Expected DndItem in question (%s), instead found %s!", id, message.choice.getClass());
                // answers
                case "The question is invalid, because it has an empty answer.": return String.format(
                    "Expected list of DndItems, but none found in choice for question id (%s)!", id);
                case "The question is invalid, because it has an invalid answer.": return String.format(
                    "Expected list of DndItems, but something else found in choice for question id (%s)!", id);
                case "The question is invalid, because it has an answer in an unrecognised format.": return String.format(
                    "Found item with missing id or drop zone id in answer for question id (%s)!", id);
                case "The question is invalid, because it has an answer with unrecognised items.": return String.format(
                    "Question contains invalid item ref. %s src %s", id, sourceFile);
                case "The question is invalid, because it has an answer with unrecognised drop zones.": return String.format(
                    "Question contains invalid drop zone ref. %s src %s", id, sourceFile);
                case "The question is invalid, because it has an answer with more items than we have gaps.": return String.format(
                    "Question has answer with more items than we have gaps. %s src %s", id, sourceFile);
                default: return null;
            }
        }
    }
}
