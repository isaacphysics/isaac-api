package uk.ac.cam.cl.dtg.segue.auth;

/**
 * An exception which indicates that an invalid password has been provided.
 * 
 * @author Stephen Cummins
 * 
 */
public class InvalidPasswordException extends Exception {
	private static final long serialVersionUID = -2703137641897650020L;

	public InvalidPasswordException(String message) {
		super(message);
	}
}
