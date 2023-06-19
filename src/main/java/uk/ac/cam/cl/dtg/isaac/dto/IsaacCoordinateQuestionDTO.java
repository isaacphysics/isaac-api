package uk.ac.cam.cl.dtg.isaac.dto;

public class IsaacCoordinateQuestionDTO extends IsaacQuestionBaseDTO {

    // If no number of coordinates is specified, we assume any number of coordinates in a choice is valid.
    private Integer numberOfCoordinates;

    // If ordered is true, then the order of the coordinates in a choice matters.
    private Boolean ordered;


    public Integer getNumberOfCoordinates() {
        return numberOfCoordinates;
    }

    public void setNumberOfCoordinates(final Integer numberOfCoordinates) {
        this.numberOfCoordinates = numberOfCoordinates;
    }

    public Boolean getOrdered() {
        return ordered;
    }

    public void setOrdered(final Boolean ordered) {
        this.ordered = ordered;
    }
}
