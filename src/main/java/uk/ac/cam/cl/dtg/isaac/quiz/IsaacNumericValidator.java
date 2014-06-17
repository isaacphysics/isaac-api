package uk.ac.cam.cl.dtg.isaac.quiz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dto.Question;
import uk.ac.cam.cl.dtg.segue.quiz.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.quiz.Validator;

public class IsaacNumericValidator implements Validator {
	private static final Logger log = LoggerFactory.getLogger(IsaacNumericValidator.class);
	
	@Override
	public QuestionValidationResponse validateQuestionResponse(
			Question question, String answer) {
		throw new UnsupportedOperationException("This method is not implemented yet.");
	}
}
