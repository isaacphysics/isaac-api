package uk.ac.cam.cl.dtg.segue.api.userNotifications;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.streams.KafkaStreamsService;

/**
 * Created by du220 on 18/07/2017.
 */
public class WebsocketDependencyProvider {

    private static KafkaStreamsService kafkaStreamsService;
    private static ILogManager logManager;
    private static UserAccountManager userManager;

    private static final Logger log = LoggerFactory.getLogger(WebsocketDependencyProvider.class);

    @Inject
    public WebsocketDependencyProvider(final KafkaStreamsService kafkaStreamsService,
                                       final ILogManager logManager,
                                       final UserAccountManager userManager) {

        this.kafkaStreamsService = kafkaStreamsService;
        this.logManager = logManager;
        this.userManager = userManager;
    }

    public static KafkaStreamsService getKafkaStreamsService() {

        while (kafkaStreamsService == null) {
            waitUntilReady();
        }

        return kafkaStreamsService;
    }

    public static ILogManager getLogManager() {

        while (logManager == null) {
            waitUntilReady();
        }

        return logManager;
    }

    public static UserAccountManager getUserManager() {

        while (userManager == null) {
            waitUntilReady();
        }
        return userManager;
    }



    private static void waitUntilReady() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

}
