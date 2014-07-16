package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;

/**
 * Common DO superclass for isaac Questions.
 *
 */
@DTOMapping(IsaacQuestionDTO.class)
@JsonType("isaacQuestion")
public class IsaacQuestion extends ChoiceQuestion {

}
