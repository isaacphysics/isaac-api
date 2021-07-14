
package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacClozeDndQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacParsonsValidator;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

@DTOMapping(IsaacClozeDndQuestionDTO.class)
@JsonContentType("isaacClozeDndQuestion")
@ValidatesWith(IsaacParsonsValidator.class)
public class IsaacClozeDndQuestion extends IsaacParsonsQuestion {
}
