package uk.ac.cam.cl.dtg.segue.dao;

/**
 *   A subclass of SegueDatabaseException specifically for lock timeouts.
 */
public class SegueDatabaseLockTimoutException extends SegueDatabaseException {
    public SegueDatabaseLockTimoutException(String message) {
        super(message);
    }
}
