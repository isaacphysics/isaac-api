/*
 * Copyright 2014 Stephen Cummins, 2019 James Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.quiz;

import com.google.api.client.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Quantity;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the user manager class.
 *
 */
@PowerMockIgnore({"jakarta.ws.*"})
public class IsaacNumericValidatorTest {
    private IsaacNumericValidator validator;
    private IsaacNumericQuestion numericQuestionNoUnits;
    private IsaacNumericQuestion numericQuestionWithUnits;

    private String correctDecimalExponentAnswer = "4.8e22";
    private String correctIntegerAnswer = "42";
    private String correctUnits = "m\\,s^{-1}";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Initial configuration of tests.
     *
     */
    @Before
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
    public final void isaacNumericValidator_NonNumericValue_InvalidResponseShouldBeReturned() {
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
    public final void isaacNumericValidator_CheckCorrectAnswerWithExponent_CorrectResponseShouldHappen() {
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
    public final void isaacNumericValidator_CheckCorrectIntegerAnswer_CorrectResponseShouldHappen() {
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
    public final void isaacNumericValidator_CheckCorrectAnswerWithCorrectUnits_CorrectResponseShouldHappen() {
        // Set up user answer:
        Quantity q = new Quantity(correctIntegerAnswer, correctUnits);

        // Test response:
        QuantityValidationResponse response = (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);
        assertTrue(response.isCorrect());
        assertTrue(response.getCorrectUnits());
    }

    /*
        Test an incorrect integer answer with correct units gets recognised as incorrect, but with correct units.
     */
    @Test
    public final void isaacNumericValidator_CheckCorrectAnswerWithIncorrectValue_IncorrectResponseShouldHappen() {
        // Set up user answer:
        Quantity q = new Quantity("43", correctUnits);

        // Test response:
        QuantityValidationResponse response = (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);

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
    public final void isaacNumericValidator_IncorrectSigFigsCorrectUnit_IncorrectResponseShouldHappen() {
        // Set up user answer:
        Quantity q_tooFew = new Quantity("4", correctUnits);

        // Test response:
        QuantityValidationResponse responseTooFew = (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q_tooFew);

        // Check answer is wrong,
        assertFalse(responseTooFew.isCorrect());
        assertFalse(responseTooFew.getCorrectValue());
        // but units are right:
        assertTrue(responseTooFew.getCorrectUnits());

        // Set up user answer:
        Quantity q_tooMany = new Quantity("42.000", correctUnits);

        // Test response:
        QuantityValidationResponse responseTooMany = (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q_tooMany);

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
    public final void isaacNumericValidator_CheckCorrectAnswerWithIncorrectUnits_IncorrectResponseShouldHappen() {
        // Set up user answer:
        Quantity q = new Quantity(correctIntegerAnswer, "m\\,h^{-1}");

        // Test response:
        QuantityValidationResponse response = (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);

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
    public final void isaacNumericValidator_CheckCorrectAnswerWithNoUnits_IncorrectResponseShouldHappen() {
        // Set up user answer without units:
        Quantity q = new Quantity(correctIntegerAnswer);

        // Test response:
        QuantityValidationResponse response = (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);

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
    public final void isaacNumericValidator_CheckCorrectAnswerWithNoValue_IncorrectResponseShouldHappen() {
        // Set up user answer without units:
        Quantity q = new Quantity("", correctUnits);

        // Test response:
        QuantityValidationResponse response = (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);

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
    public final void isaacNumericValidator_InvalidStandardForm_WarningShouldBeReturned() {
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
    public final void isaacNumericValidator_CheckExactIncorrectAnswerWithIncorrectSigFigs_KnownIncorrectResponseShouldHappen() {
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
    public final void isaacNumericValidator_CheckKnownIncorrectAnswerWithIncorrectSigFigs_KnownIncorrectResponseShouldHappen() {
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
    public final void isaacNumericValidator_CheckCorrectAnswerWrongSigFigs_IncorrectResponseShouldHappen() {
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
    public final void isaacNumericValidator_TestQuestionWithSigFigRange_IncorrectAnswers() {
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
        Quantity q_3sf = new Quantity("1.66");
        // Test response:
        QuestionValidationResponse response_3sf = validator.validateQuestionResponse(someNumericQuestion, q_3sf);
        assertFalse(response_3sf.isCorrect());
        assertFalse(response_3sf.getExplanation().getTags().contains("sig_figs"));

        // Set up a user answer:
        Quantity q_2sf = new Quantity("2.0");
        // Test response:
        QuestionValidationResponse response_2sf = validator.validateQuestionResponse(someNumericQuestion, q_2sf);
        assertFalse(response_2sf.isCorrect());
        assertFalse(response_2sf.getExplanation().getTags().contains("sig_figs"));
    }

    /*
        Test correct but incorrect sig fig answers to questions allowing a range of significant figures.
     */
    @Test
    public final void isaacNumericValidator_TestQuestionWithSigFigRange_IncorrectSigFigAnswers() {
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
        Quantity q_5sf = new Quantity("1.6875");
        // Test response:
        QuestionValidationResponse response_5sf = validator.validateQuestionResponse(someNumericQuestion, q_5sf);
        assertFalse("expected 1.6875 not to match 1.6875 to 2 or 3 sf", response_5sf.isCorrect());
        assertTrue(response_5sf.getExplanation().getTags().contains("sig_figs"));

        // Set up a user answer:
        Quantity q_1sf = new Quantity("2");
        // Test response:
        QuestionValidationResponse response_1sf = validator.validateQuestionResponse(someNumericQuestion, q_1sf);
        assertFalse("expected 2 not to match 1.6875 to 2 or 3 sf", response_1sf.isCorrect());
        assertTrue(response_1sf.getExplanation().getTags().contains("sig_figs"));
    }

    /*
    Test incorrect sig fig answers to questions allowing a range of significant figures.
 */
    @Test
    public final void isaacNumericValidator_TestQuestionWithSigFigRange_SigFigResponses() {
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
        Quantity q_5sf_corr = new Quantity("1.6875");
        // Test response is sig fig message:
        QuestionValidationResponse response_5sf_corr = validator.validateQuestionResponse(someNumericQuestion, q_5sf_corr);
        assertFalse("expected 1.6875 not to match 1.6875 to 2 or 3 sf", response_5sf_corr.isCorrect());
        assertTrue(response_5sf_corr.getExplanation().getTags().contains("sig_figs"));
        assertTrue(response_5sf_corr.getExplanation().getTags().contains("sig_figs_too_many"));

        // Set up a wrong user answer with too many sig figs:
        Quantity q_5sf_wrong = new Quantity("2.7986");
        // Test response does not mention sig figs:
        QuestionValidationResponse response_5sf_wrong = validator.validateQuestionResponse(someNumericQuestion, q_5sf_wrong);
        assertFalse("expected 2.7986 not to match 1.6875", response_5sf_wrong.isCorrect());
        assertFalse("expected 2.7986 without sig fig message", response_5sf_wrong.getExplanation().getTags().contains("sig_figs"));

        // Set up a user answer:
        Quantity q_1sf = new Quantity("5");
        // Test response:
        QuestionValidationResponse response_1sf = validator.validateQuestionResponse(someNumericQuestion, q_1sf);
        assertFalse("expected 5 not to match 1.6875 to 2 or 3 sf", response_1sf.isCorrect());
        assertTrue(response_1sf.getExplanation().getTags().contains("sig_figs"));
        assertTrue(response_1sf.getExplanation().getTags().contains("sig_figs_too_few"));
    }

    /*
        Test default feedback is used when a known answer is matched and no explanation given.
    */
    @Test
    public final void isaacNumericValidator_DefaultFeedbackAndCorrectNoExplanation_DefaultFeedbackReturned() {
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
    public final void isaacNumericValidator_CheckCorrectUnitsIncorrectChoiceMatch_CorrectUnitsResponseShouldHappen() {
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
    public final void isaacNumericValidator_NoChoices_IncorrectResponseShouldBeReturned() {
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
    public final void isaacNumericValidator_TestChoiceMissingRequiredUnits_IncorrectResponse() {
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
    public final void isaacNumericValidator_WrongChoiceType_IncorrectResponseShouldBeReturned() {
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
    public final void isaacNumericValidator_TestInvalidSigFigRange_IncorrectResponse() {
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
    public final void isaacNumericValidator_WrongQuestionType_ExceptionShouldBeThrown() {
        IsaacQuickQuestion invalidQuestionType = new IsaacQuickQuestion();
        invalidQuestionType.setId("invalidQuestionType");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("only works with Isaac Numeric Questions");

        // This should throw an exception:
        validator.validateQuestionResponse(invalidQuestionType, new Quantity());
    }

    /*
     Test that incorrect submitted choice types are detected.
    */
    @Test
    public final void isaacNumericValidator_WrongChoiceType_ExceptionShouldBeThrown() {
        IsaacNumericQuestion numericQuestion = new IsaacNumericQuestion();
        numericQuestion.setId("invalidQuestionType");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Expected Quantity for IsaacNumericQuestion");

        // This should throw an exception:
        validator.validateQuestionResponse(numericQuestion, new Choice());
    }

    /*
     Test displayUnit overrides requiresUnits.
    */
    @Test
    public final void isaacNumericValidator_TestInconsistentDisplayUnitsOverride_UnitsIgnored() {
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
            QuantityValidationResponse qResponse = (QuantityValidationResponse) response;
            assertNull(qResponse.getCorrectUnits());
        }
    }

    //  ---------- Tests from here test internal methods of the validator ----------

    /*
        Test parsing common unambiguous representations as numbers.
     */
    @Test
    public final void isaacNumericValidator_CheckParsingAsNumberWorks() throws Exception {
        String numberToMatch = "42";
        List<String> numbersToTest = Arrays.asList("42", "4.2e1", "4.2E1", "4.2x10^1", "4.2*10**1", "4.2×10^(1)", "4.2 \\times 10^{1}");

        for (String numberToTest : numbersToTest) {
            boolean result = ValidationUtils.numericValuesMatch(numberToMatch, numberToTest, 2, (Logger) Whitebox.getField(validator.getClass(), "log").get(validator));
            assertTrue(result);
        }

        String powerOfTenToMatch = "10000";
        List<String> powersOfTenToTest = Arrays.asList("10000", "1x10^4", "1e4", "1E4", "1 x 10**4", "10^4", "10**(4)", "10^{4}", "100x10^2");
        for (String powerOfTenToTest : powersOfTenToTest) {
            boolean result = ValidationUtils.numericValuesMatch(powerOfTenToMatch, powerOfTenToTest, 1, (Logger) Whitebox.getField(validator.getClass(), "log").get(validator));
            assertTrue(result);
        }
    }

    /*
        Test various numbers for significant figure rules.
     */
    @Test
    public final void isaacNumericValidator_CheckSignificantFiguresNoRangeCalculationWorks_multipleTests() throws Exception {
        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("5000", "5000e3", "-5000", "-5000e3"), Arrays.asList(1, 2, 3, 4), Collections.singletonList(5));

        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("5300", "5300e3", "-5300", "-5300e3"), Arrays.asList(2, 3, 4), Arrays.asList(1, 5));

        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("50300", "-50300"), Arrays.asList(3, 4, 5), Arrays.asList(1, 2, 6));

        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("0", "-0"), Collections.singletonList(1), Collections.singletonList(2));

        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("0000100", "-0000100"), Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6, 7));

        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("0000100.00", "-0000100.00"), Collections.singletonList(5), Arrays.asList(4, 6, 7));
    }

    /*
        Test the rounding to a specified number of significant figures works as expected.
     */
    @Test
    public final void isaacNumericValidator_CheckSignificantFiguresRoundingWorks() throws Exception {
        this.testSigFigRoundingWorks("1.25648", 1, 1.0);

        this.testSigFigRoundingWorks("1.25648", 2, 1.3);
        this.testSigFigRoundingWorks("-1.25648", 2, -1.3);

        // Check that we're using the rounding scheme known as "round half away from zero"
        this.testSigFigRoundingWorks("17.5", 2, 18);
        this.testSigFigRoundingWorks("-17.5", 2, -18);

        this.testSigFigRoundingWorks("0.2425", 3, 0.243);
        this.testSigFigRoundingWorks("-0.2425", 3, -0.243);
        this.testSigFigRoundingWorks("0.2425", 2, 0.24);
        this.testSigFigRoundingWorks("-0.2425", 2, -0.24);

        this.testSigFigRoundingWorks("1.25E11", 2, 1.3E11);
        this.testSigFigRoundingWorks("1.25E1", 2, 1.3E1);
        this.testSigFigRoundingWorks("-4.0E11", 2, -4.0E11);
        this.testSigFigRoundingWorks("-4.0E-11", 2, -4.0E-11);

        this.testSigFigRoundingWorks("0.0", 2, 0.0);
        this.testSigFigRoundingWorks("0", 2, 0.0);
    }

    /*
    Test the rounding to a specified number of significant figures works as expected.
 */
    @Test
    public final void isaacNumericValidator_CheckSignificantFiguresExtractionWorks() throws Exception {
        // Unambiguous cases:
        this.testSigFigExtractionWorks("1", 1, 10, 1);
        this.testSigFigExtractionWorks("1.23", 2, 3, 3);
        this.testSigFigExtractionWorks("0.400", 2, 3, 3);
        this.testSigFigExtractionWorks("6.0e3", 2, 2, 2);
        // Ambiguous cases:
        this.testSigFigExtractionWorks("30000", 3, 3, 3);
        this.testSigFigExtractionWorks("10", 3, 3, 3);
        this.testSigFigExtractionWorks("3333000", 2, 4, 4);
    }

    /*
        Test that the validator returns a correct response when a question's disregard sig figs flag is enabled, and
        an equivalent but excessively precise answer is provided
    */
    @Test
    public final void isaacNumericValidator_DisregardSigFigsEnabledAndExactAnswerProvided_ResponseIsCorrect() {
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
    public final void isaacNumericValidator_DisregardSigFigsDisabledAndExactAnswerProvided_ResponseIsIncorrect() {
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
    public final void isaacNumericValidator_DisregardSigFigsEnabledAndAnswerIncorrectAt3SF_ResponseIsIncorrect() {
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

    /**
     * Test that the validator converts null valued significant figures to the default value and thus evaluates the
     * the answer as correct while disregardSignificantFigures is not set
     */
    @Test
    public final void isaacNumericValidator_correctAnswerWithNullSigFigsInQuestion_responseIsCorrect() {
        // ARRANGE
        List<IsaacNumericQuestion> questions = new LinkedList<>();

        questions.add(createIsaacNumericQuestion(false, null, null));
        questions.add(createIsaacNumericQuestion(false, 1, null));
        questions.add(createIsaacNumericQuestion(false, null, 2));

        List<Choice> answerList = Lists.newArrayList();
        Quantity correctAnswer = new Quantity("2.1", "None");
        correctAnswer.setCorrect(true);
        answerList.add(correctAnswer);

        for(IsaacNumericQuestion question : questions) {
            question.setChoices(answerList);
        }

        // ACT
        for (IsaacNumericQuestion q : questions) {
            QuestionValidationResponse response = validator.validateQuestionResponse(q, correctAnswer);

        // ASSERT
            System.out.println(response.isCorrect());
            assertTrue(response.isCorrect());
        }
    }

    /**
     * Test that the validator returns an "unanswerable question" response if any of the significant figures are below
     * one while disregardSignificantFigures is not set
     */
    @Test
    public final void isaacNumericValidator_correctAnswerWithSigFigLessThanOne_unanswerableQuestionResponse() {
        // ARRANGE
        List<IsaacNumericQuestion> questions = new LinkedList<>();

        List<Choice> answerList = Lists.newArrayList();
        Quantity correctAnswer = new Quantity("2.1", "None");
        correctAnswer.setCorrect(true);
        answerList.add(correctAnswer);

        questions.add(createIsaacNumericQuestion(false, 0, 1));
        questions.add(createIsaacNumericQuestion(false, 1, 0));
        questions.add(createIsaacNumericQuestion(false, 0, 0));

        for(IsaacNumericQuestion question : questions) {
            question.setChoices(answerList);
        }

        // ACT
        for (IsaacNumericQuestion q : questions) {
            QuestionValidationResponse response = validator.validateQuestionResponse(q, correctAnswer);

        // ASSERT
            assertEquals("This question cannot be answered correctly.", response.getExplanation().getValue());
        }
    }

    /**
     * Test that the validator returns an "unanswerable question" response if maximum significant figures is below
     * the minimum significant figures while disregardSignificantFigures is not set
     */
    @Test
    public final void isaacNumericValidator_correctAnswerWithMaxLessThanMin_unanswerableQuestionResponse() {
        // ARRANGE
        IsaacNumericQuestion question = createIsaacNumericQuestion(false, 2, 1);

        List<Choice> answerList = Lists.newArrayList();

        Quantity correctAnswer = new Quantity("2.1", "None");
        correctAnswer.setCorrect(true);

        answerList.add(correctAnswer);
        question.setChoices(answerList);

        // ACT
        QuestionValidationResponse response = validator.validateQuestionResponse(question, correctAnswer);

        // ASSERT
        assertEquals("This question cannot be answered correctly.", response.getExplanation().getValue());
    }

    /**
     * Test that the validator correctly validates questions when both significant figures are set correctly while
     * disregardSignificantFigures is not set
     */
    @Test
    public final void isaacNumericValidator_correctAnswerWithMinLessThanMax_responseIsCorrect() {
        // ARRANGE
        IsaacNumericQuestion question = createIsaacNumericQuestion(false, 5, 6);

        List<Choice> answerList = Lists.newArrayList();

        Quantity correctAnswer = new Quantity("2.12345", "None");
        correctAnswer.setCorrect(true);

        answerList.add(correctAnswer);
        question.setChoices(answerList);

        // ACT
        QuestionValidationResponse response = validator.validateQuestionResponse(question, correctAnswer);

        // ASSERT
        assertTrue(response.isCorrect());
    }

    /**
     * Test that the validator returns a correct answer response regardless of whether the significant figures are
     * incorrect when disregardSignificantFigures is set
     */
    @Test
    public final void isaacNumericValidator_correctAnswerWithDisregardSigFigs_responseIsCorrectRegardless() {
        // ARRANGE
        List<IsaacNumericQuestion> questions = new LinkedList<>();

        List<Choice> answerList = Lists.newArrayList();
        Quantity correctAnswer = new Quantity("2.1", "None");
        correctAnswer.setCorrect(true);
        answerList.add(correctAnswer);

        questions.add(createIsaacNumericQuestion(false, null, null));
        questions.add(createIsaacNumericQuestion(false, 1, null));
        questions.add(createIsaacNumericQuestion(false, null, 2));
        questions.add(createIsaacNumericQuestion(true, 0, 1));
        questions.add(createIsaacNumericQuestion(true, 1, 0));
        questions.add(createIsaacNumericQuestion(true, 0, 0));
        questions.add(createIsaacNumericQuestion(true, 2, 1));
        questions.add(createIsaacNumericQuestion(true, 5, 6));

        for(IsaacNumericQuestion question : questions) {
            question.setChoices(answerList);
        }

        // ACT
        for (IsaacNumericQuestion q : questions) {
            QuestionValidationResponse response = validator.validateQuestionResponse(q, correctAnswer);

            // ASSERT
            assertTrue(response.isCorrect());
        }
    }

    //  ---------- Helper methods to test internal functionality of the validator class ----------

    private void testSigFigRoundingWorks(String inputValue, int sigFigToRoundTo, double expectedResult) throws Exception {
        double result = ValidationUtils.roundStringValueToSigFigs(inputValue, sigFigToRoundTo, (Logger) Whitebox.getField(validator.getClass(), "log").get(validator));

        assertEquals("sigfig rounding failed for value '" + inputValue + "' to " + sigFigToRoundTo
                + "sf: expected '" + expectedResult + "', got '" + result + "'", result, expectedResult, 0.0);
    }

    private void testSigFigExtractionWorks(String inputValue, int minAllowedSigFigs, int maxAllowedSigFigs,
                                           int expectedResult) throws Exception {
        int result = ValidationUtils.numberOfSignificantFiguresToValidateWith(inputValue, minAllowedSigFigs, maxAllowedSigFigs, (Logger) Whitebox.getField(validator.getClass(), "log").get(validator));

        assertTrue("sigfig extraction out of range for value " + inputValue + " (min allowed: " + minAllowedSigFigs
                + ", max allowed: " + maxAllowedSigFigs + ") got " + result, result <= maxAllowedSigFigs && result >= minAllowedSigFigs);
        assertEquals("sigfig extraction failed for value " + inputValue + ", expected: " + expectedResult
                + " got " + result, result, expectedResult);
    }


    /**
     * Helper to launch multiple sig fig tests on given numbers, when checking against a single allowed value for sig figs.
     *
     * @param numbersToTest the numbers to feed into the validator
     * @param sigFigsToPass - the number of significant figures we expect the aforementioned numbers return a pass for
     * @param sigFigsToFail - the number of significant figures we expect the aforementioned numbers return a fail for
     * @throws Exception - if we can't execute the private method.
     */
    private void verifyCorrectNumberOfSigFigsNoRange(List<String> numbersToTest, List<Integer> sigFigsToPass, List<Integer> sigFigsToFail) throws Exception {
        for (String number : numbersToTest) {

            for (Integer sigFig : sigFigsToPass) {
                boolean tooFew = ValidationUtils.tooFewSignificantFigures(number, sigFig, (Logger) Whitebox.getField(validator.getClass(), "log").get(validator));
                assertFalse("Unexpected too few sig fig for " + number + " @ " + sigFig + "sf", tooFew);
                boolean tooMany = ValidationUtils.tooManySignificantFigures(number, sigFig, (Logger) Whitebox.getField(validator.getClass(), "log").get(validator));
                assertFalse("Unexpected too many sig fig for " + number + " @ " + sigFig + "sf", tooMany);
            }

            for (Integer sigFig : sigFigsToFail) {
                boolean tooFew = ValidationUtils.tooFewSignificantFigures(number, sigFig, (Logger) Whitebox.getField(validator.getClass(), "log").get(validator));
                boolean tooMany = ValidationUtils.tooManySignificantFigures(number, sigFig, (Logger) Whitebox.getField(validator.getClass(), "log").get(validator));
                boolean incorrectSigFig = tooMany || tooFew;
                assertTrue("Expected incorrect sig fig for " + number + " @ " + sigFig + "sf", incorrectSigFig);
            }
        }
    }

    /**
     * Helper method for the recordContentTypeSpecificError tests,
     * generates an IsaacNumericQuestion with provided significant figure information
     *
     * @param disregardSigFig - Whether to set the disregardSignificantFigures to true
     * @param sigFigMin       - The minimum significant figures value
     * @param sigFigMax       - The maximum significant figures value
     * @return The new IsaacNumericQuestion object
     */
    private IsaacNumericQuestion createIsaacNumericQuestion(final Boolean disregardSigFig,
                                                            final Integer sigFigMin,
                                                            final Integer sigFigMax) {
        IsaacNumericQuestion question = new IsaacNumericQuestion();

        // TODO: See if there is a "better" way of initialising object fields
        question.setTitle("");
        question.setType("isaacQuestion");
        question.setChoices(new LinkedList<>());

        question.setDisregardSignificantFigures(disregardSigFig);
        question.setSignificantFiguresMin(sigFigMin);
        question.setSignificantFiguresMax(sigFigMax);

        return question;
    }
}
