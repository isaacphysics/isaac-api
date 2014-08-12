package uk.ac.cam.cl.dtg.segue.auth.exceptions;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * SegueDatabaseException. This is a general exception that can be used to
 * indicate a problem completing a database operation.
 * 
 */
public class DuplicateAccountException extends SegueDatabaseException {
	private static final long serialVersionUID = 7415512495026915539L;

	/**
	 * Create a new SegueDatabaseException.
	 * @param message - message to add
	 */
	public DuplicateAccountException(final String message) {
		super(message);
	}
	
	/**
	 * Create a new SegueDatabaseException.
	 * @param message - message to add
	 * @param e - exception to add.
	 */
	public DuplicateAccountException(final String message, final Exception e) {
		super(message, e);
	}

}
