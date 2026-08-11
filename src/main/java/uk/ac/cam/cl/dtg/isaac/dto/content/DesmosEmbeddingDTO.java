package uk.ac.cam.cl.dtg.isaac.dto.content;

public class DesmosEmbeddingDTO extends MediaDTO {
    private String calculatorId;
    private String calculatorType;

    public String getCalculatorId() {
        return calculatorId;
    }

    public void setCalculatorId(final String calculatorId) {
        this.calculatorId = calculatorId;
    }

    public String getCalculatorType() {
        return calculatorType;
    }

    public void setCalculatorType(final String calculatorType) {
        this.calculatorType = calculatorType;
    }
}
