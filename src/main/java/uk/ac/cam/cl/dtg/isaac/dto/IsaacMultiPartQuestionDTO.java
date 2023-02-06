package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;

import java.util.List;

@JsonContentType("isaacMultiPartQuestion")
public class IsaacMultiPartQuestionDTO extends IsaacQuestionBaseDTO {
    private List<ContentBaseDTO> parts;

    public List<ContentBaseDTO> getParts() {
        return parts;
    }

    public void setParts(final List<ContentBaseDTO> parts) {
        this.parts = parts;
    }
}
