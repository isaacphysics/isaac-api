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
				&& (isaacNumericQuestion.getRequireUnits() != null && isaacNumericQuestion
						.getRequireUnits())) {
			
			return new QuantityValidationResponse(question.getId(), null + " "
					+ answerFromUser.getUnits(), false, new Content(
							"You did not provide any units."), false, false);
		}

		if (isaacNumericQuestion.getRequireUnits() == null
				|| isaacNumericQuestion.getRequireUnits()) {
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
				if (answerFromUser.getValue().equals(
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
				} else if (answerFromUser.getValue().equals(
						quantityFromQuestion.getValue())
						&& !answerFromUser.getUnits().equals(
								quantityFromQuestion.getUnits())) {
					// matches value but not units.
					bestResponse = new QuantityValidationResponse(
							isaacNumericQuestion.getId(),
							answerFromUser.getValue() + " "
									+ answerFromUser.getUnits(), false,
							new Content("Check your units."), true, false);
				} else if (!answerFromUser.getValue().equals(
						quantityFromQuestion.getValue())
						&& answerFromUser.getUnits().equals(
								quantityFromQuestion.getUnits())) {
					// matches units but not value.
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
			return new QuestionValidationResponse(
					isaacNumericQuestion.getId(),
					answerFromUser.getValue() + " " + answerFromUser.getUnits(),
					false, null);
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
				if (answerFromUser.getValue().equals(
						quantityFromQuestion.getValue())) {
					// exact match
					bestResponse = new QuantityValidationResponse(
							isaacNumericQuestion.getId(),
							answerFromUser.getValue(),
							quantityFromQuestion.isCorrect(),
							(Content) quantityFromQuestion.getExplanation(),
							quantityFromQuestion.isCorrect(), null);
					break;
				} else if (!answerFromUser.getValue().equals(
						quantityFromQuestion.getValue())) {
					// incorrect units.
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
}
