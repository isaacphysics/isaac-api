package uk.ac.cam.cl.dtg.segue.quiz;

import uk.ac.cam.cl.dtg.segue.dto.Question;

public interface Validator {
	public QuestionValidationResponse validateQuestionResponse(Question question, String answer);
}
