package uk.ac.cam.cl.dtg.segue.comm;

/**
 * EmailMustBeVerifiedException
 * Exception to indicate that a user must have a verified email address to proceed.
 * Created by sac92 on 30/04/2016.
 */
public class EmailMustBeVerifiedException extends Exception {

	/**
	 * EmailMustBeVerifiedException.
	 * @param message indicating what is wrong.
	 */
	public EmailMustBeVerifiedException(final String message) {
		super(message);
	}
}
