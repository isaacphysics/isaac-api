package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.Date;

public class GameboardProgressSummaryDTO {
    private Long assignmentId;
    private String gameboardId;
    private String gameboardTitle;
    private Date dueDate;
    private Date creationDate;
    private Integer questionPartsCorrect;
    private Integer questionPartsIncorrect;
    private Integer questionPartsNotAttempted;
    private Integer questionPartsTotal;
    private Float passMark;
    private Integer questionPagesPerfect;
    private Integer questionPagesTotal;

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getGameboardId() {
        return gameboardId;
    }

    public void setGameboardId(String gameboardId) {
        this.gameboardId = gameboardId;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
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

    public Integer getQuestionPagesPerfect() {
        return questionPagesPerfect;
    }

    public void setQuestionPagesPerfect(Integer questionPagesPerfect) {
        this.questionPagesPerfect = questionPagesPerfect;
    }

    public Integer getQuestionPagesTotal() {
        return questionPagesTotal;
    }

    public void setQuestionPagesTotal(Integer questionPagesTotal) {
        this.questionPagesTotal = questionPagesTotal;
    }
}
