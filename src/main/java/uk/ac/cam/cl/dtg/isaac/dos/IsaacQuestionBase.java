package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;

/**
 * Common DO superclass for isaac Questions.
 *
 */
@DTOMapping(IsaacQuestionBaseDTO.class)
@JsonType("isaacQuestionBase")
public class IsaacQuestionBase extends ChoiceQuestion {

}
