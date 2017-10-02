package uk.ac.cam.cl.dtg.segue.api.userAlerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidSessionException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlert;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlerts;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_AUTH_COOKIE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_USER_ID;

@WebSocket
public class UserAlertsWebSocket implements IAlertListener {
    private ObjectMapper objectMapper;
    private UserAccountManager userManager;
    private RegisteredUserDTO connectedUser;
    private final IUserAlerts userAlerts;
    private Session session;

    public static HashMap<Long,UserAlertsWebSocket> connectedSockets = new HashMap<>();

    @Inject
    public UserAlertsWebSocket(final ObjectMapper objectMapper,
                               final UserAccountManager userManager,
                               final IUserAlerts userAlerts) {

        this.userManager = userManager;
        this.objectMapper = objectMapper;
        this.userAlerts = userAlerts;
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) throws IOException {

        System.out.println("WS TEXT: " + message);
    }


    @OnWebSocketConnect
    public void onConnect(final Session session) throws IOException, SegueDatabaseException, NoUserException, InvalidSessionException {
        this.session = session;

        String requestUri = session.getUpgradeRequest().getRequestURI().toString();

        HttpCookie segueAuthCookie = null;
        if (session.getUpgradeRequest().getCookies() == null) {
            throw new InvalidSessionException("There are no cookies set.");
        }

        List<HttpCookie> cookies = session.getUpgradeRequest().getCookies();

        for (HttpCookie c : cookies) {
            if (c.getName().equals(SEGUE_AUTH_COOKIE)) {
                segueAuthCookie = c;
            }
        }

        Map<String, String> sessionInformation = objectMapper.readValue(segueAuthCookie.getValue(),
                HashMap.class);

        if (userManager.isValidUserFromSession(sessionInformation)) {

            String userId = sessionInformation.get(SESSION_USER_ID);

            Long uId = Long.parseLong(userId);
            connectedUser = userManager.getUserDTOById(uId);

            connectedSockets.put(connectedUser.getId(), this);

            session.getRemote().sendString(objectMapper.writeValueAsString(ImmutableMap.of("notifications", userAlerts.getUserAlerts(connectedUser.getId()))));

            // TODO: Send initial set of notifications.
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        connectedSockets.remove(connectedUser.getId());
    }

    @Override
    public void notifyAlert(IUserAlert alert) {
        try {
            this.session.getRemote().sendString(objectMapper.writeValueAsString(ImmutableMap.of("notifications", ImmutableList.of(alert))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
