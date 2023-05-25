package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;

import java.util.List;

@JsonContentType("isaacMultiPartQuestion")
public class IsaacMultiPartQuestionDTO extends IsaacQuestionBaseDTO {
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
