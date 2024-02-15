package uk.ac.cam.cl.dtg.isaac.dto.content;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacStringMatchQuestionDTO;

import java.util.List;

/**
 * Inline region DTO.
 *
 */
public class InlineRegionDTO extends ContentDTO {
    private List<IsaacStringMatchQuestionDTO> inlineQuestions;
    private List<ContentBaseDTO> hints;

    public InlineRegionDTO() {}

    public List<IsaacStringMatchQuestionDTO> getInlineQuestions() {
        return inlineQuestions;
    }

    public void setInlineQuestions(List<IsaacStringMatchQuestionDTO> inlineQuestions) {
        this.inlineQuestions = inlineQuestions;
    }

    public List<ContentBaseDTO> getHints() {
        return hints;
    }

    public void setHints(List<ContentBaseDTO> hints) {
        this.hints = hints;
    }
}
