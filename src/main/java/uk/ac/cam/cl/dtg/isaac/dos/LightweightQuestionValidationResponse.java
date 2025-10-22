package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.Date;

/**
 * Lightweight Question Validation Response DO for summary statistics and calculations.
 *
 */
public class LightweightQuestionValidationResponse {
    private String questionId;
    private Boolean correct;
    private Date dateAttempted;
    private Integer marks;

    /**
     * Default Constructor for mappers.
     */
    public LightweightQuestionValidationResponse() {

    }

    /**
     * Full constructor.
     *
     * @param questionId
     *            -
     * @param correct
     *            -
     * @param dateAttempted
     *            -
     * @param marks
     *            -
     */
    public LightweightQuestionValidationResponse(final String questionId, final Boolean correct,
                                                 final Date dateAttempted, final Integer marks) {
        this.questionId = questionId;
        this.correct = correct;
        this.dateAttempted = dateAttempted;
        this.marks = marks;
    }

    /**
     * Gets the questionId.
     *
     * @return the questionId
     */
    public final String getQuestionId() {
        return questionId;
    }

    /**
     * Sets the questionId.
     *
     * @param questionId
     *            the questionId to set
     */
    public final void setQuestionId(final String questionId) {
        this.questionId = questionId;
    }

    /**
     * Gets the correct.
     *
     * @return the correct
     */
    public final Boolean isCorrect() {
        return correct;
    }

    /**
     * Sets the correct.
     *
     * @param correct
     *            the correct to set
     */
    public final void setCorrect(final Boolean correct) {
        this.correct = correct;
    }

    /**
     * Gets the dateAttempted.
     *
     * @return the dateAttempted
     */
    public Date getDateAttempted() {
        return dateAttempted;
    }

    /**
     * Sets the dateAttempted.
     *
     * @param dateAttempted
     *            the dateAttempted to set
     */
    public void setDateAttempted(final Date dateAttempted) {
        this.dateAttempted = dateAttempted;
    }

    /**
     * Gets the marks.
     *
     * @return the marks
     */
    public Integer getMarks() {
        return marks;
    }

    /**
     * Sets the marks.
     *
     * @param marks
     *            the marks to set
     */
    public void setMarks(final Integer marks) {
        this.marks = marks;
    }

    @Override
    public String toString() {
        return "QuestionValidationResponse [questionId=" + questionId + ", correct=" + correct +
                ", dateAttempted=" + dateAttempted + "]";
    }
}
