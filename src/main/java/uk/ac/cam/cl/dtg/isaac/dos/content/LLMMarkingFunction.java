package uk.ac.cam.cl.dtg.isaac.dos.content;

import java.util.List;

@JsonContentType("LLMMarkingFunction")
public class LLMMarkingFunction extends LLMMarkingExpression {
    private String type;
    private String name;
    private List<LLMMarkingExpression> arguments;

    public LLMMarkingFunction() {
    }

    public String getType() {
        return type;
    }
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
