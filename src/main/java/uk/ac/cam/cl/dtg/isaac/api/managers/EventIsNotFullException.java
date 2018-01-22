package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * Indicates the event is NOT full.
 *
 * Created by sac92 on 30/04/2016.
 */
public class EventIsNotFullException extends Exception {

	/**
	 * @param message explaining the error
	 */
	public EventIsNotFullException(final String message) {
		super(message);
	}
}
