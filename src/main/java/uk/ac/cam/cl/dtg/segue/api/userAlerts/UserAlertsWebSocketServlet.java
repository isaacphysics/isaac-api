package uk.ac.cam.cl.dtg.segue.api.userAlerts;


import com.google.inject.Guice;
import com.google.inject.Injector;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueContextNotifier;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.util.RequestIPExtractor.getClientIpAddr;

/**
 * Created by du220 on 17/07/2017.
 */

@WebServlet(name = "UserAlertsWebSocketServlet", urlPatterns = { "/api/user-alerts/*" })
public class UserAlertsWebSocketServlet extends WebSocketServlet {

    private static final Logger log = LoggerFactory.getLogger(UserAlertsWebSocketServlet.class);
    private static final int FORBIDDEN = 403;
    private static Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
    private final String hostName = injector.getInstance(PropertiesLoader.class).getProperty(HOST_NAME);

    @Override
    public void configure(WebSocketServletFactory factory) {

        factory.setCreator((servletUpgradeRequest, servletUpgradeResponse) -> SegueContextNotifier.injector.getInstance(UserAlertsWebSocket.class));

    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // We have been seeing malformed WebSocket requests. Add some debug logging to these:
        if (!"websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
            log.warn(String.format("WebSocket Upgrade request from %s has incorrect header 'Upgrade: %s', headers: %s.",
                    getClientIpAddr(request), request.getHeader("Upgrade"), Collections.list(request.getHeaderNames()).toString()));
        }
        if (null == request.getHeader("Sec-WebSocket-Key")) {
            log.warn(String.format("WebSocket Upgrade request from %s has missing 'Sec-WebSocket-Key' header."
                    + " 'Sec-WebSocket-Extensions: %s', 'Sec-WebSocket-Version: %s', 'User-Agent: %s'",
                    getClientIpAddr(request), request.getHeader("Sec-WebSocket-Extensions"),
                    request.getHeader("Sec-WebSocket-Version"), request.getHeader("User-Agent")));
            response.setStatus(400);
            return;
        }

        // WebSockets are not protected by the CORS filters in /override-api-web.xml so we must check the origin
        // explicitly here:
        String origin = request.getHeader("Origin");
        if (null == origin || !(origin.equals("https://" + hostName) || hostName.contains("localhost"))) {
            // If we have no origin, or an origin not matching the current hostname; abort the Upgrade request with
            // a HTTP Forbidden. Allow an API running on localhost to bypass these origin checks.
            log.warn("WebSocket Upgrade request has unexpected Origin: '" + origin + "'. Blocking access to: "
                     + request.getServletPath());
            response.setStatus(FORBIDDEN);
            return;
        }
        super.service(request, response);
    }
}
