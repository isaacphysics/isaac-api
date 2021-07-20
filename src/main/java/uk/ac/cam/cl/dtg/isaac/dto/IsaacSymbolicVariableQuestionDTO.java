package uk.ac.cam.cl.dtg.isaac.dto;

import org.json.JSONObject;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacSymbolicVariableValidator;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

import java.util.HashMap;

@JsonContentType("isaacSymbolicVariableQuestion")
@ValidatesWith(IsaacSymbolicVariableValidator.class)
public class IsaacSymbolicVariableQuestionDTO extends IsaacSymbolicQuestionDTO {
    private String variables;
    private HashMap<String, Integer> enumeratedVariables;

    public final String getVariables() {return variables;}

    public void setVariables(String variables) {this.variables = variables;}

    public final HashMap<String, Integer> getEnumeratedVariables() {return enumeratedVariables;}

    public void setEnumeratedVariables(HashMap<String, Integer> enumeratedVariables) {
        this.enumeratedVariables = enumeratedVariables;
    }
}
