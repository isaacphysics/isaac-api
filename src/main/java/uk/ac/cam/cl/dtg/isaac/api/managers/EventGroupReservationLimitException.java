package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * Indicates that the user is trying to request reservations exceeding the limit specified.
 */
public class EventGroupReservationLimitException extends Exception {
    /**
     * @param message explaining the error
     */
    public EventGroupReservationLimitException(final String message) {
        super(message);
    }
}
