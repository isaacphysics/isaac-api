package uk.ac.cam.cl.dtg.isaac.api.managers;

/**
 * An exception that indicates a skill question has already been attempted.
 *
 * @author Barna Magyarkuti
 */
public class DuplicateSkillsAttemptException extends Exception {

    /**
     * constructor with message.
     *
     * @param message
     *            - error message
     */
    public DuplicateSkillsAttemptException(final String message) {
        super(message);
    }
}
