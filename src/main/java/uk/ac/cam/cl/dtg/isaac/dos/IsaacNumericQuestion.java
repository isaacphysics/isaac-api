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
package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacNumericQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacNumericValidator;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

/**
 * DO for isaacNumericQuestions.
 *
 */
@DTOMapping(IsaacNumericQuestionDTO.class)
@JsonContentType("isaacNumericQuestion")
@ValidatesWith(IsaacNumericValidator.class)
public class IsaacNumericQuestion extends IsaacQuestionBase {
	private Boolean requireUnits;
	private Integer significantFigures;

	/**
	 * Gets the requireUnits.
	 * @return the requireUnits
	 */
	public final Boolean getRequireUnits() {
		if (requireUnits == null) {
			return true;
		}
		
		return requireUnits;
	}

	/**
	 * Sets the requireUnits.
	 * @param requireUnits the requireUnits to set
	 */
	public final void setRequireUnits(final Boolean requireUnits) {
		this.requireUnits = requireUnits;
	}
	
	/**
	 * Gets the expected number of significant figures.
	 * @return the number of sig figs.
	 */
	public int getSignificantFigures() {
		if (null == significantFigures) {
			return 2;
		}
		return significantFigures;
	}

	/**
	 * Sets the required number of significant figures.
	 * @param significantFigures - number of significant figures expected
	 */
	public void setSignificantFigures(final Integer significantFigures) {
		this.significantFigures = significantFigures;
	}
}