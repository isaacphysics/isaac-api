package uk.ac.cam.cl.dtg.segue.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.dao.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;
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

	private ContentMapper mapper;
	
	/**
	 * Create a default Question manager object.
	 * 
	 * @param mapper
	 *            - an auto mapper to allow us to convert to and from
	 *            QuestionValidationResponseDOs and DTOs.
	 */
	@Inject
	public QuestionManager(final ContentMapper mapper) {
		this.mapper =  mapper;
	}

	/**
	 * Validate client answer to recorded answer. 
	 * @param question
	 *            The question to which the answer must be validated against.
	 * @param answers
	 *            from the client as a list used for comparison purposes.
	 * @return A response containing a QuestionValidationResponse object.
	 */
	public final Response validateAnswer(final Question question, final List<ChoiceDTO> answers) {
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
	
	/**
	 * Modify a question objects in a page such that it contains bestAttempt
	 * information if we can provide it.
	 * 
	 * @param page
	 *            - to augment - this object may be mutated as a result of this
	 *            method. i.e BestAttempt field set on question DTOs.
	 * @param usersQuestionAttempts
	 *            - as a map of QuestionPageId to Map of QuestionId to
	 *            QuestionValidationResponseDO
	 * @return augmented page - the return result is by convenience as the page
	 *         provided as a parameter will be mutated.
	 */
	public SeguePageDTO augmentQuestionObjectWithAttemptInformation(
			final SeguePageDTO page,
			final Map<String, Map<String, List<QuestionValidationResponse>>> 
			usersQuestionAttempts) {
		
		if (null == usersQuestionAttempts) {
			return page;
		}
		
		List<QuestionDTO> questionsToAugment = QuestionManager
				.extractQuestionObjectsRecursively(page,
						new ArrayList<QuestionDTO>());
		
		for (QuestionDTO question : questionsToAugment) {
			if (!usersQuestionAttempts.containsKey(page.getId())) {
				continue;
			}
			if (usersQuestionAttempts.get(page.getId()).get(question.getId()) == null) {
				continue;
			}

			QuestionValidationResponse bestAnswer = null;
			List<QuestionValidationResponse> questionAttempts = usersQuestionAttempts
					.get(page.getId()).get(question.getId());

			// iterate in reverse order to try and find the correct answer.
			for (int i = questionAttempts.size() - 1; i >= 0; i--) {
				QuestionValidationResponse currentResponse = questionAttempts
						.get(i);
				
				if (bestAnswer == null) {
					bestAnswer = currentResponse;
				}

				if (questionAttempts.get(i).isCorrect()) {
					bestAnswer = currentResponse;
					break;
				}
			}

			question.setBestAttempt(mapper.getAutoMapper().map(bestAnswer,
					QuestionValidationResponseDTO.class));
		}
		return page;
	}

	/**
	 * Extract all of the questionObjectsRecursively.
	 * 
	 * @param toExtract
	 *            - The contentDTO which may have question objects as children.
	 * @param result
	 *            - The initially empty List which will be mutated to contain
	 *            references to all of the question objects.
	 * @return The modified result array.
	 */
	private static List<QuestionDTO> extractQuestionObjectsRecursively(
			final ContentDTO toExtract, final List<QuestionDTO> result) {
		if (toExtract instanceof QuestionDTO) {
			// we found a question so add it to the list.
			result.add((QuestionDTO) toExtract);
		}
		
		if (toExtract.getChildren() != null) {
			// Go through each child in the content object.
			for (ContentBaseDTO child : toExtract.getChildren()) {
				if (child instanceof ContentDTO) {
					// if it is not a question but it can have children then
					// continue recursing.
					ContentDTO childContent = (ContentDTO) child;
					if (childContent.getChildren() != null) {
						QuestionManager.extractQuestionObjectsRecursively(
								childContent, result);
					}
				}
			}
		}
		
		return result;
	}
}
