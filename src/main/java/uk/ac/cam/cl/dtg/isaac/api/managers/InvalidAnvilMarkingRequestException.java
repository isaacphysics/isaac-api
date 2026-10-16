package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * An exception that indicates an invalid marking response was received from the external marking server.
 */
public class InvalidAnvilMarkingRequestException extends Exception {
    private final String detailedProblem;

    /** Constructor with message, detailed problem.*/
    public InvalidAnvilMarkingRequestException(final String message, final String detailedProblem) {
        super(message);
        this.detailedProblem = detailedProblem;
    }

    /** Get detailed problem.*/
    public String getDetailedProblem() {
        return detailedProblem;
    }
}
