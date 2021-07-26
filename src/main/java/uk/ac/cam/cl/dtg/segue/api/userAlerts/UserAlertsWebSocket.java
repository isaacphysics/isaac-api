package uk.ac.cam.cl.dtg.segue.api.userAlerts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Striped;
import com.google.inject.Inject;
import io.prometheus.client.Histogram;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.IStatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlert;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlerts;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import static uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics.WEBSOCKET_LATENCY_HISTOGRAM;

/**
 * Websocket class for 2-way communication channel between front-end client and API
 * for delivering real-time data
 *
 * @author Dan Underwood
 */
@WebSocket
public class UserAlertsWebSocket implements IAlertListener {
    private static class Protocol {
        static final String HEARTBEAT = "heartbeat";
        static final String USER_SNAPSHOT_NUDGE = "user-snapshot-nudge";
        static final String NOTIFICATIONS = "notifications";
        static final String USER_SNAPSHOT = "userSnapshot";

        static final Set<String> ACCEPTED_MESSAGES = Sets.newHashSet(HEARTBEAT, USER_SNAPSHOT_NUDGE);
    }

    private UserAccountManager userManager;
    private UserAuthenticationManager userAuthenticationManager;
    private RegisteredUserDTO connectedUser;
    private final IUserAlerts userAlerts;
    private final ILogManager logManager;
    private final IStatisticsManager statisticsManager;
    private final PropertiesLoader properties;
    private Session session;
    private static ObjectMapper objectMapper = new ObjectMapper();

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
                               final UserAuthenticationManager userAuthenticationManager,
                               final IUserAlerts userAlerts,
                               final ILogManager logManager,
                               final IStatisticsManager statisticsManager,
                               final PropertiesLoader properties) {

        this.userManager = userManager;
        this.userAuthenticationManager = userAuthenticationManager;
        this.userAlerts = userAlerts;
        this.logManager = logManager;
        this.statisticsManager = statisticsManager;
        this.properties = properties;
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
        Histogram.Timer latencyTimer = null;
        if (Protocol.ACCEPTED_MESSAGES.contains(message)) {
            latencyTimer = WEBSOCKET_LATENCY_HISTOGRAM.labels(message).startTimer();
        }

        try {
            if (message.equals(Protocol.HEARTBEAT)) {
                session.getRemote().sendString(objectMapper.writeValueAsString(ImmutableMap.of(
                        Protocol.HEARTBEAT, System.currentTimeMillis()
                )));
            } else if (message.equals(Protocol.USER_SNAPSHOT_NUDGE)) {
                sendUserSnapshotData();
            } else {
                session.close(StatusCode.POLICY_VIOLATION, "Invalid message!");
            }
        } catch (IOException e) {
            log.warn("WebSocket connection failed! " + e.getClass().getSimpleName() + ": " + e.getMessage());
            session.close(StatusCode.SERVER_ERROR, "onText IOException");
        } finally {
            if (latencyTimer != null) {
                latencyTimer.observeDuration();
            }
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

            RegisteredUser validUserFromSession = userAuthenticationManager.getUserFromSession(session.getUpgradeRequest());

            if (null != validUserFromSession) {

                connectedUser = userManager.getUserDTOById(validUserFromSession.getId());

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

                    addedSocket = unsafeUsersSockets.size() <= Integer.parseInt(
                            this.properties.getProperty(Constants.MAX_CONCURRENT_WEB_SOCKETS_PER_USER));
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

                // A websocket must be created for this object to be instantiated - we close it early if they have too many
                SegueMetrics.CURRENT_OPEN_WEBSOCKETS.inc();
                if (addedSocket) {
                    log.debug("User " + connectedUserId + " opened new websocket. Total open: " + numberOfUserSockets);
                    SegueMetrics.WEBSOCKETS_OPENED_SUCCESSFULLY.inc();
                } else {
                    log.debug("User " + connectedUserId
                            + " attempted to open too many simultaneous WebSockets; sending TRY_AGAIN_LATER.");
                    session.close(StatusCode.NORMAL, "TRY_AGAIN_LATER");
                    return;
                }

                Histogram.Timer latencyTimer = WEBSOCKET_LATENCY_HISTOGRAM.labels("initial_data").startTimer();
                try {
                    // For now, we hijack this websocket class to deliver user streak information
                    sendUserSnapshotData();
                    // Send initial data
                    sendInitialNotifications(connectedUserId);
                } finally {
                    latencyTimer.observeDuration();
                }
            } else {
                log.debug("WebSocket connection failed! Expired or invalid session.");
                session.close(StatusCode.POLICY_VIOLATION, "Expired or invalid session!");
            }

        } catch (IOException e) {
            log.warn("WebSocket connection failed! " + e.getClass().getSimpleName() + ": " + e.getMessage());
            session.close(StatusCode.SERVER_ERROR, "onConnect IOException");
        } catch (NoUserException e) {
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
     * Handles errors in WebSocket connections.
     *
     * TODO: should this also deal with the closing part?
     * This for now is just to remove an error that Jetty outputs if no handler is configured.
     *
     * @param session
     *         - contains information on the currently connected session, if any
     * @param error
     *         - the error raised
     */
    @OnWebSocketError
    public void onError(final Session session, final Throwable error) {
        long connectedUserId = connectedUser.getId();
        log.warn(String.format("Error in WebSocket for user (%s): %s", connectedUserId, error));
    }

    /**
     * Sends a payload to the connected client notifying them of an important event
     *
     * @param alert
     *          - user alert instance containg details about the event
     */
    private void sendAlert(final IUserAlert alert) {
        try {
            this.session.getRemote().sendString(objectMapper.writeValueAsString(ImmutableMap.of(
                    Protocol.NOTIFICATIONS, ImmutableList.of(alert),
                    Protocol.HEARTBEAT, System.currentTimeMillis()
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
        session.getRemote().sendString(objectMapper.writeValueAsString(ImmutableMap.of(
                Protocol.USER_SNAPSHOT, statisticsManager.getDetailedUserStatistics(connectedUser),
                Protocol.HEARTBEAT, System.currentTimeMillis()
        )));
    }

    /**
     * Send any notifications or alerts registered in the database down this websocket
     * @param userId the Id of the user's alerts which will be sent.
     * @throws SegueDatabaseException can be thrown while getting the user's alerts from the database.
     * @throws IOException can be thrown when sending the notifications down the connection.
     */
    private void sendInitialNotifications(final long userId) throws SegueDatabaseException, IOException {
        List<IUserAlert> persistedAlerts = userAlerts.getUserAlerts(userId);
        if (!persistedAlerts.isEmpty()) {
            session.getRemote().sendString(objectMapper.writeValueAsString(ImmutableMap.of(
                    Protocol.NOTIFICATIONS, persistedAlerts
            )));
        }
    }
}
