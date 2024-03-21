package uk.ac.cam.cl.dtg.isaac.dto.content;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;

import java.util.List;

/**
 * Inline region DTO.
 *
 */
public class InlineRegionDTO extends ContentDTO {
    private List<IsaacQuestionBaseDTO> inlineQuestions;
    private List<ContentBaseDTO> hints;

    public InlineRegionDTO() {}

    public List<IsaacQuestionBaseDTO> getInlineQuestions() {
        return inlineQuestions;
    }

    public void setInlineQuestions(List<IsaacQuestionBaseDTO> inlineQuestions) {
        this.inlineQuestions = inlineQuestions;
    }

    public List<ContentBaseDTO> getHints() {
        return hints;
    }

    public void setHints(List<ContentBaseDTO> hints) {
        this.hints = hints;
    }
}
