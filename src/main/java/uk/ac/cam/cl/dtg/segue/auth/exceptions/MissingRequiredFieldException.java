package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception which indicates that the user provided is invalid.
 * 
 * @author Stephen Cummins
 * 
 */
public class MissingRequiredFieldException extends Exception {
	private static final long serialVersionUID = -3077816042521699195L;

	/**
	 * A required field is missing and must be present to save the user.
	 * 
	 * @param message
	 *            - a message to accompany the exception.
	 */
	public MissingRequiredFieldException(final String message) {
		super(message);
	}
}
