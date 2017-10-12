package uk.ac.cam.cl.dtg.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public class ClassVersionHash {

    public static String hashClass(Class c) {
        try {
            InputStream s = c.getResourceAsStream(c.getSimpleName() + ".class");
            return DigestUtils.sha256Hex(s);
        } catch (IOException e) {
            e.printStackTrace();
            return "ERR";
        }
    }

    public static void logHash(Logger logger, Class c) {
        String hash = hashClass(c);
        logger.warn("Hash of " + c.getSimpleName() + ": " + hash);
    }
}
