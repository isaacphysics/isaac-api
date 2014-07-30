package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception which indicates a failure during credential verification.
 * 
 * @author Stephen Cummins
 */
public class IncorrectCredentialsProvidedException extends Exception {
	private static final long serialVersionUID = -2703137641897650020L;

	/**
	 * Creates an IncorrectCredentialsProvidedException.
	 * 
	 * @param message
	 *            - indicating the fault.
	 */
	public IncorrectCredentialsProvidedException(final String message) {
		super(message);
	}
}
