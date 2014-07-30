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
