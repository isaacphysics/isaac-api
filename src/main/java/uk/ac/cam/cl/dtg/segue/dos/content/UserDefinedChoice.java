package uk.ac.cam.cl.dtg.segue.dos.content;

public class UserDefinedChoice extends Choice {
    boolean userDefinedCorrect;
    protected ContentBase userDefinedExplanation;

    public Boolean getUserDefinedCorrect() {
        return userDefinedCorrect;
    }
    public void setUserDefinedCorrect(Boolean userDefinedCorrect) {
        this.userDefinedCorrect = userDefinedCorrect;
    }
    public ContentBase getUserDefinedExplanation() {
        return userDefinedExplanation;
    }
    public void setUserDefinedExplanation(ContentBase userDefinedExplanation) {
        this.userDefinedExplanation = userDefinedExplanation;
    }

}
