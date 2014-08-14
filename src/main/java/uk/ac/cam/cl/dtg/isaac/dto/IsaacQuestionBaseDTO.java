package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceQuestionDTO;

/**
 * Common DTO superclass for isaac Questions.
 *
 */
@JsonType("isaacQuestionBase")
public class IsaacQuestionBaseDTO extends ChoiceQuestionDTO {
	public IsaacQuestionBaseDTO() {
		super();
	}
}
