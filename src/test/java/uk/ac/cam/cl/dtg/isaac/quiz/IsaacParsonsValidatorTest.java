/**
 * Copyright 2019 James Sharkey
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacParsonsQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ParsonsChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ParsonsItem;

/**
 * Test class for the Parsons Question Validator class.
 */
class IsaacParsonsValidatorTest {
  private IsaacParsonsValidator validator;
  private IsaacParsonsQuestion someParsonsQuestion;
  private final String incorrectExplanation = "EXPLANATION";

  /**
   * Initial configuration of tests.
   */
  @BeforeEach
  public final void setUp() {
    validator = new IsaacParsonsValidator();

    // Set up the question object:
    someParsonsQuestion = new IsaacParsonsQuestion();

    List<Choice> answerList = Lists.newArrayList();
    ParsonsChoice someIncorrectChoice = new ParsonsChoice();
    ParsonsChoice someCorrectAnswer = new ParsonsChoice();
    ParsonsItem item1 = new ParsonsItem("id001", "A", 0);
    ParsonsItem item2 = new ParsonsItem("id002", "B", 0);
    ParsonsItem item3 = new ParsonsItem("id003", "C", 1);
    someParsonsQuestion.setItems(ImmutableList.of(item1, item2, item3));

    // Correct and incorrect choices the same:
    someCorrectAnswer.setItems(ImmutableList.of(item1, item3));
    someCorrectAnswer.setCorrect(true);
    someIncorrectChoice.setItems(ImmutableList.of(item1, item2));
    someIncorrectChoice.setCorrect(false);
    someIncorrectChoice.setExplanation(new Content(incorrectExplanation));

    // Add both choices to question, incorrect first:
    answerList.add(someIncorrectChoice);
    answerList.add(someCorrectAnswer);
    someParsonsQuestion.setChoices(answerList);
  }

  /*
     Test that correct answers are recognised.
  */
  @Test
  final void isaacParsonsValidator_CorrectItems_CorrectResponseShouldBeReturned() {
    // Set up user answer:
    ParsonsChoice c = new ParsonsChoice();
    ParsonsItem submittedItem1 = new ParsonsItem("id001", null, 0);
    ParsonsItem submittedItem3 = new ParsonsItem("id003", null, 1);
    c.setItems(ImmutableList.of(submittedItem1, submittedItem3));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someParsonsQuestion, c);
    assertTrue(response.isCorrect());
  }

  /*
     Test that order matters for correct answers.
  */
  @Test
  final void isaacParsonsValidator_CorrectItemsWrongOrder_IncorrectResponseShouldBeReturned() {
    // Set up user answer:
    ParsonsChoice c = new ParsonsChoice();
    ParsonsItem submittedItem1 = new ParsonsItem("id001", null, 0);
    ParsonsItem submittedItem3 = new ParsonsItem("id003", null, 1);
    c.setItems(ImmutableList.of(submittedItem3, submittedItem1));  // Reverse order!

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someParsonsQuestion, c);
    assertFalse(response.isCorrect());
  }

  /*
 Test that indentation matters for correct answers.
*/
  @Test
  final void isaacParsonsValidator_CorrectItemsWrongIndentation_IncorrectResponseShouldBeReturned() {
    // Set up user answer:
    ParsonsChoice c = new ParsonsChoice();
    ParsonsItem submittedItem1 = new ParsonsItem("id001", null, 0);
    ParsonsItem submittedItem3 = new ParsonsItem("id003", null, 0);  // Wrong indentation!
    c.setItems(ImmutableList.of(submittedItem1, submittedItem3));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someParsonsQuestion, c);
    assertFalse(response.isCorrect());
  }

  /*
     Test that correct choices take precedence over incorrect choices.
  */
  @Test
  final void isaacParsonsValidator_CorrectChoicePrecedence_CorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacParsonsQuestion parsonsQuestion = new IsaacParsonsQuestion();

    List<Choice> answerList = Lists.newArrayList();
    ParsonsChoice someIncorrectChoice = new ParsonsChoice();
    ParsonsChoice someCorrectAnswer = new ParsonsChoice();
    ParsonsItem item1 = new ParsonsItem("id001", "A", 0);
    ParsonsItem item2 = new ParsonsItem("id002", "B", 0);
    ParsonsItem item3 = new ParsonsItem("id003", "C", 1);
    parsonsQuestion.setItems(ImmutableList.of(item1, item2, item3));
    List<Item> itemsForChoices = ImmutableList.of(item1, item3);

    // Correct and incorrect choices the same:
    someCorrectAnswer.setItems(itemsForChoices);
    someCorrectAnswer.setCorrect(true);
    someIncorrectChoice.setItems(itemsForChoices);
    someIncorrectChoice.setCorrect(false);

    // Add both choices to question, incorrect first:
    answerList.add(someIncorrectChoice);
    answerList.add(someCorrectAnswer);
    parsonsQuestion.setChoices(answerList);

    // Set up user answer:
    ParsonsChoice c = new ParsonsChoice();
    ParsonsItem submittedItem1 = new ParsonsItem("id001", null, 0);
    ParsonsItem submittedItem3 = new ParsonsItem("id003", null, 1);
    c.setItems(ImmutableList.of(submittedItem1, submittedItem3));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(parsonsQuestion, c);
    assertTrue(response.isCorrect());
  }

  /*
   Test that invalid answers are recognised.
  */
  @Test
  final void isaacParsonsValidator_InvalidItems_ErrorResponseShouldBeReturned() {
    // Set up invalid user answer:
    ParsonsChoice c = new ParsonsChoice();
    ParsonsItem submittedItem1 = new ParsonsItem("id005", null, 0);
    c.setItems(ImmutableList.of(submittedItem1));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someParsonsQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("unrecognised items"));
  }

  /*
   Test that invalid answers are recognised.
  */
  @Test
  final void isaacParsonsValidator_IncorrectItemType_ExceptionShouldBeThrown() {
    // Set up invalid user answer:
    ParsonsChoice c = new ParsonsChoice();
    Item submittedItem1 = new Item("id001", null);
    Item submittedItem2 = new Item("id002", null);
    c.setItems(ImmutableList.of(submittedItem1, submittedItem2));

    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
      validator.validateQuestionResponse(someParsonsQuestion, c);
    });
    assertEquals("Expected ParsonsChoice to contain ParsonsItems!", exception.getMessage());
  }

  /*
   Test that no items being submitted gets custom feedback.
  */
  @Test
  final void isaacParsonsValidator_NoItemsSubmitted_IncorrectResponseShouldBeReturned() {
    // Set up invalid user answer:
    ParsonsChoice c = new ParsonsChoice();
    c.setItems(null);

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someParsonsQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("did not provide an answer"));
  }

  /*
   Test that incorrect answers are incorrect.
  */
  @Test
  final void isaacParsonsValidator_IncorrectAnswer_IncorrectResponseShouldBeReturned() {
    // Set up user answer:
    ParsonsChoice c = new ParsonsChoice();
    ParsonsItem submittedItem1 = new ParsonsItem("id001", null, 0);
    c.setItems(ImmutableList.of(submittedItem1));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someParsonsQuestion, c);
    assertFalse(response.isCorrect());
    assertNull(response.getExplanation());
  }

  /*
     Test that incorrect choices can be matched.
  */
  @Test
  final void isaacParsonsValidator_IncorrectChoiceMatch_MatchedResponseShouldBeReturned() {
    // Set up user answer:
    ParsonsChoice c = new ParsonsChoice();
    ParsonsItem submittedItem1 = new ParsonsItem("id001", null, 0);
    ParsonsItem submittedItem2 = new ParsonsItem("id002", null, 0);
    c.setItems(ImmutableList.of(submittedItem1, submittedItem2));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someParsonsQuestion, c);
    assertFalse(response.isCorrect());
    assertEquals(response.getExplanation().getValue(), incorrectExplanation);
  }

  //  ---------- Tests from here test invalid questions themselves ----------

  /*
   Test that missing choices are detected.
  */
  @Test
  final void isaacParsonsValidator_NoChoices_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacParsonsQuestion parsonsQuestion = new IsaacParsonsQuestion();

    ParsonsItem item1 = new ParsonsItem("id001", "A", -0);
    ParsonsItem item2 = new ParsonsItem("id002", "B", 0);
    ParsonsItem item3 = new ParsonsItem("id003", "C", 1);
    parsonsQuestion.setItems(ImmutableList.of(item1, item2, item3));

    parsonsQuestion.setChoices(Lists.newArrayList());

    // Set up user answer:
    ParsonsChoice c = new ParsonsChoice();
    ParsonsItem submittedItem1 = new ParsonsItem("id001", null, 0);
    c.setItems(ImmutableList.of(submittedItem1));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(parsonsQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("not have any correct answers"));
  }

  /*
   Test that missing items are detected.
  */
  @Test
  final void isaacParsonsValidator_NoItems_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacParsonsQuestion parsonsQuestion = new IsaacParsonsQuestion();

    List<Choice> answerList = Lists.newArrayList();
    ParsonsItem item1 = new ParsonsItem("id001", "A", 0);
    ParsonsItem item2 = new ParsonsItem("id002", "B", 0);
    ParsonsItem item3 = new ParsonsItem("id003", "C", 1);
    // Don't set items on the question object itself.

    ParsonsChoice someCorrectAnswer = new ParsonsChoice();
    someCorrectAnswer.setItems(ImmutableList.of(item1, item2, item3));
    someCorrectAnswer.setCorrect(true);
    answerList.add(someCorrectAnswer);
    parsonsQuestion.setChoices(answerList);

    // Set up correct user answer:
    ParsonsChoice c = new ParsonsChoice();
    c.setItems(ImmutableList.of(item1, item2, item3));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(parsonsQuestion, c);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("not have any items to choose from"));
  }

  /*
   Test that choices missing items are detected.
  */
  @Test
  final void isaacParsonsValidator_NoItemsInChoice_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacParsonsQuestion parsonsQuestion = new IsaacParsonsQuestion();

    List<Choice> answerList = Lists.newArrayList();
    ParsonsItem item1 = new ParsonsItem("id001", "A", 0);
    ParsonsItem item2 = new ParsonsItem("id002", "B", 0);
    ParsonsItem item3 = new ParsonsItem("id003", "C", 1);
    parsonsQuestion.setItems(ImmutableList.of(item1, item2, item3));

    ParsonsChoice someCorrectAnswer = new ParsonsChoice();
    // Don't set items on the choice object.
    someCorrectAnswer.setCorrect(true);
    answerList.add(someCorrectAnswer);
    parsonsQuestion.setChoices(answerList);

    // Set up correct user answer:
    ParsonsChoice c = new ParsonsChoice();
    c.setItems(ImmutableList.of(item1, item2, item3));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(parsonsQuestion, c);
    assertFalse(response.isCorrect());
    assertNull(response.getExplanation());
  }

  /*
   Test that incorrect choice types in question are detected.
  */
  @Test
  final void isaacParsonsValidator_WrongChoiceType_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacParsonsQuestion parsonsQuestion = new IsaacParsonsQuestion();

    List<Choice> answerList = Lists.newArrayList();
    ParsonsItem item1 = new ParsonsItem("id001", "A", 0);
    ParsonsItem item2 = new ParsonsItem("id002", "B", 0);
    ParsonsItem item3 = new ParsonsItem("id003", "C", 1);
    parsonsQuestion.setItems(ImmutableList.of(item1, item2, item3));

    Choice someCorrectAnswer = new Choice();
    someCorrectAnswer.setCorrect(true);
    answerList.add(someCorrectAnswer);
    parsonsQuestion.setChoices(answerList);

    // Set up correct user answer:
    ParsonsChoice c = new ParsonsChoice();
    c.setItems(ImmutableList.of(item1, item2, item3));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(parsonsQuestion, c);
    assertFalse(response.isCorrect());
    assertNull(response.getExplanation());
  }

  /*
Test that incorrect Item types in choices are detected.
*/
  @Test
  final void isaacParsonsValidator_WrongItemTypeInChoice_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacParsonsQuestion parsonsQuestion = new IsaacParsonsQuestion();

    List<Choice> answerList = Lists.newArrayList();
    ParsonsItem item1 = new ParsonsItem("id001", "A", 0);
    ParsonsItem item2 = new ParsonsItem("id002", "B", 0);
    ParsonsItem item3 = new ParsonsItem("id003", "C", 1);
    parsonsQuestion.setItems(ImmutableList.of(item1, item2, item3));

    ParsonsChoice someCorrectAnswer = new ParsonsChoice();
    someCorrectAnswer.setItems(ImmutableList.of(new Item("id001", "A")));
    someCorrectAnswer.setCorrect(true);
    answerList.add(someCorrectAnswer);
    parsonsQuestion.setChoices(answerList);

    // Set up correct user answer:
    ParsonsChoice c = new ParsonsChoice();
    c.setItems(ImmutableList.of(item1));

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(parsonsQuestion, c);
    assertFalse(response.isCorrect());
    assertNull(response.getExplanation());
  }

  /*
   Test that incorrect question types are detected.
  */
  @Test
  final void isaacParsonsValidator_WrongQuestionType_ExceptionShouldBeThrown() {
    IsaacQuickQuestion invalidQuestionType = new IsaacQuickQuestion();
    invalidQuestionType.setId("invalidQuestionType");
    ParsonsChoice choice = new ParsonsChoice();

    Exception exception = assertThrows(IllegalArgumentException.class, () ->
        validator.validateQuestionResponse(invalidQuestionType, choice));
    assertEquals("This validator only works with IsaacParsonsQuestions "
            + "(invalidQuestionType is not ParsonsQuestion)",
        exception.getMessage());
  }

  /*
   Test that incorrect submitted choice types are detected.
  */
  @Test
  final void isaacParsonsValidator_WrongChoiceType_ExceptionShouldBeThrown() {
    IsaacParsonsQuestion parsonsQuestion = new IsaacParsonsQuestion();
    parsonsQuestion.setId("invalidQuestionType");
    Choice choice = new Choice();

    Exception exception = assertThrows(IllegalArgumentException.class, () ->
        validator.validateQuestionResponse(parsonsQuestion, choice));
    assertEquals("Expected ParsonsChoice for IsaacParsonsQuestion: invalidQuestionType."
            + " Received (class uk.ac.cam.cl.dtg.isaac.dos.content.Choice) ",
        exception.getMessage());
  }
}
