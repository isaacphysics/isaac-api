package uk.ac.cam.cl.dtg.segue.quiz;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;

/**
 * Default quiz validator for ChoiceQuestions.
 * 
 *
 */
public class ChoiceQuestionValidator implements IValidator {

	private static final Logger log = LoggerFactory
			.getLogger(ChoiceQuestionValidator.class);

	@Override
	public final QuestionValidationResponse validateQuestionResponse(
			final Question question, final Choice answer) {
		Validate.notNull(question);
		Validate.notNull(answer);

		// check that the question is of type ChoiceQuestion before we go ahead
		ChoiceQuestion choiceQuestion = null;
		if (question instanceof ChoiceQuestion) {
			choiceQuestion = (ChoiceQuestion) question;

			if (null == choiceQuestion.getChoices()
					|| choiceQuestion.getChoices().isEmpty()) {
				log.warn("Question does not have any answers. "
						+ question.getId() + " src: "
						+ question.getCanonicalSourceFile());
				return new QuestionValidationResponse(question.getId(),
						answer.getValue(), false, null);
			}

			for (Choice choice : choiceQuestion.getChoices()) {
				if (choice.getValue().equals(answer.getValue())) {
					return new QuestionValidationResponse(question.getId(),
							answer.getValue(), choice.isCorrect(),
							(Content) choice.getExplanation());
				}
			}

			log.info("Unable to find choice for question ( "
					+ question.getId()
					+ " ) matching the answer supplied ("
					+ answer
					+ "). Returning that it is incorrect with out an explanation.");

			return new QuestionValidationResponse(question.getId(),
					answer.getValue(), false, null);
		} else {
			log.error("Expected to be able to cast the question as a ChoiceQuestion "
					+ "but this cast failed.");
			throw new ClassCastException(
					"Incorrect type of question received. Unable to validate.");
		}
	}
}
