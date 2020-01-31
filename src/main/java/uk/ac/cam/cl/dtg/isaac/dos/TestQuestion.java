package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.segue.dos.content.ChoiceQuestion;

import java.util.List;

public class TestQuestion extends ChoiceQuestion {
    List<TestCase> testCases;

    public void setTestCases(List<TestCase> tests) {
        this.testCases = tests;
    }
    public List<TestCase> getTestCases() {
        return this.testCases;
    }
}