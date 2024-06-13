package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

@JsonContentType("isaacLLMFreeTextQuestion")
@ValidatesWith(IsaacLLMFreeTextValidator.class)
public class IsaacLLMFreeTextQuestionDTO extends QuestionDTO {
    public IsaacLLMFreeTextQuestionDTO() {
    }
}
