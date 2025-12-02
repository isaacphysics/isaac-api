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
import uk.ac.cam.cl.dtg.isaac.dos.DndValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacDndQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

/**
 * Validator that only provides functionality to validate Drag and drop questions.
 */
public class IsaacDndValidator implements IValidator {
    @Override
    public final DndValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        validate(question, answer);
        return mark((IsaacDndQuestion) question, (DndItemChoice) answer);
    }

    private void validate(final Question question, final Choice answer) {
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
    }

    private DndValidationResponse mark(final IsaacDndQuestion question, final DndItemChoice answer) {
        DndItemChoice matchedAnswer = question.getChoices().stream()
            .sorted(Comparator.comparingInt(c -> c.countPartialMatchesIn(answer)))
            .filter(choice -> choice.matches(answer))
            .findFirst()
            .orElse(incorrectAnswer(question));

        DndItemChoice closestCorrectAnswer = question.getChoices().stream()
            .filter(Choice::isCorrect)
            .findFirst()
            .orElse(null);

        return new DndValidationResponse(
            question.getId(),
            answer,
            matchedAnswer.isCorrect(),
            BooleanUtils.isTrue(question.getDetailedItemFeedback()) ? closestCorrectAnswer.getDropZonesCorrect(answer) : null,
            (Content) matchedAnswer.getExplanation(),
            new Date()
        );
    }

    private DndItemChoice incorrectAnswer(final IsaacDndQuestion question) {
        var choice = new DndItemChoice();
        choice.setCorrect(false);
        choice.setExplanation(question.getDefaultFeedback());
        return choice;
    }
}
