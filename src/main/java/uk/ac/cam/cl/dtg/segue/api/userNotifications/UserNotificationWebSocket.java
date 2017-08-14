package uk.ac.cam.cl.dtg.segue.api.userNotifications;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;


/**
 * Created by du220 on 17/07/2017.
 */

@WebSocket
public class UserNotificationWebSocket {

    private KafkaStreams streams;
    private ObjectMapper objectMapper;
    private ConsumerLoop loop;

    public UserNotificationWebSocket(KafkaStreams streams) {

        System.out.println("new websocket");
        this.streams = streams;
        this.objectMapper = new ObjectMapper();
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) throws IOException {
        System.out.println("Message received:" + message);
        /*if (session.isOpen()) {
            String response = message.toUpperCase();
            //session.getRemote().sendString(response);
        }*/
    }


    @OnWebSocketConnect
    public void onConnect(final Session session) throws IOException {

        String requestUri = session.getUpgradeRequest().getRequestURI().toString();
        String userId = requestUri.substring(requestUri.indexOf("user-notifications/") + 19);

        // first we query the kafka streams local user notification store to get any offline notifications
        ReadOnlyKeyValueStore<String, JsonNode> userNotifications = streams.store("store_user_notifications",
                QueryableStoreTypes.<String, JsonNode>keyValueStore());

        // send offline backlog to user
        session.getRemote().sendString(objectMapper.writeValueAsString(userNotifications.get(userId)));

        // then we set up a kafka consumer to listen for new notifications while the user remains connected
        loop = new ConsumerLoop(session, userId, "topic_user_notifications", objectMapper);
        loop.run();

    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        loop.shutdown();
    }

}
