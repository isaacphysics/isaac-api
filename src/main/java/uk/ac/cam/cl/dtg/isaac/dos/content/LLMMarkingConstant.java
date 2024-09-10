package uk.ac.cam.cl.dtg.isaac.dos.content;

/**
 * LLM Marking Constants represent numeric values to be used by the LLM marking formulae.
 */
@JsonContentType("LLMMarkingConstant")
public class LLMMarkingConstant extends LLMMarkingExpression {
    private String type;
    private String value;

    public LLMMarkingConstant() {
    }

    @Override
    public String getType() {
        return type;
    }
    @Override
    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
}
