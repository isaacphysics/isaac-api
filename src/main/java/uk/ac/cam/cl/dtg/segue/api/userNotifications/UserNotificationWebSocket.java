package uk.ac.cam.cl.dtg.segue.api.userNotifications;


import com.fasterxml.jackson.databind.JsonNode;
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

    public UserNotificationWebSocket(KafkaStreams streams) {

        System.out.println("new websocket");
        this.streams = streams;
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

        System.out.println(session.getRemoteAddress().getHostString() + " connected!");

        ReadOnlyKeyValueStore<String, JsonNode> store = streams.store("userStore",
                QueryableStoreTypes.<String, JsonNode>keyValueStore());

        KeyValueIterator<String, JsonNode> it = store.all();

        Integer i = 0;
        while (it.hasNext()) {
            //System.out.println(it.next());
            i++;
        }

        System.out.print("userStore: " + i);

    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        System.out.println(session.getRemoteAddress().getHostString() + " closed!");
    }

}
