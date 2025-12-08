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

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.api.TestAppender;
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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.logging.log4j.core.Logger;

@RunWith(Theories.class)
@SuppressWarnings("checkstyle:MissingJavadocType")
public class IsaacDndValidatorTest {
    public static final Item item_3cm = item("6d3d", "3 cm");
    public static final Item item_4cm = item("6d3e", "4 cm");
    public static final Item item_5cm = item("6d3f", "5 cm");
    public static final Item item_6cm = item("6d3g", "5 cm");
    public static final Item item_12cm = item("6d3h", "12 cm");
    public static final Item item_13cm = item("6d3i", "13 cm");

    @Test
    public final void correctness_singleCorrectMatch_CorrectResponseShouldBeReturned() {
        var question = createQuestion(
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
        );
        var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertTrue(response.isCorrect());
    }

    @Test
    public final void correctness_singleIncorrectMatch_IncorrectResponseShouldBeReturned() {
        var question = createQuestion(
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
        );
        var answer = answer(choose(item_4cm, "leg_1"), choose(item_5cm, "leg_2"), choose(item_3cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertFalse(response.isCorrect());
    }

    @Test
    public final void correctness_partialMatchForCorrect_IncorrectResponseShouldBeReturned() {
        var question = createQuestion(
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
        );
        var answer = answer(choose(item_4cm, "leg_2"), choose(item_5cm, "leg_1"), choose(item_3cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertFalse(response.isCorrect());
    }

    @Test
    public final void correctness_moreSpecificIncorrectMatchOverridesCorrect_IncorrectResponseShouldBeReturned() {
        var question = createQuestion(
            correct(choose(item_5cm, "hypothenuse")),
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
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")),
            incorrect(hypothenuseMustBeLargest, choose(item_3cm, "hypothenuse"))
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
        var question = createQuestion(correct(
            correctFeedback,
            choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")
        ));
        var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"));

        var response = testValidate(question, answer);

        assertTrue(response.isCorrect());
        assertEquals(response.getExplanation(), correctFeedback);
    }

    @Test
    public final void explanation_exactMatchIncorrectDefault_shouldReturnDefault() {
        var defaultFeedback = new Content("Isaac can't help you.");
        var question = createQuestion(
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")),
            incorrect(choose(item_4cm, "hypothenuse"))
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
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
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
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
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
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
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
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
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
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
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
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
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
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")),
            correct(choose(item_5cm, "leg_1"), choose(item_12cm, "leg_2"), choose(item_13cm, "hypothenuse"))
        );
        question.setDetailedItemFeedback(true);
        var answer = answer(choose(item_5cm, "leg_1"));

        var response = testValidate(question, answer);
        assertFalse(response.isCorrect());
        assertEquals(new DropZonesCorrectFactory().setLeg1(true).build(), response.getDropZonesCorrect());
    }

    @DataPoints
    public static AnswerValidationTestCase[] answerValidationTestCases = {
        new AnswerValidationTestCase().setTitle("itemsNull")
            .setAnswer(answer())
            .expectExplanation(Constants.FEEDBACK_NO_ANSWER_PROVIDED),
        new AnswerValidationTestCase().setTitle("itemsEmpty")
            .setAnswer(new DndItemChoice())
            .expectExplanation(Constants.FEEDBACK_NO_ANSWER_PROVIDED),
        new AnswerValidationTestCase().setTitle("itemsNotEnough")
            .setQuestion(correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2")))
            .setAnswer(answer(choose(item_3cm, "leg_1")))
            .expectExplanation("You did not provide a valid answer; it does not contain an item for each gap.")
            .expectDropZonesCorrect(feedback -> feedback.setLeg1(true)),
        new AnswerValidationTestCase().setTitle("itemsTooMany")
            .setQuestion(correct(choose(item_3cm, "leg_1")))
            .setAnswer(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2")))
            .expectExplanation("You did not provide a valid answer; it contains more items than gaps.")
            .expectDropZonesCorrect(feedback -> feedback.setLeg1(true)),
        new AnswerValidationTestCase().setTitle("itemNotOnQuestion")
            .setQuestion(correct(choose(item_3cm, "leg_1")))
            .setAnswer(answer(choose(new Item("bad_id", "some_value"), "leg_1")))
            .expectExplanation(Constants.FEEDBACK_UNRECOGNISED_ITEMS),
        new AnswerValidationTestCase().setTitle("itemMissingId")
            .setQuestion(correct(choose(item_3cm, "leg_1")))
            .setAnswer(answer(choose(new Item(null, null), "leg_1")))
            .expectExplanation(Constants.FEEDBACK_UNRECOGNISED_FORMAT),
        new AnswerValidationTestCase().setTitle("itemMissingDropZoneId")
            .setQuestion(correct(choose(item_3cm, "leg_1")))
            .setAnswer(answer(choose(item_3cm, null)))
            .expectExplanation(Constants.FEEDBACK_UNRECOGNISED_FORMAT),
        new AnswerValidationTestCase().setTitle("itemsNotEnough_providesSpecificExplanationFirst")
            .setQuestion(
                correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")),
                incorrect(new Content("Leg 1 should be less than 4 cm"), choose(item_4cm, "leg_1"))
            ).setAnswer(answer(choose(item_4cm, "leg_1")))
            .expectExplanation("Leg 1 should be less than 4 cm")
            .expectDropZonesCorrect(feedback -> feedback.setLeg1(false))
        // TODO: if drop zone does not exist in question
    };

    @Theory
    public final void testAnswerValidation(final AnswerValidationTestCase testCase) {
        testCase.question.setDetailedItemFeedback(true);

        var response = testValidate(testCase.question, testCase.answer);

        assertFalse(response.isCorrect());
        assertEquals(testCase.feedback.getValue(), response.getExplanation().getValue());
        assertEquals(testCase.dropZonesCorrect, response.getDropZonesCorrect());
    }

    // TODO: when a partial match contains incorrect items, show feedback about this,
    // rather than telling the user they needed to submit more items.

    // TODO: invalid questions that are not producible on the UI should never be marked (still return explanation)

    // TODO: check when a non-existing drop zone was used? (and anything that doesn't exist in a correct answer is invalid?)

    @DataPoints
    public static QuestionValidationTestCase[] questionValidationTestCases = {
        new QuestionValidationTestCase().setTitle("answers empty")
            .setQuestion(q -> q.setChoices(List.of()))
            .expectExplanation(Constants.FEEDBACK_NO_CORRECT_ANSWERS)
            .expectLogMessage(q -> String.format("Question does not have any answers. %s src: %s", q.getId(), q.getCanonicalSourceFile())),
        new QuestionValidationTestCase().setTitle("answers null")
            .setQuestion(q -> q.setChoices(null))
            .expectExplanation(Constants.FEEDBACK_NO_CORRECT_ANSWERS)
            .expectLogMessage(q -> String.format("Question does not have any answers. %s src: %s", q.getId(), q.getCanonicalSourceFile())),
        new QuestionValidationTestCase().setTitle("answer not for a DnD question")
            .setQuestion(q -> q.setChoices(List.of(new DndItemChoiceEx())))
            .expectExplanation("This question contains invalid answers.")
            .expectLogMessage(q -> String.format("Expected DndItem in question (%s), instead found class uk.ac.cam.cl.dtg.isaac.quiz.IsaacDndValidatorTest$DndItemChoiceEx!", q.getId())),
        new QuestionValidationTestCase().setTitle("answer with no items")
            .setQuestion(q -> q.setChoices(List.of(new DndItemChoice())))
            .expectExplanation("This question contains an empty answer.")
            .expectLogMessage(q -> String.format("Expected list of DndItems, but none found in choice for question id (%s)", q.getId()))
    };

    @Theory
    public final void testQuestionValidation(final QuestionValidationTestCase testCase) {
        testCase.question.setDetailedItemFeedback(true);

        var response = testValidate(testCase.question, testCase.answer);
        assertFalse(response.isCorrect());
        assertEquals(testCase.feedback.getValue(), response.getExplanation().getValue());
        assertEquals(testCase.dropZonesCorrect, response.getDropZonesCorrect());

        var appender = testValidateWithLogs(testCase.question, testCase.answer);
        appender.assertLevel(Level.ERROR);
        appender.assertMessage(testCase.loggedMessage);
    }

    // TODO: instead of wrongTypeChoices, assert that each choice has a drop zone id and id

    // TODO: exclude invalid choices from question
    //   - choice without items
    //   - choice with items other than Item (maybe here: DnDItem)
    //   - no correct answer

    private static DndValidationResponse testValidate(final IsaacDndQuestion question, final Choice choice) {
       return new IsaacDndValidator().validateQuestionResponse(question, choice);
    }

    private static TestAppender testValidateWithLogs(final IsaacDndQuestion question, final Choice choice) {
        var appender = new TestAppender();
        Logger logger = (Logger) LogManager.getLogger(IsaacDndValidator.class);
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);

        try {
            testValidate(question, choice);
            return appender;
        } finally {
            logger.removeAppender(new TestAppender());
        }
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

    public static DndItemChoice correct(final DndItem... list) {
        var choice = answer(list);
        choice.setCorrect(true);
        return choice;
    }

    public static DndItemChoice correct(final ContentBase explanation, final DndItem... list) {
        var choice = correct(list);
        choice.setExplanation(explanation);
        return choice;
    }

    public static DndItemChoice incorrect(final DndItem... list) {
        var choice = answer(list);
        choice.setCorrect(false);
        return choice;
    }

    public static DndItemChoice incorrect(final ContentBase explanation, final DndItem... list) {
        var choice = incorrect(list);
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

    static class TestCase<T extends TestCase<T>> {
        public IsaacDndQuestion question = createQuestion(
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
        );
        public DndItemChoice answer = answer();
        public Content feedback;
        public Map<String, Boolean> dropZonesCorrect;
        public String loggedMessage;

        public T setTitle(final String title) {
            return self();
        }

        public T setQuestion(final DndItemChoice... choices) {
            this.question = createQuestion(choices);
            return self();
        }

        public T setQuestion(final Consumer<IsaacDndQuestion> op) {
            var question = createQuestion();
            op.accept(question);
            this.question = question;
            return self();
        }

        public T setAnswer(final DndItemChoice answer) {
            this.answer = answer;
            return self();
        }

        public T expectExplanation(final String feedback) {
            this.feedback = new Content(feedback);
            return self();
        }

        public T expectDropZonesCorrect(UnaryOperator<DropZonesCorrectFactory> op) {
            this.dropZonesCorrect = op.apply(new DropZonesCorrectFactory()).build();
            return self();
        }

        public T expectLogMessage(Function<IsaacDndQuestion, String> op) {
            this.loggedMessage = op.apply(question);
            return self();
        }

        private T self() {
            return (T) this;
        }
    }

    public static class AnswerValidationTestCase extends TestCase<AnswerValidationTestCase> {}

    public static class QuestionValidationTestCase extends TestCase<QuestionValidationTestCase> {}

    public static class DndItemChoiceEx extends DndItemChoice {}
}

