package uk.ac.cam.cl.dtg.isaac.dto.content;

import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;

import java.util.List;

/**
 *  Book detail page DTO.
 */
public class IsaacBookDetailPageDTO extends SeguePageDTO {

    private List<GameboardDTO> gameboards;
    private List<GameboardDTO> extensionGameboards;

    public List<GameboardDTO> getGameboards() {
        return gameboards;
    }

    public void setGameboards(final List<GameboardDTO> gameboards) {
        this.gameboards = gameboards;
    }

    public List<GameboardDTO> getExtensionGameboards() {
        return extensionGameboards;
    }

    public void setExtensionGameboards(final List<GameboardDTO> extensionGameboards) {
        this.extensionGameboards = extensionGameboards;
    }
}
