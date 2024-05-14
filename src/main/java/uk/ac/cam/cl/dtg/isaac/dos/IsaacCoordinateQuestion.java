package uk.ac.cam.cl.dtg.isaac.dos;


import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCoordinateQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacCoordinateValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

/**
 * Content DO for IsaacCoordinateQuestions.
 *
 */
@DTOMapping(IsaacCoordinateQuestionDTO.class)
@JsonContentType("isaacCoordinateQuestion")
@ValidatesWith(IsaacCoordinateValidator.class)
public class IsaacCoordinateQuestion extends IsaacQuestionBase {

    // If no number of coordinates is specified, we assume any number of coordinates in a choice is valid.
    private Integer numberOfCoordinates;

    // If ordered is true, then the order of the coordinates in a choice matters.
    private Boolean ordered;
    private String placeholderXValue;
    private String placeholderYValue;
    private Integer significantFiguresMin;
    private Integer significantFiguresMax;

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

    public String getPlaceholderXValue() {
        return placeholderXValue;
    }

    public void setPlaceholderXValue(String placeholderXValue) {
        this.placeholderXValue = placeholderXValue;
    }

    public String getPlaceholderYValue() {
        return placeholderYValue;
    }

    public void setPlaceholderYValue(String placeholderYValue) {
        this.placeholderYValue = placeholderYValue;
    }
}
