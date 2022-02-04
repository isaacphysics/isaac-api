package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception which indicates the name provided by the user is not valid.
 *
 */
public class InvalidNameException extends Exception {

    /**
     * An exception which indicates the name provided by the user is not valid.
     *
     * @param message
     *            - message to add
     */

    public InvalidNameException(final String message) {
        super(message);
    }
}
