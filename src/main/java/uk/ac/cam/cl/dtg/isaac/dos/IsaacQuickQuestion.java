package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuickQuestionDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;

/**
 * Quick Question DO.
 */
@DTOMapping(IsaacQuickQuestionDTO.class)
@JsonType("isaacQuestion")
public class IsaacQuickQuestion extends IsaacQuestionBase {
	
}
