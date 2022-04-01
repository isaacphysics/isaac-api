package uk.ac.cam.cl.dtg.util;

import io.smallrye.common.constraint.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.AbstractIsaacFacade;

import javax.ws.rs.core.Response;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class PrototypeReplacement {
    private static final Logger log = LoggerFactory.getLogger(PrototypeReplacement.class);

    private PrototypeReplacement() { }

    public static <F extends AbstractIsaacFacade> Response prototypeEndpoint(final @NotNull String correctMethodName, final @NotNull String prototypeMethodName, final F facadeInstance, final Object... args) {

        long testTime = System.nanoTime();

        Method correctMethod, prototypeMethod;
        try {
            correctMethod = Arrays.stream(facadeInstance.getClass().getMethods()).filter(m -> m.getName().equals(correctMethodName)).collect(Collectors.toList()).get(0);
            if (correctMethod.getReturnType() != Response.class) {
                log.error("Correct method \"%s\" for testing prototype does not have the return type of \"Response\"! Sending error response...");
                return Response.status(500).build();
            }
        } catch (IndexOutOfBoundsException e) {
            log.error("Correct method \"%s\" for testing prototype against does not exist! Sending error response...");
            return Response.status(500).build();
        }

        long correctStartTime, correctEndTime;
        Response correctResponse;
        try {
            correctStartTime = System.nanoTime();
            correctResponse = (Response) correctMethod.invoke(facadeInstance, args);
            correctEndTime = System.nanoTime();
            log.info(String.format("Correct endpoint method \"%s\" took %d ms to run for test #%d.", correctMethodName, (correctEndTime - correctStartTime) / 1000000, testTime));
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("Correct method \"%s\" for testing prototype failed when we attempted to run it! Sending error response...");
            return Response.status(500).build();
        }

        try {
            prototypeMethod = Arrays.stream(facadeInstance.getClass().getMethods()).filter(m -> m.getName().equals(prototypeMethodName)).collect(Collectors.toList()).get(0);
            if (prototypeMethod.getReturnType() != Response.class) {
                log.error("Prototype method \"%s\" does not have the return type of \"Response\"! Cannot run tests against correct value.");
                return correctResponse;
            }
        } catch (IndexOutOfBoundsException e) {
            log.error(String.format("Prototype test method \"%s\" does not exist! Cannot run tests against correct value.", prototypeMethodName));
            return correctResponse;
        }

        Thread prototypeThread = new Thread(() -> {
            Response prototypeResponse;
            try {
                long prototypeStartTime, prototypeEndTime;
                prototypeStartTime = System.nanoTime();
                prototypeResponse = (Response) prototypeMethod.invoke(facadeInstance, args);
                prototypeEndTime = System.nanoTime();
                log.info(String.format("Prototype endpoint method \"%s\" took %d ms to run for test #%d.", prototypeMethodName, (prototypeEndTime - prototypeStartTime) / 1000000, testTime));
            } catch (Exception e) {
                log.error(String.format("Prototype endpoint method \"%s\" threw an error when being called: %s", prototypeMethodName, e));
                return;
            }

            if (null == prototypeResponse) {
                log.warn(String.format("Prototype endpoint method \"%s\" produced null output", prototypeMethod.getName()));
            } else if (correctResponse.equals(prototypeResponse)) {
                log.info(String.format("Prototype endpoint method \"%s\" produced the correct output when compared to correct version.", prototypeMethod.getName()));
            } else {
                log.warn(String.format("Prototype endpoint method \"%s\" produced the incorrect output when compared to correct version.\n\tCorrect response was: %s\n\tPrototype response was: %s", prototypeMethod.getName(), correctResponse, prototypeResponse));
            }
        });
        prototypeThread.setDaemon(false);
        prototypeThread.start();

        return correctResponse;
    }
}
