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
import org.apache.logging.log4j.core.Logger;
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
import uk.ac.cam.cl.dtg.isaac.dos.content.ParsonsChoice;

import java.util.ArrayList;
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

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
            .setQuestion(correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2"), item(item_5cm, "hypothenuse")))
            .setAnswer(choice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2"), item(item_5cm, "hypothenuse")))
            .expectCorrect(true),

        new CorrectnessTestCase().setTitle("singleCorrectNotMatch_Incorrect")
            .setQuestion(correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2"), item(item_5cm, "hypothenuse")))
            .setAnswer(choice(item(item_4cm, "leg_1"), item(item_5cm, "leg_2"), item(item_3cm, "hypothenuse")))
            .expectCorrect(false),

        new CorrectnessTestCase().setTitle("singleCorrectPartialMatch_Incorrect")
            .setQuestion(correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2"), item(item_5cm, "hypothenuse")))
            .setAnswer(choice(item(item_5cm, "leg_1"), item(item_4cm, "leg_2"), item(item_3cm, "hypothenuse")))
            .expectCorrect(false),

        new CorrectnessTestCase().setTitle("sameAnswerCorrectAndIncorrect_Correct")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(incorrectChoice(item(item_3cm, "leg_1")), correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(item_3cm, "leg_1")))
            .expectCorrect(true)
    };

    @Theory
    public final void testCorrectness(final CorrectnessTestCase testCase) {
        var response = testValidate(testCase.question, testCase.answer);
        assertEquals(testCase.correct, response.isCorrect());
    }

    @DataPoints
    public static ExplanationTestCase[] explanationTestCases = {
        new ExplanationTestCase().setTitle("exactMatchIncorrect_shouldReturnMatching")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(
               correctChoice(item(item_3cm, "leg_1")),
               incorrectChoice(ExplanationTestCase.testFeedback, item(item_4cm, "leg_1"))
            ).setAnswer(choice(item(item_4cm, "leg_1")))
            .expectCorrect(false)
            .expectExplanation(ExplanationTestCase.testFeedback.getValue()),

        new ExplanationTestCase().setTitle("exactMatchCorrect_shouldReturnMatching")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(ExplanationTestCase.testFeedback, item(item_3cm, "leg_1")))
            .setAnswer(choice(item(item_3cm, "leg_1")))
            .expectCorrect(true)
            .expectExplanation(ExplanationTestCase.testFeedback.getValue()),

        new ExplanationTestCase().setTitle("exactMatchIncorrect_shouldReturnDefaultFeedbackForQuestion")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")), incorrectChoice(item(item_4cm, "leg_1")))
            .tapQuestion(q -> q.setDefaultFeedback(ExplanationTestCase.testFeedback))
            .setAnswer(choice(item(item_4cm, "leg_1")))
            .expectCorrect(false)
            .expectExplanation(ExplanationTestCase.testFeedback.getValue()),

        new ExplanationTestCase().setTitle("matchIncorrectSubset_shouldReturnMatching")
            .setChildren(List.of(new Content("[drop-zone:leg_1] [drop-zone:leg_2]")))
            .setQuestion(
                correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2")),
                incorrectChoice(ExplanationTestCase.testFeedback, item(item_5cm, "leg_1"))
            ).setAnswer(choice(item(item_5cm, "leg_1"), item(item_6cm, "leg_2")))
            .expectCorrect(false)
            .expectExplanation(ExplanationTestCase.testFeedback.getValue()),

        new ExplanationTestCase().setTitle("multiMatchIncorrectSubset_shouldReturnMatching")
            .setChildren(List.of(new Content("[drop-zone:leg_1] [drop-zone:leg_2]")))
            .setQuestion(
                correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2")),
                incorrectChoice(new Content("leg_1 can't be 5"), item(item_5cm, "leg_1")),
                incorrectChoice(new Content("leg_2 can't be 6"), item(item_6cm, "leg_2"))
            ).setAnswer(choice(item(item_5cm, "leg_1"), item(item_6cm, "leg_2")))
            .expectCorrect(false)
            .expectExplanation("leg_1 can't be 5"),

        new ExplanationTestCase().setTitle("unMatchedIncorrect_shouldReturnDefaultFeedbackForQuestion")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .tapQuestion(q -> q.setDefaultFeedback(new Content("default feedback for question")))
            .setAnswer(choice(item(item_4cm, "leg_1")))
            .expectCorrect(false)
            .expectExplanation("default feedback for question"),

        new ExplanationTestCase().setTitle("partialMatchIncorrect_shouldReturnDefaultFeedbackForQuestion")
            .setChildren(List.of(new Content("[drop-zone:leg_1] [drop-zone:leg_2]")))
            .setQuestion(
                correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2")),
                incorrectChoice(new Content("feedback for choice"), item(item_5cm, "leg_1"), item(item_6cm, "leg_2"))
            ).tapQuestion(q -> q.setDefaultFeedback(new Content("default feedback for question")))
            .setAnswer(choice(item(item_5cm, "leg_1"), item(item_12cm, "leg_2")))
            .expectCorrect(false)
            .expectExplanation("default feedback for question"),

        new ExplanationTestCase().setTitle("matchedCorrectWithDefaultFeedback_shouldReturnDefaultFeedback")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .tapQuestion(q -> q.setDefaultFeedback(new Content("default feedback for question")))
            .setAnswer(choice(item(item_3cm, "leg_1")))
            .expectExplanation("default feedback for question")
            .expectCorrect(true),

        new ExplanationTestCase().setTitle("matchedCorrectNoDefaultFeedback_shouldReturnNone")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(item_3cm, "leg_1")))
            .expectNoExplanation()
            .expectCorrect(true),

        new ExplanationTestCase().setTitle("noDefaultIncorrect_shouldReturnNone")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(item_4cm, "leg_1")))
            .expectNoExplanation()
            .expectCorrect(false),

        // these highlight inconsistent behaviour when a question violates our requirements for drop zones
        new ExplanationTestCase().setTitle("unrecognisedDropZone_shouldReturnNone")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(item_3cm, "leg_2")))
            .expectNoExplanation()
            .expectCorrect(false),

        new ExplanationTestCase().setTitle("correctAnswerHasUnusedDropZones_notAcceptedCorrect")
            .setChildren(List.of(new Content("[drop-zone:leg_1] [drop-zone:leg_2]")))
            .setQuestion(
                correctChoice(item(item_3cm, "leg_1")),
                correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2"))
            ).setAnswer(choice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2")))
            .expectCorrect(false)
            .expectExplanation("The question is invalid, because it has an answer with more items than we have gaps.")
            .expectNoDropZones()
    };


    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    @Theory
    public final void testExplanation(final ExplanationTestCase testCase) {
        var response = testValidate(testCase.question, testCase.answer);
        assertEquals(response.isCorrect(), testCase.correct);
        if (testCase.feedback != null) {
            assertNotNull(response.getExplanation());
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
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(item_4cm, "leg_1")))
            .expectCorrect(false),

        disabledItemFeedbackNoDropZones.get().setTitle("correctNotRequested_NotReturned")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(item_3cm, "leg_1")))
            .expectCorrect(true),

        enabledItemFeedback.get().setTitle("allCorrect_ShouldReturnAllCorrect")
            .setQuestion(correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2"), item(item_5cm, "hypothenuse")))
            .setAnswer(choice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2"), item(item_5cm, "hypothenuse")))
            .expectCorrect(true)
            .expectDropZonesCorrect(d -> d.setLeg1(true).setLeg2(true).setHypothenuse(true)),

        enabledItemFeedback.get().setTitle("someIncorrect_ShouldReturnWhetherCorrect")
            .setQuestion(correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2"), item(item_5cm, "hypothenuse")))
            .setAnswer(choice(item(item_6cm, "leg_1"), item(item_5cm, "leg_2"), item(item_5cm, "hypothenuse")))
            .expectCorrect(false)
            .expectDropZonesCorrect(d -> d.setLeg1(false).setLeg2(false).setHypothenuse(true)),

        enabledItemFeedback.get().setTitle("multipleCorrectAnswers_decidesCorrectnessBasedOnClosesMatch")
            .setQuestion(
                correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2"), item(item_5cm, "hypothenuse")),
                correctChoice(item(item_5cm, "leg_1"), item(item_12cm, "leg_2"), item(item_13cm, "hypothenuse"))
            ).setAnswer(choice(item(item_5cm, "leg_1")))
            .expectCorrect(false)
            .expectDropZonesCorrect(d -> d.setLeg1(true)),

        // these highlight inconsistent behaviour when a question violates our requirements for drop zones
        enabledItemFeedback.get().setTitle("unrecognisedDropZone_treatsItAsAnyWrongAnswer")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(item_3cm, "leg_2")))
            .expectCorrect(false)
            .expectDropZonesCorrect(d -> d.setLeg2(false)),

        enabledItemFeedback.get().setTitle("correctAnswerHasUnusedDropZones_acceptedCorrect")
            .setChildren(List.of(new Content("[drop-zone:leg_1] [drop-zone:leg_2]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(item_3cm, "leg_1")))
            .expectCorrect(true)
            .expectDropZonesCorrect(d -> d.setLeg1(true)),
    };

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    @Theory
    public final void testDropZonesCorrect(final DropZonesTestCase testCase) {
        var response = testValidate(testCase.question, testCase.answer);
        assertEquals(testCase.correct, response.isCorrect());
        assertEquals(testCase.dropZonesCorrect, response.getDropZonesCorrect());
    }

    @DataPoints
    public static AnswerValidationTestCase[] answerValidationTestCases = {
        new AnswerValidationTestCase().setTitle("itemsNull")
            .setAnswer(choice())
            .expectExplanation("You provided an empty answer."),

        new AnswerValidationTestCase().setTitle("itemsEmpty")
            .setAnswer(new DndChoice())
            .expectExplanation("You provided an empty answer."),

        new AnswerValidationTestCase().setTitle("itemsNotEnough")
            .setChildren(List.of(new Content("[drop-zone:leg_1] [drop-zone:leg_2]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2")))
            .setAnswer(choice(item(item_3cm, "leg_1")))
            .expectExplanation(IsaacDndValidator.FEEDBACK_ANSWER_NOT_ENOUGH)
            .expectDropZonesCorrect(f -> f.setLeg1(true)),

        new AnswerValidationTestCase().setTitle("itemsTooMany")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(item_3cm, "leg_1"), item(item_4cm, "leg_1")))
            .expectExplanation("You provided an answer with more items than we have gaps."),

        new AnswerValidationTestCase().setTitle("itemNotOnQuestion")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(new Item("bad_id", "some_value"), "leg_1")))
            .expectExplanation("You provided an answer with unrecognised items."),

        new AnswerValidationTestCase().setTitle("itemMissingId")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(new Item(null, null), "leg_1")))
            .expectExplanation("You provided an answer in an unrecognised format."),

        new AnswerValidationTestCase().setTitle("itemMissingDropZoneId")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1")))
            .setAnswer(choice(item(item_3cm, null)))
            .expectExplanation("You provided an answer in an unrecognised format."),

        new AnswerValidationTestCase().setTitle("itemsNotEnough_providesSpecificExplanationFirst")
            .setQuestion(
                correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2"), item(item_5cm, "hypothenuse")),
                incorrectChoice(new Content("Leg 1 should be less than 4 cm"), item(item_4cm, "leg_1"))
            ).setAnswer(choice(item(item_4cm, "leg_1")))
            .expectExplanation("Leg 1 should be less than 4 cm")
            .expectDropZonesCorrect(f -> f.setLeg1(false)),
    };

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    @Theory
    public final void testAnswerValidation(final AnswerValidationTestCase testCase) {
        testCase.question.setDetailedItemFeedback(true);

        var response = testValidate(testCase.question, testCase.answer);

        assertFalse(response.isCorrect());
        assertEquals(testCase.feedback.getValue(), response.getExplanation().getValue());
        assertEquals(testCase.dropZonesCorrect, response.getDropZonesCorrect());
    }

    static Supplier<QuestionValidationTestCase> itemUnrecognisedFormatCase = () -> new QuestionValidationTestCase()
        .expectExplLog("The question is invalid, because it has an answer in an unrecognised format.");
    static Supplier<QuestionValidationTestCase> noAnswersTestCase = () -> new QuestionValidationTestCase()
        .expectExplLog(IsaacDndValidator.FEEDBACK_QUESTION_NO_ANSWERS);
    static Supplier<QuestionValidationTestCase> noCorrectAnswersTestCase = () -> new QuestionValidationTestCase()
        .expectExplLog(Constants.FEEDBACK_NO_CORRECT_ANSWERS);


    @DataPoints
    public static QuestionValidationTestCase[] questionValidationTestCases = {
        noAnswersTestCase.get().setTitle("answersEmpty").setQuestion(q -> q.setChoices(List.of())),

        noAnswersTestCase.get().setTitle("answersNull").setQuestion(q -> q.setChoices(null)),

        noCorrectAnswersTestCase.get().setTitle("answersAllIncorrect")
            .setQuestion(incorrectChoice(item(item_3cm, "leg_1"))),

        noCorrectAnswersTestCase.get().setTitle("answerNoExplicitCorrectness_Incorrect")
            .setQuestion(choice(item(item_3cm, "leg_1"))),

        new QuestionValidationTestCase().setTitle("answerWrongType")
            .setQuestion(q -> q.setChoices(List.of(parsonsChoice())))
            .expectExplLog(IsaacDndValidator.FEEDBACK_QUESTION_INVALID_ANS),

        new QuestionValidationTestCase().setTitle("answerItemsEmpty").setQuestion(correctChoice())
            .expectExplLog("The question is invalid, because it has an empty answer."),

        new QuestionValidationTestCase().setTitle("answerItemsNull")
            .setQuestion(q -> q.setChoices(
                Stream.of(new DndChoice()).peek(c -> c.setCorrect(true)).collect(Collectors.toList())))
            .expectExplLog("The question is invalid, because it has an empty answer."),

        new QuestionValidationTestCase().setTitle("answerItemNonDnd")
            .setQuestion(correctChoice(new DndItemEx("id", "value", "dropZoneId")))
            .expectExplLog("The question is invalid, because it has an invalid answer."),

        itemUnrecognisedFormatCase.get().setTitle("answerItemMissingItemId")
            .setQuestion(correctChoice(new DndItem(null, "value", "dropZoneId"))),

        itemUnrecognisedFormatCase.get().setTitle("answerItemEmptyItemId")
            .setQuestion(correctChoice(new DndItem("", "value", "dropZoneId"))),

        itemUnrecognisedFormatCase.get().setTitle("answerItemMissingDropZoneId")
            .setQuestion(correctChoice(new DndItem("item_id", "value", null))),

        itemUnrecognisedFormatCase.get().setTitle("answerItemEmptyDropZoneId")
            .setQuestion(correctChoice(new DndItem("item_id", "value", ""))),

        new QuestionValidationTestCase().setTitle("itemsNull")
            .tapQuestion(q -> q.setItems(null))
            .expectExplLog(IsaacDndValidator.FEEDBACK_QUESTION_MISSING_ITEMS),

        new QuestionValidationTestCase().setTitle("itemsEmpty")
            .tapQuestion(q -> q.setItems(List.of()))
            .expectExplLog(IsaacDndValidator.FEEDBACK_QUESTION_MISSING_ITEMS),

        new QuestionValidationTestCase().setTitle("answerInvalidItemReference")
            .setChildren(List.of(new Content("[drop-zone:leg_1]")))
            .setQuestion(correctChoice(new DndItem("invalid_id", "some_value", "leg_1")))
            .expectExplLog("The question is invalid, because it has an answer with unrecognised items."),

        new QuestionValidationTestCase().setTitle("answerDuplicateDropZones")
            .setChildren(List.of(new Content("[drop-zone:leg_1] [drop-zone:leg_2]")))
            .setQuestion(correctChoice(item(item_3cm, "leg_1"), item(item_3cm, "leg_1")))
            .expectExplLog("The question is invalid, because it has an answer with duplicate drop zones.")
    };

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
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

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static DndChoice choice(final DndItem... list) {
        var c = new DndChoice();
        c.setItems(List.of(list));
        c.setType("dndChoice");
        return c;
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static DndChoice correctChoice(final DndItem... list) {
        var choice = choice(list);
        choice.setCorrect(true);
        return choice;
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static DndChoice correctChoice(final ContentBase explanation, final DndItem... list) {
        var choice = correctChoice(list);
        choice.setExplanation(explanation);
        return choice;
    }

    private static DndChoice incorrectChoice(final DndItem... list) {
        var choice = choice(list);
        choice.setCorrect(false);
        return choice;
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static DndChoice incorrectChoice(final ContentBase explanation, final DndItem... list) {
        var choice = incorrectChoice(list);
        choice.setExplanation(explanation);
        return choice;
    }

    private static ParsonsChoice parsonsChoice() {
        var parsonsChoice = new ParsonsChoice();
        parsonsChoice.setCorrect(true);
        parsonsChoice.setItems(List.of(new Item("", "")));
        return parsonsChoice;
    }


    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static DndItem item(final Item item, final String dropZoneId) {
        var value = new DndItem(item.getId(), item.getValue(), dropZoneId);
        value.setType("dndItem");
        return value;
    }

    private static Item item(final String id, final String value) {
        Item item = new Item(id, value);
        item.setType("item");
        return item;
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public static IsaacDndQuestion question(final DndChoice... answers) {
        var question = new IsaacDndQuestion();
        question.setId(UUID.randomUUID().toString());
        question.setItems(List.of(item_3cm, item_4cm, item_5cm, item_6cm, item_12cm, item_13cm));
        question.setChoices(List.of(answers));
        question.setType("isaacDndQuestion");
        question.setChildren(List.of(new Content("[drop-zone:leg_1] [drop-zone:leg_2] [drop-zone:hypothenuse]")));
        return question;
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

    public static class TestCase<T extends TestCase<T>> {
        public static Content testFeedback = new Content("some test feedback");

        public String title;
        public IsaacDndQuestion question = question(
            correctChoice(item(item_3cm, "leg_1"), item(item_4cm, "leg_2"), item(item_5cm, "hypothenuse"))
        );
        public DndChoice answer = choice();
        public Content feedback = null;
        public Map<String, Boolean> dropZonesCorrect;
        public List<String> dropZones;
        public String loggedMessage;
        public boolean correct = false;
        private Function<IsaacDndQuestion, String> logMessageOp;
        private final List<Consumer<IsaacDndQuestion>> questionOps = new ArrayList<>();

        public T setTitle(final String title) {
            this.title = title;
            return self();
        }

        public T setQuestion(final DndChoice... choices) {
            this.question = question(choices);
            return self();
        }

        public T setQuestion(final Consumer<IsaacDndQuestion> op) {
            var question = question();
            op.accept(question);
            this.question = question;
            return self();
        }

        public T setChildren(final List<ContentBase> content) {
            this.questionOps.add(q -> q.setChildren(content));
            return self();
        }

        public T tapQuestion(final Consumer<IsaacDndQuestion> op) {
            this.questionOps.add(op);
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

        public T expectExplLog(final String feedback) {
            this.feedback = new Content(feedback);
            this.logMessageOp = q -> {
                String id = q.getId();
                Object[] idFile = { id, q.getCanonicalSourceFile()};
                switch (feedback) {
                    case "The question is invalid, because it has an answer with duplicate drop zones.":
                        return format("Question contains duplicate drop zones. %s src %s", idFile);
                    case "The question is invalid, because it has an answer with unrecognised drop zones.":
                        return format("Question contains invalid drop zone ref. %s src %s", idFile);
                    case "The question is invalid, because it has an answer with unrecognised items.":
                        return format("Question contains invalid item ref. %s src %s", idFile);
                    case "The question is invalid, because it has an invalid answer.":
                        return format(
                            "Expected list of DndItems, but something else found in choice for question id (%s)!", id);
                    case "The question is invalid, because it has an answer with more items than we have gaps.":
                        return format("Question has answer with more items than we have gaps. %s src %s", idFile);
                    case IsaacDndValidator.FEEDBACK_QUESTION_INVALID_ANS:
                        return format("Expected DndItem in question (%s), instead found class uk.ac.cam.cl.dtg."
                                + "isaac.dos.content.ParsonsChoice!", id);
                    case "The question is invalid, because it has an answer in an unrecognised format.":
                        return format("Found item with missing id or drop zone id in answer for question id (%s)!", id);
                    case IsaacDndValidator.FEEDBACK_QUESTION_NO_ANSWERS:
                        return format("Question does not have any answers. %s src: %s", idFile);
                    case Constants.FEEDBACK_NO_CORRECT_ANSWERS:
                        return format("Question does not have any correct answers. %s src: %s", idFile);
                    case "The question is invalid, because it has an empty answer.":
                        return format("Expected list of DndItems, but none found in choice for question id (%s)!", id);
                    case IsaacDndValidator.FEEDBACK_QUESTION_MISSING_ITEMS:
                        return format("Expected items in question (%s), but didn't find any!", id);
                    default: return "CAN'T FIND LOG MESSAGE FOR FEEDBACK";
                }
            };
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

        public T expectDropZones(final String... dropZones) {
            this.dropZones = List.of(dropZones);
            return self();
        }

        @SuppressWarnings("unchecked")
        private T self() {
            this.questionOps.forEach(op -> op.accept(this.question));
            if (this.logMessageOp != null) {
                this.loggedMessage = logMessageOp.apply(this.question);
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
