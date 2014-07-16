package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceQuestionDTO;

/**
 * Common DO superclass for isaac Questions.
 *
 */
@JsonType("isaacQuestion")
public class IsaacQuestionDTO extends ChoiceQuestionDTO {
	public IsaacQuestionDTO() {
		super();
	}
}
