package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception which indicates a failure in mapping a string to a known security provider.
 * 
 * @author Stephen Cummins
 * 
 */
public class AuthenticationProviderMappingException extends Exception {
	private static final long serialVersionUID = -284452434569344L;

	/**
	 * Creates an AuthenticationProviderMappingException.
	 * 
	 * @param message
	 *            - to store with the exception.
	 */
	public AuthenticationProviderMappingException(final String message) {
		super(message);
	}
}
