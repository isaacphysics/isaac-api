package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception to indicate that we suspect CSRF to have happened.
 * 
 * @author Stephen Cummins
 * 
 */
public class CrossSiteRequestForgeryException extends Exception {
	private static final long serialVersionUID = -8542483814754486874L;

	/**
	 * Create a CSRF Exception.
	 * 
	 * @param msg
	 *            - message to attach to the exception.
	 */
	public CrossSiteRequestForgeryException(final String msg) {
		super(msg);
	}
}
