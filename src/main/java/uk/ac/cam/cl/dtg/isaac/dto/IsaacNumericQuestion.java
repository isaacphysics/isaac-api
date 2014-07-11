package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.quiz.IsaacNumericValidator;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonType;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

@JsonType("isaacNumericQuestion")
@ValidatesWith(IsaacNumericValidator.class)
public class IsaacNumericQuestion extends IsaacQuestion {

}
