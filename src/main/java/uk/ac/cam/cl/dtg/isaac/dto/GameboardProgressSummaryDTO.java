package uk.ac.cam.cl.dtg.isaac.dto;

import java.time.Instant;

public class GameboardProgressSummaryDTO {
  private Long assignmentId;
  private String gameboardId;
  private String gameboardTitle;
  private Instant dueDate;
  private Instant creationDate;
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

  public void setAssignmentId(final Long assignmentId) {
    this.assignmentId = assignmentId;
  }

  public String getGameboardId() {
    return gameboardId;
  }

  public void setGameboardId(final String gameboardId) {
    this.gameboardId = gameboardId;
  }

  public Instant getDueDate() {
    return dueDate;
  }

  public void setDueDate(final Instant dueDate) {
    this.dueDate = dueDate;
  }

  public Instant getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(final Instant creationDate) {
    this.creationDate = creationDate;
  }

  public String getGameboardTitle() {
    return gameboardTitle;
  }

  public void setGameboardTitle(final String gameboardTitle) {
    this.gameboardTitle = gameboardTitle;
  }

  public Integer getQuestionPartsCorrect() {
    return questionPartsCorrect;
  }

  public void setQuestionPartsCorrect(final Integer questionPartsCorrect) {
    this.questionPartsCorrect = questionPartsCorrect;
  }

  public Integer getQuestionPartsIncorrect() {
    return questionPartsIncorrect;
  }

  public void setQuestionPartsIncorrect(final Integer questionPartsIncorrect) {
    this.questionPartsIncorrect = questionPartsIncorrect;
  }

  public Integer getQuestionPartsNotAttempted() {
    return questionPartsNotAttempted;
  }

  public void setQuestionPartsNotAttempted(final Integer questionPartsNotAttempted) {
    this.questionPartsNotAttempted = questionPartsNotAttempted;
  }

  public Integer getQuestionPartsTotal() {
    return questionPartsTotal;
  }

  public void setQuestionPartsTotal(final Integer questionPartsTotal) {
    this.questionPartsTotal = questionPartsTotal;
  }

  public Float getPassMark() {
    return passMark;
  }

  public void setPassMark(final Float passMark) {
    this.passMark = passMark;
  }

  public Integer getQuestionPagesPerfect() {
    return questionPagesPerfect;
  }

  public void setQuestionPagesPerfect(final Integer questionPagesPerfect) {
    this.questionPagesPerfect = questionPagesPerfect;
  }

  public Integer getQuestionPagesTotal() {
    return questionPagesTotal;
  }

  public void setQuestionPagesTotal(final Integer questionPagesTotal) {
    this.questionPagesTotal = questionPagesTotal;
  }
}
