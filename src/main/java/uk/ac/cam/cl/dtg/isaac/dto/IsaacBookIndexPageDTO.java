package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.ImageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;

/**
 * Book Index Page DTO.
 *
 */
@JsonContentType("isaacBookIndexPage")
public class IsaacBookIndexPageDTO extends SeguePageDTO {

    private ImageDTO coverImage;

    public ImageDTO getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(final ImageDTO coverImage) {
        this.coverImage = coverImage;
    }
}
