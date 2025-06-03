package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.Image;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.content.SeguePage;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacBookIndexPageDTO;

/**
 * Book Index Page DO.
 *
 */
@DTOMapping(IsaacBookIndexPageDTO.class)
@JsonContentType("isaacBookIndexPage")
public class IsaacBookIndexPage extends SeguePage {

    private Image coverImage;

    public Image getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(final Image coverImage) {
        this.coverImage = coverImage;
    }
}
