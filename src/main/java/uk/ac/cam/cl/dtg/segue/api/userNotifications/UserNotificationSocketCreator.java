package uk.ac.cam.cl.dtg.segue.api.userNotifications;

import org.apache.kafka.streams.KafkaStreams;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;

/**
 * Created by du220 on 18/07/2017.
 */
public class UserNotificationSocketCreator implements WebSocketCreator {

    private KafkaStreams streams;
    private ILogManager logManager;
    private UserAccountManager userManager;

    public UserNotificationSocketCreator(final KafkaStreams streams,
                                         final ILogManager logManager,
                                         final UserAccountManager userManager) {
        this.streams = streams;
        this.logManager = logManager;
        this.userManager =  userManager;
    }

    @Override
    public Object createWebSocket(final ServletUpgradeRequest servletUpgradeRequest, final ServletUpgradeResponse servletUpgradeResponse) {
        return new UserNotificationWebSocket(streams, logManager, userManager);
    }
}
