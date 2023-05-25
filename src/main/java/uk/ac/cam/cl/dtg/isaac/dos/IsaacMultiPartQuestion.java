package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacMultiPartQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacMultiPartQuestionValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.List;

@DTOMapping(IsaacMultiPartQuestionDTO.class)
@JsonContentType("isaacMultiPartQuestion")
@ValidatesWith(IsaacMultiPartQuestionValidator.class)
public class IsaacMultiPartQuestion extends IsaacQuestionBase {
    private Boolean ordered;
    private Boolean allowDuplicates;
    private List<ContentBaseDTO> parts;

    public Boolean getOrdered() {
        return ordered;
    }

    public void setOrdered(final Boolean ordered) {
        this.ordered = ordered;
    }

    public Boolean getAllowDuplicates() {
        return allowDuplicates;
    }

    public void setAllowDuplicates(final Boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    public List<ContentBaseDTO> getParts() {
        return parts;
    }

    public void setParts(final List<ContentBaseDTO> parts) {
        this.parts = parts;
    }
}
