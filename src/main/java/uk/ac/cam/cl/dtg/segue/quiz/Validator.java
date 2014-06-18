package uk.ac.cam.cl.dtg.segue.quiz;

import uk.ac.cam.cl.dtg.segue.dto.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;

public interface Validator {
	public QuestionValidationResponse validateQuestionResponse(Question question, String answer);
}
