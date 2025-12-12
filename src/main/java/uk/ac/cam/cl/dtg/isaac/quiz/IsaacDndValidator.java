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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
        List<DndChoice> sortedAnswers = question.getDndChoices().stream()
            .sorted((rhs, lhs) -> {
                int compared = lhs.countPartialMatchesIn(answer) - rhs.countPartialMatchesIn(answer);
                if (compared == 0) {
                    return lhs.isCorrect() && rhs.isCorrect() ? 0 : (lhs.isCorrect() ? 1 : -1);
                }
                return compared;
            })
            .collect(Collectors.toList());
        Optional<DndChoice> matchedAnswer = sortedAnswers.stream().filter(lhs -> lhs.matches(answer)).findFirst();
        DndChoice closestCorrect = sortedAnswers.stream().filter(Choice::isCorrect).findFirst().orElse(null);

        var isCorrect = matchedAnswer.map(Choice::isCorrect).orElse(false);
        var dropZonesCorrect = question.getDetailedItemFeedback() ? closestCorrect.getDropZonesCorrect(answer) : null;
        var feedback = (Content) matchedAnswer.map(Choice::getExplanation).orElseGet(() -> {
            if (isCorrect) {
                return null;
            }
            if (answer.getItems().size() < closestCorrect.getItems().size()) {
                return new Content("You did not provide a valid answer; it does not contain an item for each gap.");
            }
            if (answer.getItems().size() > closestCorrect.getItems().size()) {
                return new Content("You did not provide a valid answer; it contains more items than gaps.");
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
            .add("This question contains an empty answer.", (q, a) -> q.getDndChoices().stream().anyMatch(c -> logged(
                c.getItems() == null || c.getItems().isEmpty(),
                "Expected list of DndItems, but none found in choice for question id (%s)!", q.getId()
            )))
            .add("This question contains at least one invalid answer.", (q, a) -> q.getDndChoices().stream().anyMatch(c -> logged(
                c.getItems().stream().anyMatch(i -> i.getClass() != DndItem.class),
                "Expected list of DndItems, but something else found in choice for question id (%s)!", q.getId()
            )))
            .add("This question contains at least one answer in an unrecognised format.", (q, a) -> q.getDndChoices().stream().anyMatch(c -> logged(
                c.getItems().stream().anyMatch(i -> i.getId() == null || i.getDropZoneId() == null || Objects.equals(i.getId(), "") || Objects.equals(i.getDropZoneId(), "")),
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
            .add(Constants.FEEDBACK_UNRECOGNISED_FORMAT,
                  (q, a) -> a.getItems().stream().anyMatch(i -> i.getId() == null)
                      || a.getItems().stream().anyMatch(i -> i.getDropZoneId() == null))
            .check((IsaacDndQuestion) question, (DndChoice) answer);
    }

    private boolean logged(final boolean result, final String message, final Object... args) {
        if (result) {
            log.error(String.format(message, args));
        }
        return result;
    }
}
