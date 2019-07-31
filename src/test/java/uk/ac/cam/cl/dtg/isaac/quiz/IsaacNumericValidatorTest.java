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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.reflect.Whitebox;

import com.google.api.client.util.Lists;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;

/**
 * Test class for the user manager class.
 *
 */
@PowerMockIgnore({"javax.ws.*"})
public class IsaacNumericValidatorTest {
    private IsaacNumericValidator validator;

    /**
     * Initial configuration of tests.
     *
     */
    @Before
    public final void setUp() {
        validator = new IsaacNumericValidator();
    }

    /*
        Test that the "not a valid number" response is returned for non-numeric input.
     */
    @Test
    public final void isaacNumericValidator_NonNumericValue_InvalidResponseShouldBeReturned() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("4.8e22");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        String explanationShouldContain = "not a valid number";

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("4.8[]3");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains(explanationShouldContain));
    }

    /*
        Test a correct answer with an exponent in it gets recognised as correct.
     */
    @Test
    public final void isaacNumericValidator_CheckCorrectAnswerWithExponent_CorrectResponseShouldHappen() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("4.8e22");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("4.8e22");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);
        assertTrue(response.isCorrect());
    }

    /*
        Test a correct integer answer without units gets recognised as correct.
     */
    @Test
    public final void isaacNumericValidator_CheckCorrectIntegerAnswer_CorrectResponseShouldHappen() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("42");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("42");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);
        assertTrue(response.isCorrect());
    }

    /*
        Test a correct integer answer with correct units gets recognised as correct.
     */
    @Test
    public final void isaacNumericValidator_CheckCorrectAnswerWithCorrectUnits_CorrectResponseShouldHappen() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(true);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("42");
        someCorrectAnswer.setUnits("m\\,s^{-1}");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("42");
        q.setUnits("m\\,s^{-1}");

        // Test response:
        QuantityValidationResponse response = (QuantityValidationResponse) validator.validateQuestionResponse(someNumericQuestion, q);
        assertTrue("check question is marked as correct", response.isCorrect());
        assertTrue("check units is correct", response.getCorrectUnits());
    }

    /*
        Test an incorrect integer answer with correct units gets recognised as incorrect, but with correct units.
     */
    @Test
    public final void isaacNumericValidator_CheckCorrectAnswerWithIncorrectValue_IncorrectResponseShouldHappen() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(true);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("42");
        someCorrectAnswer.setUnits("m\\,s^{-1}");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("43");
        q.setUnits("m\\,s^{-1}");

        // Test response:
        QuantityValidationResponse response = (QuantityValidationResponse) validator.validateQuestionResponse(someNumericQuestion, q);

        assertFalse("check question is marked as incorrect", response.isCorrect());

        assertFalse("check value is marked as incorrect", response.getCorrectValue());
        assertTrue("check units are marked as correct", response.getCorrectUnits());
    }

    /*
        Test a correct integer answer with incorrect units gets recognised as incorrect, but with correct value.
     */
    @Test
    public final void isaacNumericValidator_CheckCorrectAnswerWithIncorrectUnits_IncorrectResponseShouldHappen() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(true);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("42");
        someCorrectAnswer.setUnits("m\\,s^{-1}");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("42");
        q.setUnits("m\\,h^{-1}");

        // Test response:
        QuantityValidationResponse response = (QuantityValidationResponse) validator.validateQuestionResponse(someNumericQuestion, q);

        assertFalse("check question is marked as incorrect", response.isCorrect());

        assertTrue("check value is marked as correct", response.getCorrectValue());
        assertFalse("check units are marked as incorrect", response.getCorrectUnits());
    }

    /*
        Test a correct answer with missing units gets recognised as incorrect but with correct units.
     */
    @Test
    public final void isaacNumericValidator_CheckCorrectAnswerWithNoUnits_IncorrectResponseShouldHappen() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(true);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("42");
        someCorrectAnswer.setUnits("m\\,s^{-1}");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("42");

        // Test response:
        QuantityValidationResponse response = (QuantityValidationResponse) validator.validateQuestionResponse(someNumericQuestion, q);

        assertFalse("check question is marked as incorrect", response.isCorrect());
        assertFalse("check units are marked as incorrect", response.getCorrectUnits());

        assertTrue("Appropriate message provided", response.getExplanation().getValue().contains("units"));
    }

    /*
        Test known wrong answers take precedence over sig fig warning.
     */
    @Test
    public final void isaacNumericValidator_CheckKnownIncorrectAnswerWithIncorrectSigFigs_KnownIncorrectResponseShouldHappen() {
        // Set up the question object:
        String someExplanation = "some explanation";
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someIncorrectAnswer = new Quantity();
        someIncorrectAnswer.setValue("0");
        someIncorrectAnswer.setCorrect(false);
        someIncorrectAnswer.setExplanation(new Content(someExplanation));
        answerList.add(someIncorrectAnswer);

        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("0.13");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("0");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

        assertFalse("Response should be incorrect", response.isCorrect());

        assertTrue("provide specific explanation", response.getExplanation().getValue().toLowerCase().contains(someExplanation.toLowerCase()));
    }

    /*
        Test significant figure warning message in answer with exponent.
     */
    @Test
    public final void isaacNumericValidator_CheckCorrectAnswerWithExponentIncorrectSigFigs_IncorrectResponseShouldHappen() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("4.8e22");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("4.81e22");

        // Test response:
        String explanationShouldContain = "significant figures";

        QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

        assertFalse("Response should be incorrect", response.isCorrect());

        assertTrue("Explanation should warn about sig figs", response.getExplanation().getValue().toLowerCase().contains(explanationShouldContain.toLowerCase()));
    }

    /*
        Test incorrect answer with exponent and without units recognised as incorrect.
     */
    @Test
    public final void isaacNumericValidator_CheckIncorrectAnswerWithExponent_IncorrectResponseShouldHappen() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("4.8e22");
        someCorrectAnswer.setCorrect(false);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("4.8e22");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

        assertFalse(response.isCorrect());
    }

    /*
        Test incorrect response provided when no answers exist for a question.
     */
    @Test
    public final void isaacNumericValidator_CheckAnswerNotFoundWhenNoChoicesProvided_IncorrectResponseShouldHappen() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);

        List<Choice> answerList = Lists.newArrayList();
        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("4.8e22");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

        assertFalse(response.isCorrect());
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
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("4.8");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("4.81");

        // Test response:
        String explanationShouldContain = "significant figures";

        QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

        assertFalse(response.isCorrect());

        assertTrue(response.getExplanation().getValue().toLowerCase().contains(explanationShouldContain.toLowerCase()));
    }

    /*
        Test significant figure warning for incorrect answer without exponent.
     */
    @Test
    public final void isaacNumericValidator_CheckIncorrectAnswerWrongSigFigs_IncorrectResponseShouldHappen() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("4.8");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("4.881");

        // Test response:
        String explanationShouldContain = "significant figures";

        QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

        assertFalse(response.isCorrect());

        assertTrue(response.getExplanation().getValue().toLowerCase().contains(explanationShouldContain.toLowerCase()));
    }

    /*
        Test known incorrect answer with negative exponent.
     */
    @Test
    public final void isaacNumericValidator_CheckKnownIncorrectAnswerWithNegativeExponent_IncorrectResponseShouldHappenWithExplain() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("4.8e22");
        someCorrectAnswer.setCorrect(true);

        Quantity someIncorrectAnswer = new Quantity();
        Content someExplanation = new Content("someIncorrectExplanation");

        someIncorrectAnswer.setValue("1.2e-28");
        someIncorrectAnswer.setCorrect(false);
        someIncorrectAnswer.setExplanation(someExplanation);

        answerList.add(someCorrectAnswer);
        answerList.add(someIncorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("1.2e-28");

        // Test response:
        String explanationShouldContain = someExplanation.getValue();

        QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

        assertFalse(response.isCorrect());

        assertTrue(response.getExplanation().getValue().equals(explanationShouldContain));
    }

    /*
        Test unknown incorrect answer with negative exponent.
     */
    @Test
    public final void isaacNumericValidator_CheckUnknownIncorrectAnswerWithNegativeExponent_GeneralIncorrectResponseShouldHappen() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("4.8e22");
        someCorrectAnswer.setCorrect(true);

        Quantity someIncorrectAnswer = new Quantity();
        Content someExplanation = new Content("some Incorrect Explanation"); // this should not be what we see.

        someIncorrectAnswer.setValue("1.2e-28");
        someIncorrectAnswer.setCorrect(false);
        someIncorrectAnswer.setExplanation(someExplanation);

        answerList.add(someCorrectAnswer);
        answerList.add(someIncorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        // Set up user answer:
        Quantity q = new Quantity();
        q.setValue("5e-22");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);

        assertFalse(response.isCorrect());

        assertTrue(!response.getExplanation().getValue().equals(someExplanation.getValue()));
    }

    /*
        Test correct answers to questions allowing a range of significant figures.
     */
    @Test
    public final void isaacNumericValidator_TestQuestionWithSigFigRange_CorrectAnswers() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);
        someNumericQuestion.setSignificantFiguresMin(2);
        someNumericQuestion.setSignificantFiguresMax(3);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("1.6875");
        someCorrectAnswer.setCorrect(true);

        answerList.add(someCorrectAnswer);
        someNumericQuestion.setChoices(answerList);

        // Set up a user answer:
        Quantity q_3sf = new Quantity();
        q_3sf.setValue("1.69");
        // Test response:
        QuestionValidationResponse response_3sf = validator.validateQuestionResponse(someNumericQuestion, q_3sf);
        assertTrue("expected 1.69 to match 1.6875 to 2 or 3 sf", response_3sf.isCorrect());

        // Set up a user answer:
        Quantity q_2sf = new Quantity();
        q_2sf.setValue("1.7");
        // Test response:
        QuestionValidationResponse response_2sf = validator.validateQuestionResponse(someNumericQuestion, q_2sf);
        assertTrue("expected 1.7 to match 1.6875 to 2 or 3 sf", response_2sf.isCorrect());
    }

    /*
        Test incorrect answers to questions allowing a range of significant figures.
     */
    @Test
    public final void isaacNumericValidator_TestQuestionWithSigFigRange_IncorrectAnswers() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);
        someNumericQuestion.setSignificantFiguresMin(2);
        someNumericQuestion.setSignificantFiguresMax(3);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("1.6875");
        someCorrectAnswer.setCorrect(true);

        answerList.add(someCorrectAnswer);
        someNumericQuestion.setChoices(answerList);

        // Set up a user answer:
        Quantity q_3sf = new Quantity();
        q_3sf.setValue("1.66");
        // Test response:
        QuestionValidationResponse response_3sf = validator.validateQuestionResponse(someNumericQuestion, q_3sf);
        assertFalse("expected 1.66 not to match 1.6875 to 2 or 3 sf", response_3sf.isCorrect());
        assertFalse("expected default incorrect response",
                response_3sf.getExplanation().getValue().toLowerCase().contains("significant figures"));

        // Set up a user answer:
        Quantity q_2sf = new Quantity();
        q_2sf.setValue("2.0");
        // Test response:
        QuestionValidationResponse response_2sf = validator.validateQuestionResponse(someNumericQuestion, q_2sf);
        assertFalse("expected 2.0 to match 1.6875 to 2 or 3 sf", response_2sf.isCorrect());
        assertFalse("expected default incorrect response",
                response_2sf.getExplanation().getValue().toLowerCase().contains("significant figures"));
    }

    /*
        Test incorrect sig fig answers to questions allowing a range of significant figures.
     */
    @Test
    public final void isaacNumericValidator_TestQuestionWithSigFigRange_IncorrectSigFigAnswers() {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);
        someNumericQuestion.setSignificantFiguresMin(2);
        someNumericQuestion.setSignificantFiguresMax(3);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("1.6875");
        someCorrectAnswer.setCorrect(true);

        answerList.add(someCorrectAnswer);
        someNumericQuestion.setChoices(answerList);

        // Set up a user answer:
        Quantity q_5sf = new Quantity();
        q_5sf.setValue("1.6875");
        // Test response:
        QuestionValidationResponse response_5sf = validator.validateQuestionResponse(someNumericQuestion, q_5sf);
        assertFalse("expected 1.6875 not to match 1.6875 to 2 or 3 sf", response_5sf.isCorrect());
        assertTrue("expected sig fig incorrect response",
                response_5sf.getExplanation().getValue().toLowerCase().contains("significant figures"));

        // Set up a user answer:
        Quantity q_1sf = new Quantity();
        q_1sf.setValue("2");
        // Test response:
        QuestionValidationResponse response_1sf = validator.validateQuestionResponse(someNumericQuestion, q_1sf);
        assertFalse("expected 2 not to match 1.6875 to 2 or 3 sf", response_1sf.isCorrect());
        assertTrue("expected sig fig incorrect response",
                response_1sf.getExplanation().getValue().toLowerCase().contains("significant figures"));
    }

    /*
        Test parsing common unambiguous representations as numbers.
     */
    @Test
    public final void isaacNumericValidator_CheckParsingAsNumberWorks() throws Exception {
        // Set up the question object:
        IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
        someNumericQuestion.setRequireUnits(false);

        List<Choice> answerList = Lists.newArrayList();
        Quantity someCorrectAnswer = new Quantity();
        someCorrectAnswer.setValue("42");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someNumericQuestion.setChoices(answerList);

        List<String> numbersToTest = Arrays.asList("42", "4.2e1", "4.2E1", "4.2x10^1", "4.2*10**1", "4.2×10^(1)", "4.2 \\times 10^{1}");

        for (String numberToTest : numbersToTest) {
            // Set up user answer:
            Quantity q = new Quantity();
            q.setValue(numberToTest);

            // Test response:
            QuestionValidationResponse response = validator.validateQuestionResponse(someNumericQuestion, q);
            assertTrue("Expected '" + numberToTest + "' to be valid and correct!", response.isCorrect());
        }
    }

    /*
        Test various numbers for significant figure rules.
     */
    @Test
    public final void isaacNumericValidator_CheckSignificantFiguresNoRangeCalculationWorks_multipleTests() throws Exception {
        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("5000", "5000e3", "-5000", "-5000e3"), Arrays.asList(1, 2, 3, 4), Arrays.asList(5));

        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("5300", "5300e3", "-5300", "-5300e3"), Arrays.asList(2, 3, 4), Arrays.asList(1, 5));

        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("50300", "-50300"), Arrays.asList(3, 4, 5), Arrays.asList(1, 2, 6));

        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("0", "-0"), Arrays.asList(1), Arrays.asList(2));

        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("0000100", "-0000100"), Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6, 7));

        verifyCorrectNumberOfSigFigsNoRange(Arrays.asList("0000100.00", "-0000100.00"), Arrays.asList(5), Arrays.asList(4, 6, 7));
    }

    /*
        Test the rounding to a specified number of significant figures works as expected.
     */
    @Test
    public final void isaacNumericValidator_CheckSignificantFiguresRoundingWorks() throws Exception {
        IsaacNumericValidator test = new IsaacNumericValidator();
        this.testSigFigRoundingWorks(test, "1.25648", 1, 1.0);

        this.testSigFigRoundingWorks(test, "1.25648", 2, 1.3);
        this.testSigFigRoundingWorks(test, "-1.25648", 2, -1.3);

        // Check that we're using the rounding scheme known as "round half away from zero"
        this.testSigFigRoundingWorks(test, "17.5", 2, 18);
        this.testSigFigRoundingWorks(test, "-17.5", 2, -18);

        this.testSigFigRoundingWorks(test, "0.2425", 3, 0.243);
        this.testSigFigRoundingWorks(test, "-0.2425", 3, -0.243);
        this.testSigFigRoundingWorks(test, "0.2425", 2, 0.24);
        this.testSigFigRoundingWorks(test, "-0.2425", 2, -0.24);

        this.testSigFigRoundingWorks(test, "1.25E11", 2, 1.3E11);
        this.testSigFigRoundingWorks(test, "1.25E1", 2, 1.3E1);
        this.testSigFigRoundingWorks(test, "-4.0E11", 2, -4.0E11);
        this.testSigFigRoundingWorks(test, "-4.0E-11", 2, -4.0E-11);

        this.testSigFigRoundingWorks(test, "0.0", 2, 0.0);
        this.testSigFigRoundingWorks(test, "0", 2, 0.0);
    }

    /*
    Test the rounding to a specified number of significant figures works as expected.
 */
    @Test
    public final void isaacNumericValidator_CheckSignificantFiguresExtractionWorks() throws Exception {
        IsaacNumericValidator test = new IsaacNumericValidator();
        // Unambiguous cases:
        this.testSigFigExtractionWorks(test, "1", 1, 10, 1);
        this.testSigFigExtractionWorks(test, "1.23", 2, 3, 3);
        this.testSigFigExtractionWorks(test, "0.400", 2, 3, 3);
        this.testSigFigExtractionWorks(test, "6.0e3", 2, 2, 2);
        // Ambiguous cases:
        this.testSigFigExtractionWorks(test, "30000", 3, 3, 3);
        this.testSigFigExtractionWorks(test, "10", 3, 3, 3);
        this.testSigFigExtractionWorks(test, "3333000", 2, 4, 4);
    }

    private void testSigFigRoundingWorks(IsaacNumericValidator classUnderTest, String inputValue, int sigFigToRoundTo, double expectedResult) throws Exception {
        double result = Whitebox.<Double>invokeMethod(classUnderTest, "roundStringValueToSigFigs", inputValue, sigFigToRoundTo);

        assertTrue("sigfig rounding failed for value '" + inputValue + "' to " + sigFigToRoundTo
                + "sf: expected '" + expectedResult + "', got '" + result + "'", result == expectedResult);
    }

    private void testSigFigExtractionWorks(IsaacNumericValidator classUnderTest, String inputValue, int minAllowedSigFigs,
                                           int maxAllowedSigFigs, int expectedResult) throws Exception {
        int result = Whitebox.<Integer>invokeMethod(classUnderTest, "numberOfSignificantFiguresToValidateWith",
                inputValue, minAllowedSigFigs, maxAllowedSigFigs);

        assertTrue("sigfig extraction out of range for value " + inputValue + " (min allowed: " + minAllowedSigFigs
                + ", max allowed: " + maxAllowedSigFigs + ") got " + result, result <= maxAllowedSigFigs && result >= minAllowedSigFigs);
        assertTrue("sigfig extraction failed for value " + inputValue + ", expected: " + expectedResult
                + " got " + result, result == expectedResult);
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
        IsaacNumericValidator test = new IsaacNumericValidator();
        for (String number : numbersToTest) {

            for (Integer sigFig : sigFigsToPass) {
                boolean validate = Whitebox.<Boolean>invokeMethod(test, "verifyCorrectNumberOfSignificantFigures", number, sigFig, sigFig);
                assertTrue("Verifying sigfig success failed " + number + " " + sigFig, validate);
            }

            for (Integer sigFig : sigFigsToFail) {
                boolean validate = Whitebox.<Boolean>invokeMethod(test, "verifyCorrectNumberOfSignificantFigures", number, sigFig, sigFig);
                assertFalse("Verifying sigfig failures failed " + number + " " + sigFig, validate);
            }
        }
    }
}
