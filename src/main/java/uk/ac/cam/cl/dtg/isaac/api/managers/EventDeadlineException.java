package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * Indicates the event booking deadline has passed.
 * <br>
 * Created by sac92 on 30/04/2016.
 */
public class EventDeadlineException extends Exception {

  /**
   * Exception constructor with message for a passed booking deadline.
   *
   * @param message explaining the error
   */
  public EventDeadlineException(final String message) {
    super(message);
  }

}
