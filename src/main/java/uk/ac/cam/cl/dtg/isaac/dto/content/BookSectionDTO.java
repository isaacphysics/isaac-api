package uk.ac.cam.cl.dtg.isaac.dto.content;

/**
 * DTO to represent sections inside BookChapters.
 */
public class BookSectionDTO extends ContentDTO {

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
