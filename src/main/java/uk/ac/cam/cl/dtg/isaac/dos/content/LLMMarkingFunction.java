package uk.ac.cam.cl.dtg.isaac.dos.content;

import java.util.List;

/**
 * LLM marking functions represent the functions that can be applied to the arguments in the LLM marking formulae.
 */
@JsonContentType("LLMMarkingFunction")
public class LLMMarkingFunction extends LLMMarkingExpression {
    public enum FunctionName {
        SUM, MIN, MAX
    }

    private String type;
    private FunctionName name;
    private List<LLMMarkingExpression> arguments;

    public LLMMarkingFunction() {
    }

    @Override
    public String getType() {
        return type;
    }
    @Override
    public void setType(String type) {
        this.type = type;
    }

    public FunctionName getName() {
        return name;
    }
    public void setName(FunctionName name) {
        this.name = name;
    }

    public List<LLMMarkingExpression> getArguments() {
        return arguments;
    }
    public void setArguments(List<LLMMarkingExpression> arguments) {
        this.arguments = arguments;
    }
}
