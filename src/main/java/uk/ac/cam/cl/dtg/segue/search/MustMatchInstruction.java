package uk.ac.cam.cl.dtg.segue.search;

public class MustMatchInstruction extends AbstractMatchInstruction {
    private final String field;
    private final String value;

    public MustMatchInstruction(final String field, final String value) {
        this.field = field;
        this.value = value;
    }
    public String getField() {
        return this.field;
    }
    public String getValue() {
        return this.value;
    }
}
