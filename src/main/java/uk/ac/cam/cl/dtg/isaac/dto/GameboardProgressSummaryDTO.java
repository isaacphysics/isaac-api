package uk.ac.cam.cl.dtg.isaac.dto;

public class GameboardProgressSummaryDTO {
    private String gameboardId;
    private String gameboardTitle;
    private Integer questionPartsCorrect;
    private Integer questionPartsIncorrect;
    private Integer questionPartsNotAttempted;
    private Integer questionPartsTotal;
    private Float passMark;

    public String getGameboardId() {
        return gameboardId;
    }

    public void setGameboardId(String gameboardId) {
        this.gameboardId = gameboardId;
    }

    public String getGameboardTitle() {
        return gameboardTitle;
    }

    public void setGameboardTitle(String gameboardTitle) {
        this.gameboardTitle = gameboardTitle;
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
