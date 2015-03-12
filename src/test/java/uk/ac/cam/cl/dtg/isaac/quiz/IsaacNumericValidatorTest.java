/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
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
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dto.QuantityValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuantityDTO;

/**
 * Test class for the user manager class.
 * 
 */
@PowerMockIgnore({"javax.ws.*"})
public class IsaacNumericValidatorTest {

	/**
	 * Initial configuration of tests.
	 * 
	 */
	@Before
	public final void setUp() {

	}

	@Test
	public final void isaacNumericValidator_NonNumericValue_InvalidResponseShouldBeReturned() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(false);
				
		List<Choice> answerList = Lists.newArrayList();
		Quantity someCorrectAnswer = new Quantity();
		someCorrectAnswer.setValue("4.8e22");
		someCorrectAnswer.setCorrect(true);
		answerList.add(someCorrectAnswer);
		
		someNumericQuestion.setChoices(answerList);
		
		String explanationShouldContain = "valid number";
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("4.8[]3");
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuestionValidationResponseDTO response = validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertFalse(response.isCorrect());
		assertTrue(response.getExplanation().getValue().contains(explanationShouldContain));
	}
	
	@Test
	public final void isaacNumericValidator_CheckCorrectAnswerWithExponent_CorrectResponseShouldHappen() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(false);
				
		List<Choice> answerList = Lists.newArrayList();
		Quantity someCorrectAnswer = new Quantity();
		someCorrectAnswer.setValue("4.8e22");
		someCorrectAnswer.setCorrect(true);
		answerList.add(someCorrectAnswer);
		
		someNumericQuestion.setChoices(answerList);
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("4.8e22");
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuestionValidationResponseDTO response = validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertTrue(response.isCorrect());
	}
	
	@Test
	public final void isaacNumericValidator_CheckCorrectIntegerAnswer_CorrectResponseShouldHappen() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(false);
				
		List<Choice> answerList = Lists.newArrayList();
		Quantity someCorrectAnswer = new Quantity();
		someCorrectAnswer.setValue("42");
		someCorrectAnswer.setCorrect(true);
		answerList.add(someCorrectAnswer);
		
		someNumericQuestion.setChoices(answerList);
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("42");
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuestionValidationResponseDTO response = validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertTrue(response.isCorrect());
	}	
	
	
	@Test
	public final void isaacNumericValidator_CheckCorrectAnswerWithCorrectUnits_CorrectResponseShouldHappen() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(true);
		
		List<Choice> answerList = Lists.newArrayList();
		Quantity someCorrectAnswer = new Quantity();
		someCorrectAnswer.setValue("42");
		someCorrectAnswer.setUnits("m\\,s^{-1}");
		someCorrectAnswer.setCorrect(true);
		answerList.add(someCorrectAnswer);
		
		someNumericQuestion.setChoices(answerList);
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("42");
		q.setUnits("m\\,s^{-1}");
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuantityValidationResponseDTO response = (QuantityValidationResponseDTO) validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertTrue("check question is marked as correct", response.isCorrect());
		assertTrue("check units is correct", response.getCorrectUnits());
	}

	@Test
	public final void isaacNumericValidator_CheckCorrectAnswerWithIncorrectValue_IncorrectResponseShouldHappen() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(true);
		
		List<Choice> answerList = Lists.newArrayList();
		Quantity someCorrectAnswer = new Quantity();
		someCorrectAnswer.setValue("42");
		someCorrectAnswer.setUnits("m\\,s^{-1}");
		someCorrectAnswer.setCorrect(true);
		answerList.add(someCorrectAnswer);
		
		someNumericQuestion.setChoices(answerList);
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("43");
		q.setUnits("m\\,s^{-1}");
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuantityValidationResponseDTO response = (QuantityValidationResponseDTO) validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertFalse("check question is marked as incorrect", response.isCorrect());
		
		assertFalse("check value is marked as incorrect", response.getCorrectValue());
		assertTrue("check units are marked as correct", response.getCorrectUnits());
	}
	
	@Test
	public final void isaacNumericValidator_CheckCorrectAnswerWithIncorrectUnits_IncorrectResponseShouldHappen() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(true);
		
		List<Choice> answerList = Lists.newArrayList();
		Quantity someCorrectAnswer = new Quantity();
		someCorrectAnswer.setValue("42");
		someCorrectAnswer.setUnits("m\\,s^{-1}");
		someCorrectAnswer.setCorrect(true);
		answerList.add(someCorrectAnswer);
		
		someNumericQuestion.setChoices(answerList);
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("42");
		q.setUnits("m\\,h^{-1}");
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuantityValidationResponseDTO response = (QuantityValidationResponseDTO) validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertFalse("check question is marked as incorrect", response.isCorrect());
		
		assertTrue("check value is marked as correct", response.getCorrectValue());
		assertFalse("check units are marked as incorrect", response.getCorrectUnits());
	}
	
	@Test
	public final void isaacNumericValidator_CheckCorrectAnswerWithNoUnits_IncorrectResponseShouldHappen() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(true);
		
		List<Choice> answerList = Lists.newArrayList();
		Quantity someCorrectAnswer = new Quantity();
		someCorrectAnswer.setValue("42");
		someCorrectAnswer.setUnits("m\\,s^{-1}");
		someCorrectAnswer.setCorrect(true);
		answerList.add(someCorrectAnswer);
		
		someNumericQuestion.setChoices(answerList);
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("42");
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuantityValidationResponseDTO response = (QuantityValidationResponseDTO) validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertFalse("check question is marked as incorrect", response.isCorrect());
		assertFalse("check units are marked as incorrect", response.getCorrectUnits());
		
		assertTrue("Appropriate message provided", response.getExplanation().getValue().contains("units"));
	}
	
	@Test
	public final void isaacNumericValidator_CheckCorrectAnswerWithExponentIncorrectSigFigs_CorrectResponseShouldHappen() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(false);
				
		List<Choice> answerList = Lists.newArrayList();
		Quantity someCorrectAnswer = new Quantity();
		someCorrectAnswer.setValue("4.8e22");
		someCorrectAnswer.setCorrect(true);
		answerList.add(someCorrectAnswer);
		
		someNumericQuestion.setChoices(answerList);
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("4.81e22");
		
		String explanationShouldContain = "significant figures";
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuestionValidationResponseDTO response = validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertFalse("Response should be incorrect", response.isCorrect());
		
		assertTrue("Explanation should warn about sig figs", response.getExplanation().getValue().contains(explanationShouldContain));
	}
	
	@Test
	public final void isaacNumericValidator_CheckIncorrectAnswerWithExponent_IncorrectResponseShouldHappen() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(false);
				
		List<Choice> answerList = Lists.newArrayList();
		Quantity someCorrectAnswer = new Quantity();
		someCorrectAnswer.setValue("4.8e22");
		someCorrectAnswer.setCorrect(false);
		answerList.add(someCorrectAnswer);
		
		someNumericQuestion.setChoices(answerList);
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("4.8e22");
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuestionValidationResponseDTO response = validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertFalse(response.isCorrect());
	}
	
	@Test
	public final void isaacNumericValidator_CheckAnswerNotFoundWhenNoChoicesProvided_IncorrectResponseShouldHappen() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(false);
				
		List<Choice> answerList = Lists.newArrayList();	
		someNumericQuestion.setChoices(answerList);
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("4.8e22");
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuestionValidationResponseDTO response = validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertFalse(response.isCorrect());
	}
	
	@Test
	public final void isaacNumericValidator_CheckCorrectAnswerWrongSigFigs_IncorrectResponseShouldHappen() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(false);
				
		List<Choice> answerList = Lists.newArrayList();
		Quantity someCorrectAnswer = new Quantity();
		someCorrectAnswer.setValue("4.8");
		someCorrectAnswer.setCorrect(true);
		answerList.add(someCorrectAnswer);
		
		someNumericQuestion.setChoices(answerList);
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("4.81");
		
		String explanationShouldContain = "significant figures";
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuestionValidationResponseDTO response = validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertFalse(response.isCorrect());
		
		assertTrue(response.getExplanation().getValue().contains(explanationShouldContain));
	}
	
	@Test
	public final void isaacNumericValidator_CheckIncorrectAnswerWrongSigFigs_IncorrectResponseShouldHappen() {
		IsaacNumericQuestion someNumericQuestion = new IsaacNumericQuestion();
		someNumericQuestion.setRequireUnits(false);
				
		List<Choice> answerList = Lists.newArrayList();
		Quantity someCorrectAnswer = new Quantity();
		someCorrectAnswer.setValue("4.8");
		someCorrectAnswer.setCorrect(true);
		answerList.add(someCorrectAnswer);
		
		someNumericQuestion.setChoices(answerList);
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("4.881");
		
		String explanationShouldContain = "significant figures";
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuestionValidationResponseDTO response = validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertFalse(response.isCorrect());
		
		assertTrue(response.getExplanation().getValue().contains(explanationShouldContain));
	}
	
	@Test
	public final void isaacNumericValidator_CheckKnownIncorrectAnswerWithNegativeExponent_IncorrectResponseShouldHappenWithExplain() {
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
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("1.2e-28");
		
		String explanationShouldContain = someExplanation.getValue();
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuestionValidationResponseDTO response = validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertFalse(response.isCorrect());
		
		assertTrue(response.getExplanation().getValue().equals(explanationShouldContain));
	}
	
	@Test
	public final void isaacNumericValidator_CheckUnknownIncorrectAnswerWithNegativeExponent_GeneralIncorrectResponseShouldHappen() {
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
		
		// setup users answer
		QuantityDTO q = new QuantityDTO();
		q.setValue("5e-22");
		
		IsaacNumericValidator validator = new IsaacNumericValidator();
		QuestionValidationResponseDTO response = validator.validateQuestionResponse(someNumericQuestion, q);
		
		assertFalse(response.isCorrect());
		
		System.out.println(response.getExplanation().getValue());
		assertTrue(!response.getExplanation().getValue().equals(someExplanation.getValue()));
	}
	
	@Test
	public final void isaacNumericValidator_CheckSignificantFiguresCalculationWorks_multipleTests() throws Exception {		
		verifyCorrectNumberOfSigFigs(Arrays.asList("5000", "5000e3", "-5000", "-5000e3"), Arrays.asList(1,2,3,4), Arrays.asList(5));
		
		verifyCorrectNumberOfSigFigs(Arrays.asList("5300", "5300e3", "-5300", "-5300e3"), Arrays.asList(2,3,4), Arrays.asList(1,5));
		
		verifyCorrectNumberOfSigFigs(Arrays.asList("50300", "-50300"), Arrays.asList(3,4,5), Arrays.asList(1,2,6));
		
		verifyCorrectNumberOfSigFigs(Arrays.asList("0", "-0"), Arrays.asList(1), Arrays.asList(2));
		
		verifyCorrectNumberOfSigFigs(Arrays.asList("0000100", "-0000100"), Arrays.asList(1,2,3), Arrays.asList(4,5,6,7));
		
		verifyCorrectNumberOfSigFigs(Arrays.asList("0000100.00", "-0000100.00"), Arrays.asList(5), Arrays.asList(4,6,7));
	}
	
	@Test
	public final void isaacNumericValidator_CheckSignificantFiguresRoundingWorks() throws Exception {
		IsaacNumericValidator test = new IsaacNumericValidator();
		this.testSigFigRoundingWorks(test, 1.25648, 1, 1.0);
		this.testSigFigRoundingWorks(test, 1.25648, 2, 1.3);
		
		this.testSigFigRoundingWorks(test, -1.25648, 2, -1.3);

		this.testSigFigRoundingWorks(test, 1.25E11, 2, 1.3E11);
		this.testSigFigRoundingWorks(test, 1.25E1, 2, 1.3E1);
		
		this.testSigFigRoundingWorks(test, -4.0E11, 2, -4.0E11);
		this.testSigFigRoundingWorks(test, -4.0E-11, 2, -4.0E-11);

		this.testSigFigRoundingWorks(test, 0.0, 2, 0.0);
	}	

	private void testSigFigRoundingWorks(IsaacNumericValidator classUnderTest, double inputValue, int sigFigToRoundTo, double expectedResult) throws Exception {
		double result = Whitebox.<Double> invokeMethod(classUnderTest, "roundToSigFigs", inputValue, sigFigToRoundTo);				
		
		assertTrue("sigfig rounding failed for value " + inputValue + " to " + sigFigToRoundTo
				+ " expected: " + expectedResult + " got " + result, result == expectedResult);
	}
	
	
	/**
	 * Helper to launch multiple sig fig tests on given numbers.
	 * 
	 * @param numbersToTest the numbers to feed into the validator
	 * @param sigFigsToPass - the number of significant figures we expect the aforementioned numbers return a pass for
	 * @param sigFigsToFail - the number of significant figures we expect the aforementioned numbers return a fail for
	 * @throws Exception - if we can't execute the private method.
	 */
	private final void verifyCorrectNumberOfSigFigs(List<String> numbersToTest, List<Integer> sigFigsToPass, List<Integer> sigFigsToFail) throws Exception {
		IsaacNumericValidator test = new IsaacNumericValidator();
		for (String number : numbersToTest) {
			
			for(Integer sigFig : sigFigsToPass) {
				boolean validate = Whitebox.<Boolean> invokeMethod(test, "verifyCorrectNumberofSignificantFigures", number, sigFig);				
				assertTrue("Verifying sigfig success failed " + number + " " + sigFig, validate);
			}
			
			for(Integer sigFig : sigFigsToFail) {
				boolean validate = Whitebox.<Boolean> invokeMethod(test, "verifyCorrectNumberofSignificantFigures", number, sigFig); 
				assertFalse("Verifying sigfig failures failed " + number + " " + sigFig, validate);
			}
		}
	}
}
