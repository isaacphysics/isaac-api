package uk.ac.cam.cl.dtg.isaac.dos.content;

/**
 * LLM marking variables represent the identifiers for the json fields returned by the LLM for each mark or the maxMarks
 * value specified by the question. When evaluated, these variables will be replaced with the actual values from the
 * current context - the numeric value awarded for that specific mark by the LLM for the student's answer.
 */
@JsonContentType("LLMMarkingVariable")
public class LLMMarkingVariable extends LLMMarkingExpression {
    private String type;
    private String name;

    public LLMMarkingVariable() {
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
}
