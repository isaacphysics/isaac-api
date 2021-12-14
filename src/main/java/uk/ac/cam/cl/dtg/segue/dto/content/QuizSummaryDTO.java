package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.List;

public class QuizSummaryDTO extends ContentSummaryDTO {
    private boolean visibleToStudents;
    private List<String> hiddenFromRoles;

    public QuizSummaryDTO() {

    }

    public boolean getVisibleToStudents() {
        return visibleToStudents;
    }

    public void setVisibleToStudents(final boolean visibleToStudents) {
        this.visibleToStudents = visibleToStudents;
    }

    public List<String> getHiddenFromRoles() {
        return hiddenFromRoles;
    }

    public void setHiddenFromRoles(List<String> hiddenFromRoles) {
        this.hiddenFromRoles = hiddenFromRoles;
    }
}
