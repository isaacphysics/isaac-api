package uk.ac.cam.cl.dtg.isaac.dos.content;

import java.util.Map;

public class LlmFreeTextMarkedExample {
    private String answer;
    private Map<String, Integer> marks;
    private Integer marksAwarded;

    public LlmFreeTextMarkedExample() {
    }

    public String getAnswer() {
        return answer;
    }
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Map<String, Integer> getMarks() {
        return marks;
    }
    public void setMarks(Map<String, Integer> marks) {
        this.marks = marks;
    }

    public Integer getMarksAwarded() {
        return marksAwarded;
    }
    public void setMarksAwarded(Integer marksAwarded) {
        this.marksAwarded = marksAwarded;
    }
}
