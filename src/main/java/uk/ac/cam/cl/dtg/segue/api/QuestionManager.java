package uk.ac.cam.cl.dtg.segue.api;

import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.quiz.IMultiFieldValidator;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

/**
 * This class is responsible for validating correct answers using the
 * ValidatesWith annotation when it is applied on to Questions.
 * 
 */
public class QuestionManager {
	private static final Logger log = LoggerFactory
			.getLogger(QuestionManager.class);

	/**
	 * Create a default Question manager object.
	 */
	public QuestionManager() {

	}

	/**
	 * Validate client answer to recorded answer. 
	 * @param question
	 *            The question to which the answer must be validated against.
	 * @param answers
	 *            as a list used for comparison purposes.
	 * @return A response containing a QuestionValidationResponse object
	 */
	public final Response validateAnswer(final Question question, final List<Choice> answers) {
		IValidator validator = locateValidator(question.getClass());

		if (null == validator) {
			log.error("Unable to locate a valid validator for this question "
					+ question.getId());
			return Response
					.serverError()
					.entity("Unable to detect question validator for "
							+ "this object. Unable to verify answer")
					.build();
		}

		if (validator instanceof IMultiFieldValidator) {
			IMultiFieldValidator multiFieldValidator = (IMultiFieldValidator) validator;
			// we need to call the multifield validator instead.
			return Response.ok(
					multiFieldValidator.validateMultiFieldQuestionResponses(
							question, answers)).build();
		} else { // use the standard IValidator
		
			// ok so we are expecting there just to be one choice?
			if (answers.isEmpty() || answers.size() > 1) {
				log.debug("We only expected one answer for this question...");
				SegueErrorResponse error = new SegueErrorResponse(
						Status.BAD_REQUEST,
						"We only expected one answer for this question (id "
								+ question.getId()
								+ ") and we were given a list.");
				return error.toResponse();
			}

			return Response
					.ok(validator.validateQuestionResponse(question,
							answers.get(0))).build();
		}
	}

	/**
	 * Reflection to try and determine the associated validator for the question
	 * being answered.
	 * 
	 * @param questionType - the type of question being answered.
	 * @return a Validator
	 */
	@SuppressWarnings("unchecked")
	public static IValidator locateValidator(
			final Class<? extends Question> questionType) {
		// check we haven't gone too high up the superclass tree
		if (!Question.class.isAssignableFrom(questionType)) {
			return null;
		}

		// Does this class have the correct annotation?
		if (questionType.isAnnotationPresent(ValidatesWith.class)) {
			try {
				log.debug("Validator for question validation found. Using : "
						+ questionType.getAnnotation(ValidatesWith.class)
								.value());
				return questionType.getAnnotation(ValidatesWith.class).value()
						.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				log.error(
						"Error while trying to find annotations in the class provided.",
						e);
				return null;
			}
		} else if (questionType.equals(Question.class)) {
			// so if we get here then we haven't found a ValidatesWith class, so
			// we should just give up and return null.
			return null;
		}

		// we will continue our search of the superclasses for the annotation
		return locateValidator((Class<? extends Question>) questionType
				.getSuperclass());
	}
}
