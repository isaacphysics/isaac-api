package uk.ac.cam.cl.dtg.segue.dto;

import uk.ac.cam.cl.dtg.segue.dos.content.Content;

/**
 * The DTO which can be used to inform clients of the result of an answered
 * question.
 * 
 * 
 */
public class QuantityValidationResponse extends QuestionValidationResponse {
	private boolean correctValue;
	private boolean correctUnits;
	
	/**
	 * Default constructor.
	 */
	public QuantityValidationResponse() {

	}

	public QuantityValidationResponse(String questionId, String answer,
			boolean correct, Content explanation, boolean correctValue,
			boolean correctUnits) {
		super(questionId, answer, correct, explanation);
		this.correctValue = correctValue;
		this.correctUnits = correctUnits;
	}

	public boolean isCorrectValue() {
		return correctValue;
	}

	public void setCorrectValue(boolean correctValue) {
		this.correctValue = correctValue;
	}

	public boolean isCorrectUnits() {
		return correctUnits;
	}

	public void setCorrectUnits(boolean correctUnits) {
		this.correctUnits = correctUnits;
	}
}
