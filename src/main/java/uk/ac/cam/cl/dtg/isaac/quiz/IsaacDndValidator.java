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

        return new ValidationUtils.BiRuleValidator<IsaacDndQuestion, DndChoice>()
            .add((q, a) -> questionValidator(IsaacDndValidator::logIfTrue).check(q))
            .add((q, a) -> answerValidator(IsaacDndValidator::noLog).check(q, a).map(m -> "You provided " + m))
            .check((IsaacDndQuestion) question, (DndChoice) answer);
    }

    /** A validator whose .check method determines whether the given question is valid. */
    public static ValidationUtils.RuleValidator<IsaacDndQuestion> questionValidator(
        final BiFunction<Boolean, String, Boolean> logged
    ) {
        return new ValidationUtils.RuleValidator<IsaacDndQuestion>()
            // items
            .add(FEEDBACK_QUESTION_MISSING_ITEMS, q -> logged.apply(
                q.getItems() == null || q.getItems().isEmpty(),
                format("Expected items in question (%s), but didn't find any!", q.getId())
            ))
            // dropZones
            .add(FEEDBACK_QUESTION_NO_DZ, q -> logged.apply(
                DropZones.getFromQuestion(q).isEmpty(),
                format("Question does not have any drop zones. %s src %s", q.getId(), q.getCanonicalSourceFile())
            ))
            .add(FEEDBACK_QUESTION_DUP_DZ, q -> logged.apply(
                DropZones.getFromQuestion(q).size() != new HashSet<>(DropZones.getFromQuestion(q)).size(),
                format("Question contains duplicate drop zones. %s src %s", q.getId(), q.getCanonicalSourceFile())
            ))
            // answers
            .add(Constants.FEEDBACK_NO_CORRECT_ANSWERS, q -> logged.apply(
                q.getChoices() == null || q.getChoices().isEmpty(),
                format("Question does not have any answers. %s src: %s", q.getId(), q.getCanonicalSourceFile())
            ))
            .add(FEEDBACK_QUESTION_INVALID_ANS, q -> q.getChoices().stream().anyMatch(c -> logged.apply(
                !DndChoice.class.equals(c.getClass()),
                format("Expected DndItem in question (%s), instead found %s!", q.getId(), c.getClass())
            )))
            .add(q -> q.getChoices().stream()
                .flatMap(c -> answerValidator(IsaacDndValidator::logIfTrue).check(q, (DndChoice) c)
                    .map(m -> "The question is invalid, because it has " + m).stream())
                .findFirst()
            )
            .add(Constants.FEEDBACK_NO_CORRECT_ANSWERS, q -> logged.apply(
                q.getChoices().stream().noneMatch(Choice::isCorrect),
                format("Question does not have any correct answers. %s src: %s", q.getId(), q.getCanonicalSourceFile())
            ))
            .add(FEEDBACK_QUESTION_UNUSED_DZ, q -> q.getChoices().stream().anyMatch(c -> logged.apply(
                c.isCorrect() && DropZones.getFromQuestion(q).size() != ((DndChoice) c).getItems().size(),
                format("Question contains correct answer that doesn't use all drop zones. %s src %s",
                    q.getId(), q.getCanonicalSourceFile())
            )));
    }

    private static ValidationUtils.BiRuleValidator<IsaacDndQuestion, DndChoice> answerValidator(
        final BiFunction<Boolean, String, Boolean> logged
    ) {
        return new ValidationUtils.BiRuleValidator<IsaacDndQuestion, DndChoice>()
            .add("an empty answer.", (q, a) -> logged.apply(
                a.getItems() == null || a.getItems().isEmpty(),
                format("Expected list of DndItems, but none found in choice for question id (%s)!", q.getId())
            ))
            .add("an invalid answer.", (q, a) -> a.getItems().stream().anyMatch(i -> logged.apply(
                i.getClass() != DndItem.class,
                format("Expected list of DndItems, but something else found in choice for question id (%s)!", q.getId())
            )))
            .add("an answer in an unrecognised format.", (q, a) -> a.getItems().stream().anyMatch(i -> logged.apply(
                i.getId() == null || i.getDropZoneId() == null
                    || Objects.equals(i.getId(), "") || Objects.equals(i.getDropZoneId(), ""),
                format("Found item with missing id or drop zone id in answer for question id (%s)!", q.getId()))
            ))
            .add("an answer with unrecognised items.", (q, a) -> a.getItems().stream().anyMatch(i -> logged.apply(
                !q.getItems().contains(i),
                format("Question contains invalid item ref. %s src %s", q.getId(), q.getCanonicalSourceFile())
            )))
            .add("an answer with unrecognised drop zones.", (q, a) -> a.getItems().stream().anyMatch(i -> logged.apply(
                !DropZones.getFromQuestion(q).contains(i.getDropZoneId()),
                format("Question contains invalid drop zone ref. %s src %s", q.getId(), q.getCanonicalSourceFile())
            )))
            .add("an answer with more items than we have gaps.", (q, a) -> logged.apply(
                a.getItems().size() > DropZones.getFromQuestion(q).size(),
                format("Question has answer with more items than we have gaps. %s src %s",
                       q.getId(), q.getCanonicalSourceFile())
            ))
            .add("an answer with duplicate drop zones.", (q, a) -> {
                var dropZoneIds = a.getItems().stream().map(DndItem::getDropZoneId).collect(Collectors.toList());
                return logged.apply(dropZoneIds.size() != new HashSet<>(dropZoneIds).size(),
                    format("Question contains duplicate drop zones. %s src %s", q.getId(), q.getCanonicalSourceFile())
                );
            });
    }

    private static boolean logIfTrue(final boolean result, final String message) {
        if (result) {
            log.error(message);
        }
        return result;
    }

    public static boolean noLog(final boolean result, final String message) {
        return result;
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
}
