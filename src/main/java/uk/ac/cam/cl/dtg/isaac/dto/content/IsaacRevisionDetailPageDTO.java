package uk.ac.cam.cl.dtg.isaac.dto.content;

import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;

import java.util.List;

/**
 *  Revision page detail DTO.
 */
public class IsaacRevisionDetailPageDTO extends SeguePageDTO {

    private List<GameboardDTO> gameboards;

    public List<GameboardDTO> getGameboards() {
        return gameboards;
    }

    public void setGameboards(final List<GameboardDTO> gameboards) {
        this.gameboards = gameboards;
    }
}