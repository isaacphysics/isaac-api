package uk.ac.cam.cl.dtg.segue.auth;

/**
 * An exception which indicates that there are no credentials configured for the
 * user so we cannot authenticate using this provider.
 * 
 * @author Stephen Cummins
 * 
 */
public class NoCredentialsAvailableException extends Exception {
	private static final long serialVersionUID = -2703137641897650020L;

	public NoCredentialsAvailableException(String message) {
		super(message);
	}
}
