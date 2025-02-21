package uk.ac.cam.cl.dtg.isaac.dto.content;

import java.util.List;

public class QuizSummaryDTO extends ContentSummaryDTO {
    private List<String> hiddenFromRoles;

    public QuizSummaryDTO() {

    }

    public List<String> getHiddenFromRoles() {
        return hiddenFromRoles;
    }

    public void setHiddenFromRoles(List<String> hiddenFromRoles) {
        this.hiddenFromRoles = hiddenFromRoles;
    }
}
