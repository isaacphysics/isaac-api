package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.BookSectionDTO;

/**
 * DO to represent sections inside BookChapters.
 */
@DTOMapping(BookSectionDTO.class)
@JsonContentType("bookSection")
public class BookSection extends Content {

    private String label;
    private String gameboardId;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getGameboardId() {
        return gameboardId;
    }

    public void setGameboardId(String gameboardId) {
        this.gameboardId = gameboardId;
    }
}
