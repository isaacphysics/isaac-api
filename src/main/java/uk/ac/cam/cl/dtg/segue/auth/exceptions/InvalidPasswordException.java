package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception which indicates that an invalid password has been provided when
 * trying to create or update a password for a given user.
 * 
 * @author Stephen Cummins
 * 
 */
public class InvalidPasswordException extends Exception {
	private static final long serialVersionUID = -2703137641897650020L;

	/**
	 * Creates an InvalidPasswordException.
	 * 
	 * @param message
	 *            - to store with the password.
	 */
	public InvalidPasswordException(final String message) {
		super(message);
	}
}
