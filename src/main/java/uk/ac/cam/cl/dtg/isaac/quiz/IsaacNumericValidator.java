package uk.ac.cam.cl.dtg.isaac.quiz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

/**
 * Validator that only provides functionality to validate Numeric questions.
 */
public class IsaacNumericValidator implements IValidator {
	private static final Logger log = LoggerFactory
			.getLogger(IsaacNumericValidator.class);

	@Override
	public final QuestionValidationResponse validateQuestionResponse(
			final Question question, final Choice answer) {
		if (!(question instanceof IsaacNumericQuestion)) {
			log.error("Incorrect validator used for question: "
					+ question.getId());
			throw new IllegalArgumentException(
					"This validator only works with Isaac Numeric Questions...");
		}

		if (!(answer instanceof Quantity)) {
			log.error("Expected Quantity for IsaacNumericQuestion: "
					+ question.getId());
			
			return new QuantityValidationResponse(question.getId(), null + " "
					+ answer, false, new Content(
							"The answer we received was not in Quantity format."),
					false, false);
		}

		IsaacNumericQuestion isaacNumericQuestion = (IsaacNumericQuestion) question;
		Quantity answerFromUser = (Quantity) answer;
		if (null == isaacNumericQuestion.getChoices()
				|| isaacNumericQuestion.getChoices().isEmpty()) {
			log.warn("Question does not have any answers. " + question.getId()
					+ " src: " + question.getCanonicalSourceFile());

			return new QuantityValidationResponse(question.getId(), null,
					false, new Content(""), false, false);
		}

		if (null == answerFromUser.getValue()) {
			
			return new QuantityValidationResponse(question.getId(), null + " "
					+ answerFromUser.getUnits(), false, new Content(
							"You did not provide a complete answer."), false, false);
		} else if (null == answerFromUser.getUnits()
				&& (isaacNumericQuestion.getRequireUnits())) {
			
			return new QuantityValidationResponse(question.getId(), null + " "
					+ answerFromUser.getUnits(), false, new Content(
							"You did not provide any units."), false, false);
		}

		if (isaacNumericQuestion.getRequireUnits()) {
			return this.validateWithUnits(isaacNumericQuestion, answerFromUser);
		} else {
			return this.validateWithoutUnits(isaacNumericQuestion,
					answerFromUser);
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
	private QuestionValidationResponse validateWithUnits(
			final IsaacNumericQuestion isaacNumericQuestion,
			final Quantity answerFromUser) {
		QuantityValidationResponse bestResponse = null;
		for (Choice c : isaacNumericQuestion.getChoices()) {
			if (c instanceof Quantity) {
				Quantity quantityFromQuestion = (Quantity) c;

				if (quantityFromQuestion.getUnits() == null) {
					log.error("Expected units and no units can be found for question id: "
							+ isaacNumericQuestion.getId());
					continue;
				}

				// match known choices
				if (numericValuesMatch(answerFromUser.getValue(),
						quantityFromQuestion.getValue())
						&& answerFromUser.getUnits().equals(
								quantityFromQuestion.getUnits())) {
					// exact match
					bestResponse = new QuantityValidationResponse(
							isaacNumericQuestion.getId(),
							answerFromUser.getValue() + " "
									+ answerFromUser.getUnits(),
							quantityFromQuestion.isCorrect(),
							(Content) quantityFromQuestion.getExplanation(),
							quantityFromQuestion.isCorrect(),
							quantityFromQuestion.isCorrect());

					break;
				} else if (numericValuesMatch(answerFromUser.getValue(),
						quantityFromQuestion.getValue())
						&& !answerFromUser.getUnits().equals(
								quantityFromQuestion.getUnits())
						&& quantityFromQuestion.isCorrect()) {
					// matches value but not units of a correct choice.
					bestResponse = new QuantityValidationResponse(
							isaacNumericQuestion.getId(),
							answerFromUser.getValue() + " "
									+ answerFromUser.getUnits(), false,
							new Content("Check your units."), true, false);
				} else if (!numericValuesMatch(answerFromUser.getValue(),
						quantityFromQuestion.getValue())
						&& answerFromUser.getUnits().equals(
								quantityFromQuestion.getUnits())
						&& quantityFromQuestion.isCorrect()) {
					// matches units but not value of a correct choice.
					bestResponse = new QuantityValidationResponse(
							isaacNumericQuestion.getId(),
							answerFromUser.getValue() + " "
									+ answerFromUser.getUnits(), false,
							new Content("Check your working."), false, true);
				}
			} else {
				log.error("Isaac Numeric Validator for questionId: " + isaacNumericQuestion.getId()
						+ " expected there to be a Quantity. Instead it found a Choice.");
			}
		}

		if (null == bestResponse) {
			// tell them they got it wrong but we cannot find an
			// feedback for them.
			return new QuantityValidationResponse(
					isaacNumericQuestion.getId(),
					answerFromUser.getValue() + " "
							+ answerFromUser.getUnits(), false,
					new Content("Check your working."), false, false);
			
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
	private QuestionValidationResponse validateWithoutUnits(
			final IsaacNumericQuestion isaacNumericQuestion,
			final Quantity answerFromUser) {
		QuantityValidationResponse bestResponse = null;
		for (Choice c : isaacNumericQuestion.getChoices()) {
			if (c instanceof Quantity) {
				Quantity quantityFromQuestion = (Quantity) c;

				// match known choices
				if (numericValuesMatch(answerFromUser.getValue(),
						quantityFromQuestion.getValue())) {
					// value match
					bestResponse = new QuantityValidationResponse(
							isaacNumericQuestion.getId(),
							answerFromUser.getValue(),
							quantityFromQuestion.isCorrect(),
							(Content) quantityFromQuestion.getExplanation(),
							quantityFromQuestion.isCorrect(), null);
					break;
				} else {
					// value doesn't match this choice
					bestResponse = new QuantityValidationResponse(
							isaacNumericQuestion.getId(),
							answerFromUser.getValue(), false, new Content(
									"Check your working."), false, null);
				}
			} else {
				log.error("Isaac Numeric Validator "
						+ "expected there to be a Quantity. Instead it found a Choice.");
			}
		}

		if (null == bestResponse) {
			// tell them they got it wrong but we cannot find an
			// feedback for them.
			return new QuestionValidationResponse(
					isaacNumericQuestion.getId(),
					answerFromUser.getValue() + " " + answerFromUser.getUnits(),
					false, null);
		} else {
			return bestResponse;
		}
	}
	
	/**
	 * Test whether two quantity values match. Parse the strings as doubles,
	 * supporting notation of 3x10^12 to mean 3e12, then test that they match
	 * to 3 s.f.
	 * 
	 * @param v1
	 * 			- first number
	 * @param v2
	 * 			- second number
	 * @return true when the numbers match
	 */
	private boolean numericValuesMatch(String v1, String v2) {
		
		// First replace "x10^" with "e";
		
		v1 = v1.replace("x10^", "e");
		v2 = v2.replace("x10^", "e");
		
		double f1, f2;
		
		try {
			f1 = Double.parseDouble(v1);
			f2 = Double.parseDouble(v2);
		} catch (NumberFormatException e) {
			// One of the values was not a valid float.
			return false;
		}
		
		// Round to 3 s.f.
		
		int sigFigs = 3;
		
		f1 = roundToSigFigs(f1, sigFigs);
		f2 = roundToSigFigs(f2, sigFigs);
		
		return Math.abs(f1 - f2) < 1e-12 * Math.max(f1,  f2);
	}
	
	/**
	 * Round a double to a given number of significant figures.
	 * 
	 * @param f
	 * 			- number to round
	 * @param sigFigs
	 * 			- number of significant figures required
	 * @return the rounded number.
	 */
	private double roundToSigFigs(double f, int sigFigs) {
		
		int mag = (int) Math.floor(Math.log10(f));
		
		double normalised = f / Math.pow(10, mag);
				
		return Math.round(normalised * Math.pow(10, sigFigs - 1)) * Math.pow(10, mag) / Math.pow(10, sigFigs - 1);
	}
}
