package uk.ac.cam.cl.dtg.segue.api.userAlerts;


import org.eclipse.jetty.websocket.servlet.*;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueContextNotifier;

import javax.servlet.annotation.WebServlet;

/**
 * Created by du220 on 17/07/2017.
 */

@WebServlet(name = "UserAlertsWebSocketServlet", urlPatterns = { "/user-alerts/*" })
public class UserAlertsWebSocketServlet extends WebSocketServlet {

    @Override
    public void configure(WebSocketServletFactory factory) {

        factory.getPolicy().setIdleTimeout(60000);

        factory.setCreator((servletUpgradeRequest, servletUpgradeResponse) -> SegueContextNotifier.injector.getInstance(UserAlertsWebSocket.class));

    }
}
