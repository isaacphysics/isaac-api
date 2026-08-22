package uk.ac.cam.cl.dtg.segue.auth.exceptions;

/**
 * An exception to indicate that the user is age restricted from accessing the service.
 *
 * @author Jaycie Brown
 *
 */
public class AgeRestrictedException extends Exception {
    public AgeRestrictedException(final String message) {
        super(message);
    }
}
