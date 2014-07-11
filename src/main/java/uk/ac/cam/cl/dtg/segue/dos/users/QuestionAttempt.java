package uk.ac.cam.cl.dtg.segue.dos.users;

import java.util.Date;

/**
 * Question Attempt DO to record users previous question attempts.
 * 
 * @author Stephen Cummins
 */
public class QuestionAttempt {
	private String questionId;
	private Date dateAttempted;
	private boolean success;
	private String detail;

	/**
	 * Default constructor for QuestionAttempt.
	 */
	public QuestionAttempt() {

	}
	
	/**
	 * Create a question attempt object.
	 * 
	 * @param questionId - Question Id which has been attempted
	 * @param dateAttempted - date/time that it was attempted.
	 * @param success - result of attempt. e.g. correct, incorrect.
	 * @param detail - additional information e.g. which answer given.
	 */
	public QuestionAttempt(final String questionId, final Date dateAttempted,
			final boolean success, final String detail) {
		this.questionId = questionId;
		this.dateAttempted = dateAttempted;
		this.success = success;
		this.detail = detail;
	}

	/**
	 * Gets the questionId.
	 * @return the questionId
	 */
	public final String getQuestionId() {
		return questionId;
	}

	/**
	 * Sets the questionId.
	 * @param questionId the questionId to set
	 */
	public final void setQuestionId(final String questionId) {
		this.questionId = questionId;
	}

	/**
	 * Gets the dateAttempted.
	 * @return the dateAttempted
	 */
	public final Date getDateAttempted() {
		return dateAttempted;
	}

	/**
	 * Sets the dateAttempted.
	 * @param dateAttempted the dateAttempted to set
	 */
	public final void setDateAttempted(final Date dateAttempted) {
		this.dateAttempted = dateAttempted;
	}

	/**
	 * Gets the result.
	 * @return the result
	 */
	public final boolean isSuccess() {
		return success;
	}

	/**
	 * Sets the result.
	 * @param success the result to set
	 */
	public final void setResult(final boolean success) {
		this.success = success;
	}

	/**
	 * Gets the detail.
	 * @return the detail
	 */
	public final String getDetail() {
		return detail;
	}

	/**
	 * Sets the detail.
	 * @param detail the detail to set
	 */
	public final void setDetail(final String detail) {
		this.detail = detail;
	}
}
