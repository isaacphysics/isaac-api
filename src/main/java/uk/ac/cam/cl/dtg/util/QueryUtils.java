package uk.ac.cam.cl.dtg.util;

import java.util.Arrays;
import java.util.List;

public final class QueryUtils {
  public static List<String> splitCsvStringQueryParam(final String queryParamCsv) {
    if (null != queryParamCsv && !queryParamCsv.isEmpty()) {
      return Arrays.asList(queryParamCsv.split(","));
    } else {
      return null;
    }
  }
}
