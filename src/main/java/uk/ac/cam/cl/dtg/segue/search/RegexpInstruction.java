package uk.ac.cam.cl.dtg.segue.search;

/**
 * Returns documents that contain match a regex in a provided field.
 */
public class RegexpInstruction extends AbstractInstruction {
    private final String field;
    private final String regex;

    public RegexpInstruction(final String field, final String regex) {
        this.field = field;
        this.regex = regex;
    }

    public String getField() {
        return this.field;
    }

    public String getRegex() {
        return this.regex;
    }
}
