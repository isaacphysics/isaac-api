package uk.ac.cam.cl.dtg.isaac.dto.content;

import java.util.List;

/**
 * DTO to represent chapters inside IsaacBookPages.
 */
public class BookChapterDTO extends ContentDTO {

    private String label;
    private List<BookSectionDTO> sections;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<BookSectionDTO> getSections() {
        return sections;
    }

    public void setSections(List<BookSectionDTO> sections) {
        this.sections = sections;
    }
}
