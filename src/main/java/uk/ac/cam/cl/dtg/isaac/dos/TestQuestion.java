package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.ChoiceQuestion;

import java.util.List;

public class TestQuestion {
    List<Choice> userDefinedChoices;
    List<TestCase> testCases;

    public void setUserDefinedChoices(List<Choice> userDefinedChoices) {
        this.userDefinedChoices = userDefinedChoices;
    }
    public List<Choice> getUserDefinedChoices() {
        return this.userDefinedChoices;
    }
    public void setTestCases(List<TestCase> tests) {
        this.testCases = tests;
    }
    public List<TestCase> getTestCases() {
        return this.testCases;
    }
}