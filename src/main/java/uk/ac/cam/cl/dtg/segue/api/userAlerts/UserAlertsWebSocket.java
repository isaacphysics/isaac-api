package uk.ac.cam.cl.dtg.segue.api.userAlerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
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

    public static ConcurrentHashMap<Long, List<UserAlertsWebSocket>> connectedSockets = new ConcurrentHashMap<>();


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
     * @throws IOException
     * @throws SegueDatabaseException
     */
    @OnWebSocketMessage
    public void onText(Session session, String message) throws IOException, SegueDatabaseException {

        if (message.equals("user-snapshot-nudge")) {
            sendUserSnapshotData();
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

                if (!connectedSockets.containsKey(connectedUser.getId())) {
                    connectedSockets.put(connectedUser.getId(), new LinkedList<>());
                }
                connectedSockets.get(connectedUser.getId()).add(this);

                // For now, we hijack this websocket class to deliver user streak information
                sendUserSnapshotData();

                // TODO: Send initial set of notifications.
                List<IUserAlert> persistedAlerts = userAlerts.getUserAlerts(connectedUser.getId());
                if (!persistedAlerts.isEmpty()) {
                    session.getRemote().sendString(objectMapper.writeValueAsString(ImmutableMap.of("notifications", persistedAlerts)));
                }

            }

        } catch (InvalidSessionException | NoUserException | SegueDatabaseException | IOException e) {
            e.printStackTrace();
            session.close();
        }
    }

    @OnWebSocketError
    public void onError(Session session, Throwable cause) {
        cause.printStackTrace();
        session.close();
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
    public void onClose(Session session, int status, String reason) throws IOException {

        if (connectedUser != null) {

            connectedSockets.get(connectedUser.getId()).remove(this);

            // if the user has no websocket conenctions open, remove them from the map
            connectedSockets.remove(connectedUser.getId(), Lists.newArrayList());

        }
    }


    /**
     * Sends a payload to the connected client notifying them of an important event
     *
     * @param alert
     *          - user alert instance containg details about the event
     */
    @Override
    public void notifyAlert(IUserAlert alert) {
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
    private Map<String, String> getSessionInformation(Session session) throws IOException, InvalidSessionException {

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
        Map<String, String> sessionInformation = this.objectMapper.readValue(segueAuthCookie.getValue(),
                HashMap.class);

        return sessionInformation;

    }

}
