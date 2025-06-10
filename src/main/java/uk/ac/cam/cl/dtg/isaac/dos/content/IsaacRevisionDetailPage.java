package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.IsaacRevisionDetailPageDTO;

import java.util.List;

/**
 *  Revision page detail DO.
 */
@DTOMapping(IsaacRevisionDetailPageDTO.class)
@JsonContentType("isaacRevisionDetailPage")
public class IsaacRevisionDetailPage extends SeguePage {

    private List<String> gameboards;

    public List<String> getGameboards() {
        return gameboards;
    }

    public void setGameboards(final List<String> gameboards) {
        this.gameboards = gameboards;
    }
}
