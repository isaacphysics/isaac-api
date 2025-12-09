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
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * Validator that only provides functionality to validate Drag and drop questions.
 */
public class IsaacDndValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacDndValidator.class);

    @Override
    public final DndValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        var errorResponse = validateSyntax(question, answer);
        return errorResponse.orElseGet(
            () -> validateMarks((IsaacDndQuestion) question, (DndItemChoice) answer)
        );
    }

    private DndValidationResponse validateMarks(final IsaacDndQuestion question, final DndItemChoice answer) {
        List<DndItemChoice> sortedAnswers = question.getChoices().stream()
            .sorted(Comparator.comparingInt(c -> c.countPartialMatchesIn(answer)))
            .collect(Collectors.toList());

        Optional<DndItemChoice> matchedAnswer = sortedAnswers.stream().filter(lhs -> lhs.matches(answer)).findFirst();

        DndItemChoice closestCorrectAnswer = sortedAnswers.stream().filter(Choice::isCorrect).findFirst().orElse(null);

        var id = question.getId();
        var isCorrect = matchedAnswer.map(Choice::isCorrect).orElse(false);
        var dropZonesCorrect = BooleanUtils.isTrue(question.getDetailedItemFeedback())
            ? closestCorrectAnswer.getDropZonesCorrect(answer)
            : null;
        var explanation = explain(isCorrect, closestCorrectAnswer, question, answer, matchedAnswer);
        var date = new Date();
        return new DndValidationResponse(id, answer, isCorrect, dropZonesCorrect, explanation, date);
    }

    private Content explain(
        final boolean isCorrect, final DndItemChoice correctAnswer, final IsaacDndQuestion question,
        final DndItemChoice answer, final Optional<DndItemChoice> matchedAnswer
    ) {
        return (Content) matchedAnswer.map(Choice::getExplanation).orElseGet(() -> {
            if (isCorrect) {
                return null;
            }
            if (answer.getItems().size() < correctAnswer.getItems().size()) {
                return new Content("You did not provide a valid answer; it does not contain an item for each gap.");
            }
            if (answer.getItems().size() > correctAnswer.getItems().size()) {
                return new Content("You did not provide a valid answer; it contains more items than gaps.");
            }
            return question.getDefaultFeedback();
        });
    }

    private Optional<DndValidationResponse> validateSyntax(final Question question, final Choice answer) {
        Objects.requireNonNull(question);
        Objects.requireNonNull(answer);

        if (!(answer instanceof DndItemChoice)) {
            throw new IllegalArgumentException(String.format(
                "This validator only works with DndItemChoices (%s is not DndItemChoice)", question.getId()));
        }

        if (!(question instanceof IsaacDndQuestion)) {
            throw new IllegalArgumentException(String.format(
                "This validator only works with IsaacDndQuestions (%s is not IsaacDndQuestion)", question.getId()));
        }

        return new ValidatorRules()
            // question
            .add(Constants.FEEDBACK_NO_CORRECT_ANSWERS, (q, a) -> logged(
                q.getChoices() == null || q.getChoices().isEmpty(),
                "Question does not have any answers. %s src: %s", q.getId(), q.getCanonicalSourceFile()
            ))
            .add("This question contains at least one invalid answer.", (q, a) -> q.getChoices().stream().anyMatch(c -> logged(
                !DndItemChoice.class.equals(c.getClass()),
                "Expected DndItem in question (%s), instead found %s!", q.getId(), c.getClass()
            )))
            .add("This question contains an empty answer.", (q, a) -> q.getChoices().stream().anyMatch(c -> logged(
                c.getItems() == null || c.getItems().isEmpty(),
                "Expected list of DndItems, but none found in choice for question id (%s)!", q.getId()
            )))
            .add("This question contains at least one invalid answer.", (q, a) -> q.getChoices().stream().anyMatch(c -> logged(
                c.getItems().stream().anyMatch(i -> i.getClass() != DndItem.class),
                "Expected list of DndItems, but something else found in choice for question id (%s)!", q.getId(), c.getClass()
            )))

            // answer
            .add(Constants.FEEDBACK_NO_ANSWER_PROVIDED, (q, a) -> a.getItems() == null || a.getItems().isEmpty())
            .add(Constants.FEEDBACK_UNRECOGNISED_ITEMS,
                 (q, a) -> a.getItems().stream().anyMatch(answerItem -> !q.getItems().contains(answerItem)))
            .add(Constants.FEEDBACK_UNRECOGNISED_FORMAT,
                  (q, a) -> a.getItems().stream().anyMatch(i -> i.getId() == null)
                      || a.getItems().stream().anyMatch(i -> i.getDropZoneId() == null))
            .check((IsaacDndQuestion) question, (DndItemChoice) answer);
    }

    private boolean logged(final boolean result, final String message, final Object... args) {
        if (result) {
            log.error(String.format(message, args));
        }
        return result;
    }

    private static class ValidatorRules {
        private final List<Rule> rules = new ArrayList<>();

        public ValidatorRules add(final String key, final BiPredicate<IsaacDndQuestion, DndItemChoice> rule) {
            rules.add(new Rule(key, rule));
            return this;
        }

        public Optional<DndValidationResponse> check(final IsaacDndQuestion q, final DndItemChoice a) {
            return rules.stream()
                .filter(r -> r.predicate.test(q, a))
                .map(e -> new DndValidationResponse(q.getId(), a, false, null, new Content(e.message), new Date()))
                .findFirst();
        }

        private static class Rule {
            public final String message;
            public final BiPredicate<IsaacDndQuestion, DndItemChoice> predicate;

            public Rule(final String message, final BiPredicate<IsaacDndQuestion, DndItemChoice> predicate) {
                this.message = message;
                this.predicate = predicate;
            }
        }
    }
}
