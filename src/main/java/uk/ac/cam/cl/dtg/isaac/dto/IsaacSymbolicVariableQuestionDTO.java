package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.quiz.IsaacSymbolicVariableValidator;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

@JsonContentType("isaacSymbolicVariableQuestion")
@ValidatesWith(IsaacSymbolicVariableValidator.class)
public class IsaacSymbolicVariableQuestionDTO extends IsaacSymbolicQuestionDTO {
    private String variables;

    public final String getVariables() {return variables;}

    public void setVariables(String variables) {this.variables = variables;}

}
