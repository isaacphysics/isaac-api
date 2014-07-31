package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.segue.dto.content.VideoDTO;

/**
 * Video Content object.
 *
 */
@DTOMapping(VideoDTO.class)
@JsonType("video")
public class Video extends Media {

}
