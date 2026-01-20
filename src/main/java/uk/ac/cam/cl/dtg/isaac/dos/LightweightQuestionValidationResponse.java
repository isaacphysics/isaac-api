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
     * @param marks
     *            -
     * @param dateAttempted
     *            -
     */
    public LightweightQuestionValidationResponse(final String questionId, final Boolean correct,
                                                 final Integer marks, final Date dateAttempted) {
        this.questionId = questionId;
        this.correct = correct;
        this.marks = marks;
        this.dateAttempted = dateAttempted;
    }

    /**
     * Constructor without specifying marks (instead derived from 'correct')
     *
     * @param questionId
     *            -
     * @param correct
     *            -
     * @param dateAttempted
     *            -
     */
    public LightweightQuestionValidationResponse(final String questionId, final Boolean correct,
                                                 final Date dateAttempted) {
        this.questionId = questionId;
        this.correct = correct;
        this.marks = (correct != null && correct) ? 1 : 0;
        this.dateAttempted = dateAttempted;
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

    @Override
    public String toString() {
        return "QuestionValidationResponse [questionId=" + questionId + ", correct=" + correct +
                ", marks=" + marks + ", dateAttempted=" + dateAttempted + "]";
    }
}
