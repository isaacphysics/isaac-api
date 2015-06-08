package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * An exception that indicates an invalid gameboard has been provided.
 * 
 * @author Stephen Cummins
 */
public class InvalidGameboardException extends Exception {
    private static final long serialVersionUID = -2812103839917179690L;

    /**
     * Default constructor.
     */
    public InvalidGameboardException() {

    }

    /**
     * constructor with message.
     * 
     * @param message
     *            - error message
     */
    public InvalidGameboardException(final String message) {
        super(message);
    }
}
