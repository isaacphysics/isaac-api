package uk.ac.cam.cl.dtg.segue.dto.content.values;

/**
 * Created by ipd21 on 12/06/2017.
 */
public class ExpressionDTO {
    private String pythonExpression;
    private boolean requiresExactMatch;

    /**
     * Default constructor required for mapping.
     */
    public ExpressionDTO() {

    }

    /**
     * Gets the python expression.
     *
     * @return the python expression
     */
    public final String getPythonExpression() {
        return pythonExpression;
    }

    /**
     * Sets the python expression.
     *
     * @param pythonExpression
     *            the python expression to set
     */
    public final void setPythonExpression(final String pythonExpression) {
        this.pythonExpression = pythonExpression;
    }

}
