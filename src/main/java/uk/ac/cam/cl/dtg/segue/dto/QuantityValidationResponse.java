package uk.ac.cam.cl.dtg.segue.dto;

import uk.ac.cam.cl.dtg.segue.dos.content.Content;

/**
 * The DTO which can be used to inform clients of the result of an answered
 * question.
 * 
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

	public QuantityValidationResponse(String questionId, String answer,
			Boolean correct, Content explanation, Boolean correctValue,
			Boolean correctUnits) {
		super(questionId, answer, correct, explanation);
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
