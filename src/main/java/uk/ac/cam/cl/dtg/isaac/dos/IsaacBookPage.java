package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.BookChapter;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.Image;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacBookPageDTO;

import java.util.List;

/**
 * Book Page DO.
 *
 */
@DTOMapping(IsaacBookPageDTO.class)
@JsonContentType("isaacBookPage")
public class IsaacBookPage extends SeguePage {
    private String label;
    private Image coverImage;
    private List<BookChapter> chapters;

    /**
     * Default constructor for Jackson.
     */
    public IsaacBookPage() {
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<BookChapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<BookChapter> chapters) {
        this.chapters = chapters;
    }

    public Image getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(Image coverImage) {
        this.coverImage = coverImage;
    }
}
