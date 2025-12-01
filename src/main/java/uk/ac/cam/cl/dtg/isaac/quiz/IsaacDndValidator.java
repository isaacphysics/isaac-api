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

import com.google.api.client.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacClozeQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacDndQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.ItemValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;

/**
 * Validator that only provides functionality to validate Cloze questions.
 */
public class IsaacDndValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacClozeValidator.class);
    protected static final String NULL_CLOZE_ITEM_ID = "NULL_CLOZE_ITEM";

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
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
        IsaacDndQuestion dndQuestion = (IsaacDndQuestion) question;
        DndItemChoice userAnswer = (DndItemChoice) answer;
        var correctAnswers = dndQuestion.getChoices().stream().filter(Choice::isCorrect);
        var isCorrect = correctAnswers.anyMatch(correctAnswer -> correctAnswer.matches(userAnswer));
        return new ItemValidationResponse(question.getId(), answer, isCorrect, null, null, new Date());
    }

    @Override
    public List<Choice> getOrderedChoices(final List<Choice> choices) {
        return IsaacItemQuestionValidator.getOrderedChoicesWithSubsets(choices);
    }
}
