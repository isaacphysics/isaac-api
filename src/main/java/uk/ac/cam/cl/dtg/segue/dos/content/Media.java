package uk.ac.cam.cl.dtg.segue.dos.content;

/**
 * Media (Abstract) Domain Object To be used anywhere that a figure should be
 * displayed in the CMS.
 * 
 */
@JsonType("media")
public abstract class Media extends Content {
	protected String src;
	protected String altText;

	/**
	 * Gets the src.
	 * 
	 * @return the src
	 */
	public String getSrc() {
		return src;
	}

	/**
	 * Sets the src.
	 * 
	 * @param src
	 *            the src to set
	 */
	public void setSrc(final String src) {
		this.src = src;
	}

	/**
	 * Gets the altText.
	 * 
	 * @return the altText
	 */
	public String getAltText() {
		return altText;
	}

	/**
	 * Sets the altText.
	 * 
	 * @param altText
	 *            the altText to set
	 */
	public void setAltText(final String altText) {
		this.altText = altText;
	}

}
