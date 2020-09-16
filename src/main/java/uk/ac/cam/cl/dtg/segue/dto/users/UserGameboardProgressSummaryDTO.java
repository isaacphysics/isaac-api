package uk.ac.cam.cl.dtg.segue.dto.users;

public class UserGameboardProgressSummaryDTO {
    private UserSummaryDTO userSummary;
    private Integer questionPartsCorrect;
    private Integer questionPartsIncorrect;
    private Integer questionPartsNotAttempted;
    private Integer questionPartsTotal;
    private Float passMark;

    public UserGameboardProgressSummaryDTO() {
        this.userSummary = null;
        this.questionPartsCorrect = 0;
        this.questionPartsIncorrect = 0;
        this.questionPartsNotAttempted = 0;
        this.questionPartsTotal = 0;
        this.passMark = 0.0f;
    }

    public UserSummaryDTO getUserSummary() {
        return userSummary;
    }

    public void setUserSummary(UserSummaryDTO userSummary) {
        this.userSummary = userSummary;
    }

    public Integer getQuestionPartsCorrect() {
        return questionPartsCorrect;
    }

    public void setQuestionPartsCorrect(Integer questionPartsCorrect) {
        this.questionPartsCorrect = questionPartsCorrect;
    }

    public Integer getQuestionPartsIncorrect() {
        return questionPartsIncorrect;
    }

    public void setQuestionPartsIncorrect(Integer questionPartsIncorrect) {
        this.questionPartsIncorrect = questionPartsIncorrect;
    }

    public Integer getQuestionPartsNotAttempted() {
        return questionPartsNotAttempted;
    }

    public void setQuestionPartsNotAttempted(Integer questionPartsNotAttempted) {
        this.questionPartsNotAttempted = questionPartsNotAttempted;
    }

    public Integer getQuestionPartsTotal() {
        return questionPartsTotal;
    }

    public void setQuestionPartsTotal(Integer questionPartsTotal) {
        this.questionPartsTotal = questionPartsTotal;
    }

    public Float getPassMark() {
        return passMark;
    }

    public void setPassMark(Float passMark) {
        this.passMark = passMark;
    }
}
