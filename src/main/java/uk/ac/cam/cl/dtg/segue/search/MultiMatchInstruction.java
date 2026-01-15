package uk.ac.cam.cl.dtg.segue.search;

public class MultiMatchInstruction extends AbstractInstruction {
    private final Long boost;
    private final String term;
    private final String[] fields;

    public MultiMatchInstruction(final String term, final String[] fields, final Long boost) {
        this.term = term;
        this.fields = fields;
        this.boost = boost;
    }

    public String getTerm() {
        return this.term;
    }

    public String[] getFields() {
        return this.fields;
    }

    public Long getBoost() {
        return this.boost;
    }
}
