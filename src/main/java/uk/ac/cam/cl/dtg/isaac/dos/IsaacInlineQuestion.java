package uk.ac.cam.cl.dtg.isaac.dos;


import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCoordinateQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacInlineQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacCoordinateValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.List;

/**
 * Content DO for IsaacCoordinateQuestions.
 *
 */
@DTOMapping(IsaacInlineQuestionDTO.class)
@JsonContentType("isaacInlineQuestion")
@ValidatesWith(IsaacCoordinateValidator.class)
public class IsaacInlineQuestion extends IsaacQuestionBase {
    private List<IsaacStringMatchQuestion> inlineQuestions;

    public void setInlineQuestions(List<IsaacStringMatchQuestion> inlineQuestions) {
        this.inlineQuestions = inlineQuestions;
    }

    public List<IsaacStringMatchQuestion> getInlineQuestions() {
        return inlineQuestions;
    }
}
