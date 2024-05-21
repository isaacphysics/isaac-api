package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * Indicates the event is NOT full.
 * <br>
 * Created by sac92 on 30/04/2016.
 */
public class EventIsNotFullException extends Exception {

  /**
   * Exception constructor with message for trying to reserve an event with open spaces.
   *
   * @param message explaining the error
   */
  public EventIsNotFullException(final String message) {
    super(message);
  }
}
