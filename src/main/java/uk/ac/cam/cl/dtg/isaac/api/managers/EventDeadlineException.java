package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * Indicates the event booking deadline has passed.
 *
 * Created by sac92 on 30/04/2016.
 */
public class EventDeadlineException extends Exception {

	/**
	 * @param message explaining the error
	 */
	public EventDeadlineException(final String message) {
		super(message);
	}

}
