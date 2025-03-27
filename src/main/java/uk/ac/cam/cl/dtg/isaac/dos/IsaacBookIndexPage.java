package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.BookChapter;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.Image;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacBookIndexPageDTO;

import java.util.List;

/**
 * BookIndex Page DO.
 *
 */
@DTOMapping(IsaacBookIndexPageDTO.class)
@JsonContentType("isaacBookIndexPage")
public class IsaacBookIndexPage extends SeguePage {
    private String label;
    private Image coverImage;
    private List<BookChapter> chapters;

    /**
     * Default constructor for Jackson.
     */
    public IsaacBookIndexPage() {
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public List<BookChapter> getChapters() {
        return chapters;
    }

    public void setChapters(final List<BookChapter> chapters) {
        this.chapters = chapters;
    }

    public Image getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(final Image coverImage) {
        this.coverImage = coverImage;
    }
}
