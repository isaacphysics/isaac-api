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

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacReorderQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ParsonsChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ParsonsItem;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class IsaacReorderValidatorTest {
    private IsaacReorderValidator validator;
    private IsaacReorderQuestion someReorderQuestion;
    // Some items:
    private final Item item1 = new Item("id001", "A");
    private final Item item2 = new Item("id002", "B");
    private final Item item3 = new Item("id003", "C");

    private final String incorrectExplanation = "INCORRECT";
    private final String subsetMatchExplanation = "SUBSET";
    private final String defaultExplanation = "DEFAULT";

    /**
     * Initial configuration of tests.
     */
    @BeforeEach
    public final void setUp() {
        validator = new IsaacReorderValidator();

        // Set up the question object:
        someReorderQuestion = new IsaacReorderQuestion();

        List<Choice> answerList = Lists.newArrayList();
        ItemChoice someIncorrectChoice = new ItemChoice();
        ItemChoice someSubsetChoice = new ItemChoice();
        ItemChoice someCorrectAnswer = new ItemChoice();
        someReorderQuestion.setItems(ImmutableList.of(item1, item2, item3));

        // Correct and incorrect choices the same:
        someCorrectAnswer.setItems(ImmutableList.of(item1, item2, item3));
        someCorrectAnswer.setCorrect(true);
        someIncorrectChoice.setItems(ImmutableList.of(item3, item2, item1));
        someIncorrectChoice.setCorrect(false);
        someIncorrectChoice.setAllowSubsetMatch(false);
        someIncorrectChoice.setExplanation(new Content(incorrectExplanation));
        someSubsetChoice.setItems(ImmutableList.of(item1, item3));
        someSubsetChoice.setAllowSubsetMatch(true);
        someSubsetChoice.setExplanation(new Content(subsetMatchExplanation));

        // Add both choices to question, incorrect first:
        answerList.add(someIncorrectChoice);
        answerList.add(someSubsetChoice);
        answerList.add(someCorrectAnswer);
        someReorderQuestion.setChoices(answerList);
    }

    /*
        Test that correct answers are recognised.
    */
    @Test
    public final void isaacReorderValidator_CorrectItems_CorrectResponseShouldBeReturned() {
        // Set up user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1, item2, item3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someReorderQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
        Test that incorrect answers are not recognised.
    */
    @Test
    public final void isaacReorderValidator_IncorrectItems_IncorrectResponseShouldBeReturned() {
        // Set up user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item2, item1, item3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someReorderQuestion, c);
        assertFalse(response.isCorrect());
    }

    /*
        Test that subset match answers can be matched.
    */
    @Test
    public final void isaacReorderValidator_UserSubsetMatch_IncorrectResponseShouldBeReturned() {
        // Set up user answer with subset at end of match:
        ItemChoice c1 = new ItemChoice();
        c1.setItems(ImmutableList.of(item2, item1, item3));

        // Test response:
        QuestionValidationResponse response1 = validator.validateQuestionResponse(someReorderQuestion, c1);
        assertFalse(response1.isCorrect());
        assertEquals(subsetMatchExplanation, response1.getExplanation().getValue());

        // Set up user answer with subset at start:
        ItemChoice c2 = new ItemChoice();
        c2.setItems(ImmutableList.of(item1, item3, item2));

        // Test response:
        QuestionValidationResponse response2 = validator.validateQuestionResponse(someReorderQuestion, c2);
        assertFalse(response2.isCorrect());
        assertEquals(subsetMatchExplanation, response2.getExplanation().getValue());
    }

    /*
        Test that known incorrect answers can be matched.
    */
    @Test
    public final void isaacReorderValidator_KnownIncorrect_IncorrectResponseShouldBeReturned() {
        // Set up user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item3, item2, item1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someReorderQuestion, c);
        assertFalse(response.isCorrect());
        assertEquals(incorrectExplanation, response.getExplanation().getValue());
    }

    /*
        Test that answers which are too short get feedback.
    */
    @Test
    public final void isaacReorderValidator_NotEnoughItems_IncorrectResponseShouldBeReturned() {
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someReorderQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("does not contain enough items"));
    }

    /*
        Test that answers which are too short get feedback.
     */
    @Test
    public final void isaacReorderValidator_TooManyItems_IncorrectResponseShouldBeReturned() {
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1, item2, item3, item1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someReorderQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("contains too many items"));
    }

    /*
        Test that answers with unknown items are rejected.
    */
    @Test
    public final void isaacReorderValidator_UnknownItems_IncorrectResponseShouldBeReturned() {
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1, new Item("unknown", null)));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someReorderQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("unrecognised items"));
    }

    /*
        Test that answers with no items are rejected.
    */
    @Test
    public final void isaacReorderValidator_NoUserItems_IncorrectResponseShouldBeReturned() {
        ItemChoice c = new ItemChoice();

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someReorderQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("did not provide an answer"));
    }

    /*
        Test that answers with incorrect type items are rejected.
    */
    @Test
    public final void isaacReorderValidator_IncorrectTypeUserItems_IncorrectResponseShouldBeReturned() {
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(new ParsonsItem("id001", null, null)));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someReorderQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("not in a recognised format"));
    }

    /*
        Test that default feedback works.
    */
    @Test
    public final void isaacReorderValidator_DefaultFeedbackReturned() {
        // Set up the question object:
        IsaacReorderQuestion reorderQuestion = new IsaacReorderQuestion();
        reorderQuestion.setDefaultFeedback(new Content(defaultExplanation));
        reorderQuestion.setItems(ImmutableList.of(item1, item2, item3));

        ItemChoice someCorrectAnswer = new ItemChoice();
        someCorrectAnswer.setItems(ImmutableList.of(item1));
        someCorrectAnswer.setCorrect(true);
        reorderQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

        // Set up user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item3));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(reorderQuestion, c);
        assertFalse(response.isCorrect());
        assertEquals(response.getExplanation().getValue(), defaultExplanation);
    }

    //  ---------- Tests from here test invalid questions themselves ----------

    /*
     Test that missing choices are detected.
    */
    @Test
    public final void isaacReorderValidator_NoChoices_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacReorderQuestion reorderQuestion = new IsaacReorderQuestion();
        reorderQuestion.setItems(ImmutableList.of(item1, item2, item3));

        reorderQuestion.setChoices(Lists.newArrayList());

        // Set up user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(reorderQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("not have any correct answers"));
    }

    /*
        Test that wrong choice types are detected.
    */
    @Test
    public final void isaacReorderValidator_WrongTypeChoices_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacReorderQuestion reorderQuestion = new IsaacReorderQuestion();
        ParsonsItem parsonsItem = new ParsonsItem("id001", null, null);
        reorderQuestion.setItems(ImmutableList.of(parsonsItem));

        ItemChoice someCorrectAnswer = new ParsonsChoice();
        someCorrectAnswer.setItems(ImmutableList.of(parsonsItem));
        someCorrectAnswer.setCorrect(true);
        reorderQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

        // Set up user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(reorderQuestion, c);
        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    /*
     Test that missing items are detected.
    */
    @Test
    public final void isaacReorderValidator_NoItems_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacReorderQuestion reorderQuestion = new IsaacReorderQuestion();
        reorderQuestion.setItems(ImmutableList.of(item1, item2, item3));

        ItemChoice someCorrectAnswer = new ItemChoice();
        someCorrectAnswer.setItems(Collections.emptyList());
        someCorrectAnswer.setCorrect(true);
        reorderQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

        // Set up user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(reorderQuestion, c);
        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    /*
     Test that missing items are detected.
    */
    @Test
    public final void isaacReorderValidator_NoItemsInQuestion_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacReorderQuestion reorderQuestion = new IsaacReorderQuestion();

        ItemChoice someCorrectAnswer = new ItemChoice();
        someCorrectAnswer.setItems(ImmutableList.of(item1));
        someCorrectAnswer.setCorrect(true);
        reorderQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

        // Set up user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(reorderQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("does not have any items"));
    }

    /*
     Test that incorrect item types are detected.
    */
    @Test
    public final void isaacReorderValidator_ParsonsItems_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacReorderQuestion reorderQuestion = new IsaacReorderQuestion();
        reorderQuestion.setItems(ImmutableList.of(item1, item2, item3));

        ItemChoice someCorrectAnswer = new ItemChoice();
        someCorrectAnswer.setItems(ImmutableList.of(new ParsonsItem("id001", null, null)));
        someCorrectAnswer.setCorrect(true);
        reorderQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

        // Set up user answer:
        ItemChoice c = new ItemChoice();
        c.setItems(ImmutableList.of(item1));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(reorderQuestion, c);
        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    /*
     Test that incorrect question types are detected.
    */
    @Test
    public final void isaacReorderValidator_WrongQuestionType_ExceptionShouldBeThrown() {
        IsaacQuickQuestion invalidQuestionType = new IsaacQuickQuestion();
        invalidQuestionType.setId("invalidQuestionType");

        // This should throw an exception:
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validateQuestionResponse(invalidQuestionType, new ItemChoice());
        });
        assertTrue(exception.getMessage().contains("only works with IsaacReorderQuestions"));
    }

    /*
     Test that incorrect submitted choice types are detected.
    */
    @Test
    public final void isaacReorderValidator_WrongChoiceType_ExceptionShouldBeThrown() {
        IsaacReorderQuestion reorderQuestion = new IsaacReorderQuestion();

        // This should throw an exception:
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            validator.validateQuestionResponse(reorderQuestion, new Choice());
        });
        assertTrue(exception.getMessage().contains("Expected ItemChoice for IsaacReorderQuestion"));
    }
}
