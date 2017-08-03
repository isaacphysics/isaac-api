package uk.ac.cam.cl.dtg.segue.api.userNotifications;


import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.annotation.WebServlet;

/**
 * Created by du220 on 17/07/2017.
 */

@WebServlet(name = "WebSocketServlet", urlPatterns = { "/user-notifications" })
public class UserNotificationWebSocketServlet extends WebSocketServlet {

    @Override
    public void configure(WebSocketServletFactory webSocketServletFactory) {

        webSocketServletFactory.register(UserNotificationWebSocket.class);

        webSocketServletFactory.getPolicy().setIdleTimeout(5000);
        //webSocketServletFactory.setCreator(new UserNotificationSocketCreator(KafkaStreamsProvider.getInstance().getStream()));

    }
}
