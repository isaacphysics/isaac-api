package uk.ac.cam.cl.dtg.util;

import static org.apache.commons.text.StringEscapeUtils.escapeJava;

public class LogUtils {
    public static String sanitiseLogValue(String value) {
        return escapeJava(value);
    }
}
