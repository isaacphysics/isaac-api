package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;

public class TestCase extends QuestionValidationResponse {
    Boolean expected;

    public void setExpected(Boolean expected) {
        this.expected = expected;
    }
    public Boolean getExpected() {
        return this.expected;
    }
}
