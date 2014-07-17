package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;


/**
 * Segue Page object.
 *
 */
@DTOMapping(SeguePageDTO.class)
@JsonType("page")
public class SeguePage extends Content {
	private String summary;
	
	/**
	 * Gets the summary.
	 * @return the summary
	 */
	public final String getSummary() {
		return summary;
	}

	/**
	 * Sets the summary.
	 * @param summary the summary to set
	 */
	public final void setSummary(final String summary) {
		this.summary = summary;
	}
}
