package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * An exception that indicates an invalid marking response was received from the external marking server.
 */
public class InvalidMarkingResponseException extends Exception {
    /**
     * Constructor with message.
     */
    public InvalidMarkingResponseException(final String message) {
        super(message);
    }
}
