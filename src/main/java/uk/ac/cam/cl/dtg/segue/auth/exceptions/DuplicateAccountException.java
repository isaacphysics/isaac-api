package uk.ac.cam.cl.dtg.segue.auth.exceptions;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * DuplicateAccountException. This exception means that an account already exists with an key field.
 * 
 */
public class DuplicateAccountException extends SegueDatabaseException {
	private static final long serialVersionUID = 7415512495026915539L;

	/**
	 * Create a new DuplicateAccountException.
	 * @param message - message to add
	 */
	public DuplicateAccountException(final String message) {
		super(message);
	}
	
	/**
	 * Create a new DuplicateAccountException.
	 * @param message - message to add
	 * @param e - exception to add.
	 */
	public DuplicateAccountException(final String message, final Exception e) {
		super(message, e);
	}

}
