package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.BookChapterDTO;

import java.util.List;

/**
 * DO to represent chapters inside IsaacBookPages.
 */
@DTOMapping(BookChapterDTO.class)
@JsonContentType("bookChapter")
public class BookChapter extends Content {

    private String label;
    private List<BookSection> sections;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<BookSection> getSections() {
        return sections;
    }

    public void setSections(List<BookSection> sections) {
        this.sections = sections;
    }
}
