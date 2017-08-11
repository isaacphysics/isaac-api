package uk.ac.cam.cl.dtg.segue.api.userNotifications;

import org.apache.kafka.streams.KafkaStreams;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;

/**
 * Created by du220 on 18/07/2017.
 */
public class UserNotificationSocketCreator implements WebSocketCreator {

    private KafkaStreams streams;

    public UserNotificationSocketCreator(KafkaStreams streams) {
        this.streams = streams;
    }

    @Override
    public Object createWebSocket(ServletUpgradeRequest servletUpgradeRequest, ServletUpgradeResponse servletUpgradeResponse) {
        return new UserNotificationWebSocket(streams);
    }
}
