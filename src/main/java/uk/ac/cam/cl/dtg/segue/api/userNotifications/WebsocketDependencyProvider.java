package uk.ac.cam.cl.dtg.segue.api.userNotifications;

import com.google.inject.Inject;
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

    @Inject
    public WebsocketDependencyProvider(final KafkaStreamsService kafkaStreamsService,
                                       final ILogManager logManager,
                                       final UserAccountManager userManager) {

        this.kafkaStreamsService = kafkaStreamsService;
        this.logManager = logManager;
        this.userManager = userManager;
    }

    public static KafkaStreamsService getKafkaStreamsService() {
        return kafkaStreamsService;
    }

    public static ILogManager getLogManager() {
        return logManager;
    }

    public static UserAccountManager getUserManager() {
        return userManager;
    }

}
