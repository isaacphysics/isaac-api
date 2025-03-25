package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.BookSectionDTO;

/**
 * DO to represent sections inside BookChapters.
 */
@DTOMapping(BookSectionDTO.class)
@JsonContentType("bookSection")
public class BookSection extends Content {

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
