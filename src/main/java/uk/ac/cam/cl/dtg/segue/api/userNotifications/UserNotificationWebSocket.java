package uk.ac.cam.cl.dtg.segue.api.userNotifications;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;

import java.io.IOException;


/**
 * Created by du220 on 17/07/2017.
 */

@WebSocket
public class UserNotificationWebSocket {

    private KafkaStreams streams;
    private ILogManager logManager;
    private ObjectMapper objectMapper;
    private ConsumerLoop consumerLoop;
    private Thread thread;

    private UserAccountManager userManager;
    private AbstractSegueUserDTO connectedUser;

    public UserNotificationWebSocket(final KafkaStreams streams,
                                     final ILogManager logManager,
                                     final UserAccountManager userManager) {

        System.out.println("new websocket");
        this.streams = streams;
        this.logManager = logManager;
        this.objectMapper = new ObjectMapper();
        this.userManager = userManager;
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) throws IOException {

        // if the user has viewed their notifications, log this event.
        // this will also cascade down through kafka streams and reduce the size of notification state store record for this user.
        if (message.equals("VIEW_NOTIFICATIONS")) {
            logManager.logInternalEvent(connectedUser, "VIEW_NOTIFICATIONS", "VIEW_NOTIFICATIONS");
        }

    }


    @OnWebSocketConnect
    public void onConnect(final Session session) throws IOException, SegueDatabaseException, NoUserException {

        String requestUri = session.getUpgradeRequest().getRequestURI().toString();
        String userId = requestUri.substring(requestUri.indexOf("user-notifications/") + 19);

        Long uId = Long.parseLong(userId);
        connectedUser = userManager.getUserDTOById(uId);

        // add security checking!


        // first we query the kafka streams local user notification store to get any offline notifications
        ReadOnlyKeyValueStore<String, JsonNode> userNotifications = streams.store("store_user_notifications",
                QueryableStoreTypes.<String, JsonNode>keyValueStore());

        // send offline backlog to user
        session.getRemote().sendString(objectMapper.writeValueAsString(userNotifications.get(userId)));

        // then we set up a kafka consumer to listen for new notifications while the user remains connected
        consumerLoop = new ConsumerLoop(session, userId, "topic_user_notifications", objectMapper);
        thread = new Thread(consumerLoop);
        thread.start();

    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        if (thread != null) {
            consumerLoop.shutdown();
        }
    }

}
