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
    private Boolean useBrackets;
    private String separator;
    private String[] prefixes;
    private String[] suffixes;
    private String buttonText;

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

    /**
     * Gets whether to use brackets around the coordinates.
     *
     * @return true or null if brackets should be used, false otherwise.
     */
    public Boolean getUseBrackets() {
        return useBrackets;
    }

    /**
     * Sets whether to use brackets around the coordinates.
     *
     * @param useBrackets
     *           - whether to use brackets around the coordinates
     */
    public void setUseBrackets(final Boolean useBrackets) {
        this.useBrackets = useBrackets;
    }

    /**
     * Gets the separator to use between values.
     *
     * @return separator to use between values, or null to default to comma.
     */
    public String getSeparator() {
        return separator;
    }

    /**
     * Sets the separator to use between values.
     *
     * @param separator
     *           - separator to use between values
     */
    public void setSeparator(final String separator) {
        this.separator = separator;
    }

    /**
     * Gets the prefixes to use before each value.
     *
     * @return the prefixes to use before each value.
     */
    public String[] getPrefixes() {
        return prefixes;
    }

    /**
     * Sets the prefixes to use before each value.
     *
     * @param prefixes
     *           - prefixes to use before each value
     */
    public void setPrefixes(final String[] prefixes) {
        this.prefixes = prefixes;
    }

    /**
     * Gets the suffixes to use after each value.
     *
     * @return the suffixes to use after each value.
     */
    public String[] getSuffixes() {
        return suffixes;
    }

    /**
     * Sets the suffixes to use after each value.
     *
     * @param suffixes
     *           - suffixes to use after each value
     */
    public void setSuffixes(final String[] suffixes) {
        this.suffixes = suffixes;
    }

    /**
     * Gets the text to display on the button used to add additional coordinates.
     *
     * @return the button text, or null to use default text.
     */
    public String getButtonText() {
        return buttonText;
    }

    /**
     * Sets the text to display on the button used to add additional coordinates.
     *
     * @param buttonText
     *          - button text
     */
    public void setButtonText(final String buttonText) {
        this.buttonText = buttonText;
    }
}
