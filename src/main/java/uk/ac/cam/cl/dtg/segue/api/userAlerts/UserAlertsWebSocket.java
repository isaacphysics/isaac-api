package uk.ac.cam.cl.dtg.segue.api.userAlerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidSessionException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.UserStatisticsStreamsApplication;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlert;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlerts;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Websocket class for 2-way communication channel between front-end client and API
 * for delivering real-time data
 *
 * @author Dan Underwood
 */
@WebSocket
public class UserAlertsWebSocket implements IAlertListener {
    private UserAccountManager userManager;
    private RegisteredUserDTO connectedUser;
    private final IUserAlerts userAlerts;
    private final ILogManager logManager;
    private final UserStatisticsStreamsApplication userStatisticsStreamsApplication;
    private Session session;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static ConcurrentHashMap<Long, ConcurrentLinkedQueue<UserAlertsWebSocket>> connectedSockets = new ConcurrentHashMap<>();
    private static Long websocketsOpened = 0L;
    private static Long websocketsClosed = 0L;

    private static final Logger log = LoggerFactory.getLogger(UserAlertsWebSocket.class);
    private static final int MaxConcurrentWebSocketsPerUser = 10;

    /**
     * Injectable constructor
     *
     * @param userManager
     *              - to get user information for the conencteds socket
     * @param userAlerts
     *              - to get/update persisted user alerts
     * @param logManager
     *              - so that we can log events for users.
     * @param userStatisticsStreamsApplication
     *              - to enable querying of user stat stream process state stores
     */
    @Inject
    public UserAlertsWebSocket(final UserAccountManager userManager,
                               final IUserAlerts userAlerts,
                               final ILogManager logManager,
                               final UserStatisticsStreamsApplication userStatisticsStreamsApplication) {

        this.userManager = userManager;
        this.userAlerts = userAlerts;
        this.logManager = logManager;
        this.userStatisticsStreamsApplication = userStatisticsStreamsApplication;
    }


    /**
     * Handles incoming messages from a connected client.
     *
     * @param session
     *          - contains information about the currently connected session
     * @param message
     *          - text-based message from client
     */
    @OnWebSocketMessage
    public void onText(final Session session, final String message) {
        try {
            if (message.equals("user-snapshot-nudge")) {
                sendUserSnapshotData();
            }
        } catch (IOException e) {
            log.warn("WebSocket connection failed! " + e.getClass().getSimpleName() + ": " + e.getMessage());
            session.close(StatusCode.SERVER_ERROR, "onText IOException");
        }
    }


    /**
     * Handles a new client websocket connection
     *
     * @param session
     *          - contains information about the session to be started
     */
    @OnWebSocketConnect
    public void onConnect(final Session session) {
        try {
            this.session = session;

            Map<String, String> sessionInformation = getSessionInformation(session);

            if (userManager.isValidUserFromSession(sessionInformation)) {

                connectedUser = userManager.getUserDTOById(Long.parseLong(sessionInformation.get(SESSION_USER_ID)));

                // Do not let one user open too many WebSockets:
                if (connectedSockets.containsKey(connectedUser.getId())
                        && (connectedSockets.get(connectedUser.getId()).size() >= MaxConcurrentWebSocketsPerUser)) {
                    log.warn("User attempted to open too many simultaneous WebSockets; sending TRY_AGAIN_LATER.");
                    session.close(StatusCode.NORMAL, "TRY_AGAIN_LATER");
                    return;
                }

                connectedSockets.putIfAbsent(connectedUser.getId(), new ConcurrentLinkedQueue<>());

                connectedSockets.get(connectedUser.getId()).add(this);
                log.debug("User " + connectedUser.getId() + " opened new websocket. Total open: " + connectedSockets.get(connectedUser.getId()).size());

                // For now, we hijack this websocket class to deliver user streak information
                sendUserSnapshotData();

                // TODO: Send initial set of notifications.
                List<IUserAlert> persistedAlerts = userAlerts.getUserAlerts(connectedUser.getId());
                if (!persistedAlerts.isEmpty()) {
                    session.getRemote().sendString(objectMapper.writeValueAsString(ImmutableMap.of("notifications", persistedAlerts)));
                }

                websocketsOpened++;
            } else {
                log.debug("WebSocket connection failed! Expired or invalid session.");
                session.close(StatusCode.POLICY_VIOLATION, "Expired or invalid session!");
            }

        } catch (IOException e) {
            log.warn("WebSocket connection failed! " + e.getClass().getSimpleName() + ": " + e.getMessage());
            session.close(StatusCode.SERVER_ERROR, "onConnect IOException");
        } catch (InvalidSessionException | NoUserException e) {
            log.debug("WebSocket connection failed! " + e.getClass().getSimpleName() + ": " + e.getMessage());
            session.close(StatusCode.POLICY_VIOLATION, e.getClass().getSimpleName());
        } catch (SegueDatabaseException e) {
            log.warn("WebSocket connection failed! " + e.getClass().getSimpleName() + ": " + e.getMessage());
            session.close(StatusCode.SERVER_ERROR, "onConnect Database Error");
        }


    }


    /**
     * Handles the closing of the websocket conenction
     *
     * @param session
     **          - contains information on the currently connected session
     * @param status
     *          - the status of the connection
     * @param reason
     *          - states the cause for closing the connection
     */
    @OnWebSocketClose
    public void onClose(final Session session, final int status, final String reason) {
        connectedSockets.get(connectedUser.getId()).remove(this);
        log.debug("User " + connectedUser.getId() + " closed a websocket. Total still open: " + connectedSockets.get(connectedUser.getId()).size());

        // if the user has no websocket conenctions open, remove them from the map
        /*synchronized (connectedSockets) {
            if (connectedSockets.containsKey(connectedUser.getId()) && connectedSockets.get(connectedUser.getId()).isEmpty()) {
                connectedSockets.remove(connectedUser.getId(), connectedSockets.get(connectedUser.getId()));
            }
        }

        if (connectedSockets.containsKey(connectedUser.getId()) && connectedSockets.get(connectedUser.getId()).isEmpty()) {
            log.info("User " + connectedUser.getId() + " has no websocket connections but still contains entry in hashmap!");
        }*/

        websocketsClosed++;
    }


    /**
     * Sends a payload to the connected client notifying them of an important event
     *
     * @param alert
     *          - user alert instance containg details about the event
     */
    @Override
    public void notifyAlert(final IUserAlert alert) {
        try {
            this.session.getRemote().sendString(objectMapper.writeValueAsString(ImmutableMap.of("notifications", ImmutableList.of(alert))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Method to send a payload to the connected user with details of their current stats snapshot
     * TODO: Currently only delivers user streak information but we can generalise it later to deliver more data
     *
     * @throws IOException
     */
    private void sendUserSnapshotData() throws IOException {

        session.getRemote().sendString(objectMapper.writeValueAsString(ImmutableMap.of("userSnapshot",
                userStatisticsStreamsApplication.getUserSnapshot(connectedUser))));
    }



    /**
     * Extracts the segue session information from the given session.
     *
     * @param session
     *            - possibly containing a segue cookie.
     * @return The segue session information (unchecked or validated)
     * @throws IOException
     *             - problem parsing session information.
     * @throws InvalidSessionException
     *             - if there is no session set or if it is not valid.
     */
    private Map<String, String> getSessionInformation(final Session session) throws IOException, InvalidSessionException {

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

        if (segueAuthCookie == null) {
            throw new InvalidSessionException("There is no Segue authorisation cookie set.");
        }

        @SuppressWarnings("unchecked")
        Map<String, String> sessionInformation = objectMapper.readValue(segueAuthCookie.getValue(),
                HashMap.class);

        return sessionInformation;

    }

    public static Map<String, Long> getWebsocketCounts() {
        return ImmutableMap.of("numWebsocketsOpenedOverTime", websocketsOpened, "numWebsocketsClosedOverTime", websocketsClosed);

    }

}
