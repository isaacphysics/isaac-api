package uk.ac.cam.cl.dtg.segue.quiz;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;

public interface IValidator {
	public QuestionValidationResponse validateQuestionResponse(
			Question question, Choice answer);
}
