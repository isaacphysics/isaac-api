package uk.ac.cam.cl.dtg.segue.api.userAlerts;

import com.fasterxml.jackson.databind.JsonNode;
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
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlert;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlerts;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.*;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

@WebSocket
public class UserAlertsWebSocket implements IAlertListener {
    private ObjectMapper objectMapper;
    private UserAccountManager userManager;
    private RegisteredUserDTO connectedUser;
    private final IUserAlerts userAlerts;
    private final ILogManager logManager;
    private Session session;

    public static HashMap<Long, List<UserAlertsWebSocket>> connectedSockets = new HashMap<>();

    @Inject
    public UserAlertsWebSocket(final ObjectMapper objectMapper,
                               final UserAccountManager userManager,
                               final IUserAlerts userAlerts,
                               final ILogManager logManager) {

        this.userManager = userManager;
        this.objectMapper = objectMapper;
        this.userAlerts = userAlerts;
        this.logManager = logManager;
    }

    @OnWebSocketMessage
    public void onText(Session session, String message) throws IOException, SegueDatabaseException {

        JsonNode alertFeedback = objectMapper.readTree(message);
        String feedbackType = alertFeedback.path("feedbackType").asText();

        // TODO: right now these log internal events, meaning we get no info from a HTTPRequest object

        if (feedbackType.equals(NOTIFICATION_VIEW_LIST)) {

            Iterator<JsonNode> iter = alertFeedback.path("notificationIds").elements();

            while (iter.hasNext())
                userAlerts.recordAlertEvent(iter.next().asLong(), IUserAlert.AlertEvents.SEEN);


            Map<String, Object> eventDetails = new ImmutableMap.Builder<String, Object>()
                    .put("notification_ids", alertFeedback.path("notificationIds")).build();


            logManager.logInternalEvent(connectedUser, NOTIFICATION_VIEW_LIST, eventDetails);

        } else if (feedbackType.equals(IUserAlert.AlertEvents.CLICKED.name())) {

            userAlerts.recordAlertEvent(alertFeedback.path("notificationId").asLong(), IUserAlert.AlertEvents.CLICKED);

            Map<String, Object> eventDetails = new ImmutableMap.Builder<String, Object>()
                    .put("notification_id", alertFeedback.path("notificationId").asLong()).build();

            logManager.logInternalEvent(connectedUser, NOTIFICATION_CLICK, eventDetails);

        } else if (feedbackType.equals(IUserAlert.AlertEvents.DISMISSED.name())) {

            userAlerts.recordAlertEvent(alertFeedback.path("notificationId").asLong(), IUserAlert.AlertEvents.DISMISSED);

            Map<String, Object> eventDetails = new ImmutableMap.Builder<String, Object>()
                    .put("notification_id", alertFeedback.path("notificationId").asLong()).build();

            logManager.logInternalEvent(connectedUser, NOTIFICATION_DISMISS, eventDetails);

        }
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

            if (!connectedSockets.containsKey(connectedUser.getId()))
                connectedSockets.put(connectedUser.getId(), new LinkedList<>());
            connectedSockets.get(connectedUser.getId()).add(this);

            session.getRemote().sendString(objectMapper.writeValueAsString(ImmutableMap.of("notifications", userAlerts.getUserAlerts(connectedUser.getId()))));

            // TODO: Send initial set of notifications.
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int status, String reason) {
        connectedSockets.get(connectedUser.getId()).remove(this);
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
