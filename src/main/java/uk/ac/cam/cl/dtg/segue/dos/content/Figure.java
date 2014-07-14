package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.segue.dto.content.FigureDTO;

/**
 * Figure class is a specialisation of an Image.
 */
@DTOMapping(FigureDTO.class)
@JsonType("figure")
public class Figure extends Image {

}
