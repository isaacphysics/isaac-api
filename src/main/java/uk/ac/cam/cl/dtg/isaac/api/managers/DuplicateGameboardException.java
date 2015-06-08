package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * An exception that indicates a gameboard already exists with the id provided.
 * 
 * @author Stephen Cummins
 */
public class DuplicateGameboardException extends Exception {
    private static final long serialVersionUID = -2812103839917179690L;

    /**
     * Default constructor.
     */
    public DuplicateGameboardException() {

    }

    /**
     * constructor with message.
     * 
     * @param message
     *            - error message
     */
    public DuplicateGameboardException(final String message) {
        super(message);
    }
}
