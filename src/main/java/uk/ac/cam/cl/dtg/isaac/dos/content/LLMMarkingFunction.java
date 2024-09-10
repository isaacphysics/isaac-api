package uk.ac.cam.cl.dtg.isaac.dos.content;

import java.util.List;

/**
 * LLM marking functions represent the functions that can be applied to the arguments in the LLM marking formulae.
 */
@JsonContentType("LLMMarkingFunction")
public class LLMMarkingFunction extends LLMMarkingExpression {
    private String type;
    private String name;
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

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public List<LLMMarkingExpression> getArguments() {
        return arguments;
    }
    public void setArguments(List<LLMMarkingExpression> arguments) {
        this.arguments = arguments;
    }
}
