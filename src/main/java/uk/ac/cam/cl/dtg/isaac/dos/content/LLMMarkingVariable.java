package uk.ac.cam.cl.dtg.isaac.dos.content;

@JsonContentType("LLMMarkingVariable")
public class LLMMarkingVariable extends LLMMarkingExpression {
    private String type;
    private String name;

    public LLMMarkingVariable() {
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
}
