package uk.ac.cam.cl.dtg.segue.dao.schools;

/**
 * An exception that is thrown when there is a problem building the index for a
 * list of schools.
 * 
 */
public class UnableToIndexSchoolsException extends Exception {
	private static final long serialVersionUID = 3524664884686464375L;

	/**
	 * Creates an UnableToIndexSchoolsException.
	 * 
	 * @param message
	 *            - addition information to help identify the exception.
	 */
	public UnableToIndexSchoolsException(final String message) {
		super(message);
	}
	
	/** Creates an UnableToIndexSchoolsException.
	 * 
	 * @param message
	 *            - addition information to help identify the exception.
	 * @param e - addition exception objects.
	 */
	public UnableToIndexSchoolsException(final String message, final Exception e) {
		super(message, e);
	}
}
