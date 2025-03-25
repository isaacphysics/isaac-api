package uk.ac.cam.cl.dtg.isaac.dto.content;

/**
 * DTO to represent sections inside BookChapters.
 */
public class BookSectionDTO extends ContentDTO {

    private String label;
    private String bookPageId;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getBookPageId() {
        return bookPageId;
    }

    public void setBookPageId(String bookPageId) {
        this.bookPageId = bookPageId;
    }
}
