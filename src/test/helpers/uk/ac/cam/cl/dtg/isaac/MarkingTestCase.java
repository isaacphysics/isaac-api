package uk.ac.cam.cl.dtg.isaac;

import uk.ac.cam.cl.dtg.isaac.marks.Mark;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;

public class MarkingTestCase {
    private final String testDescription;
    private final IsaacLLMFreeTextQuestion question;
    private final Mark response;
    private final boolean expectedResult;
    private final int expectedMark;

    public static MarkingTestCase testCase(String testDescription, IsaacLLMFreeTextQuestion question, Mark response,
                                           boolean expectedResult, int expectedMark) {
        return new MarkingTestCase(testDescription, question, response, expectedResult, expectedMark);
    }

    private MarkingTestCase(String testDescription, IsaacLLMFreeTextQuestion question, Mark response,
                            boolean expectedResult, int expectedMark) {
        this.testDescription = testDescription;
        this.question = question;
        this.response = response;
        this.expectedResult = expectedResult;
        this.expectedMark = expectedMark;
    }

    public IsaacLLMFreeTextQuestion getQuestion() {
        return question;
    }

    public Mark getResponse() {
        return response;
    }

    public boolean getExpectedResult() {
        return expectedResult;
    }

    public int getExpectedMark() {
        return expectedMark;
    }

    public String toString() {
        return this.testDescription;
    }
}