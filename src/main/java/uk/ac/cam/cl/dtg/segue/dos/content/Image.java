package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.segue.dto.content.ImageDTO;

/**
 * Image is in any picture.
 */
@DTOMapping(ImageDTO.class)
@JsonType("image")
public class Image extends Media {
	/**
	 * Default constructor required for mapping purposes.
	 */
	public Image() {

	}
}
