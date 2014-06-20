package uk.ac.cam.cl.dtg.segue.quiz;

import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.Choice;
import uk.ac.cam.cl.dtg.segue.dto.content.Question;

public interface IValidator {
	public QuestionValidationResponse validateQuestionResponse(Question question, Choice answer);
}
