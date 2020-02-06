package uk.ac.cam.cl.dtg.segue.dto.content;

public class UserDefinedChoiceDTO extends ChoiceDTO {
    boolean userDefinedCorrect;
    protected ContentBaseDTO userDefinedExplanation;

    public Boolean getUserDefinedCorrect() {
        return userDefinedCorrect;
    }
    public void setUserDefinedCorrect(Boolean userDefinedCorrect) {
        this.userDefinedCorrect = userDefinedCorrect;
    }
    public ContentBaseDTO getUserDefinedExplanation() {
        return userDefinedExplanation;
    }
    public void setUserDefinedExplanation(ContentBaseDTO userDefinedExplanation) {
        this.userDefinedExplanation = userDefinedExplanation;
    }
}
