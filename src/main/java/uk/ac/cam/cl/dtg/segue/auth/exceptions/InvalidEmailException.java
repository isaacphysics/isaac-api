package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception which indicates the email address provided by the user is not valid.
 */
public class InvalidEmailException extends Exception {

  /**
   * An exception which indicates the email address provided by the user is not valid.
   *
   * @param message - message to add
   */

  public InvalidEmailException(final String message) {
    super(message);
  }
}
