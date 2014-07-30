package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * This exception is used for when we are unable to get an authentication code.
 * 
 * e.g. if the user denies access to our app.
 * 
 */
public class AuthenticationCodeException extends Exception {
	private static final long serialVersionUID = -5464852652296975735L;

	/**
	 * Creates an AuthenticationCode Exception.
	 * 
	 * @param msg
	 *            - message to include with the exception.
	 */
	public AuthenticationCodeException(final String msg) {
		super(msg);
	}

}
