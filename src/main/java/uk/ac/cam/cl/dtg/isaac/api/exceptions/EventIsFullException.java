package uk.ac.cam.cl.dtg.isaac.api.exceptions;

/**
 * Indicates the event is full.
 * <br>
 * Created by sac92 on 30/04/2016.
 */
public class EventIsFullException extends Exception {

  /**
   * Exception constructor with message for trying to book on a full event.
   *
   * @param message explaining the error
   */
  public EventIsFullException(final String message) {
    super(message);
  }
}
