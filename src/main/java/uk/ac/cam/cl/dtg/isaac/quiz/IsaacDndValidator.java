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
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.dos.DndValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacDndQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
            .orElseGet(() -> mark(IsaacDndQuestionEx.of(question), DndChoiceEx.of(answer)));
    }

    private DndValidationResponse mark(final IsaacDndQuestionEx question, final DndChoiceEx answer) {
        // TODO: extract sorting, to comply with IValidator interface
        List<DndChoiceEx> sortedAnswers = question.getDndChoices()
            .sorted((rhs, lhs) -> {
                int compared = lhs.countPartialMatchesIn(answer) - rhs.countPartialMatchesIn(answer);
                if (compared == 0) {
                    return lhs.isCorrect() && rhs.isCorrect() ? 0 : (lhs.isCorrect() ? 1 : -1);
                }
                return compared;
            })
            .collect(Collectors.toList());
        Optional<DndChoiceEx> matchedAnswer = sortedAnswers.stream().filter(lhs -> lhs.matches(answer)).findFirst();
        DndChoiceEx closestCorrect = sortedAnswers.stream().filter(DndChoiceEx::isCorrect).findFirst().orElse(null);

        var isCorrect = matchedAnswer.map(DndChoiceEx::isCorrect).orElse(false);
        var dropZonesCorrect = question.getDetailedItemFeedback() ? closestCorrect.getDropZonesCorrect(answer) : null;
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

        return new ValidationUtils.BiRuleValidator<IsaacDndQuestionEx, DndChoiceEx>()
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
            .add("This question contains an empty answer.", (q, a) -> q.getDndChoices().anyMatch(c -> logged(
                c.getItems() == null || c.getItems().isEmpty(),
                "Expected list of DndItems, but none found in choice for question id (%s)!", q.getId()
            )))
            .add("This question contains at least one invalid answer.", (q, a) -> q.getDndChoices().anyMatch(c -> logged(
                c.getItems().stream().anyMatch(i -> i.getClass() != DndItem.class),
                "Expected list of DndItems, but something else found in choice for question id (%s)!", q.getId()
            )))
            .add("This question contains at least one answer in an unrecognised format.", (q, a) -> q.getDndChoices().anyMatch(c -> logged(
                c.getDndItems().anyMatch(i -> i.getId() == null || i.getDropZoneId() == null || Objects.equals(i.getId(), "") || Objects.equals(i.getDropZoneId(), "")),
                "Found item with missing id or drop zone id in answer for question id (%s)!", q.getId()
            )))
            .add("This question is missing items", (q, a) -> logged(
                q.getItems() == null || q.getItems().isEmpty(),
                "Expected items in question (%s), but didn't find any!", q.getId()
            ))

            // answer
            .add(Constants.FEEDBACK_NO_ANSWER_PROVIDED, (q, a) -> a.getItems() == null || a.getItems().isEmpty())
            .add(Constants.FEEDBACK_UNRECOGNISED_ITEMS,
                 (q, a) -> a.getItems().stream().anyMatch(answerItem -> !q.getItems().contains(answerItem)))
            .add(Constants.FEEDBACK_UNRECOGNISED_FORMAT, (q, a) -> a.getDndItems().anyMatch(i -> i.getId() == null)
                      || a.getDndItems().anyMatch(i -> i.getDropZoneId() == null))
            .add("You did not provide a valid answer; it contains more items than gaps.",
                (q, a) -> a.getItems().size() > q.getAnyCorrect().map(c -> c.getItems().size()).orElse(0))
            .check(IsaacDndQuestionEx.of(question), DndChoiceEx.of(answer));
    }

    private boolean logged(final boolean result, final String message, final Object... args) {
        if (result) {
            log.error(String.format(message, args));
        }
        return result;
    }

    private static class IsaacDndQuestionEx extends IsaacDndQuestion {
        public static IsaacDndQuestionEx of(final Question question) {
            return new IsaacDndQuestionEx((IsaacDndQuestion) question);
        }

        public IsaacDndQuestionEx(final IsaacDndQuestion question) {
            super();
            setItems(question.getItems());
            setId(question.getId());
            setCanonicalSourceFile(question.getCanonicalSourceFile());
            setChoices(question.getChoices());
            setDetailedItemFeedback(question.getDetailedItemFeedback());
            setDefaultFeedback(question.getDefaultFeedback());
        }

        public Stream<DndChoiceEx> getDndChoices() {
            return super.getChoices().stream().map(DndChoiceEx::of);
        }

        @Override
        public Boolean getDetailedItemFeedback() {
            return BooleanUtils.isTrue(super.getDetailedItemFeedback());
        }

        public Optional<DndChoiceEx> getAnyCorrect() {
            return this.getDndChoices().filter(DndChoiceEx::isCorrect).findFirst();
        }
    }

    private static class DndChoiceEx extends DndChoice {
        public static DndChoiceEx of(final Choice c) {
            return new DndChoiceEx((DndChoice) c);
        }

        DndChoiceEx(final DndChoice choice) {
            super();
            setItems(choice.getItems());
            setCorrect(choice.isCorrect());
            setExplanation(choice.getExplanation());
            setType(choice.getType());
        }

        public Stream<DndItem> getDndItems() {
            return super.getItems().stream().map(i -> (DndItem) i);
        }

        public boolean matches(final DndChoiceEx rhs) {
            return this.getDndItems().allMatch(lhsItem -> dropZoneEql(lhsItem, rhs));
        }

        public int countPartialMatchesIn(final DndChoiceEx rhs) {
            return this.getDndItems()
                .map(lhsItem -> dropZoneEql(lhsItem, rhs) ? 1 : 0)
                .mapToInt(Integer::intValue)
                .sum();
        }

        public Map<String, Boolean> getDropZonesCorrect(final DndChoiceEx rhs) {
            return this.getDndItems()
                .filter(lhsItem -> rhs.getItemByDropZone(lhsItem.getDropZoneId()).isPresent())
                .collect(Collectors.toMap(
                    DndItem::getDropZoneId,
                    lhsItem -> dropZoneEql(lhsItem, rhs))
                );
        }

        private static boolean dropZoneEql(final DndItem lhsItem, final DndChoiceEx rhs) {
            return rhs.getItemByDropZone(lhsItem.getDropZoneId())
            .map(rhsItem -> rhsItem.getId().equals(lhsItem.getId()))
            .orElse(false);
        }

        private Optional<DndItem> getItemByDropZone(final String dropZoneId) {
            return this.getDndItems()
            .filter(item -> item.getDropZoneId().equals(dropZoneId))
            .findFirst();
        }
    }
}
