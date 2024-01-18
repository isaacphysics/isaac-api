/**
 * Copyright 2014 Stephen Cummins, 2019 James Sharkey
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Quantity;

/**
 * Test class for the user manager class.
 */
class IsaacNumericValidatorTest {
  private IsaacNumericValidator validator;
  private IsaacNumericQuestion numericQuestionNoUnits;
  private IsaacNumericQuestion numericQuestionWithUnits;

  private final String correctDecimalExponentAnswer = "4.8e22";
  private final String correctIntegerAnswer = "42";
  private final String correctUnits = "m\\,s^{-1}";

  /**
   * Initial configuration of tests.
   */
  @BeforeEach
  public final void setUp() {
    validator = new IsaacNumericValidator();

    // Set up a question object which does not require units:
    numericQuestionNoUnits = new IsaacNumericQuestion();
    numericQuestionNoUnits.setRequireUnits(false);
    List<Choice> noUnitsAnswerList = Lists.newArrayList();

    Quantity correctDecimalExponentChoice = new Quantity(correctDecimalExponentAnswer);
    correctDecimalExponentChoice.setCorrect(true);
    noUnitsAnswerList.add(correctDecimalExponentChoice);

    Quantity correctIntegerChoice = new Quantity(correctIntegerAnswer);
    correctIntegerChoice.setCorrect(true);
    noUnitsAnswerList.add(correctIntegerChoice);

    numericQuestionNoUnits.setChoices(noUnitsAnswerList);

    // Set up a question object does require units:
    numericQuestionWithUnits = new IsaacNumericQuestion();
    List<Choice> withUnitsAnswerList = Lists.newArrayList();

    Quantity correctIntegerChoiceWithUnits = new Quantity(correctIntegerAnswer, correctUnits);
    correctIntegerChoiceWithUnits.setCorrect(true);
    withUnitsAnswerList.add(correctIntegerChoiceWithUnits);

    numericQuestionWithUnits.setChoices(withUnitsAnswerList);
  }

  /*
      Test that the "not a valid number" response is returned for non-numeric input.
   */
  @Test
  final void isaacNumericValidator_NonNumericValue_InvalidResponseShouldBeReturned() {
    // Set up user answer:
    Quantity q = new Quantity("NOT_A_NUMBER");

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(numericQuestionNoUnits, q);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getTags().contains("unrecognised_format"));
  }

  /*
      Test a correct answer with an exponent in it gets recognised as correct.
   */
  @Test
  final void isaacNumericValidator_CheckCorrectAnswerWithExponent_CorrectResponseShouldHappen() {
    // Set up user answer:
    Quantity q = new Quantity(correctDecimalExponentAnswer);

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(numericQuestionNoUnits, q);
    assertTrue(response.isCorrect());
  }

  /*
      Test a correct integer answer without units gets recognised as correct.
   */
  @Test
  final void isaacNumericValidator_CheckCorrectIntegerAnswer_CorrectResponseShouldHappen() {
    // Set up user answer:
    Quantity q = new Quantity(correctIntegerAnswer);

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(numericQuestionNoUnits, q);
    assertTrue(response.isCorrect());
  }

  /*
      Test a correct integer answer with correct units gets recognised as correct.
   */
  @Test
  final void isaacNumericValidator_CheckCorrectAnswerWithCorrectUnits_CorrectResponseShouldHappen() {
    // Set up user answer:
    Quantity q = new Quantity(correctIntegerAnswer, correctUnits);

    // Test response:
    QuantityValidationResponse response =
        (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);
    assertTrue(response.isCorrect());
    assertTrue(response.getCorrectUnits());
  }

  /*
      Test an incorrect integer answer with correct units gets recognised as incorrect, but with correct units.
   */
  @Test
  final void isaacNumericValidator_CheckCorrectAnswerWithIncorrectValue_IncorrectResponseShouldHappen() {
    // Set up user answer:
    Quantity q = new Quantity("43", correctUnits);

    // Test response:
    QuantityValidationResponse response =
        (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);

    // Check answer is wrong,
    assertFalse(response.isCorrect());
    assertFalse(response.getCorrectValue());
    // but units are right:
    assertTrue(response.getCorrectUnits());
  }

  /*
  Test an incorrect sig fig answer with correct units gets recognised as incorrect, but with correct units.
*/
  @Test
  final void isaacNumericValidator_IncorrectSigFigsCorrectUnit_IncorrectResponseShouldHappen() {
    // Set up user answer:
    Quantity quantityTooFew = new Quantity("4", correctUnits);

    // Test response:
    QuantityValidationResponse responseTooFew =
        (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, quantityTooFew);

    // Check answer is wrong,
    assertFalse(responseTooFew.isCorrect());
    assertFalse(responseTooFew.getCorrectValue());
    // but units are right:
    assertTrue(responseTooFew.getCorrectUnits());

    // Set up user answer:
    Quantity quantityTooMany = new Quantity("42.000", correctUnits);

    // Test response:
    QuantityValidationResponse responseTooMany =
        (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, quantityTooMany);

    // Check answer is wrong,
    assertFalse(responseTooMany.isCorrect());
    assertFalse(responseTooMany.getCorrectValue());
    // but units are right:
    assertTrue(responseTooMany.getCorrectUnits());
  }

  /*
      Test a correct integer answer with incorrect units gets recognised as incorrect, but with correct value.
   */
  @Test
  final void isaacNumericValidator_CheckCorrectAnswerWithIncorrectUnits_IncorrectResponseShouldHappen() {
    // Set up user answer:
    Quantity q = new Quantity(correctIntegerAnswer, "m\\,h^{-1}");

    // Test response:
    QuantityValidationResponse response =
        (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);

    // Check answer is wrong:
    assertFalse(response.isCorrect());
    assertFalse(response.getCorrectUnits());
    // but value is right:
    assertTrue(response.getCorrectValue());
  }

  /*
      Test a correct answer with missing units gets recognised as incorrect.
   */
  @Test
  final void isaacNumericValidator_CheckCorrectAnswerWithNoUnits_IncorrectResponseShouldHappen() {
    // Set up user answer without units:
    Quantity q = new Quantity(correctIntegerAnswer);

    // Test response:
    QuantityValidationResponse response =
        (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);

    // Check answer is wrong:
    assertFalse(response.isCorrect());
    assertFalse(response.getCorrectUnits());
    // Check feedback contains correct warning:
    assertEquals(IsaacNumericValidator.DEFAULT_NO_UNIT_VALIDATION_RESPONSE, response.getExplanation().getValue());
  }

  /*
  Test a correct answer with missing value gets recognised as incorrect.
*/
  @Test
  final void isaacNumericValidator_CheckCorrectAnswerWithNoValue_IncorrectResponseShouldHappen() {
    // Set up user answer without units:
    Quantity q = new Quantity("", correctUnits);

    // Test response:
    QuantityValidationResponse response =
        (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);

    // Check answer is wrong:
    assertFalse(response.isCorrect());
    assertFalse(response.getCorrectValue());
    // Check feedback contains correct warning:
    assertEquals(IsaacNumericValidator.DEFAULT_NO_ANSWER_VALIDATION_RESPONSE, response.getExplanation().getValue());
  }

  /*
      Test that the "invalid negative standard form" response is returned for poorly formed input.
   */
  @Test
  final void isaacNumericValidator_InvalidStandardForm_WarningShouldBeReturned() {
    // Set up user answer:
    Quantity q = new Quantity("4.2x10-3");

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(numericQuestionNoUnits, q);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getTags().contains("unrecognised_format"));
    assertTrue(response.getExplanation().getTags().contains("invalid_std_form"));
  }

  /*
      Test known wrong answers take precedence over sig fig warning.
   */
  @Test
  final void isaacNumericValidator_CheckExactIncorrectAnswerWithIncorrectSigFigs_KnownIncorrectResponseShouldHappen() {
    // Set up the question object:
    String someExplanation = "some explanation";
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setRequireUnits(false);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someIncorrectAnswer = new Quantity("0");
    someIncorrectAnswer.setCorrect(false);
    someIncorrectAnswer.setExplanation(new Content(someExplanation));
    answerList.add(someIncorrectAnswer);
    someNumericQuestion.setChoices(answerList);

    // Set up user answer:
    Quantity q = new Quantity("0");

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

    // Check response is not correct and explanation is that of expected choice:
    assertFalse(response.isCorrect());
    assertEquals(response.getExplanation().getValue(), someExplanation);
  }

  /*
  Test known wrong answers take precedence over sig fig warning, even when match is not exact.
*/
  @Test
  final void isaacNumericValidator_CheckKnownIncorrectAnswerWithIncorrectSigFigs_KnownIncorrectResponseShouldHappen() {
    // Set up the question object:
    String someExplanation = "some explanation";
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setRequireUnits(false);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someIncorrectAnswer = new Quantity("1");
    someIncorrectAnswer.setCorrect(false);
    someIncorrectAnswer.setExplanation(new Content(someExplanation));
    answerList.add(someIncorrectAnswer);
    someNumericQuestion.setChoices(answerList);

    // Set up user answer:
    Quantity q = new Quantity("1.000001");

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

    // Check response is not correct and explanation is that of expected choice:
    assertFalse(response.isCorrect());
    assertEquals(response.getExplanation().getValue(), someExplanation);
  }

  /*
      Test significant figure warning in answer without an exponent.
   */
  @Test
  final void isaacNumericValidator_CheckCorrectAnswerWrongSigFigs_IncorrectResponseShouldHappen() {
    // Set up the question object:
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setRequireUnits(false);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity("4.8");
    someCorrectAnswer.setCorrect(true);
    answerList.add(someCorrectAnswer);

    someNumericQuestion.setChoices(answerList);

    // Set up user answer:
    Quantity q = new Quantity("4.81");

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getTags().contains("sig_figs"));
  }

  /*
      Test incorrect answers with correct sig figs to questions allowing a range of significant figures.
   */
  @Test
  final void isaacNumericValidator_TestQuestionWithSigFigRange_IncorrectAnswers() {
    // Set up the question object:
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setRequireUnits(false);
    someNumericQuestion.setSignificantFiguresMin(2);
    someNumericQuestion.setSignificantFiguresMax(3);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity("1.6875");
    someCorrectAnswer.setCorrect(true);

    answerList.add(someCorrectAnswer);
    someNumericQuestion.setChoices(answerList);

    // Set up a user answer:
    Quantity quantity3sf = new Quantity("1.66");
    // Test response:
    QuestionValidationResponse response3sf = validator.validateQuestionResponse(someNumericQuestion, quantity3sf);
    assertFalse(response3sf.isCorrect());
    assertFalse(response3sf.getExplanation().getTags().contains("sig_figs"));

    // Set up a user answer:
    Quantity quantity2sf = new Quantity("2.0");
    // Test response:
    QuestionValidationResponse response2sf = validator.validateQuestionResponse(someNumericQuestion, quantity2sf);
    assertFalse(response2sf.isCorrect());
    assertFalse(response2sf.getExplanation().getTags().contains("sig_figs"));
  }

  /*
      Test correct but incorrect sig fig answers to questions allowing a range of significant figures.
   */
  @Test
  final void isaacNumericValidator_TestQuestionWithSigFigRange_IncorrectSigFigAnswers() {
    // Set up the question object:
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setRequireUnits(false);
    someNumericQuestion.setSignificantFiguresMin(2);
    someNumericQuestion.setSignificantFiguresMax(3);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity("1.6875");
    someCorrectAnswer.setCorrect(true);

    answerList.add(someCorrectAnswer);
    someNumericQuestion.setChoices(answerList);

    // Set up a user answer:
    Quantity quantity5sf = new Quantity("1.6875");
    // Test response:
    QuestionValidationResponse response5sf = validator.validateQuestionResponse(someNumericQuestion, quantity5sf);
    assertFalse(response5sf.isCorrect(), "expected 1.6875 not to match 1.6875 to 2 or 3 sf");
    assertTrue(response5sf.getExplanation().getTags().contains("sig_figs"));

    // Set up a user answer:
    Quantity quantity1sf = new Quantity("2");
    // Test response:
    QuestionValidationResponse response1sf = validator.validateQuestionResponse(someNumericQuestion, quantity1sf);
    assertFalse(response1sf.isCorrect(), "expected 2 not to match 1.6875 to 2 or 3 sf");
    assertTrue(response1sf.getExplanation().getTags().contains("sig_figs"));
  }

  /*
  Test incorrect sig fig answers to questions allowing a range of significant figures.
*/
  @Test
  final void isaacNumericValidator_TestQuestionWithSigFigRange_SigFigResponses() {
    // Set up the question object:
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setRequireUnits(false);
    someNumericQuestion.setSignificantFiguresMin(2);
    someNumericQuestion.setSignificantFiguresMax(3);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity("1.6875");
    someCorrectAnswer.setCorrect(true);

    answerList.add(someCorrectAnswer);
    someNumericQuestion.setChoices(answerList);

    // Set up a correct user answer with too many sig figs:
    Quantity quantity5sfCorrect = new Quantity("1.6875");
    // Test response is sig fig message:
    QuestionValidationResponse response5sfCorrect =
        validator.validateQuestionResponse(someNumericQuestion, quantity5sfCorrect);
    assertFalse(response5sfCorrect.isCorrect(), "expected 1.6875 not to match 1.6875 to 2 or 3 sf");
    assertTrue(response5sfCorrect.getExplanation().getTags().contains("sig_figs"));
    assertTrue(response5sfCorrect.getExplanation().getTags().contains("sig_figs_too_many"));

    // Set up a wrong user answer with too many sig figs:
    Quantity quantity5sfWrong = new Quantity("2.7986");
    // Test response does not mention sig figs:
    QuestionValidationResponse response5sfWrong =
        validator.validateQuestionResponse(someNumericQuestion, quantity5sfWrong);
    assertFalse(response5sfWrong.isCorrect(), "expected 2.7986 not to match 1.6875");
    assertFalse(response5sfWrong.getExplanation().getTags().contains("sig_figs"),
        "expected 2.7986 without sig fig message");

    // Set up a user answer:
    Quantity quantity1sf = new Quantity("5");
    // Test response:
    QuestionValidationResponse response1sf = validator.validateQuestionResponse(someNumericQuestion, quantity1sf);
    assertFalse(response1sf.isCorrect(), "expected 5 not to match 1.6875 to 2 or 3 sf");
    assertTrue(response1sf.getExplanation().getTags().contains("sig_figs"));
    assertTrue(response1sf.getExplanation().getTags().contains("sig_figs_too_few"));
  }

  /*
      Test default feedback is used when a known answer is matched and no explanation given.
  */
  @Test
  final void isaacNumericValidator_DefaultFeedbackAndCorrectNoExplanation_DefaultFeedbackReturned() {
    // Set up the question object:
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setRequireUnits(false);
    Content defaultFeedback = new Content("DEFAULT FEEDBACK!");
    someNumericQuestion.setDefaultFeedback(defaultFeedback);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity("10");
    someCorrectAnswer.setCorrect(true);
    answerList.add(someCorrectAnswer);

    someNumericQuestion.setChoices(answerList);

    // Set up user answer:
    Quantity q = new Quantity("10");

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

    assertTrue(response.isCorrect());
    assertEquals(response.getExplanation(), defaultFeedback);
  }

  /*
      Test that correct units are noted even when matching a known incorrect choice.

      (There is no way to record that only the value is wrong, but we can use other choices
       to decide if the units are correct or not).
  */
  @Test
  final void isaacNumericValidator_CheckCorrectUnitsIncorrectChoiceMatch_CorrectUnitsResponseShouldHappen() {
    // Set up the question object:
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setRequireUnits(true);
    someNumericQuestion.setSignificantFiguresMin(2);
    someNumericQuestion.setSignificantFiguresMax(3);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity("31.4", "m");
    someCorrectAnswer.setCorrect(true);
    answerList.add(someCorrectAnswer);
    Quantity someIncorrectAnswer = new Quantity("20.0", "m");
    someIncorrectAnswer.setCorrect(false);
    answerList.add(someIncorrectAnswer);

    someNumericQuestion.setChoices(answerList);

    // Set up user answer:
    Quantity q = new Quantity("20", "m");

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);
    QuantityValidationResponse quantityValidationResponse = (QuantityValidationResponse) response;

    assertFalse(response.isCorrect());
    assertTrue(quantityValidationResponse.getCorrectUnits());
  }

  //  ---------- Tests from here test invalid questions themselves ----------

  /*
   Test that missing choices are detected.
  */
  @Test
  final void isaacNumericValidator_NoChoices_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacNumericQuestion numericQuestion = new IsaacNumericQuestion();
    numericQuestion.setChoices(Lists.newArrayList());

    // Set up user answer:
    Quantity q = new Quantity("4.81");

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(numericQuestion, q);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("not have any correct answers"));
  }

  /*
   Test choices missing units required by question are detected.
  */
  @Test
  final void isaacNumericValidator_TestChoiceMissingRequiredUnits_IncorrectResponse() {
    // Set up the question object:
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setRequireUnits(true);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity(correctIntegerAnswer, null);
    someCorrectAnswer.setCorrect(true);
    answerList.add(someCorrectAnswer);
    someNumericQuestion.setChoices(answerList);

    // Set up a user answer:
    Quantity q = new Quantity(correctIntegerAnswer, correctUnits);
    QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);
    assertFalse(response.isCorrect());
    // FIXME - this needs to test that the log message occurs!
  }

  /*
   Test that incorrect choice types in question are detected.
  */
  @Test
  final void isaacNumericValidator_WrongChoiceType_IncorrectResponseShouldBeReturned() {
    // Set up the question object:
    IsaacNumericQuestion numericQuestion = new IsaacNumericQuestion();
    numericQuestion.setRequireUnits(false);

    List<Choice> answerList = Lists.newArrayList();
    Choice someCorrectAnswer = new Choice();
    someCorrectAnswer.setCorrect(true);
    answerList.add(someCorrectAnswer);
    numericQuestion.setChoices(answerList);

    // Set up user answer:
    Quantity q = new Quantity(correctIntegerAnswer);

    // Test response:
    QuestionValidationResponse response = validator.validateQuestionResponse(numericQuestion, q);
    assertFalse(response.isCorrect());

    // Alter the question to require units:
    numericQuestion.setRequireUnits(true);
    q.setUnits(correctUnits);
    // Test response:
    QuestionValidationResponse responseWithUnits = validator.validateQuestionResponse(numericQuestion, q);
    assertFalse(responseWithUnits.isCorrect());

    // FIXME - this needs to test that the log message occurs!
  }

  /*
   Test invalid sig figs rules are detected.
  */
  @Test
  final void isaacNumericValidator_TestInvalidSigFigRange_IncorrectResponse() {
    // Set up the question object:
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setRequireUnits(false);
    someNumericQuestion.setSignificantFiguresMin(3);
    someNumericQuestion.setSignificantFiguresMax(1);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity(correctIntegerAnswer);
    someCorrectAnswer.setCorrect(true);
    answerList.add(someCorrectAnswer);
    someNumericQuestion.setChoices(answerList);

    // Set up a user answer:
    Quantity q = new Quantity("1");
    QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);
    assertFalse(response.isCorrect());
    assertTrue(response.getExplanation().getValue().contains("cannot be answered correctly"));
  }

  /*
   Test that incorrect question types are detected.
  */
  @Test
  final void isaacNumericValidator_WrongQuestionType_ExceptionShouldBeThrown() {
    IsaacQuickQuestion invalidQuestionType = new IsaacQuickQuestion();
    invalidQuestionType.setId("invalidQuestionType");
    Quantity quantity = new Quantity();

    Exception exception = assertThrows(IllegalArgumentException.class,
        () -> validator.validateQuestionResponse(invalidQuestionType, quantity));
    assertEquals("This validator only works with Isaac Numeric Questions... (invalidQuestionType is not numeric)",
        exception.getMessage());
  }

  /*
   Test that incorrect submitted choice types are detected.
  */
  @Test
  final void isaacNumericValidator_WrongChoiceType_ExceptionShouldBeThrown() {
    IsaacNumericQuestion numericQuestion = new IsaacNumericQuestion();
    numericQuestion.setId("invalidQuestionType");
    Choice choice = new Choice();

    Exception exception = assertThrows(IllegalArgumentException.class,
        () -> validator.validateQuestionResponse(numericQuestion, choice));
    assertEquals(
        "Expected Quantity for IsaacNumericQuestion: invalidQuestionType. Received (class uk.ac.cam.cl.dtg.isaac.dos.content.Choice)",
        exception.getMessage());
  }

  /*
   Test displayUnit overrides requiresUnits.
  */
  @Test
  final void isaacNumericValidator_TestInconsistentDisplayUnitsOverride_UnitsIgnored() {
    // Set up the question object:
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setRequireUnits(true);
    someNumericQuestion.setDisplayUnit("SOME-FAKE-UNIT");

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity(correctIntegerAnswer, "SOME-FAKE-UNIT");
    someCorrectAnswer.setCorrect(true);
    answerList.add(someCorrectAnswer);
    someNumericQuestion.setChoices(answerList);

    // Set up a user answer without units:
    Quantity q = new Quantity(correctIntegerAnswer);
    QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

    // Check that units are ignored for validation:
    assertTrue(response.isCorrect());
    if (response instanceof QuantityValidationResponse) {
      QuantityValidationResponse quantityResponse = (QuantityValidationResponse) response;
      assertNull(quantityResponse.getCorrectUnits());
    }
  }

  //  ---------- Tests from here test internal methods of the validator ----------

  /*
      Test parsing common unambiguous representations as numbers.
   */
  @ParameterizedTest
  @ValueSource(strings = {"42", "4.2e1", "4.2E1", "4.2x10^1", "4.2*10**1", "4.2×10^(1)", "4.2 \\times 10^{1}"})
  void isaacNumericValidator_CheckParsingAsNumberWorks_baseValue(String input) {
    assertTrue(validator.numericValuesMatch("42", input, 2));
  }

  @ParameterizedTest
  @ValueSource(strings = {"10000", "1x10^4", "1e4", "1E4", "1 x 10**4", "10^4", "10**(4)", "10^{4}", "100x10^2"})
  void isaacNumericValidator_CheckParsingAsNumberWorks_exponentValue(String input) {
    assertTrue(validator.numericValuesMatch("10000", input, 1));
  }

  /*
      Test various numbers for significant figure rules.
   */
  @ParameterizedTest
  @MethodSource
  void isaacNumericValidator_CheckSignificantFiguresNoRangeCalculationWorks_multipleTests(
      List<String> numbersToTest, List<Integer> sigFigsToPass, List<Integer> sigFigsToFail) {
    for (String number : numbersToTest) {
      for (Integer sigFig : sigFigsToPass) {
        String onFailedFirstAssertionMessage =
            String.format("Unexpected too few sig fig for %s @ %dsf", number, sigFig);
        assertFalse(validator.tooFewSignificantFigures(number, sigFig), onFailedFirstAssertionMessage);
        String onFailedSecondAssertionMessage =
            String.format("Unexpected too many sig fig for %s @ %dsf", number, sigFig);
        assertFalse(validator.tooManySignificantFigures(number, sigFig), onFailedSecondAssertionMessage);
      }
      for (Integer sigFig : sigFigsToFail) {
        String onFailedThirdAssertionMessage =
            String.format("Expected incorrect sig fig for %s @ %dsf", number, sigFig);
        boolean incorrectSigFigs =
            validator.tooFewSignificantFigures(number, sigFig) || validator.tooManySignificantFigures(number, sigFig);
        assertTrue(incorrectSigFigs, onFailedThirdAssertionMessage);
      }
    }
  }

  /*
   * Parameters to launch multiple sig fig tests on given numbers, when checking against a single allowed value for sig figs.
   * numbersToTest the numbers to feed into the validator
   * sigFigsToPass - the number of significant figures we expect the aforementioned numbers return a pass for
   * sigFigsToFail - the number of significant figures we expect the aforementioned numbers return a fail for
   */
  private static Stream<Arguments> isaacNumericValidator_CheckSignificantFiguresNoRangeCalculationWorks_multipleTests() {
    return Stream.of(
        Arguments.of(Arrays.asList("5000", "5000e3", "-5000", "-5000e3"), Arrays.asList(1, 2, 3, 4),
            Collections.singletonList(5)),
        Arguments.of(Arrays.asList("5300", "5300e3", "-5300", "-5300e3"), Arrays.asList(2, 3, 4), Arrays.asList(1, 5)),
        Arguments.of(Arrays.asList("50300", "-50300"), Arrays.asList(3, 4, 5), Arrays.asList(1, 2, 6)),
        Arguments.of(Arrays.asList("0", "-0"), Collections.singletonList(1), Collections.singletonList(2)),
        Arguments.of(Arrays.asList("0000100", "-0000100"), Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6, 7)),
        Arguments.of(Arrays.asList("0000100.00", "-0000100.00"), Collections.singletonList(5), Arrays.asList(4, 6, 7))
    );
  }

  /*
      Test the rounding to a specified number of significant figures works as expected.
   */
  @ParameterizedTest
  @MethodSource
  void isaacNumericValidator_CheckSignificantFiguresRoundingWorks(String inputValue, int sigFigsToRoundTo,
                                                                         double expectedResult) {
    double actualResult = validator.roundStringValueToSigFigs(inputValue, sigFigsToRoundTo);
    String onFailedAssertionMessage =
        String.format("sigfig rounding failed for value '%s' to %dsf: expected '%f', got '%f'", inputValue,
            sigFigsToRoundTo, expectedResult, actualResult);
    assertEquals(actualResult, expectedResult, 0.0, onFailedAssertionMessage);
  }

  private static Stream<Arguments> isaacNumericValidator_CheckSignificantFiguresRoundingWorks() {
    return Stream.of(
        Arguments.of("1.25648", 1, 1.0),

        Arguments.of("1.25648", 2, 1.3),
        Arguments.of("-1.25648", 2, -1.3),
        // Check that we're using the rounding scheme known as "round half away from zero"
        Arguments.of("17.5", 2, 18),
        Arguments.of("-17.5", 2, -18),

        Arguments.of("0.2425", 3, 0.243),
        Arguments.of("-0.2425", 3, -0.243),
        Arguments.of("0.2425", 2, 0.24),
        Arguments.of("-0.2425", 2, -0.24),

        Arguments.of("1.25E11", 2, 1.3E11),
        Arguments.of("1.25E1", 2, 1.3E1),
        Arguments.of("-4.0E11", 2, -4.0E11),
        Arguments.of("-4.0E-11", 2, -4.0E-11),

        Arguments.of("0.0", 2, 0.0),
        Arguments.of("0", 2, 0.0)
    );
  }

  /*
  Test the rounding to a specified number of significant figures works as expected.
*/
  @ParameterizedTest
  @MethodSource
  void isaacNumericValidator_CheckSignificantFiguresExtractionWorks(String inputValue, int minAllowedSigFigs,
                                                                           int maxAllowedSigFigs, int expectedResult) {
    int actualResult =
        validator.numberOfSignificantFiguresToValidateWith(inputValue, minAllowedSigFigs, maxAllowedSigFigs);
    String onFailedFirstAssertionMessage =
        String.format("sigfig extraction out of range for value %s (min allowed: %d, max allowed: %d) got %d",
            inputValue, minAllowedSigFigs, maxAllowedSigFigs, actualResult);
    assertTrue(actualResult <= maxAllowedSigFigs && actualResult >= minAllowedSigFigs, onFailedFirstAssertionMessage);
    String onFailedSecondAssertionMessage =
        String.format("sigfig extraction failed for value %s, expected: %d got %d", inputValue, expectedResult,
            actualResult);
    assertEquals(expectedResult, actualResult, onFailedSecondAssertionMessage);
  }

  private static Stream<Arguments> isaacNumericValidator_CheckSignificantFiguresExtractionWorks() {
    return Stream.of(
        // Unambiguous cases:
        Arguments.of("1", 1, 10, 1),
        Arguments.of("1.23", 2, 3, 3),
        Arguments.of("0.400", 2, 3, 3),
        Arguments.of("6.0e3", 2, 2, 2),
        // Ambiguous cases:
        Arguments.of("30000", 3, 3, 3),
        Arguments.of("10", 3, 3, 3),
        Arguments.of("3333000", 2, 4, 4)
    );
  }

  /*
      Test that the validator returns a correct response when a question's disregard sig figs flag is enabled, and
      an equivalent but excessively precise answer is provided
  */
  @Test
  final void isaacNumericValidator_DisregardSigFigsEnabledAndExactAnswerProvided_ResponseIsCorrect() {
    // Arrange
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setDisregardSignificantFigures(true);
    someNumericQuestion.setRequireUnits(false);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity("13");
    someCorrectAnswer.setCorrect(true);

    answerList.add(someCorrectAnswer);
    someNumericQuestion.setChoices(answerList);

    // Act
    Quantity userSubmittedAnswerWithExcessivePrecision = new Quantity("13.000");
    QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion,
        userSubmittedAnswerWithExcessivePrecision);

    // Assert
    assertTrue(response.isCorrect());
  }

  /*
      Test that the validator returns an incorrect response when a question's disregard sig figs flag is disabled, and
      an equivalent but excessively precise answer is provided
  */
  @Test
  final void isaacNumericValidator_DisregardSigFigsDisabledAndExactAnswerProvided_ResponseIsIncorrect() {
    // Arrange
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setDisregardSignificantFigures(false);
    someNumericQuestion.setRequireUnits(false);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity("13");
    someCorrectAnswer.setCorrect(true);

    answerList.add(someCorrectAnswer);
    someNumericQuestion.setChoices(answerList);

    // Act
    Quantity userSubmittedAnswerWithExcessivePrecision = new Quantity("13.000");
    QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion,
        userSubmittedAnswerWithExcessivePrecision);

    // Assert
    assertFalse(response.isCorrect());
  }

  /*
  Test that the validator returns an incorrect response when a question's disregard sig figs flag is enabled, and
  the user submitted answer is incorrect at a significant figure the correct answer is not specified to.
  */
  @Test
  final void isaacNumericValidator_DisregardSigFigsEnabledAndAnswerIncorrectAt3SF_ResponseIsIncorrect() {
    // Arrange
    IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
    someNumericQuestion.setDisregardSignificantFigures(true);

    List<Choice> answerList = Lists.newArrayList();
    Quantity someCorrectAnswer = new Quantity("2.1", "None");
    someCorrectAnswer.setCorrect(true);

    answerList.add(someCorrectAnswer);
    someNumericQuestion.setChoices(answerList);

    // Act
    Quantity userSubmittedAnswer = new Quantity("2.11", "None");
    QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion,
        userSubmittedAnswer);

    // Assert
    assertFalse(response.isCorrect());
  }
}
