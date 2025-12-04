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

import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.dos.DndValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacDndQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class IsaacDndValidatorTest {
    @Test
    public final void correctness_singleCorrectMatch_CorrectResponseShouldBeReturned() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertTrue(response.isCorrect());
    }

    @Test
    public final void correctness_singleIncorrectMatch_IncorrectResponseShouldBeReturned() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        var answer = answer(choose(item_4cm, "leg_1"), choose(item_5cm, "leg_2"), choose(item_3cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertFalse(response.isCorrect());
    }

    @Test
    public final void correctness_partialMatchForCorrect_IncorrectResponseShouldBeReturned() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        var answer = answer(choose(item_4cm, "leg_2"), choose(item_5cm, "leg_1"), choose(item_3cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertFalse(response.isCorrect());
    }

    @Test
    public final void correctness_moreSpecificIncorrectMatchOverridesCorrect_IncorrectResponseShouldBeReturned() {
        var question = createQuestion(
            correct(answer(choose(item_5cm, "hypothenuse"))),
            incorrect(answer(choose(item_3cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        var answer = answer(choose(item_3cm, "leg_2"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertFalse(response.isCorrect());
    }

    // Test that subset match answers return an appropriate explanation
    // TODO: correct-incorrect contradiction among levels should be invalid question (during ETL?)
    //  - James says we should just accept as correct when contradiction
    // TODO: multiple matching explanations
    //  - on same level? (or even across levels?)
    //  - should return all?
    //  - should return just one, but predictably?
    //
    @Test
    public final void explanation_exactMatchIncorrect_shouldReturnMatching() {
        var hypothenuseMustBeLargest = new Content("The hypothenuse must be the longest side of a right triangle");
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))),
            incorrect(answer(choose(item_3cm, "hypothenuse")), hypothenuseMustBeLargest)
        );
        var answer = answer(choose(item_3cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertFalse(response.isCorrect());
        assertEquals(response.getExplanation(), hypothenuseMustBeLargest);
    }

    @Test
    public final void explanation_exactMatchCorrect_shouldReturnMatching() {
        var correctFeedback = new Content("That's how it's done! Observe that the hypothenuse is always the longest"
            + " side of a right triangle");
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")),
            correctFeedback)
        );
        var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertTrue(response.isCorrect());
        assertEquals(response.getExplanation(), correctFeedback);
    }

    @Test
    public final void explanation_exactMatchIncorrectDefault_shouldReturnDefault() {
        var defaultFeedback = new Content("Isaac can't help you.");
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))),
            incorrect(answer(choose(item_4cm, "hypothenuse")))
        );
        question.setDefaultFeedback(defaultFeedback);
        var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_4cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertFalse(response.isCorrect());
        assertEquals(response.getExplanation(), defaultFeedback);
    }

    @Test
    public final void explanation_defaultIncorrect_shouldReturnDefault() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        var defaultFeedback = new Content("Isaac cannot help you.");
        question.setDefaultFeedback(defaultFeedback);
        var answer = answer(choose(item_4cm, "leg_1"), choose(item_5cm, "leg_2"), choose(item_3cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertFalse(response.isCorrect());
        assertEquals(response.getExplanation(), defaultFeedback);
    }

    @Test
    public final void explanation_defaultCorrect_shouldReturnNone() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        var defaultFeedback = new Content("Isaac cannot help you.");
        question.setDefaultFeedback(defaultFeedback);
        var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertTrue(response.isCorrect());
        assertNull(response.getExplanation());
    }

    @Test
    public final void dropZonesCorrect_incorrectNotRequested_shouldReturnNull() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        question.setDetailedItemFeedback(false);
        var answer = answer(choose(item_3cm, "leg_2"), choose(item_4cm, "leg_1"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertNull(response.getDropZonesCorrect());
    }

    @Test
    public final void dropZonesCorrect_correctNotRequested_shouldReturnNull() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        question.setDetailedItemFeedback(false);
        var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, answer);
        assertTrue(response.isCorrect());
        assertNull(response.getDropZonesCorrect());
    }

    @Test
    public final void dropZonesCorrect_allCorrect_shouldReturnAllCorrect() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        question.setDetailedItemFeedback(true);
        var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, answer);
        assertTrue(response.isCorrect());
        assertEquals(
            new DropZonesCorrectFactory().setLeg1(true).setLeg2(true).setHypothenuse(true).build(),
            response.getDropZonesCorrect()
        );
    }

    @Test
    public final void dropZonesCorrect_someIncorrect_shouldReturnWhetherCorrect() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        question.setDetailedItemFeedback(true);
        var answer = answer(choose(item_6cm, "leg_1"), choose(item_5cm, "leg_2"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertEquals(
            new DropZonesCorrectFactory().setLeg1(false).setLeg2(false).setHypothenuse(true).build(),
            response.getDropZonesCorrect()
        );
    }

    @Test
    public final void dropZonesCorrect_multipleCorrectAnswers_decidesCorrectnessBasedOnClosestOne() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))),
            correct(answer(choose(item_5cm, "leg_1"), choose(item_12cm, "leg_2"), choose(item_13cm, "hypothenuse")))
        );
        question.setDetailedItemFeedback(true);
        var answer = answer(choose(item_5cm, "leg_1"));

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertEquals(new DropZonesCorrectFactory().setLeg1(true).build(), response.getDropZonesCorrect());
    }

    @Test
    public final void answerValidation_empty_incorrect() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        question.setDetailedItemFeedback(true);
        var answer = answer();

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertEquals(new Content(Constants.FEEDBACK_NO_ANSWER_PROVIDED), response.getExplanation());
        assertNull(response.getDropZonesCorrect());
    }

    @Test
    public final void answerValidation_emptyNoItems_incorrect() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        question.setDetailedItemFeedback(true);
        var answer = new DndItemChoice();

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertEquals(new Content(Constants.FEEDBACK_NO_ANSWER_PROVIDED), response.getExplanation());
        assertNull(response.getDropZonesCorrect());
    }

    @Test
    public final void answerValidation_someMissing_explainsMissingItems() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
        );
        question.setDetailedItemFeedback(true);
        var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"));

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("does not contain an item for each gap"));
        assertEquals(new DropZonesCorrectFactory().setLeg1(true).setLeg2(true).build(), response.getDropZonesCorrect());
    }

    /*
     * Test that when the user submits an answer with missing items, we first show any matching feedback
     * about the incorrect answer, rather than the more generic feedback about missing items.
     *
     * Cloze questions don't even look at matches in that case, but I think this is better UX.
     */
    @Test
    public final void answerValidation_someMissing_providesSpecificExplanationFirst() {
        var incorrectFeedback = new Content("Leg 1 should be less than 4 cm");
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))),
            incorrect(answer(choose(item_4cm, "leg_1")), incorrectFeedback)
        );
        question.setDetailedItemFeedback(true);
        var answer = answer(choose(item_4cm, "leg_1"));

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertEquals(incorrectFeedback, response.getExplanation());
        assertEquals(new DropZonesCorrectFactory().setLeg1(false).build(), response.getDropZonesCorrect());
    }

    // TODO: when a partial match contains incorrect items, show feedback about this,
    // rather than telling the user they needed to submit more items.

    // TODO: invalid questions that are not producible on the UI should never be marked (still return explanation)
    @Test
    public final void answerValidation_tooMany_explainsTooManyItems() {
        var question = createQuestion(
            correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2")))
        );
        question.setDetailedItemFeedback(true);
        var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("it contains more items than gaps"));
        assertEquals(new DropZonesCorrectFactory().setLeg1(true).setLeg2(true).build(), response.getDropZonesCorrect());
    }

    @Test
    public final void answerValidation_unknownItems_explainsUnknownItems() {
        var question = createQuestion(correct(answer(choose(item_3cm, "leg_1"))));
        question.setDetailedItemFeedback(true);
        var answer = answer(choose(new Item("bad_id", "some_value"), "leg_1"));

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertEquals(new Content(Constants.FEEDBACK_UNRECOGNISED_ITEMS), response.getExplanation());
        assertEquals(null, response.getDropZonesCorrect());
    }

    @Test
    public final void answerValidation_missingId_explainsUnrecognisedFormat() {
        var question = createQuestion(correct(answer(choose(item_3cm, "leg_1"))));
        question.setDetailedItemFeedback(true);
        var answer = answer(choose(new Item(null, null), "leg_1"));

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertEquals(new Content(Constants.FEEDBACK_UNRECOGNISED_FORMAT), response.getExplanation());
        assertNull(response.getDropZonesCorrect());
    }

    @Test
    public final void answerValidation_missingDropZoneId_explainsUnrecognisedFormat() {
        var question = createQuestion(correct(answer(choose(item_3cm, "leg_1"))));
        question.setDetailedItemFeedback(true);
        var answer = answer(choose(item_3cm, null));

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertEquals(new Content(Constants.FEEDBACK_UNRECOGNISED_FORMAT), response.getExplanation());
        assertNull(response.getDropZonesCorrect());
    }

//    @Test
//    public final void answerValidation_wrongItems_explainsIncorrectItems() {
//        var question = createQuestion(correct(answer(choose(item_3cm, "leg_1"))));
//        question.setDetailedItemFeedback(true);
//        var item = new ParsonsItem(item_3cm.getId(), null, null);
//        var answer = new ItemChoice();
//        answer.setItems(List.of(item));
//
//        var response = testValidate(question, answer);
//        assertFalse(response.isCorrect());
//        assertEquals(new Content(Constants.FEEDBACK_UNRECOGNISED_FORMAT), response.getExplanation());
//        assertEquals(new DropZonesCorrectFactory().setLeg1(false).build(), response.getDropZonesCorrect());
//    }

    // TODO: check when a non-existing drop zone was used? (and anything that doesn't exist in a correct answer is invalid?)

    private static DndValidationResponse testValidate(final IsaacDndQuestion question, final Choice choice) {
        return new IsaacDndValidator().validateQuestionResponse(question, choice);
    }

    @SuppressWarnings("checkstyle:MissingJavadocType")
    public static DndItemChoice answer(final DndItem... list) {
        var c = new DndItemChoice();
        c.setItems(List.of(list));
        c.setType("dndChoice");
        return c;
    }

    @SuppressWarnings("checkstyle:MissingJavadocType")
    public static DndItem choose(final Item item, final String dropZoneId) {
        var value =  new DndItem(item.getId(), item.getValue(), dropZoneId);
        value.setType("dndItem");
        return value;
    }

    @SuppressWarnings("checkstyle:MissingJavadocType")
    public static IsaacDndQuestion createQuestion(final DndItemChoice... answers) {
        var question = new IsaacDndQuestion();
        question.setId(UUID.randomUUID().toString());
        question.setItems(List.of(item_3cm, item_4cm, item_5cm, item_6cm, item_12cm, item_13cm));
        question.setChoices(List.of(answers));
        question.setType("isaacDndQuestion");
        return question;
    }

    public static DndItemChoice correct(final DndItemChoice choice) {
        choice.setCorrect(true);
        return choice;
    }

    public static DndItemChoice correct(final DndItemChoice choice, ContentBase explanation) {
        choice.setCorrect(true);
        choice.setExplanation(explanation);
        return choice;
    }

    public static DndItemChoice incorrect(final DndItemChoice choice) {
        choice.setCorrect(false);
        return choice;
    }

    public static DndItemChoice incorrect(final DndItemChoice choice, ContentBase explanation) {
        choice.setCorrect(false);
        choice.setExplanation(explanation);
        return choice;
    }

    @SuppressWarnings("checkstyle:MissingJavadocType")
    public static Item item(final String id, final String value) {
        Item item = new Item(id, value);
        item.setType("item");
        return item;
    }

    public static class DropZonesCorrectFactory {
        private final Map<String, Boolean> map = new HashMap<>();

        public DropZonesCorrectFactory setLeg1(final Boolean value) {
            map.put("leg_1", value);
            return this;
        }

        public DropZonesCorrectFactory setLeg2(final boolean value) {
            map.put("leg_2", value);
            return this;
        }

        public DropZonesCorrectFactory setHypothenuse(final boolean value) {
            map.put("hypothenuse", value);
            return this;
        }

        public Map<String, Boolean> build() {
            return map;
        }
    }

    public static final Item item_3cm = item("6d3d", "3 cm");
    public static final Item item_4cm = item("6d3e", "4 cm");
    public static final Item item_5cm = item("6d3f", "5 cm");
    public static final Item item_6cm = item("6d3g", "5 cm");
    public static final Item item_12cm = item("6d3h", "12 cm");
    public static final Item item_13cm = item("6d3i", "13 cm");
}

