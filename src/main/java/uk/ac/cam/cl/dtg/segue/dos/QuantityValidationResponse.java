/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dos;

import java.util.Date;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dto.QuantityValidationResponseDTO;

/**
 * The DO which can be used to inform clients of the result of an answered
 * question.
 * 
 */
@DTOMapping(QuantityValidationResponseDTO.class)
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

	@Override
	public String toString() {
		return "QuantityValidationResponse [correctValue=" + correctValue + ", correctUnits=" + correctUnits
				+ "]";
	}
}
