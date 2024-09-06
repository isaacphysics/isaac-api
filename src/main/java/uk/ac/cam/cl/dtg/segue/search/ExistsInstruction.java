package uk.ac.cam.cl.dtg.segue.search;

public class ExistsInstruction extends AbstractInstruction {

    private String field;

    public ExistsInstruction(String field) {
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
