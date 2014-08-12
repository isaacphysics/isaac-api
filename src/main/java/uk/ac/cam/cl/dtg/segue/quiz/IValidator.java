package uk.ac.cam.cl.dtg.segue.quiz;

import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

/**
 * Interface that allows the quiz engine to validate questions and answers.
 * @author Stephen Cummins
 *
 */
public interface IValidator {
	
	/**
	 * validateQuestionResponse
	 * This method is specifically for single field questions.
	 * 
	 * i.e. when a question expects a single answer from the user.
	 *  
	 * @param question - question to check against.
	 * @param answer - answer from the user.
	 * @return a QuestionValidationResponseDTO
	 */
	QuestionValidationResponseDTO validateQuestionResponse(
			Question question, ChoiceDTO answer);
}
