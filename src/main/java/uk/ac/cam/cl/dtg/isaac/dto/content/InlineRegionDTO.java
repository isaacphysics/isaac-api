package uk.ac.cam.cl.dtg.isaac.dto.content;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacStringMatchQuestionDTO;

import java.util.List;

/**
 * Inline region DTO.
 *
 */
public class InlineRegionDTO extends ContentDTO {
    private List<IsaacStringMatchQuestionDTO> inlineQuestions;

    public InlineRegionDTO() {}

    public List<IsaacStringMatchQuestionDTO> getInlineQuestions() {
        return inlineQuestions;
    }

    public void setInlineQuestions(List<IsaacStringMatchQuestionDTO> inlineQuestions) {
        this.inlineQuestions = inlineQuestions;
    }
}
