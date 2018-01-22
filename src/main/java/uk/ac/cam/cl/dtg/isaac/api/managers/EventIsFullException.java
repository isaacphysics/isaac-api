package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * Indicates the event is full.
 *
 * Created by sac92 on 30/04/2016.
 */
public class EventIsFullException extends Exception {

	/**
	 * @param message explaining the error
	 */
	public EventIsFullException(final String message) {
		super(message);
	}
}
