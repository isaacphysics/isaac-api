package uk.ac.cam.cl.dtg.segue.dto;

/**
 * Represents any content related data that can be stored by the api
 *
 */
public abstract class ContentBase {
	private String canonicalSourceFile;

	public String getCanonicalSourceFile() {
		return canonicalSourceFile;
	}

	public void setCanonicalSourceFile(String canonicalSourceFile) {
		this.canonicalSourceFile = canonicalSourceFile;
	}
}
