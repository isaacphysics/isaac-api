package uk.ac.cam.cl.dtg.segue.auth;

/**
 * An exception which indicates that we failed to set a password.
 * 
 * @author Stephen Cummins
 * 
 */
public class FailedToSetPasswordException extends Exception {
	private static final long serialVersionUID = -3422483699332261694L;

	public FailedToSetPasswordException(String message) {
		super(message);
	}
}
