package uk.ac.cam.cl.dtg.segue.search;

public class WildcardMatchInstruction extends AbstractMatchInstruction {
    private Long boost;
    private String field;
    private String value;

    public WildcardMatchInstruction(final String field, final String value, final Long boost) {
        this.field = field;
        this.value = value;
        this.boost = boost;
    }
    public WildcardMatchInstruction(final String field, final String value) {
        this(field, value, 1L);
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
