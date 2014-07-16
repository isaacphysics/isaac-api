package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.quiz.IsaacNumericValidator;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

/**
 * DO for isaacNumericQuestions.
 * 
 */
@JsonType("isaacNumericQuestion")
@ValidatesWith(IsaacNumericValidator.class)
public class IsaacNumericQuestionDTO extends IsaacQuestionDTO {

//	/**
//	 * This method overrides the choices getter to prevent choices from being
//	 * output for this type of question.
//	 * @return null
//	 */
//	@Override
//	public List<ChoiceDTO> getChoices() {
//		return null;
//	}
}
