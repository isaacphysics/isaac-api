package uk.ac.cam.cl.dtg.segue.api.userAlerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Striped;
import com.google.inject.Inject;
import javafx.util.Pair;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.IStatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidSessionException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlert;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlerts;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

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
    private final IStatisticsManager statisticsManager;
    private Session session;
    private static ObjectMapper objectMapper = new ObjectMapper();

    private static final int MAX_CONCURRENT_WEB_SOCKETS_PER_USER = 10;

    // Named unsafeConnectedSockets because, although non-aggregate operations on the concurrent hash map are fine,
    // operations on the user sets of websockets are unsafe unless used with the matching user lock.
    private static Map<Long, Set<UserAlertsWebSocket>> unsafeConnectedSockets = Maps.newConcurrentMap();
    private static final int MAX_NUMBER_OF_CONCURRENT_USER_TAB_OPERATIONS = 200;
    // If we move to supporting connections across multiple APIs, we could use postgres for a distributed lock.
    private static Striped<Lock> userLocks = Striped.lazyWeakLock(MAX_NUMBER_OF_CONCURRENT_USER_TAB_OPERATIONS);

    private static final Logger log = LoggerFactory.getLogger(UserAlertsWebSocket.class);

    /**
     * This static method obtains a user lock and sends an alert to each of that user's websockets.
     * @param userId ID of the user to send the messages, we do not check its validity here.
     * @param alert the alert to send to the user.
     */
    public static void notifyUserOfAlert(final long userId, final IUserAlert alert) {
        Lock userLock = userLocks.get(userId);
        userLock.lock();
        try {
            if (unsafeConnectedSockets.containsKey(userId)) {
                for (UserAlertsWebSocket listener : unsafeConnectedSockets.get(userId)) {
                    listener.sendAlert(alert);
                }
            }
        } finally {
            userLock.unlock();
        }
    }

    /**
     * Injectable constructor
     *
     * @param userManager
     *              - to get user information for the conencteds socket
     * @param userAlerts
     *              - to get/update persisted user alerts
     * @param logManager
     *              - so that we can log events for users.
     */
    @Inject
    public UserAlertsWebSocket(final UserAccountManager userManager,
                               final IUserAlerts userAlerts,
                               final ILogManager logManager,
                               final IStatisticsManager statisticsManager) {

        this.userManager = userManager;
        this.userAlerts = userAlerts;
        this.logManager = logManager;
        this.statisticsManager = statisticsManager;
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
            if (message.equals("heartbeat")) {
                session.getRemote().sendString(objectMapper.writeValueAsString(
                        ImmutableMap.of("heartbeat", System.currentTimeMillis())));
            } else if (message.equals("user-snapshot-nudge")) {
                sendUserSnapshotData();
            } else {
                session.close(StatusCode.POLICY_VIOLATION, "Invalid message!");
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

                long connectedUserId = connectedUser.getId();
                boolean addedSocket;
                boolean addedUser;
                int numberOfUserSockets;

                // We use boolean values to track state change so that logging is not done while holding the lock
                Lock userLock = userLocks.get(connectedUserId);
                userLock.lock();
                try {
                    Object nullIfNewUser = unsafeConnectedSockets.putIfAbsent(connectedUserId, Sets.newHashSet());
                    addedUser = null == nullIfNewUser;
                    Set<UserAlertsWebSocket> unsafeUsersSockets = unsafeConnectedSockets.get(connectedUserId);

                    addedSocket = unsafeUsersSockets.size() < MAX_CONCURRENT_WEB_SOCKETS_PER_USER;
                    if (addedSocket) {
                        unsafeUsersSockets.add(this);
                    }
                    numberOfUserSockets = unsafeUsersSockets.size();
                } finally {
                    userLock.unlock();
                }

                // Report on state change
                if (addedUser) {
                    log.debug("User " + connectedUserId + " started a new websocket session.");
                    SegueMetrics.CURRENT_WEBSOCKET_USERS.inc();
                }

                if (addedSocket) {
                    log.debug("User " + connectedUserId + " opened new websocket. Total open: " + numberOfUserSockets);
                    SegueMetrics.CURRENT_OPEN_WEBSOCKETS.inc();
                    SegueMetrics.WEBSOCKETS_OPENED.inc();
                } else {
                    log.debug("User " + connectedUserId
                            + " attempted to open too many simultaneous WebSockets; sending TRY_AGAIN_LATER.");
                    session.close(StatusCode.NORMAL, "TRY_AGAIN_LATER");
                    return;
                }

                // For now, we hijack this websocket class to deliver user streak information
                sendUserSnapshotData();
                // Send initial data
                sendInitialNotifications(connectedUserId);

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
        long connectedUserId = connectedUser.getId();
        boolean removeUser;
        int numberOfUserSockets;

        // We use boolean values to track state change so that logging is not done while holding the lock
        Lock userLock = userLocks.get(connectedUserId);
        userLock.lock();
        try {
            Set unsafeUsersSockets = unsafeConnectedSockets.get(connectedUserId);
            unsafeUsersSockets.remove(this);
            numberOfUserSockets = unsafeUsersSockets.size();
            removeUser = numberOfUserSockets == 0;
            if (removeUser) {
                unsafeConnectedSockets.remove(connectedUserId);
            }
        } finally {
            userLock.unlock();
        }

        // Report on state change
        SegueMetrics.CURRENT_OPEN_WEBSOCKETS.dec();
        SegueMetrics.WEBSOCKETS_CLOSED.inc();

        if (removeUser) {
            log.debug("User " + connectedUserId + " closed all of its open websockets");
            SegueMetrics.CURRENT_WEBSOCKET_USERS.dec();
        } else {
            log.debug("User " + connectedUserId + " closed a websocket. Total still open: " + numberOfUserSockets);
        }
    }

    /**
     * Sends a payload to the connected client notifying them of an important event
     *
     * @param alert
     *          - user alert instance containg details about the event
     */
    private void sendAlert(final IUserAlert alert) {
        try {
            this.session.getRemote().sendString(objectMapper.writeValueAsString(
                    ImmutableMap.of(
                            "notifications", ImmutableList.of(alert),
                            "heartbeat", System.currentTimeMillis()
                    )));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Method to send a payload to the connected user with details of their current stats snapshot
     * TODO: Currently only delivers user streak information but we can generalise it later to deliver more data
     *
     * @throws IOException
     *             - if the WebSocket is unexpectedly closed or in an invalid state
     */
    private void sendUserSnapshotData() throws IOException {

        session.getRemote().sendString(objectMapper.writeValueAsString(
                ImmutableMap.of(
                        "userSnapshot", statisticsManager.getDetailedUserStatistics(connectedUser),
                        "heartbeat", System.currentTimeMillis()
                )));
    }

    /**
     * Send any notifications or alerts registered in the database down this websocket
     * @param userId the Id of the user's alerts which will be sent.
     * @throws SegueDatabaseException can be thrown while getting the user's alerts from the database.
     * @throws IOException can be thrown when sending the notifications down the connection.
     */
    private void sendInitialNotifications(final long userId) throws SegueDatabaseException, IOException {
        // TODO: Send initial set of notifications.
        List<IUserAlert> persistedAlerts = userAlerts.getUserAlerts(userId);
        if (!persistedAlerts.isEmpty()) {
            session.getRemote().sendString(
                    objectMapper.writeValueAsString(ImmutableMap.of("notifications", persistedAlerts)));
        }
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
        Map<String, String> sessionInformation = objectMapper.readValue(segueAuthCookie.getValue(), HashMap.class);

        return sessionInformation;

    }

}
