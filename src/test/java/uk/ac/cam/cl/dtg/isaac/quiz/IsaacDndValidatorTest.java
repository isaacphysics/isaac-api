/*
 * Copyright 2022 James Sharkey
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

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacDndQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class IsaacDndValidatorTest {

    /*
        Test that correct answers are recognised.
    */
    @Test
    public final void correctItems_CorrectResponseShouldBeReturned() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        var choices = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, choices);

        assertTrue(response.isCorrect());
    }

    /*
        Test that incorrect answers are not recognised.
    */
    @Test
    public final void incorrectItems_IncorrectResponseShouldBeReturned() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        var choices = answer(choose(item_4cm, "leg_1"), choose(item_5cm, "leg_2"), choose(item_3cm, "hypothenuse"));

        var response = testValidate(question, choices);

        assertFalse(response.isCorrect());
    }

    private static QuestionValidationResponse testValidate(final IsaacDndQuestion question, final Choice choice) {
        return new IsaacDndValidator().validateQuestionResponse(question, choice);
    }

    private static DndItemChoice answer(final DndItem... list) {
        var c = new DndItemChoice();
        c.setItems(List.of(list));
        return c;
    }

    private static DndItem choose(final Item item, final String str) {
        return new DndItem(item.getId(), item.getValue(), str);
    }

    private static IsaacDndQuestion createQuestion(final DndItemChoice... answers) {
        var question = new IsaacDndQuestion();
        question.setItems(List.of(item_3cm, item_4cm, item_5cm, item_6cm));
        question.setChoices(List.of(answers));
        return question;
    }

    private static DndItemChoice correct(final DndItemChoice choice) {
        choice.setCorrect(true);
        return choice;
    }

    private static final Item item_3cm = new Item("6d3d", "3 cm");
    private static final Item item_4cm = new Item("6d3e", "4 cm");
    private static final Item item_5cm = new Item("6d3f", "5 cm");
    private static final Item item_6cm = new Item("6d3g", "5 cm");
}
