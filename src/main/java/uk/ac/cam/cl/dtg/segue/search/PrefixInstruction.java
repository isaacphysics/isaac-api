package uk.ac.cam.cl.dtg.segue.search;

/**
 * Returns documents that contain a specific prefix in a provided field.
 */
public class PrefixInstruction extends AbstractInstruction {
    private final Long boost;
    private final String field;
    private final String value;

    public PrefixInstruction(final String field, final String value, final Long boost) {
        this.field = field;
        this.value = value;
        this.boost = boost;
    }

    public String getField() {
        return this.field;
    }

    public String getValue() {
        return this.value;
    }

    public Long getBoost() {
        return this.boost;
    }
}
