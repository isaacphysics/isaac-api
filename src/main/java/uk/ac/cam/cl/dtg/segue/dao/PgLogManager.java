/*
 * Copyright 2015 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.LogEvent;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.Constants.*;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.util.RequestIPExtractor;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics.LOG_EVENT;

/**
 * @author sac92
 *
 */
public class PgLogManager implements ILogManager {
    private static final Logger log = LoggerFactory.getLogger(PgLogManager.class);

    private final PostgresSqlDb database;
    private final LocationManager locationManager;
    private final boolean loggingEnabled;
    private final ObjectMapper objectMapper;

    /**
     * PgLogManager.
     * 
     * @param database
     *            client for postgres.
     * @param objectMapper
     *            - so we can map event details to and from json
     * @param loggingEnabled
     *            - whether the log event should be persisted or not?
     * @param locationManager
     *            - Helps identify a rough location for an ip address.
     */
    @Inject
    public PgLogManager(final PostgresSqlDb database, final ObjectMapper objectMapper,
            @Named(Constants.LOGGING_ENABLED) final boolean loggingEnabled,
            final LocationManager locationManager) {

        this.database = database;
        this.objectMapper = objectMapper;
        this.loggingEnabled = loggingEnabled;
        this.locationManager = locationManager;
    }

    @Override
    public void logEvent(final AbstractSegueUserDTO user, final HttpServletRequest httpRequest, final LogType eventType,
                         final Object eventDetails) {
        Validate.notNull(user);
        try {
            if (user instanceof RegisteredUserDTO) {
                this.persistLogEvent(((RegisteredUserDTO) user).getId().toString(), null, eventType.name(), eventDetails,
                        RequestIPExtractor.getClientIpAddr(httpRequest));
            } else {
                this.persistLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType.name(), eventDetails,
                        RequestIPExtractor.getClientIpAddr(httpRequest));
            }

        } catch (JsonProcessingException e) {
            log.error("Unable to serialize eventDetails as json string", e);
        } catch (SegueDatabaseException e) {
            log.error("Unable to save log event to the database", e);
        }
    }

    @Override
    public void logExternalEvent(final AbstractSegueUserDTO user, final HttpServletRequest httpRequest,
                         final String eventType, final Object eventDetails) {
        Validate.notNull(user);
        try {
            if (user instanceof RegisteredUserDTO) {
                this.persistLogEvent(((RegisteredUserDTO) user).getId().toString(), null, eventType, eventDetails,
                        RequestIPExtractor.getClientIpAddr(httpRequest));
            } else {
                this.persistLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType, eventDetails,
                        RequestIPExtractor.getClientIpAddr(httpRequest));
            }

        } catch (JsonProcessingException e) {
            log.error("Unable to serialize eventDetails as json string", e);
        } catch (SegueDatabaseException e) {
            log.error("Unable to save log event to the database", e);
        }
    }

    @Override
    public void logInternalEvent(final AbstractSegueUserDTO user, final LogType eventType, final Object eventDetails) {
        Validate.notNull(user);
        try {
            if (user instanceof RegisteredUserDTO) {
                this.persistLogEvent(((RegisteredUserDTO) user).getId().toString(), null, eventType.name(), eventDetails,
                        null);
            } else {
                this.persistLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType.name(), eventDetails, null);
            }

        } catch (JsonProcessingException e) {
            log.error("Unable to serialize eventDetails as json string", e);
        } catch (SegueDatabaseException e) {
            log.error("Unable to save log event to the databasse", e);
        }
    }

    @Override
    public void transferLogEventsToRegisteredUser(final String oldUserId, final String newUserId) {
        String query = "UPDATE logged_events SET user_id = ?, anonymous_user = TRUE WHERE user_id = ?;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, newUserId);
            pst.setString(2, oldUserId);

            pst.executeUpdate();

        } catch (SQLException e) {
            log.error("Unable to transfer log events", e);
        }
    }

    @Override
    public Long getLogCountByType(final String type) throws SegueDatabaseException {
        String query = "SELECT COUNT(*) AS TOTAL FROM logged_events WHERE event_type = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, type);

            try (ResultSet results = pst.executeQuery()) {
                results.next();
                return results.getLong("TOTAL");
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception: Unable to count log events by type", e);
        }
    }

    /**
     * log an event in the database.
     * 
     * @param userId
     *            -
     * @param anonymousUserId
     *            -
     * @param eventType
     *            -
     * @param eventDetails
     *            -
     * @param ipAddress
     *            -
     * @throws JsonProcessingException
     *             - if we are unable to serialize the eventDetails as a string.
     * @throws SegueDatabaseException - if we cannot persist the event in the database.
     */
    private void persistLogEvent(final String userId, final String anonymousUserId, final String eventType,
            final Object eventDetails, final String ipAddress) throws JsonProcessingException, SegueDatabaseException {
        // don't do anything if logging is not enabled.
        if (!this.loggingEnabled) {
            return;
        }

        LogEvent logEvent = this.buildLogEvent(userId, anonymousUserId, eventType, eventDetails, ipAddress);

        // Record log event occurrence for internal metrics
        if (ALL_ACCEPTED_LOG_TYPES.contains(eventType)) {
            LOG_EVENT.labels(eventType).inc();
        }

        String query = "INSERT INTO logged_events(user_id, anonymous_user, event_type, event_details_type,"
                + " event_details, ip_address, timestamp) VALUES (?, ?, ?, ?, ?::text::jsonb, ?::inet, ?);";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ) {
            pst.setString(1, logEvent.getUserId());
            pst.setBoolean(2, logEvent.isAnonymousUser());
            pst.setString(3, logEvent.getEventType());
            pst.setString(4, logEvent.getEventDetailsType());
            pst.setString(5, objectMapper.writeValueAsString(logEvent.getEventDetails()));
            pst.setString(6, logEvent.getIpAddress());
            pst.setTimestamp(7, new java.sql.Timestamp(new Date().getTime()));

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save user.");
            }

            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * Generate a logEvent object.
     * 
     * @param userId
     *            - owner user id
     * @param anonymousUserId
     *            - id to use if not logged in
     * @param eventType
     *            - the type of event that has occurred
     * @param eventDetails
     *            - event details if further details are required.
     * @param ipAddress
     *            - the ip address of the client making the request
     * @return a log event.
     */
    private LogEvent buildLogEvent(final String userId, final String anonymousUserId, final String eventType,
            final Object eventDetails, final String ipAddress) {
        if (null == userId && null == anonymousUserId) {
            throw new IllegalArgumentException("UserId or anonymousUserId must be set.");
        }

        LogEvent logEvent = new LogEvent();

        if (null != userId) {
            logEvent.setUserId(userId);
            logEvent.setAnonymousUser(false);
        } else {
            logEvent.setUserId(anonymousUserId);
            logEvent.setAnonymousUser(true);
        }

        logEvent.setEventType(eventType);

        if (eventDetails != null) {
            logEvent.setEventDetailsType(eventDetails.getClass().getCanonicalName());
            logEvent.setEventDetails(eventDetails);
        }

        if (ipAddress != null) {
            logEvent.setIpAddress(ipAddress.split(",")[0]);

            try {
                // split based on the fact that we usually get ip addresses of the form
                // [user_ip], [balancer/gateway_ip]
                locationManager.refreshLocation(ipAddress.split(",")[0]);
            } catch (SegueDatabaseException | IOException e1) {
                log.error("Unable to record location information for ip Address: " + ipAddress, e1);
            }
        }

        logEvent.setTimestamp(new Date());

        return logEvent;
    }
}
