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

import org.apache.commons.lang3.BooleanUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Validator that only provides functionality to validate Drag and drop questions.
 */
public class IsaacDndValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacDndValidator.class);

    @Override
    public final DndValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        return validate(question, answer)
            .map(msg -> new DndValidationResponse(question.getId(), answer, false, null, new Content(msg), new Date()))
            .orElseGet(() -> mark((IsaacDndQuestion) question, (DndChoice) answer));
    }

    private DndValidationResponse mark(final IsaacDndQuestion question, final DndChoice answer) {
        List<DndChoice> sortedAnswers = QuestionHelpers.getChoices(question)
            .sorted((rhs, lhs) -> {
                int compared = ChoiceHelpers.countPartialMatchesIn(lhs, answer)
                    - ChoiceHelpers.countPartialMatchesIn(rhs, answer);
                if (compared == 0) {
                    return lhs.isCorrect() && rhs.isCorrect() ? 0 : (lhs.isCorrect() ? 1 : -1);
                }
                return compared;
            })
            .collect(Collectors.toList());
        var matchedAnswer = sortedAnswers.stream().filter(lhs -> ChoiceHelpers.matches(lhs, answer)).findFirst();
        var closestCorrect = sortedAnswers.stream().filter(DndChoice::isCorrect).findFirst().orElse(null);

        var isCorrect = matchedAnswer.map(DndChoice::isCorrect).orElse(false);
        var dropZonesCorrect = QuestionHelpers.getDetailedItemFeedback(question)
                ? ChoiceHelpers.getDropZonesCorrect(closestCorrect, answer) : null;
        var feedback = (Content) matchedAnswer.map(Choice::getExplanation).orElseGet(() -> {
            if (isCorrect) {
                return null;
            }
            if (answer.getItems().size() < closestCorrect.getItems().size()) {
                return new Content("You did not provide a valid answer; it does not contain an item for each gap.");
            }
            return question.getDefaultFeedback();
        });
        return new DndValidationResponse(question.getId(), answer, isCorrect, dropZonesCorrect, feedback, new Date());
    }

    private Optional<String> validate(final Question question, final Choice answer) {
        Objects.requireNonNull(question);
        Objects.requireNonNull(answer);

        if (!(answer instanceof DndChoice)) {
            throw new IllegalArgumentException(String.format(
                "This validator only works with DndChoices (%s is not DndChoice)", question.getId()));
        }

        if (!(question instanceof IsaacDndQuestion)) {
            throw new IllegalArgumentException(String.format(
                "This validator only works with IsaacDndQuestions (%s is not IsaacDndQuestion)", question.getId()));
        }

        return new ValidationUtils.BiRuleValidator<IsaacDndQuestion, DndChoice>()
            // question
            .add(Constants.FEEDBACK_NO_CORRECT_ANSWERS, (q, a) -> logged(
                q.getChoices() == null || q.getChoices().isEmpty(),
                "Question does not have any answers. %s src: %s", q.getId(), q.getCanonicalSourceFile()
            ))
            .add(Constants.FEEDBACK_NO_CORRECT_ANSWERS, (q, a) -> logged(
                q.getChoices().stream().noneMatch(Choice::isCorrect),
                "Question does not have any correct answers. %s src: %s", q.getId(), q.getCanonicalSourceFile()
            ))
            .add("This question contains at least one invalid answer.", (q, a) -> q.getChoices().stream().anyMatch(c -> logged(
                !DndChoice.class.equals(c.getClass()),
                "Expected DndItem in question (%s), instead found %s!", q.getId(), c.getClass()
            )))
            .add("This question contains an empty answer.", (q, a) -> QuestionHelpers.getChoices(q).anyMatch(c -> logged(
                c.getItems() == null || c.getItems().isEmpty(),
                "Expected list of DndItems, but none found in choice for question id (%s)!", q.getId()
            )))
            .add("This question contains at least one invalid answer.", (q, a) -> QuestionHelpers.getChoices(q).anyMatch(c -> logged(
                c.getItems().stream().anyMatch(i -> i.getClass() != DndItem.class),
                "Expected list of DndItems, but something else found in choice for question id (%s)!", q.getId()
            )))
            .add("This question contains at least one answer in an unrecognised format.", (q, a) -> QuestionHelpers.getChoices(q).anyMatch(c -> logged(
                ChoiceHelpers.getItems(c).anyMatch(i -> i.getId() == null || i.getDropZoneId() == null || Objects.equals(i.getId(), "") || Objects.equals(i.getDropZoneId(), "")),
                "Found item with missing id or drop zone id in answer for question id (%s)!", q.getId()
            )))
            .add("This question is missing items.", (q, a) -> logged(
                q.getItems() == null || q.getItems().isEmpty(),
                "Expected items in question (%s), but didn't find any!", q.getId()
            ))
            .add("This question doesn't have any drop zones.", (q, a) -> logged(
                QuestionHelpers.getDropZones(q).isEmpty(),
                "Question does not have any drop zones. %s src %s", q.getId(), q.getCanonicalSourceFile()
            ))
            .add("This question contains duplicate drop zones.", (q, a) -> logged(
                QuestionHelpers.getDropZones(q).size() != new HashSet<>(QuestionHelpers.getDropZones(q)).size(),
                "Question contains duplicate drop zones. %s src %s", q.getId(), q.getCanonicalSourceFile()
            ))

            // answer
            .add(Constants.FEEDBACK_NO_ANSWER_PROVIDED, (q, a) -> a.getItems() == null || a.getItems().isEmpty())
            .add(Constants.FEEDBACK_UNRECOGNISED_ITEMS,
                 (q, a) -> a.getItems().stream().anyMatch(answerItem -> !q.getItems().contains(answerItem)))
            .add(Constants.FEEDBACK_UNRECOGNISED_FORMAT, (q, a) -> ChoiceHelpers.getItems(a).anyMatch(i -> i.getId() == null)
                      || ChoiceHelpers.getItems(a).anyMatch(i -> i.getDropZoneId() == null))
            .add("You did not provide a valid answer; it contains more items than gaps.",
                (q, a) -> a.getItems().size() > QuestionHelpers.getAnyCorrect(q).map(c -> c.getItems().size()).orElse(0))
            .check((IsaacDndQuestion) question, (DndChoice) answer);
    }

    private static boolean logged(final boolean result, final String message, final Object... args) {
        if (result) {
            log.error(String.format(message, args));
        }
        return result;
    }

    public static class QuestionHelpers {
        public static Stream<DndChoice> getChoices(final IsaacDndQuestion question) {
            return question.getChoices().stream().map(c -> (DndChoice) c);
        }

        public static Optional<DndChoice> getAnyCorrect(final IsaacDndQuestion question) {
            return getChoices(question).filter(DndChoice::isCorrect).findFirst();
        }

        public static boolean getDetailedItemFeedback(final IsaacDndQuestion question) {
            return BooleanUtils.isTrue(question.getDetailedItemFeedback());
        }

        public static List<String> getDropZones(final IsaacDndQuestion question) {
            if (question.getChildren() == null) {
                return List.of();
            }
            return question.getChildren().stream()
                .flatMap(QuestionHelpers::getContentDropZones)
                .collect(Collectors.toList());
        }

        private static Stream<String> getContentDropZones(final ContentBase content) {
            if (content instanceof Figure && ((Figure) content).getFigureRegions() != null) {
                var figure = (Figure) content;
                return figure.getFigureRegions().stream().map(FigureRegion::getId);
            }
            if (content instanceof Content && ((Content) content).getValue() != null) {
                var textContent = (Content) content;
                String dndDropZoneRegexStr = "\\[drop-zone:(?<id>[a-zA-Z0-9_-]+)(?<params>\\|(?<width>w-\\d+?)?(?<height>h-\\d+?)?)?]";
                Pattern dndDropZoneRegex = Pattern.compile(dndDropZoneRegexStr);
                return dndDropZoneRegex.matcher(textContent.getValue()).results().map(mr -> mr.group(1));
            }
            if (content instanceof Content && ((Content) content).getChildren() != null) {
                return ((Content) content).getChildren().stream().flatMap(QuestionHelpers::getContentDropZones);
            }

            return Stream.of();
        }
    }

    private static class ChoiceHelpers {
        public static Stream<DndItem> getItems(final DndChoice choice) {
            return choice.getItems().stream().map(i -> (DndItem) i);
        }

        public static boolean matches(final DndChoice lhs, final DndChoice rhs) {
            return getItems(lhs).allMatch(lhsItem -> dropZoneEql(rhs, lhsItem));
        }

        public static int countPartialMatchesIn(final DndChoice lhs, final DndChoice rhs) {
            return getItems(lhs)
                .map(lhsItem -> dropZoneEql(rhs, lhsItem) ? 1 : 0)
                .mapToInt(Integer::intValue)
                .sum();
        }

        public static Map<String, Boolean> getDropZonesCorrect(final DndChoice lhs, final DndChoice rhs) {
            return getItems(lhs)
                .filter(lhsItem -> getItemByDropZone(rhs, lhsItem.getDropZoneId()).isPresent())
                .collect(Collectors.toMap(
                    DndItem::getDropZoneId,
                    lhsItem -> dropZoneEql(rhs, lhsItem))
                );
        }

        private static boolean dropZoneEql(final DndChoice choice, final DndItem item) {
            return getItemByDropZone(choice, item.getDropZoneId())
                .map(choiceItem -> choiceItem.getId().equals(item.getId()))
                .orElse(false);
        }

        private static Optional<DndItem> getItemByDropZone(final DndChoice choice, final String dropZoneId) {
            return getItems(choice)
                .filter(item -> item.getDropZoneId().equals(dropZoneId))
                .findFirst();
        }
    }
}
