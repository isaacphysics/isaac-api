package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCoordinateQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacCoordinateValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.List;

/**
 * Content DO for IsaacCoordinateQuestions.
 *
 */
@DTOMapping(IsaacCoordinateQuestionDTO.class)
@JsonContentType("isaacCoordinateQuestion")
@ValidatesWith(IsaacCoordinateValidator.class)
public class IsaacCoordinateQuestion extends IsaacQuestionBase {

    private Integer numberOfCoordinates;  // If not specified, we assume any number of coordinates is allowed.
    private Integer numberOfDimensions;

    private Boolean ordered;  // If true, the order of the coordinates in a choice matters.
    private List<String> placeholderValues;
    private Integer significantFiguresMin;
    private Integer significantFiguresMax;

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

    /**
     * Gets the minimum allowed number of significant figures.
     *
     * @return the number of sig figs.
     */
    public Integer getSignificantFiguresMin() {
        return significantFiguresMin;
    }

    /**
     * Sets the minimum allowed number of significant figures.
     *
     * @param significantFigures
     *            - minimum allowed number of significant figures
     */
    public void setSignificantFiguresMin(final Integer significantFigures) {
        this.significantFiguresMin = significantFigures;
    }

    /**
     * Gets the maximum allowed number of significant figures.
     *
     * @return the maximum allowed number of sig figs.
     */
    public Integer getSignificantFiguresMax() {
        return significantFiguresMax;
    }

    /**
     * Sets the maximum allowed number of significant figures.
     *
     * @param significantFigures
     *            - maximum allowed number of significant figures
     */
    public void setSignificantFiguresMax(final Integer significantFigures) {
        this.significantFiguresMax = significantFigures;
    }

    public List<String> getPlaceholderValues() {
        return placeholderValues;
    }

    public void setPlaceholderValues(List<String> placeholderValues) {
        this.placeholderValues = placeholderValues;
    }
}
