package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception which indicates a failure in confirming the security or
 * authenticity of an authentication request / response.
 * 
 * @author Stephen Cummins
 * 
 */
public class AuthenticatorSecurityException extends Exception {
	private static final long serialVersionUID = -2844599433274739344L;

	/**
	 * Creates an AuthenticatorSecurityException.
	 * 
	 * @param message
	 *            - to store with the exception.
	 */
	public AuthenticatorSecurityException(final String message) {
		super(message);
	}
}
