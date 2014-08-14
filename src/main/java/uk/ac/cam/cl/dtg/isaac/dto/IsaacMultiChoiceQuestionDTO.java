package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

/**
 * Content DO for isaacMultiChoiceQuestions.
 *
 */
@JsonType("isaacMultiChoiceQuestion")
public class IsaacMultiChoiceQuestionDTO extends IsaacQuestionBaseDTO {
	
	/**
	 * We would like the choices to appear in a different order each time.
	 * @return randomized list of choice objects.
	 */
	@Override
	public List<ChoiceDTO> getChoices() {
		long seed = System.nanoTime();
		Collections.shuffle(super.choices, new Random(seed));
		return super.choices;
	}
}
