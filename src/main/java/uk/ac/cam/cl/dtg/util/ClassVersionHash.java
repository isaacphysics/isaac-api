package uk.ac.cam.cl.dtg.util;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;

public final class ClassVersionHash {

    private ClassVersionHash() { }

    public static String hashClass(final Class c) {
        try {
            InputStream s = c.getResourceAsStream(c.getSimpleName() + ".class");
            return DigestUtils.sha256Hex(s);
        } catch (IOException e) {
            e.printStackTrace();
            return "ERR";
        }
    }

    public static void logHash(final Logger logger, final Class c) {
        String hash = hashClass(c);
        logger.warn("Hash of " + c.getSimpleName() + ": " + hash);
    }
}
