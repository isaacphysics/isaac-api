package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.BookChapterDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;

import java.util.List;

/**
 * BookIndex Page DTO.
 *
 */
@JsonContentType("isaacBookIndexPage")
public class IsaacBookIndexPageDTO extends SeguePageDTO {
    private String label;
    private ImageDTO coverImage;
    private List<BookChapterDTO> chapters;

    /**
     * Default constructor for Jackson.
     */
    public IsaacBookIndexPageDTO() {
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<BookChapterDTO> getChapters() {
        return chapters;
    }

    public void setChapters(List<BookChapterDTO> chapters) {
        this.chapters = chapters;
    }

    public ImageDTO getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(ImageDTO coverImage) {
        this.coverImage = coverImage;
    }
}
