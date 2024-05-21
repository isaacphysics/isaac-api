package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * DuplicateBookingException
 * This exception should be used when a user is already booked onto an event.
 * Created by sac92 on 30/04/2016.
 */
public class DuplicateBookingException extends Exception {

  /**
   * Constructor with message for trying to book on an event when already booked onto it.
   *
   * @param message the message explaining the errors.
   */
  public DuplicateBookingException(final String message) {
    super(message);
  }
}
