package uk.ac.cam.cl.dtg.util;

import static org.apache.commons.text.StringEscapeUtils.escapeJava;

public final class LogUtils {
  private LogUtils() {
  }

  public static String sanitiseLogValue(final String value) {
    return escapeJava(value);
  }
}
