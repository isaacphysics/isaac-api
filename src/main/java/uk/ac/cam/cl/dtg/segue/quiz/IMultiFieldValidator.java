package uk.ac.cam.cl.dtg.segue.quiz;

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

public interface IMultiFieldValidator extends IValidator {
	public List<QuestionValidationResponseDTO> validateMultiFieldQuestionResponses(
			Question question, List<ChoiceDTO> answer);
}
