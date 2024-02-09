package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacStringMatchQuestion;
import uk.ac.cam.cl.dtg.isaac.dto.content.InlineRegionDTO;

import java.util.List;

/**
 * Inline Region DO.
 */
@DTOMapping(InlineRegionDTO.class)
@JsonContentType("isaacInlineRegion")
public class InlineRegion extends Content {
    private List<IsaacStringMatchQuestion> inlineQuestions;

    public InlineRegion() {}

    public List<IsaacStringMatchQuestion> getInlineQuestions() {
        return inlineQuestions;
    }

    public void setInlineQuestions(List<IsaacStringMatchQuestion> inlineQuestions) {
        this.inlineQuestions = inlineQuestions;
    }
}
