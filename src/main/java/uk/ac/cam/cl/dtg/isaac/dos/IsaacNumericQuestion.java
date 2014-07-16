package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacNumericQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacNumericValidator;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

/**
 * DO for isaacNumericQuestions.
 *
 */
@DTOMapping(IsaacNumericQuestionDTO.class)
@JsonType("isaacNumericQuestion")
@ValidatesWith(IsaacNumericValidator.class)
public class IsaacNumericQuestion extends IsaacQuestion {

}
