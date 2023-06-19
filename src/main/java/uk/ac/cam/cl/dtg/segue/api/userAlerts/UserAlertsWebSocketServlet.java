package uk.ac.cam.cl.dtg.segue.api.userAlerts;

import com.google.inject.Injector;
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueContextNotifier;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.util.RequestIPExtractor.getClientIpAddr;

/**
 * Created by du220 on 17/07/2017.
 */

@WebServlet(name = "UserAlertsWebSocketServlet", urlPatterns = { "/api/user-alerts/*" })
public class UserAlertsWebSocketServlet extends JettyWebSocketServlet {

    private static final Logger log = LoggerFactory.getLogger(UserAlertsWebSocketServlet.class);
    private static final int BAD_REQUEST = 400;
    private static final int FORBIDDEN = 403;
    private static final Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
    private final String hostName = injector.getInstance(AbstractConfigLoader.class).getProperty(HOST_NAME);

    @Override
    public void configure(JettyWebSocketServletFactory factory) {

        factory.setCreator((servletUpgradeRequest, servletUpgradeResponse) -> SegueContextNotifier.injector.getInstance(UserAlertsWebSocket.class));

    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // We have been seeing malformed WebSocket requests. Add some debug logging to these:
        if (!"websocket".equalsIgnoreCase(request.getHeader("Upgrade"))) {
            log.debug(String.format("WebSocket Upgrade request from %s has incorrect header 'Upgrade: %s', headers: %s, 'Via: %s'.",
                    getClientIpAddr(request), request.getHeader("Upgrade"), Collections.list(request.getHeaderNames()).toString(),
                    request.getHeader("Via")));
        }
        if (null == request.getHeader("Sec-WebSocket-Key")) {
            log.warn(String.format("WebSocket Upgrade request from %s has missing 'Sec-WebSocket-Key' header."
                    + " 'Sec-WebSocket-Extensions: %s', 'Sec-WebSocket-Version: %s', 'User-Agent: %s'",
                    getClientIpAddr(request), request.getHeader("Sec-WebSocket-Extensions"),
                    request.getHeader("Sec-WebSocket-Version"), request.getHeader("User-Agent")));
            response.setStatus(BAD_REQUEST);
            return;
        }

        // WebSockets are not protected by CORS, so we must check the origin explicitly here:
        String origin = request.getHeader("Origin");
        if (!hostName.contains("localhost") && (null == origin || !origin.equals("https://" + hostName))) {
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
