package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.IsaacBookDetailPageDTO;

import java.util.List;

/**
 *  Book detail page DO.
 */
@DTOMapping(IsaacBookDetailPageDTO.class)
@JsonContentType("isaacBookDetailPage")
public class IsaacBookDetailPage extends SeguePage {

    private List<String> gameboards;
    private List<String> extensionGameboards;

    public List<String> getGameboards() {
        return gameboards;
    }

    public void setGameboards(final List<String> gameboards) {
        this.gameboards = gameboards;
    }

    public List<String> getExtensionGameboards() {
        return extensionGameboards;
    }

    public void setExtensionGameboards(final List<String> extensionGameboards) {
        this.extensionGameboards = extensionGameboards;
    }
}
