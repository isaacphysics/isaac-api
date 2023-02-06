package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacMultiPartQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacMultiPartQuestionValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.List;

@DTOMapping(IsaacMultiPartQuestionDTO.class)
@JsonContentType("isaacMultiPartQuestion")
@ValidatesWith(IsaacMultiPartQuestionValidator.class)
public class IsaacMultiPartQuestion extends IsaacQuestionBase {
    // FIXME these should be a list of IsaacBaseQuestion objects, but the Orika converter doesn't like me
    private List<ContentBase> parts;

    public List<ContentBase> getParts() {
        return parts;
    }

    public void setParts(final List<ContentBase> parts) {
        this.parts = parts;
    }
}
