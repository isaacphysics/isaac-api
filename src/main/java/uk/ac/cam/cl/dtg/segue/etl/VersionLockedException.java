package uk.ac.cam.cl.dtg.segue.etl;

/**
 * Created by Ian on 01/11/2016.
 */
public class VersionLockedException extends Exception {

  VersionLockedException(final String version) {
    super("Failed to acquire lock for version " + version);
  }
}
