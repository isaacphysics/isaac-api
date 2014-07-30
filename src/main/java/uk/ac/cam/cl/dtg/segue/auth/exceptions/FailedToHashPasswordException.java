package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception which indicates that we failed to set a password due to some
 * internal error.
 * 
 * @author Stephen Cummins
 * 
 */
public class FailedToHashPasswordException extends RuntimeException {
	private static final long serialVersionUID = -3422483699332261694L;

	/**
	 * Creates a failed to hash password exception.
	 * 
	 * @param message
	 *            - message to include with the exception.
	 */
	public FailedToHashPasswordException(final String message) {
		super(message);
	}
}
