package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

public class IsaacCoordinateQuestionDTO extends IsaacQuestionBaseDTO {

    // If no number of coordinates is specified, we assume any number of coordinates in a choice is valid.
    private Integer numberOfCoordinates;
    private Integer numberOfDimensions;

    // If ordered is true, then the order of the coordinates in a choice matters.
    private Boolean ordered;

    private List<String> placeholderValues;

    public Integer getNumberOfCoordinates() {
        return numberOfCoordinates;
    }

    public void setNumberOfCoordinates(final Integer numberOfCoordinates) {
        this.numberOfCoordinates = numberOfCoordinates;
    }

    public Integer getNumberOfDimensions() {
        return numberOfDimensions;
    }

    public void setNumberOfDimensions(Integer numberOfDimensions) {
        this.numberOfDimensions = numberOfDimensions;
    }

    public Boolean getOrdered() {
        return ordered;
    }

    public void setOrdered(final Boolean ordered) {
        this.ordered = ordered;
    }

    public List<String> getPlaceholderValues() {
        return placeholderValues;
    }

    public void setPlaceholderValues(List<String> placeholderValues) {
        this.placeholderValues = placeholderValues;
    }
}
