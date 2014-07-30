package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.segue.dto.content.QuantityDTO;

/**
 * Quantity class is a specialised choice object that allows both a
 * value and a unit to be stored.
 */
@DTOMapping(QuantityDTO.class)
@JsonType("quantity")
public class Quantity extends Choice {
	private String units;

	/**
	 * Default constructor required for mapping.
	 */
	public Quantity() {
		
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
