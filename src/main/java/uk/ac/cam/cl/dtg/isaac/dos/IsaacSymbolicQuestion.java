package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacSymbolicQuestionDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;

/**
 * Isaac Symbolic Question DO.
 *
 */
@DTOMapping(IsaacSymbolicQuestionDTO.class)
@JsonType("isaacSymbolicQuestion")
public class IsaacSymbolicQuestion extends IsaacQuestionBase {

}
