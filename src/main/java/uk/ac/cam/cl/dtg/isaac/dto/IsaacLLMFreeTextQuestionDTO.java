package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import static uk.ac.cam.cl.dtg.segue.api.Constants.LLM_FREE_TEXT_QUESTION_TYPE;

@JsonContentType(LLM_FREE_TEXT_QUESTION_TYPE)
@ValidatesWith(IsaacLLMFreeTextValidator.class)
public class IsaacLLMFreeTextQuestionDTO extends QuestionDTO {
    private Integer maxMarks;

    public IsaacLLMFreeTextQuestionDTO() {
    }

    public Integer getMaxMarks() {
        return maxMarks;
    }
    public void setMaxMarks(Integer maxMarks) {
        this.maxMarks = maxMarks;
    }
}
