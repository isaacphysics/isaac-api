package uk.ac.cam.cl.dtg.isaac.models.content;

import uk.ac.cam.cl.dtg.isaac.quiz.IsaacNumericValidator;
import uk.ac.cam.cl.dtg.segue.dto.content.JsonType;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

@JsonType("isaacNumericQuestion")
@ValidatesWith(IsaacNumericValidator.class)
public class IsaacNumericQuestion extends IsaacQuestion {

}
