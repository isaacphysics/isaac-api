package uk.ac.cam.cl.dtg.segue.dos.content.values;

import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;


/**
 * Created by ipd21 on 12/06/2017.
 */
@DTOMapping(ExpressionDTO.class)
@JsonContentType("expression")
public class Expression extends Value {
    private java.lang.String pythonExpression;
    private boolean requiresExactMatch;

    public Expression() {

    }

    /**
     * @return the pythonExpression
     */
    public java.lang.String getPythonExpression() {
        return pythonExpression;
    }

    /**
     * @param pythonExpression the pythonExpression to set
     */
    public void setPythonExpression(final java.lang.String pythonExpression) {
        this.pythonExpression = pythonExpression;
    }

    // TODO: requiresExactMatch is gone from here. We need a specialised choice type for that.
}
