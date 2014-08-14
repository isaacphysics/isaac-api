package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacMultiChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;

/**
 * Content DO for isaacMultiChoiceQuestions.
 *
 */
@DTOMapping(IsaacMultiChoiceQuestionDTO.class)
@JsonType("isaacMultiChoiceQuestion")
public class IsaacMultiChoiceQuestion extends IsaacQuestionBase {
	
}
