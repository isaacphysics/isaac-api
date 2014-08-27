package uk.ac.cam.cl.dtg.segue.auth.exceptions;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * AccountAlreadyLinkedException. This happens when an 3rd party authenticator 
 * account is already linked to a segue account.
 * 
 */
public class AccountAlreadyLinkedException extends SegueDatabaseException {

	private static final long serialVersionUID = -9089794206162108597L;

	/**
	 * Create a new AccountAlreadyLinkedException.
	 * @param message - message to add
	 */
	public AccountAlreadyLinkedException(final String message) {
		super(message);
	}
	
	/**
	 * Create a new SegueDatabaseException.
	 * @param message - message to add
	 * @param e - exception to add.
	 */
	public AccountAlreadyLinkedException(final String message, final Exception e) {
		super(message, e);
	}

}
