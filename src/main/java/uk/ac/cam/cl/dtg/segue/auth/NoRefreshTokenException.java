package uk.ac.cam.cl.dtg.segue.auth;

public class NoRefreshTokenException extends Exception {

	private static final long serialVersionUID = -2529083161172180688L;
	
	/**
	 * Authorization URL to which to redirect the user.
	 */
	private String authorizationUrl;

	/**
	 * Construct a NoRefreshTokenException.
	 * 
	 * @param authorizationUrl
	 *          The authorization URL to redirect the user to.
	 */
	public NoRefreshTokenException(String authorizationUrl) {
		this.authorizationUrl = authorizationUrl;
	}

	/**
	 * @return Authorization URL to which to redirect the user.
	 */
	public String getAuthorizationUrl() {
		return authorizationUrl;
	}
}