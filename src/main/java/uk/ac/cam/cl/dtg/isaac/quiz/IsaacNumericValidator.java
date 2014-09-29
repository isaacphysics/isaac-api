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

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuantityValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuantityDTO;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

/**
 * Validator that only provides functionality to validate Numeric questions.
 */
public class IsaacNumericValidator implements IValidator {
	private static final Logger log = LoggerFactory.getLogger(IsaacNumericValidator.class);

	@Override
	public final QuestionValidationResponseDTO validateQuestionResponse(final Question question,
			final ChoiceDTO answer) {
		if (!(question instanceof IsaacNumericQuestion)) {
			log.error("Incorrect validator used for question: " + question.getId());
			throw new IllegalArgumentException("This validator only works with Isaac Numeric Questions...");
		}

		if (!(answer instanceof QuantityDTO)) {
			log.error("Expected Quantity for IsaacNumericQuestion: " + question.getId());

			return new QuantityValidationResponseDTO(question.getId(), answer, false, new Content(
					"The answer we received was not in Quantity format."), false, false, new Date());
		}

		IsaacNumericQuestion isaacNumericQuestion = (IsaacNumericQuestion) question;
		QuantityDTO answerFromUser = (QuantityDTO) answer;
		if (null == isaacNumericQuestion.getChoices() || isaacNumericQuestion.getChoices().isEmpty()) {
			log.error("Question does not have any answers. " + question.getId() + " src: "
					+ question.getCanonicalSourceFile());

			return new QuantityValidationResponseDTO(question.getId(), null, false, new Content(""), false,
					false, new Date());
		}

		if (null == answerFromUser.getValue()) {
			return new QuantityValidationResponseDTO(question.getId(), answerFromUser, false, new Content(
					"You did not provide an answer."), false, false, new Date());

		} else if (null == answerFromUser.getUnits() && (isaacNumericQuestion.getRequireUnits())) {
			return new QuantityValidationResponseDTO(question.getId(), answerFromUser, false, new Content(
					"You did not provide any units."), null, false, new Date());
		} else if (!this.verifyCorrectNumberofSignificantFigures(answerFromUser.getValue(),
				isaacNumericQuestion.getSignificantFigures())) {
			// make sure that the answer is to the right number of sig figs
			// before we proceed.

			return new QuantityValidationResponseDTO(question.getId(), answerFromUser, false, new Content(
					"Please provide your answer to the correct number of significant figures."), false, null,
					new Date());
		}

		if (isaacNumericQuestion.getRequireUnits()) {
			return this.validateWithUnits(isaacNumericQuestion, answerFromUser);
		} else {
			return this.validateWithoutUnits(isaacNumericQuestion, answerFromUser);
		}

	}

	/**
	 * Validate the students answer ensuring that the correct unit value is
	 * specified.
	 * 
	 * @param isaacNumericQuestion
	 *            - question to validate.
	 * @param answerFromUser
	 *            - answer from user
	 * @return the validation response
	 */
	private QuestionValidationResponseDTO validateWithUnits(final IsaacNumericQuestion isaacNumericQuestion,
			final QuantityDTO answerFromUser) {
		QuantityValidationResponseDTO bestResponse = null;
		for (Choice c : isaacNumericQuestion.getChoices()) {
			if (c instanceof Quantity) {
				Quantity quantityFromQuestion = (Quantity) c;

				if (quantityFromQuestion.getUnits() == null) {
					log.error("Expected units and no units can be found for question id: "
							+ isaacNumericQuestion.getId());
					continue;
				}

				// match known choices
				if (numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(),
						isaacNumericQuestion.getSignificantFigures())
						&& answerFromUser.getUnits().equals(quantityFromQuestion.getUnits())) {
					// exact match
					bestResponse = new QuantityValidationResponseDTO(isaacNumericQuestion.getId(),
							answerFromUser, quantityFromQuestion.isCorrect(),
							(Content) quantityFromQuestion.getExplanation(),
							quantityFromQuestion.isCorrect(), quantityFromQuestion.isCorrect(), new Date());

					break;
				} else if (numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(),
						isaacNumericQuestion.getSignificantFigures())
						&& !answerFromUser.getUnits().equals(quantityFromQuestion.getUnits())
						&& quantityFromQuestion.isCorrect()) {
					// matches value but not units of a correct choice.
					bestResponse = new QuantityValidationResponseDTO(isaacNumericQuestion.getId(),
							answerFromUser, false, new Content("Check your units."), true, false, new Date());
				} else if (!numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(),
						isaacNumericQuestion.getSignificantFigures())
						&& answerFromUser.getUnits().equals(quantityFromQuestion.getUnits())
						&& quantityFromQuestion.isCorrect()) {
					// matches units but not value of a correct choice.
					bestResponse = new QuantityValidationResponseDTO(isaacNumericQuestion.getId(),
							answerFromUser, false, new Content("Check your working."), false, true,
							new Date());
				}
			} else {
				log.error("Isaac Numeric Validator for questionId: " + isaacNumericQuestion.getId()
						+ " expected there to be a Quantity. Instead it found a Choice.");
			}
		}

		if (null == bestResponse) {
			// tell them they got it wrong but we cannot find an
			// feedback for them.
			return new QuantityValidationResponseDTO(isaacNumericQuestion.getId(), answerFromUser, false,
					new Content("Check your working."), false, false, new Date());

		} else {
			return bestResponse;
		}
	}

	/**
	 * Question validation response without units being considered.
	 * 
	 * @param isaacNumericQuestion
	 *            - question to validate.
	 * @param answerFromUser
	 *            - answer from user
	 * @return the validation response
	 */
	private QuestionValidationResponseDTO validateWithoutUnits(
			final IsaacNumericQuestion isaacNumericQuestion, final QuantityDTO answerFromUser) {
		QuantityValidationResponseDTO bestResponse = null;
		for (Choice c : isaacNumericQuestion.getChoices()) {
			if (c instanceof Quantity) {
				Quantity quantityFromQuestion = (Quantity) c;

				// match known choices
				if (numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(),
						isaacNumericQuestion.getSignificantFigures())) {
					// value match
					bestResponse = new QuantityValidationResponseDTO(isaacNumericQuestion.getId(),
							answerFromUser, quantityFromQuestion.isCorrect(),
							(Content) quantityFromQuestion.getExplanation(),
							quantityFromQuestion.isCorrect(), null, new Date());
					break;
				} else {
					// value doesn't match this choice
					bestResponse = new QuantityValidationResponseDTO(isaacNumericQuestion.getId(),
							answerFromUser, false, new Content("Check your working."), false, null,
							new Date());
				}
			} else {
				log.error("Isaac Numeric Validator "
						+ "expected there to be a Quantity. Instead it found a Choice.");
			}
		}

		if (null == bestResponse) {
			// tell them they got it wrong but we cannot find an
			// feedback for them.
			return new QuestionValidationResponseDTO(isaacNumericQuestion.getId(), answerFromUser, false,
					null, new Date());
		} else {
			return bestResponse;
		}
	}

	/**
	 * Test whether two quantity values match. Parse the strings as doubles,
	 * supporting notation of 3x10^12 to mean 3e12, then test that they match to
	 * N s.f.
	 * 
	 * @param trustedValue
	 *            - first number
	 * @param untrustedValue
	 *            - second number
	 * @param significantFiguresRequired
	 *            - the number of significant figures that the answer provided
	 *            should match
	 * @return true when the numbers match
	 */
	private boolean numericValuesMatch(final String trustedValue, final String untrustedValue,
			final int significantFiguresRequired) {
		double trustedDouble, untrustedDouble;

		// Replace "x10^" with "e";
		String untrustedParsedValue = untrustedValue.replace("x10^", "e");

		try {
			trustedDouble = Double.parseDouble(trustedValue.replace("x10^", "e"));
			untrustedDouble = Double.parseDouble(untrustedParsedValue);
		} catch (NumberFormatException e) {
			// One of the values was not a valid float.
			return false;
		}

		// Round to N s.f. for trusted value
		trustedDouble = roundToSigFigs(trustedDouble, significantFiguresRequired);
		untrustedDouble = roundToSigFigs(untrustedDouble, significantFiguresRequired);

		return Math.abs(trustedDouble - untrustedDouble) < Math.max(
				1e-12 * Math.max(trustedDouble, untrustedDouble), 1e-12);
	}

	/**
	 * Round a double to a given number of significant figures.
	 * 
	 * @param f
	 *            - number to round
	 * @param sigFigs
	 *            - number of significant figures required
	 * @return the rounded number.
	 */
	private double roundToSigFigs(final double f, final int sigFigs) {

		int mag = (int) Math.floor(Math.log10(f));

		double normalised = f / Math.pow(10, mag);

		return Math.round(normalised * Math.pow(10, sigFigs - 1)) * Math.pow(10, mag)
				/ Math.pow(10, sigFigs - 1);
	}

	/**
	 * Calculate the number of significant figures provided by an input.
	 * 
	 * @param input
	 *            as a big decimal (probably constructed using a string)
	 * @return an integer representing the number of significant figures
	 *         provided.
	 */
	private int calculateSignificantDigits(final BigDecimal input) {
		if (input.scale() <= 0) {
			return input.precision() + input.stripTrailingZeros().scale();
		} else {
			return input.precision();
		}
	}

	/**
	 * Helper method to verify if the answer given is to the correct number of
	 * significant figures.
	 * 
	 * @param valueToCheck
	 *            - the value as a string from the user to check.
	 * @param significantFigures
	 *            - the number of significant figures that is expected for the
	 *            answer to be correct.
	 * @return true if yes false if not.
	 */
	private boolean verifyCorrectNumberofSignificantFigures(final String valueToCheck,
			final int significantFigures) {
		// Replace "x10^" with "e";
		String untrustedParsedValue = valueToCheck.replace("x10^", "e");

		// check significant figures match
		int untrustedValueSigfigs = this.calculateSignificantDigits(new BigDecimal(untrustedParsedValue));
		if (untrustedParsedValue.contains(".") && untrustedValueSigfigs != significantFigures) {
			return false;
		}

		return true;
	}
}
