package uk.ac.cam.cl.dtg.segue.dto.content;

/**
 * DTO representing a segue page.
 *
 */
public class SeguePageDTO extends ContentDTO {
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
