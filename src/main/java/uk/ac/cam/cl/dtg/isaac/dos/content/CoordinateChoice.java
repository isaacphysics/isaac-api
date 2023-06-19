package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.CoordinateChoiceDTO;

@DTOMapping(CoordinateChoiceDTO.class)
@JsonContentType("coordinateChoice")
public class CoordinateChoice extends ItemChoice {
    /**
     * Default constructor required for mapping.
     */
    public CoordinateChoice() {
    }
}
