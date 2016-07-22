package uk.ac.cam.cl.dtg.segue.quiz;

/**
 * An exception which indicates that the question validator is unavailable.
 *
 * @author James Sharkey
 *
 */
public class ValidatorUnavailableException extends Exception {
    private static final long serialVersionUID = -5088917052521699195L;

    /**
     * Creates a ValidatorUnavailableException.
     *
     * @param message
     *            - to store with the exception.
     */
    public ValidatorUnavailableException(final String message) {
        super(message);
    }
}
