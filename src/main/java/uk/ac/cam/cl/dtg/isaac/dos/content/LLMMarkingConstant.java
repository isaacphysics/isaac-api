package uk.ac.cam.cl.dtg.isaac.dos.content;

/**
 * LLM Marking Constants represent numeric values to be used by the LLM marking formulae.
 */
@JsonContentType("LLMMarkingConstant")
public class LLMMarkingConstant extends LLMMarkingExpression {
    private String type;
    private Integer value;

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

    public Integer getValue() {
        return value;
    }
    public void setValue(Integer value) {
        this.value = value;
    }
}
