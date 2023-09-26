package uk.ac.cam.cl.dtg.util;

import static org.apache.commons.text.StringEscapeUtils.escapeJava;

import java.util.Map;

public final class LogUtils {
  private LogUtils() {
  }
  /* While there is currently no difference in the handling of 'internal' and 'external' value sources, I have tried
   to distinguish between values in log statements that could be provided by general users of the platform and values
   that are sourced from the database or used in log statements that should only be reached by trusted sources such as
   the ETL and other infrastructure functions or internal staff serving as platform administrators.
   */

  /**
   * Sanitise a value to be logged.
   * Any characters outside the unicode range of 32-127 (Latin and ASCII alphabet, symbols and digits) will be escaped.
   * <br>
   * 'Internal' values are expected to be provided by sources such infrastructure functions or admin users. While it is
   * still necessary to guard against forged requests, the originating endpoints should never be accessed by general
   * users.
   *
   * @param value the String to be sanitised
   * @return the sanitised String
   */
  public static String sanitiseInternalLogValue(final String value) {
    return escapeJava(value);
  }

  /**
   * Sanitise a value to be logged.
   * Any characters outside the unicode range of 32-127 (Latin and ASCII alphabet, symbols and digits) will be escaped.
   * <br>
   * 'External' values originate from endpoints expected to be called on behalf of general users during normal operation
   * of the platform. While the front-end may provide some validation or selection process under normal circumstances,
   * the relevant endpoints are more visible to more casual attackers.
   *
   * @param value the String to be sanitised
   * @return the sanitised String
   */
  public static String sanitiseExternalLogValue(final String value) {
    return escapeJava(value);
  }

  /**
   * Sanitise a value to be logged.
   * Any characters outside the unicode range of 32-127 (Latin and ASCII alphabet, symbols and digits) will be escaped.
   * <br>
   * 'External' values originate from endpoints expected to be called on behalf of general users during normal operation
   * of the platform. While the front-end may provide some validation or selection process under normal circumstances,
   * the relevant endpoints are more visible to more casual attackers.
   *
   * @param value the String to be sanitised
   * @return the sanitised String
   */
  public static String sanitiseExternalLogValue(final Map<String, Object> value) {
    if (value != null) {
      return sanitiseExternalLogValue(value.toString());
    }
    return null;
  }
}
