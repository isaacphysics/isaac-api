package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacInlineValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.List;

/**
 * Isaac Inline Question DTO.
 */
@JsonContentType("isaacInlineQuestion")
@ValidatesWith(IsaacInlineValidator.class)
public class IsaacInlineQuestionDTO extends IsaacQuestionBaseDTO {
    private List<IsaacStringMatchQuestionDTO> inlineQuestions;

    public void setInlineQuestions(List<IsaacStringMatchQuestionDTO> inlineQuestions) {
        this.inlineQuestions = inlineQuestions;
    }

    public List<IsaacStringMatchQuestionDTO> getInlineQuestions() {
        return inlineQuestions;
    }
}
