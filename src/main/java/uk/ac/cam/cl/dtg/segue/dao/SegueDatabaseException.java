package uk.ac.cam.cl.dtg.segue.dao;

/**
 * SegueDatabaseException. This is a general exception that can be used to
 * indicate a problem completing a database operation.
 * 
 */
public class SegueDatabaseException extends Exception {
	private static final long serialVersionUID = -5212563770896174988L;
	
	/**
	 * Create a new SegueDatabaseException.
	 * @param message - message to add
	 */
	public SegueDatabaseException(final String message) {
		super(message);
	}
	
	/**
	 * Create a new SegueDatabaseException.
	 * @param message - message to add
	 * @param e - exception to add.
	 */
	public SegueDatabaseException(final String message, final Exception e) {
		super(message, e);
	}

}
