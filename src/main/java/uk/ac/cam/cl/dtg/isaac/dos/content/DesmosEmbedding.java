package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.DesmosEmbeddingDTO;

@DTOMapping(DesmosEmbeddingDTO.class)
@JsonContentType("desmosEmbedding")
public class DesmosEmbedding extends Media {
    private String calculatorId;

    public String getCalculatorId() {
        return calculatorId;
    }

    public void setCalculatorId(final String calculatorId) {
        this.calculatorId = calculatorId;
    }
}
