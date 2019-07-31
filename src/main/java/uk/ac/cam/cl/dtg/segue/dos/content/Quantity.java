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
package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.segue.dto.content.QuantityDTO;

/**
 * Quantity class is a specialised choice object that allows both a value and a unit to be stored.
 */
@DTOMapping(QuantityDTO.class)
@JsonContentType("quantity")
public class Quantity extends Choice {
    private String units;

    /**
     * Default constructor required for mapping.
     */
    public Quantity() {

    }

    /**
     * Additional constructor to help unit testing.
     *
     * @param value - the value of the Quantity
     */
    public Quantity(final String value) {
        this.value = value;

    }

    /**
     * Additional constructor to help unit testing.
     *
     * @param value - the value of the Quantity
     * @param units - the units of the Quantity
     */
    public Quantity(final String value, final String units) {
        this.value = value;
        this.units = units;

    }

    /**
     * Gets the units.
     * 
     * @return the units
     */
    public final String getUnits() {
        return units;
    }

    /**
     * Sets the units.
     * 
     * @param units
     *            the units to set
     */
    public final void setUnits(final String units) {
        this.units = units;
    }
}
