package uk.ac.cam.cl.dtg.util;

/**
 * Unchecked exception thrown in the event that the ReflectionUtils is unable to retrieve one or more Classes contained
 * within the specified package.
 */
public class ClassReflectionException extends RuntimeException {
  public ClassReflectionException() {
    super();
  }

  public ClassReflectionException(String message) {
    super(message);
  }

  public ClassReflectionException(Throwable cause) {
    super(cause);
  }

  public ClassReflectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
