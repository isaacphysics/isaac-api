package uk.ac.cam.cl.dtg.isaac.dos.content;

@DTOMapping(DesmosEmbedding.class)
@JsonContentType("desmosEmbedding")
public class DesmosEmbedding extends Media {
    private String calculatorId;

    public String getCalculatorId() {
        return calculatorId;
    }

    public void setCalculatorId(String calculatorId) {
        this.calculatorId = calculatorId;
    }
}
