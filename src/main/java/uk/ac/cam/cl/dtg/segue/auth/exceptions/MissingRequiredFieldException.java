package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception which indicates that the user provided is invalid.
 * 
 * @author Stephen Cummins
 * 
 */
public class MissingRequiredFieldException extends Exception {
	private static final long serialVersionUID = -3077816042521699195L;

	public MissingRequiredFieldException(String message) {
		super(message);
	}
}
