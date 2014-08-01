package uk.ac.cam.cl.dtg.segue.dos;

import java.util.Date;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;

/**
 * The DO which can be used to inform clients of the result of an answered
 * question.
 * 
 */
public class QuantityValidationResponse extends QuestionValidationResponse {
	private Boolean correctValue;
	private Boolean correctUnits;
	
	/**
	 * Default constructor.
	 */
	public QuantityValidationResponse() {

	}
	
	/**
	 * Full constructor. 
	 * @param questionId - 
	 * @param answer - 
	 * @param correct - 
	 * @param explanation - 
	 * @param correctValue - 
 	 * @param correctUnits - 
	 * @param dateAttempted -
	 */
	public QuantityValidationResponse(final String questionId,
			final Choice answer, final Boolean correct,
			final Content explanation, final Boolean correctValue,
			final Boolean correctUnits, final Date dateAttempted) {
		super(questionId, answer, correct, explanation, dateAttempted);
		this.correctValue = correctValue;
		this.correctUnits = correctUnits;
	}

	/**
	 * Gets the correctValue.
	 * @return the correctValue
	 */
	public final Boolean getCorrectValue() {
		return correctValue;
	}

	/**
	 * Sets the correctValue.
	 * @param correctValue the correctValue to set
	 */
	public final void setCorrectValue(final Boolean correctValue) {
		this.correctValue = correctValue;
	}

	/**
	 * Gets the correctUnits.
	 * @return the correctUnits
	 */
	public final Boolean getCorrectUnits() {
		return correctUnits;
	}

	/**
	 * Sets the correctUnits.
	 * @param correctUnits the correctUnits to set
	 */
	public final void setCorrectUnits(final Boolean correctUnits) {
		this.correctUnits = correctUnits;
	}
}
