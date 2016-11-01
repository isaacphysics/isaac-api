package uk.ac.cam.cl.dtg.segue.etl;

/**
 * Created by Ian on 01/11/2016.
 */
public class VersionLockedException extends Exception {

    private String version;

    VersionLockedException(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "Cannot index version " + version + ": Lock unavailable.";
    }
}
