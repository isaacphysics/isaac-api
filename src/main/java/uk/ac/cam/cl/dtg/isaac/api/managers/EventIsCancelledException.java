package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * EventCancelledException.
 * <br>
 * This exception should be used if a user attempts to book themselves or another user onto
 * a cancelled event (or if any other event-specific action fails because the event is cancelled)
 * <br>
 * Created by cp766 on 24/01/2023
 */
public class EventIsCancelledException extends Exception {

  /**
   * Exception constructor with message for trying to book a cancelled event.
   *
   * @param message the message explaining the errors.
   */
  public EventIsCancelledException(final String message) {
    super(message);
  }
}
