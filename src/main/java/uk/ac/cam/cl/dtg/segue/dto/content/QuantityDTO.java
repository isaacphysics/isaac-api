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
package uk.ac.cam.cl.dtg.segue.dto.content;

/**
 * DTO to represent a Quantity choice.
 *
 */
public class QuantityDTO extends ChoiceDTO {
	private String units;

	/**
	 * Default constructor required for mapping.
	 */
	public QuantityDTO() {

	}

	/**
	 * Gets the units.
	 * @return the units
	 */
	public final String getUnits() {
		return units;
	}

	/**
	 * Sets the units.
	 * @param units the units to set
	 */
	public final void setUnits(final String units) {
		this.units = units;
	}
}
