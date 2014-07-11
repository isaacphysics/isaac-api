package uk.ac.cam.cl.dtg.segue.quiz;

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;

public interface IMultiFieldValidator extends IValidator {
	public List<QuestionValidationResponse> validateMultiFieldQuestionResponses(
			Question question, List<Choice> answer);
}
