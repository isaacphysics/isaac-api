package uk.ac.cam.cl.dtg.segue.dto;

import java.util.Date;

import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

/**
 * The DTO which can be used to inform clients of the result of an answered
 * question.
 * 
 * 
 */
public class QuantityValidationResponseDTO extends QuestionValidationResponseDTO {
	private Boolean correctValue;
	private Boolean correctUnits;
	
	/**
	 * Default constructor.
	 */
	public QuantityValidationResponseDTO() {

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
	public QuantityValidationResponseDTO(final String questionId, final ChoiceDTO answer,
			final Boolean correct, final Content explanation, final Boolean correctValue,
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
