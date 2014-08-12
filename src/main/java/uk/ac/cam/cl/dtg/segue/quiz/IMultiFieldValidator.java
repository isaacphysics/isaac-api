package uk.ac.cam.cl.dtg.segue.quiz;

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

/**
 * Interface that allows questions to have multiple fields. 
 * 
 * @author Stephen Cummins
 */
public interface IMultiFieldValidator extends IValidator {
	
	/**
	 * ValidateMultiFieldQuestionResponses
	 * This will allow a questoin with multiple fields to be validated by the 
	 * quiz engine.
	 * 
	 * @param question question to be validated.
	 * @param answer answer from the user to validate.
	 * @return a List of QuestionValidationResponseDTOs.
	 */
	List<QuestionValidationResponseDTO> validateMultiFieldQuestionResponses(
			Question question, List<ChoiceDTO> answer);
}
