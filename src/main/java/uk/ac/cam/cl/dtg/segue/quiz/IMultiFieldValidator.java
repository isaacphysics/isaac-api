package uk.ac.cam.cl.dtg.segue.quiz;

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.Choice;
import uk.ac.cam.cl.dtg.segue.dto.content.Question;

public interface IMultiFieldValidator extends IValidator {
	public List<QuestionValidationResponse> validateMultiFieldQuestionResponses(Question question, List<Choice> answer);
}
