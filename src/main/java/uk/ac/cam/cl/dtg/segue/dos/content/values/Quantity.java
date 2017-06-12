package uk.ac.cam.cl.dtg.segue.dos.content.values;

import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;


/**
 * Created by ipd21 on 12/06/2017.
 */
@DTOMapping(QuantityDTO.class)
@JsonContentType("quantity")
public class Quantity extends Value {
    private java.lang.String units;

    /**
     * Default constructor required for mapping.
     */
    public Quantity() {

    }

    /**
     * Gets the units.
     *
     * @return the units
     */
    public final java.lang.String getUnits() {
        return units;
    }

    /**
     * Sets the units.
     *
     * @param units
     *            the units to set
     */
    public final void setUnits(final java.lang.String units) {
        this.units = units;
    }

}
