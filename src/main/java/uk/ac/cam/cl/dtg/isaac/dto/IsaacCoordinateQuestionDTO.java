package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.List;

public class IsaacCoordinateQuestionDTO extends IsaacQuestionBaseDTO {

    // If no number of coordinates is specified, we assume any number of coordinates in a choice is valid.
    private Integer numberOfCoordinates;
    private Integer numberOfDimensions;

    // If ordered is true, then the order of the coordinates in a choice matters.
    private Boolean ordered;

    private List<String> placeholderValues;
    private Boolean useBrackets;
    private String separator;
    private String[] prefixes;
    private String[] suffixes;
    private String buttonText;

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
     *           - the separator to use between values
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
     * @return suffixes to use after each value.
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
