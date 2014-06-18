package uk.ac.cam.cl.dtg.segue.quiz;

import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.Question;

public interface Validator {
	public QuestionValidationResponse validateQuestionResponse(Question question, String answer);
}
