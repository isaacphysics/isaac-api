package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;
import uk.ac.cam.cl.dtg.isaac.dto.content.InlineRegionDTO;

import java.util.List;

/**
 * Inline Region DO.
 */
@DTOMapping(InlineRegionDTO.class)
@JsonContentType("isaacInlineRegion")
public class InlineRegion extends Content {
    private List<IsaacQuestionBase> inlineQuestions;
    private List<ContentBase> hints;

    public InlineRegion() {}

    public List<IsaacQuestionBase> getInlineQuestions() {
        return inlineQuestions;
    }

    public void setInlineQuestions(List<IsaacQuestionBase> inlineQuestions) {
        this.inlineQuestions = inlineQuestions;
    }

    public List<ContentBase> getHints() {
        return hints;
    }

    public void setHints(final List<ContentBase> hints) {
        this.hints = hints;
    }
}
