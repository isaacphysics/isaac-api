package uk.ac.cam.cl.dtg.segue.api;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dto.Question;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;
import uk.ac.cam.cl.dtg.segue.quiz.Validator;

/**
 * This class is responsible for validating correct answers using the ValidatesWith annotation when it is applied on to Questions.
 * 
 */
public class QuestionManager {
	private static final Logger log = LoggerFactory.getLogger(QuestionManager.class);
	
	public QuestionManager(){
		
	}

	/**
	 *  
	 * @param question The question to which the answer must be validated against.
	 * @param answer as a string used for comparison purposes.
	 * @return A response containing a QuestionValidationResponse object
	 */
	public Response validateAnswer(Question question, String answer){		
		Validator validator = this.locateValidator(question.getClass());
		
		if(null == validator){
			log.error("Unable to locate a valid validator for this question " + question.getId());
			return Response.serverError().entity("Unable to detect question validator for this object. Unable to verify answer").build();
		}
		
		return Response.ok(validator.validateQuestionResponse(question, answer)).build();
	}
	
	/**
	 * Reflection to try and determine the associated validator for the question being answered.
	 * 
	 * @param questionType
	 * @return a Validator
	 */
	@SuppressWarnings("unchecked")
	private Validator locateValidator(Class<? extends Question> questionType){
		// check we haven't gone too high up the superclass tree
		if(!Question.class.isAssignableFrom(questionType)){
			return null;
		}
		
		// Does this class have the correct annotation?
		if(questionType.isAnnotationPresent(ValidatesWith.class)){
			try {
				log.debug("Validator for question validation found. Using : " + questionType.getAnnotation(ValidatesWith.class).value());
				return questionType.getAnnotation(ValidatesWith.class).value().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				log.error("Error while trying to find annotations in the class provided.",e);
				return null;
			}
		}
		else if(questionType.equals(Question.class)){
			// so if we get here then we haven't found a ValidatesWith class, so we should just give up and return null.
			return null;
		}
		
		// we will continue our search of the superclasses for the annotation
		return locateValidator((Class <? extends Question>) questionType.getSuperclass());
	}
}
