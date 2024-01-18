/**
 * Copyright 2022 James Sharkey
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.quiz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacClozeQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.ItemValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ParsonsChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ParsonsItem;

class IsaacClozeValidatorTest {
  private IsaacClozeValidator validator;
  private IsaacClozeQuestion someClozeQuestion;
  // Some items:
  private final Item item1 = new Item("id001", "A");
  private final Item item2 = new Item("id002", "B");
  private final Item item3 = new Item("id003", "C");
  private final Item nullPlaceholder = new Item(IsaacClozeValidator.NULL_CLOZE_ITEM_ID, null);

  private final String incorrectExplanation = "INCORRECT";
  private final String subsetMatchExplanation = "SUBSET";
  private final String defaultExplanation = "DEFAULT";

  /**
   * Initial configuration of tests.
   */
  @BeforeEach
  public final void setUp() {
    validator = new IsaacClozeValidator();

    // Set up the question object:
    someClozeQuestion = new IsaacClozeQuestion();

    List<Choice> answerList = Lists.newArrayList();
    ItemChoice someIncorrectChoice = new ItemChoice();
    ItemChoice someSubsetChoice = new ItemChoice();
    ItemChoice someCorrectAnswer = new ItemChoice();
    someClozeQuestion.setItems(ImmutableList.of(item1, item2, item3));

    // Correct and incorrect choices the same:
    someCorrectAnswer.setItems(ImmutableList.of(item1, item3));
    someCorrectAnswer.setCorrect(true);
    someIncorrectChoice.setItems(ImmutableList.of(item1, item2));
    someIncorrectChoice.setCorrect(false);
    someIncorrectChoice.setAllowSubsetMatch(false);
    someIncorrectChoice.setExplanation(new Content(incorrectExplanation));
    someSubsetChoice.setItems(ImmutableList.of(nullPlaceholder, item3));
    someSubsetChoice.setAllowSubsetMatch(true);
    someSubsetChoice.setExplanation(new Content(subsetMatchExplanation));

    // Add both choices to question, incorrect first:
    answerList.add(someIncorrectChoice);
    answerList.add(someSubsetChoice);
    answerList.add(someCorrectAnswer);
    someClozeQuestion.setChoices(answerList);
  }

  /*
      Test that correct answers are recognised.
  */
  @Test
  final void isaacClozeValidator_CorrectItems_CorrectResponseShouldBeReturned() {
    // Set up user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1, item3));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someClozeQuestion, c);
    assertTrue(response.isCorrect());
  }

  /*
      Test that incorrect answers are not recognised.
  */
  @Test
  final void isaacClozeValidator_IncorrectItems_IncorrectResponseShouldBeReturned() {
    // Set up user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item2, item3));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someClozeQuestion, c);
    assertFalse(response.isCorrect());
  }

  /*
      Test that subset match answers can be matched.
  */
  @Test
  final void isaacClozeValidator_UserSubsetMatch_IncorrectResponseShouldBeReturned() {
    // Set up user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item2, item3));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someClozeQuestion, c);
    assertFalse(response.isCorrect());
    assertEquals(subsetMatchExplanation, response.getExplanation().getValue());
  }

  /*
      Test that known incorrect answers can be matched.
  */
  @Test
  final void isaacClozeValidator_KnownIncorrect_IncorrectResponseShouldBeReturned() {
    // Set up user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1, item2));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someClozeQuestion, c);
    assertFalse(response.isCorrect());
    assertEquals(incorrectExplanation, response.getExplanation().getValue());
  }

  /*
  Test that known incorrect answers can be matched.
*/
  @Test
  final void isaacClozeValidator_KnownIncorrectDetailedFeedback_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacClozeQuestion clozeQuestion = new IsaacClozeQuestion();
    clozeQuestion.setItems(ImmutableList.of(item1, item2, item3));
    clozeQuestion.setDetailedItemFeedback(true);

    ItemChoice someCorrectAnswer = new ItemChoice();
    someCorrectAnswer.setItems(ImmutableList.of(item1, item3));
    someCorrectAnswer.setCorrect(true);
    clozeQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

    // Set up user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1, item2));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(clozeQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response instanceof ItemValidationResponse);
    ItemValidationResponse clozeResponse = (ItemValidationResponse) response;
    assertEquals(ImmutableList.of(true, false), clozeResponse.getItemsCorrect());
  }

  /*
       Test that all null-placeholder answers are rejected.
  */
  @Test
  final void isaacClozeValidator_AllNull_IncorrectResponseShouldBeReturned() {
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(nullPlaceholder, nullPlaceholder));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someClozeQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("did not provide an answer"));
  }

  /*
      Test that answers missing items are rejected.
  */
  @Test
  final void isaacClozeValidator_NotEnoughItems_IncorrectResponseShouldBeReturned() {
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someClozeQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("does not contain an item for each gap"));
  }

  /*
      Test that answers with too many items are rejected.
  */
  @Test
  final void isaacClozeValidator_TooManyItems_IncorrectResponseShouldBeReturned() {
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1, item2, item3));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someClozeQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("contains more items than gaps"));
  }

  /*
      Test that answers with unknown items are rejected.
  */
  @Test
  final void isaacClozeValidator_UnknownItems_IncorrectResponseShouldBeReturned() {
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1, new Item("unknown", null)));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someClozeQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("unrecognised items"));
  }

  /*
      Test that answers with no items are rejected.
  */
  @Test
  final void isaacClozeValidator_NoUserItems_IncorrectResponseShouldBeReturned() {
    ItemChoice c = new ItemChoice();

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someClozeQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("did not provide an answer"));
  }

  /*
      Test that answers with no items are rejected.
  */
  @Test
  final void isaacClozeValidator_IncorrectTypeUserItems_IncorrectResponseShouldBeReturned() {
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(new ParsonsItem("id001", null, null)));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someClozeQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("not in a recognised format"));
  }

  /*
      Test that default feedback is returned.
  */
  @Test
  final void isaacClozeValidator_DefaultFeedbackReturned() {
    // Set up the question object:
    IsaacClozeQuestion clozeQuestion = new IsaacClozeQuestion();
    clozeQuestion.setDefaultFeedback(new Content(defaultExplanation));
    clozeQuestion.setItems(ImmutableList.of(item1, item2, item3));

    ItemChoice someCorrectAnswer = new ItemChoice();
    someCorrectAnswer.setItems(ImmutableList.of(item1));
    someCorrectAnswer.setCorrect(true);
    clozeQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

    // Set up user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item3));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(clozeQuestion, c);
    assertFalse(response.isCorrect());
    assertEquals(response.getExplanation().getValue(), defaultExplanation);
  }

  //  ---------- Tests from here test invalid questions themselves ----------

  /*
   Test that missing choices are detected.
  */
  @Test
  final void isaacClozeValidator_NoChoices_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacClozeQuestion clozeQuestion = new IsaacClozeQuestion();
    clozeQuestion.setItems(ImmutableList.of(item1, item2, item3));

    clozeQuestion.setChoices(Lists.newArrayList());

    // Set up user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(clozeQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("not have any correct answers"));
  }

  /*
      Test that wrong choice types are detected.
  */
  @Test
  final void isaacClozeValidator_WrongTypeChoices_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacClozeQuestion clozeQuestion = new IsaacClozeQuestion();
    ParsonsItem parsonsItem = new ParsonsItem("id001", null, null);
    clozeQuestion.setItems(ImmutableList.of(parsonsItem));

    ItemChoice someCorrectAnswer = new ParsonsChoice();
    someCorrectAnswer.setItems(ImmutableList.of(parsonsItem));
    someCorrectAnswer.setCorrect(true);
    clozeQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

    // Set up user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(clozeQuestion, c);
    assertFalse(response.isCorrect());
    assertNull(response.getExplanation());
  }

  /*
   Test that missing items are detected.
  */
  @Test
  final void isaacClozeValidator_NoItems_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacClozeQuestion clozeQuestion = new IsaacClozeQuestion();
    clozeQuestion.setItems(ImmutableList.of(item1, item2, item3));

    ItemChoice someCorrectAnswer = new ItemChoice();
    someCorrectAnswer.setItems(Collections.emptyList());
    someCorrectAnswer.setCorrect(true);
    clozeQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

    // Set up user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(clozeQuestion, c);
    assertFalse(response.isCorrect());
    assertNull(response.getExplanation());
  }

  /*
   Test that missing items are detected.
  */
  @Test
  final void isaacClozeValidator_NoItemsInQuestion_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacClozeQuestion clozeQuestion = new IsaacClozeQuestion();

    ItemChoice someCorrectAnswer = new ItemChoice();
    someCorrectAnswer.setItems(ImmutableList.of(item1));
    someCorrectAnswer.setCorrect(true);
    clozeQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

    // Set up user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(clozeQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("does not have any items"));
  }

  /*
   Test that incorrect item types are detected.
  */
  @Test
  final void isaacClozeValidator_ParsonsItems_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacClozeQuestion clozeQuestion = new IsaacClozeQuestion();
    clozeQuestion.setItems(ImmutableList.of(item1, item2, item3));

    ItemChoice someCorrectAnswer = new ItemChoice();
    someCorrectAnswer.setItems(ImmutableList.of(new ParsonsItem("id001", null, null)));
    someCorrectAnswer.setCorrect(true);
    clozeQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

    // Set up user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(clozeQuestion, c);
    assertFalse(response.isCorrect());
    assertNull(response.getExplanation());
  }

  /*
      Test that null placeholders do not match if "allowSubset" is not marked.
  */
  @Test
  final void isaacClozeValidator_NullButNoSubset_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacClozeQuestion clozeQuestion = new IsaacClozeQuestion();
    clozeQuestion.setItems(ImmutableList.of(item1, item2, item3));

    ItemChoice someCorrectAnswer = new ItemChoice();
    someCorrectAnswer.setItems(ImmutableList.of(item1, nullPlaceholder));
    someCorrectAnswer.setCorrect(true);
    clozeQuestion.setChoices(ImmutableList.of(someCorrectAnswer));

    // Set up identical user answer:
    ItemChoice c = new ItemChoice();
    c.setItems(ImmutableList.of(item1, nullPlaceholder));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(clozeQuestion, c);
    assertFalse(response.isCorrect());
    assertNull(response.getExplanation());
  }

  /*
   Test that incorrect question types are detected.
  */
  @Test
  final void isaacClozeValidator_WrongQuestionType_ExceptionShouldBeThrown() {
    IsaacQuickQuestion invalidQuestionType = new IsaacQuickQuestion();
    invalidQuestionType.setId("invalidQuestionType");
    Choice choice = new Choice();

    // This should throw an exception:
    Exception exception = assertThrows(IllegalArgumentException.class,
        () -> validator.validateQuestionResponse(invalidQuestionType, choice));
    assertTrue(exception.getMessage().contains("only works with IsaacClozeQuestions"));
  }

  /*
   Test that incorrect submitted choice types are detected.
  */
  @Test
  final void isaacClozeValidator_WrongChoiceType_ExceptionShouldBeThrown() {
    IsaacClozeQuestion clozeQuestion = new IsaacClozeQuestion();
    Choice choice = new Choice();

    // This should throw an exception:
    Exception exception =
        assertThrows(IllegalArgumentException.class, () -> validator.validateQuestionResponse(clozeQuestion, choice));
    assertTrue(exception.getMessage().contains("Expected ItemChoice for IsaacClozeQuestion"));
  }
}
