package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * EventCancelledException.
 * <p>
 * This exception should be used if a user attempts to book themselves or another user onto
 * a cancelled event (or if any other event-specific action fails because the event is cancelled)
 * <p>
 * Created by cp766 on 24/01/2023
 */
public class EventIsCancelledException extends Exception {

  /**
   * @param message the message explaining the errors.
   */
  public EventIsCancelledException(final String message) {
    super(message);
  }
}
