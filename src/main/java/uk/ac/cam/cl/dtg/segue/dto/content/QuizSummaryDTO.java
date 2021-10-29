package uk.ac.cam.cl.dtg.segue.dto.content;

public class QuizSummaryDTO extends ContentSummaryDTO {
    private boolean visibleToStudents;

    public QuizSummaryDTO() {

    }

    public boolean getVisibleToStudents() {
        return visibleToStudents;
    }

    public void setVisibleToStudents(final boolean visibleToStudents) {
        this.visibleToStudents = visibleToStudents;
    }
}
