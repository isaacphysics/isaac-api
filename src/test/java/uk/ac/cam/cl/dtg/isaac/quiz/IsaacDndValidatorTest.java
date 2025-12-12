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
import uk.ac.cam.cl.dtg.isaac.dos.content.DndChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.apache.logging.log4j.core.Logger;
import uk.ac.cam.cl.dtg.isaac.dos.content.ParsonsChoice;

@RunWith(Theories.class)
@SuppressWarnings("checkstyle:MissingJavadocType")
public class IsaacDndValidatorTest {
    public static final Item item_3cm = item("6d3d", "3 cm");
    public static final Item item_4cm = item("6d3e", "4 cm");
    public static final Item item_5cm = item("6d3f", "5 cm");
    public static final Item item_6cm = item("6d3g", "5 cm");
    public static final Item item_12cm = item("6d3h", "12 cm");
    public static final Item item_13cm = item("6d3i", "13 cm");

    @DataPoints
    public static CorrectnessTestCase[] correctnessTestCases = {
        new CorrectnessTestCase().setTitle("singleCorrectMatch_Correct")
            .setQuestion(correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
            .setAnswer(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
            .expectCorrect(true),
        new CorrectnessTestCase().setTitle("singleIncorrectMatch_Incorrect")
            .setQuestion(correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
            .setAnswer(answer(choose(item_4cm, "leg_1"), choose(item_5cm, "leg_2"), choose(item_3cm, "hypothenuse")))
            .expectCorrect(false),
        new CorrectnessTestCase().setTitle("partialMatchForCorrect_Incorrect")
            .setQuestion(correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
            .setAnswer(answer(choose(item_5cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_3cm, "hypothenuse")))
            .expectCorrect(false),
        new CorrectnessTestCase().setTitle("moreSpecificIncorrectMatchOverridesCorrect_Incorrect")
            .setQuestion(
                correct(choose(item_5cm, "hypothenuse")),
                incorrect(answer(choose(item_3cm, "leg_2"), choose(item_5cm, "hypothenuse")))
            ).setAnswer(answer(choose(item_3cm, "leg_2"), choose(item_5cm, "hypothenuse")))
            .expectCorrect(false),
        new CorrectnessTestCase().setTitle("sameAnswerCorrectAndIncorrect_Correct")
            .setQuestion(incorrect(choose(item_3cm, "leg_1")), correct(choose(item_3cm, "leg_1")))
            .setAnswer(answer(choose(item_3cm, "leg_1")))
            .expectCorrect(true)
    };

    @Theory
    public final void testCorrectness(final CorrectnessTestCase testCase) {
        var response = testValidate(testCase.question, testCase.answer);
        assertEquals(testCase.correct, response.isCorrect());
    }

    // Test that subset match answers return an appropriate explanation
    // TODO: multiple matching explanations
    //  - on same level? (or even across levels?)
    //  - should return all?
    //  - should return just one, but predictably?
    //

    @DataPoints
    public static ExplanationTestCase[] explanationTestCases = {
        new ExplanationTestCase().setTitle("exactMatchIncorrect_shouldReturnMatching")
            .setQuestion(
               correct(choose(item_3cm, "leg_1")),
               incorrect(ExplanationTestCase.testFeedback, choose(item_4cm, "leg_1"))
            ).setAnswer(answer(choose(item_4cm, "leg_1")))
            .expectCorrect(false),
        new ExplanationTestCase().setTitle("exactMatchCorrect_shouldReturnMatching")
            .setQuestion(correct(ExplanationTestCase.testFeedback, choose(item_3cm, "leg_1")))
            .setAnswer(answer(choose(item_3cm, "leg_1")))
            .expectCorrect(true),
        new ExplanationTestCase().setTitle("exactMatchIncorrect_shouldReturnDefaultFeedbackForQuestion")
            .setQuestion(correct(choose(item_3cm, "leg_1")), incorrect(choose(item_4cm, "leg_1")))
            .tapQuestion(q -> q.setDefaultFeedback(ExplanationTestCase.testFeedback))
            .setAnswer(answer(choose(item_4cm, "leg_1")))
            .expectCorrect(false),
        new ExplanationTestCase().setTitle("unMatchedIncorrect_shouldReturnDefaultFeedbackForQuestion")
            .setQuestion(correct(choose(item_3cm, "leg_1")))
            .tapQuestion(q -> q.setDefaultFeedback(ExplanationTestCase.testFeedback))
            .setAnswer(answer(choose(item_4cm, "leg_1")))
            .expectCorrect(false),
        new ExplanationTestCase().setTitle("partialMatchIncorrect_shouldReturnDefaultFeedbackForQuestion")
            .setQuestion(
                correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2")),
                incorrect(new Content("feedback for choice"), choose(item_5cm, "leg_1"), choose(item_6cm, "leg_2"))
            )
            .tapQuestion(q -> q.setDefaultFeedback(new Content("feedback for question")))
            .setAnswer(answer(choose(item_5cm, "leg_1"), choose(item_12cm, "leg_2")))
            .expectCorrect(false)
            .expectExplanation("feedback for question"),
        new ExplanationTestCase().setTitle("defaultCorrect_shouldReturnNone")
            .setQuestion(correct(choose(item_3cm, "leg_1")))
            .setAnswer(answer(choose(item_3cm, "leg_1")))
            .expectNoExplanation()
            .expectCorrect(true)
        // todo expect null explanation on incorrect answer? (check cloze behaviour)
    };


    @Theory
    public final void testExplanation(ExplanationTestCase testCase) {
        var response = testValidate(testCase.question, testCase.answer);
        assertEquals(response.isCorrect(), testCase.correct);
        if (testCase.feedback != null) {
            assertEquals(testCase.feedback.getValue(), response.getExplanation().getValue());
        } else {
            assertNull(response.getExplanation());
        }
    }

    static Supplier<DropZonesTestCase> disabledItemFeedbackNoDropZones = () -> new DropZonesTestCase()
        .tapQuestion(q -> q.setDetailedItemFeedback(false))
        .expectNoDropZones();

    static Supplier<DropZonesTestCase> enabledItemFeedback = () -> new DropZonesTestCase()
        .tapQuestion(q -> q.setDetailedItemFeedback(true));

    @DataPoints
    public static DropZonesTestCase[] dropZonesCorrectTestCases = {
        disabledItemFeedbackNoDropZones.get().setTitle("incorrectNotRequestsed_NotReturned")
            .setQuestion(correct(choose(item_3cm, "leg_1")))
            .setAnswer(answer(choose(item_4cm, "leg_1")))
            .expectCorrect(false),
        disabledItemFeedbackNoDropZones.get().setTitle("correctNotRequested_NotReturned")
            .setQuestion(correct(choose(item_3cm, "leg_1")))
            .setAnswer(answer(choose(item_3cm, "leg_1")))
            .expectCorrect(true),
        enabledItemFeedback.get().setTitle("allCorrect_ShouldReturnAllCorrect")
            .setQuestion(correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
            .setAnswer(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
            .expectCorrect(true)
            .expectDropZonesCorrect(d -> d.setLeg1(true).setLeg2(true).setHypothenuse(true)),
        enabledItemFeedback.get().setTitle("someIncorrect_ShouldReturnWhetherCorrect")
            .setQuestion(correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
            .setAnswer(answer(choose(item_6cm, "leg_1"), choose(item_5cm, "leg_2"), choose(item_5cm, "hypothenuse")))
            .expectCorrect(false)
            .expectDropZonesCorrect(d -> d.setLeg1(false).setLeg2(false).setHypothenuse(true)),
        enabledItemFeedback.get().setTitle("multipleCorrectAnswers_decidesCorrectnessBasedOnClosesMatch")
            .setQuestion(
                correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")),
                correct(choose(item_5cm, "leg_1"), choose(item_12cm, "leg_2"), choose(item_13cm, "hypothenuse"))
            ).setAnswer(answer(choose(item_5cm, "leg_1")))
            .expectCorrect(false)
            .expectDropZonesCorrect(d -> d.setLeg1(true))
    };

    @Theory
    public final void testDropZonesCorrect(final DropZonesTestCase testCase) {
        var response = testValidate(testCase.question, testCase.answer);
        assertEquals(response.isCorrect(), testCase.correct);
        assertEquals(response.getDropZonesCorrect(), testCase.dropZonesCorrect);
    }

    @DataPoints
    public static AnswerValidationTestCase[] answerValidationTestCases = {
        new AnswerValidationTestCase().setTitle("itemsNull")
            .setAnswer(answer())
            .expectExplanation(Constants.FEEDBACK_NO_ANSWER_PROVIDED),
        new AnswerValidationTestCase().setTitle("itemsEmpty")
            .setAnswer(new DndChoice())
            .expectExplanation(Constants.FEEDBACK_NO_ANSWER_PROVIDED),
        new AnswerValidationTestCase().setTitle("itemsNotEnough")
            .setQuestion(correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2")))
            .setAnswer(answer(choose(item_3cm, "leg_1")))
            .expectExplanation("You did not provide a valid answer; it does not contain an item for each gap.")
            .expectDropZonesCorrect(f -> f.setLeg1(true)),
        new AnswerValidationTestCase().setTitle("itemsTooMany")
            .setQuestion(correct(choose(item_3cm, "leg_1")))
            .setAnswer(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2")))
            .expectExplanation("You did not provide a valid answer; it contains more items than gaps.")
            .expectDropZonesCorrect(f -> f.setLeg1(true)),
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
            .expectDropZonesCorrect(f -> f.setLeg1(false))
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

    // TODO: check when a non-existing drop zone was used? (and anything that doesn't exist in a correct answer is invalid?)

    static Supplier<QuestionValidationTestCase> itemUnrecognisedFormatCase = () -> new QuestionValidationTestCase()
        .expectExplanation("This question contains at least one answer in an unrecognised format.")
        .expectLogMessage(q -> String.format("Found item with missing id or drop zone id in answer for question id (%s)!", q.getId()));

    static Supplier<QuestionValidationTestCase> noAnswersTestCase = () -> new QuestionValidationTestCase()
        .expectExplanation(Constants.FEEDBACK_NO_CORRECT_ANSWERS)
        .expectLogMessage(q -> String.format("Question does not have any answers. %s src: %s", q.getId(), q.getCanonicalSourceFile()));

    static Supplier<QuestionValidationTestCase> noCorrectAnswersTestCase = () -> noAnswersTestCase.get()
        .expectLogMessage(q -> String.format("Question does not have any correct answers. %s src: %s", q.getId(), q.getCanonicalSourceFile()));

    static Supplier<QuestionValidationTestCase> answerEmptyItemsTestCase = () -> new QuestionValidationTestCase()
        .expectExplanation("This question contains an empty answer.")
        .expectLogMessage(q -> String.format("Expected list of DndItems, but none found in choice for question id (%s)!", q.getId()));

    static Supplier<QuestionValidationTestCase> questionEmptyAnswersTestCase = () -> new QuestionValidationTestCase()
        .expectExplanation("This question is missing items")
        .expectLogMessage(q -> String.format("Expected items in question (%s), but didn't find any!", q.getId()));

    @DataPoints
    public static QuestionValidationTestCase[] questionValidationTestCases = {
        noAnswersTestCase.get().setTitle("answers empty").setQuestion(q -> q.setChoices(List.of())),
        noAnswersTestCase.get().setTitle("answers null").setQuestion(q -> q.setChoices(null)),
        noCorrectAnswersTestCase.get().setTitle("only incorrect answers")
            .setQuestion(incorrect(choose(item_3cm, "leg_1"))),
        noCorrectAnswersTestCase.get().setTitle("answers without explicit correctness are treated as incorrect")
            .setQuestion(answer(choose(item_3cm, "leg_1"))),
        new QuestionValidationTestCase().setTitle("answer not for a DnD question")
            .setQuestion(q -> q.setChoices(List.of(new ParsonsChoice() {{correct = true; setItems(List.of(new Item("", ""))); }})))
            .expectExplanation("This question contains at least one invalid answer.")
            .expectLogMessage(q -> String.format("Expected DndItem in question (%s), instead found class uk.ac.cam.cl.dtg.isaac.quiz.IsaacDndValidatorTest$1!", q.getId())),
        answerEmptyItemsTestCase.get().setTitle("answer with empty items").setQuestion(correct()),
        answerEmptyItemsTestCase.get().setTitle("answer with null items")
            .setQuestion(q -> q.setChoices(Stream.of(new DndChoice()).peek(c -> c.setCorrect(true)).collect(Collectors.toList()))),
        new QuestionValidationTestCase().setTitle("answer with non-dnd items")
            .setQuestion(correct(new DndItemEx("id", "value", "dropZoneId")))
            .expectExplanation("This question contains at least one invalid answer.")
            .expectLogMessage(q -> String.format("Expected list of DndItems, but something else found in choice for question id (%s)!", q.getId())),
        itemUnrecognisedFormatCase.get().setTitle("answer with missing item_id")
            .setQuestion(correct(new DndItem(null, "value", "dropZoneId"))),
        itemUnrecognisedFormatCase.get().setTitle("answer with empty item_id")
            .setQuestion(correct(new DndItem("", "value", "dropZoneId"))),
        itemUnrecognisedFormatCase.get().setTitle("answer with missing dropZoneId")
            .setQuestion(correct(new DndItem("item_id", "value", null))),
        itemUnrecognisedFormatCase.get().setTitle("answer with empty dropZoneId")
            .setQuestion(correct(new DndItem("item_id", "value", ""))),
        questionEmptyAnswersTestCase.get().setTitle("items is null")
            .tapQuestion(q -> q.setItems(null)),
        questionEmptyAnswersTestCase.get().setTitle("items is empty")
            .tapQuestion(q -> q.setItems(List.of()))
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

    public static DndChoice answer(final DndItem... list) {
        var c = new DndChoice();
        c.setItems(List.of(list));
        c.setType("dndChoice");
        return c;
    }

    public static DndItem choose(final Item item, final String dropZoneId) {
        var value =  new DndItem(item.getId(), item.getValue(), dropZoneId);
        value.setType("dndItem");
        return value;
    }

    public static IsaacDndQuestion createQuestion(final DndChoice... answers) {
        var question = new IsaacDndQuestion();
        question.setId(UUID.randomUUID().toString());
        question.setItems(List.of(item_3cm, item_4cm, item_5cm, item_6cm, item_12cm, item_13cm));
        question.setChoices(List.of(answers));
        question.setType("isaacDndQuestion");
        return question;
    }

    public static DndChoice correct(final DndItem... list) {
        var choice = answer(list);
        choice.setCorrect(true);
        return choice;
    }

    public static DndChoice correct(final ContentBase explanation, final DndItem... list) {
        var choice = correct(list);
        choice.setExplanation(explanation);
        return choice;
    }

    public static DndChoice incorrect(final DndItem... list) {
        var choice = answer(list);
        choice.setCorrect(false);
        return choice;
    }

    public static DndChoice incorrect(final ContentBase explanation, final DndItem... list) {
        var choice = incorrect(list);
        choice.setExplanation(explanation);
        return choice;
    }

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
        public static Content testFeedback = new Content("some test feedback");

        public IsaacDndQuestion question = createQuestion(
            correct(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
        );
        public DndChoice answer = answer();
        public Content feedback = testFeedback;
        public Map<String, Boolean> dropZonesCorrect;
        public String loggedMessage;
        public boolean correct = false;
        private Function<IsaacDndQuestion, String> logMessageOp;
        private Consumer<IsaacDndQuestion> questionOp;

        public T setTitle(final String title) {
            return self();
        }

        public T setQuestion(final DndChoice... choices) {
            this.question = createQuestion(choices);
            return self();
        }

        public T setQuestion(final Consumer<IsaacDndQuestion> op) {
            var question = createQuestion();
            op.accept(question);
            this.question = question;
            return self();
        }

        public T tapQuestion(final Consumer<IsaacDndQuestion> op) {
            this.questionOp = op;
            return self();
        }

        public T setAnswer(final DndChoice answer) {
            this.answer = answer;
            return self();
        }

        public T expectCorrect(final boolean correct) {
            this.correct = correct;
            return self();
        }

        public T expectExplanation(final String feedback) {
            this.feedback = new Content(feedback);
            return self();
        }

        public T expectNoExplanation() {
            this.feedback = null;
            return self();
        }

        public T expectDropZonesCorrect(final UnaryOperator<DropZonesCorrectFactory> op) {
            this.dropZonesCorrect = op.apply(new DropZonesCorrectFactory()).build();
            return self();
        }

        public T expectNoDropZones() {
            this.dropZonesCorrect = null;
            return self();
        }

        public T expectLogMessage(final Function<IsaacDndQuestion, String> op) {
            this.logMessageOp = op;
            return self();
        }

        private T self() {
            if (this.logMessageOp != null) {
                this.loggedMessage = logMessageOp.apply(this.question);
            }
            if (this.questionOp != null) {
                questionOp.accept(this.question);
            }
            return (T) this;
        }
    }

    public static class AnswerValidationTestCase extends TestCase<AnswerValidationTestCase> {}

    public static class QuestionValidationTestCase extends TestCase<QuestionValidationTestCase> {}

    public static class CorrectnessTestCase extends TestCase<CorrectnessTestCase> {}

    public static class ExplanationTestCase extends TestCase<ExplanationTestCase> {}

    public static class DropZonesTestCase extends TestCase<DropZonesTestCase> {}

    public static class DndItemEx extends DndItem {
        public DndItemEx(final String id, final String value, final String dropZoneId) {
            super(id, value, dropZoneId);
        }
    }
}
