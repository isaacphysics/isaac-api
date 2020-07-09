package uk.ac.cam.cl.dtg.segue.search;

public class ShouldMatchInstruction extends AbstractMatchInstruction {
    private Long boost;
    private String field;
    private String value;
    private boolean fuzzy;

    // TODO If we need more parameters in the future we should probably follow a Builder pattern
    public ShouldMatchInstruction(final String field, final String value, final Long boost, final boolean fuzzy) {
        this.field = field;
        this.value = value;
        this.boost = boost;
        this.fuzzy = fuzzy;
    }
    public ShouldMatchInstruction(final String field, final String value) {
        this(field, value, 1L, false);
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
    public boolean getFuzzy() {
        return this.fuzzy;
    }
}
