package uk.ac.cam.cl.dtg.segue.search;

public class MustMatchInstruction extends AbstractMatchInstruction {
    private String field;
    private String value;

    public MustMatchInstruction(String field, String value) {
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
