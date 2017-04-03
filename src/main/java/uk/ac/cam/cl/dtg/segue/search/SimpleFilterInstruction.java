package uk.ac.cam.cl.dtg.segue.search;

/**
 * Created by sac92 on 25/02/17.
 */
public class SimpleFilterInstruction extends AbstractFilterInstruction {
    private final String mustMatchValue;

    public SimpleFilterInstruction(String mustMatchValue) {

        this.mustMatchValue = mustMatchValue;
    }

    public String getMustMatchValue() {
        return mustMatchValue;
    }
}
