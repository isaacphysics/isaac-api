/**
 * Copyright 2015 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dos.eventbookings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
import org.elasticsearch.common.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import com.google.api.client.util.Lists;

import javax.annotation.Nullable;

/**
 * PgEventBookings.
 * 
 * Postgres aware EventBookings.
 *
 */
public class PgEventBookings implements EventBookings {
    private static final Logger log = LoggerFactory.getLogger(PgEventBookings.class);
    private PostgresSqlDb ds;

    private ObjectMapper objectMapper;
    private static final String TABLE_NAME = "event_bookings";

    /**
     * 
     * @param ds
     *            connection to the database.
     */
    public PgEventBookings(final PostgresSqlDb ds, final ObjectMapper mapper) {
        this.ds = ds;
        this.objectMapper = mapper;
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBookings#add(uk.ac.cam.
     * cl.dtg.isaac.dos.eventbookings.EventBooking)
     */
    @Override
    public EventBooking add(final String eventId, final Long userId, final BookingStatus status, Map<String, String> additionalEventInformation) throws SegueDatabaseException {
        PreparedStatement pst;

        if (null == additionalEventInformation) {
            additionalEventInformation = Maps.newHashMap();
        }

        try (Connection conn = ds.getDatabaseConnection()) {
            Date creationDate = new Date();
            pst = conn.prepareStatement(
                    "INSERT INTO event_bookings (id, user_id, event_id, status, created, updated, additional_booking_information) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?::text::jsonb)",
                    Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, userId);
            pst.setString(2, eventId);
            pst.setString(3, status.name());
            pst.setTimestamp(4, new java.sql.Timestamp(creationDate.getTime()));
            pst.setTimestamp(5, new java.sql.Timestamp(creationDate.getTime()));
            pst.setString(6, objectMapper.writeValueAsString(additionalEventInformation));
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save event booking.");
            }

            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    return new PgEventBooking(ds, id, userId, eventId, status, creationDate, creationDate, additionalEventInformation);
                } else {
                    throw new SQLException("Creating event booking failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to convert json to string for persistence.", e);
        }
    }

    @Override
    public void updateStatus(final String eventId, final Long userId, final BookingStatus status, final Map<String, String> additionalEventInformation) throws SegueDatabaseException {
        PreparedStatement pst;

        try (Connection conn = ds.getDatabaseConnection()) {

            if (additionalEventInformation != null) {
                pst = conn.prepareStatement("UPDATE event_bookings " +
                    "SET status = ?, updated = ?, additional_booking_information = ?::text::jsonb " +
                    "WHERE event_id = ? AND user_id = ?;");
                pst.setString(1, status.name());
                pst.setTimestamp(2, new java.sql.Timestamp(new Date().getTime()));
                pst.setString(3, objectMapper.writeValueAsString(additionalEventInformation));
                pst.setString(4, eventId);
                pst.setLong(5, userId);
            } else {
                pst = conn.prepareStatement("UPDATE event_bookings " +
                    "SET status = ?, updated = ? " +
                    "WHERE event_id = ? AND user_id = ?;");
                pst.setString(1, status.name());
                pst.setTimestamp(2, new java.sql.Timestamp(new Date().getTime()));
                pst.setString(3, eventId);
                pst.setLong(4, userId);
            }

            int executeUpdate = pst.executeUpdate();

            if (executeUpdate == 0) {
                throw new ResourceNotFoundException("Could not delete the requested booking.");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception while trying to update event booking", e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to convert json to string for persistence.", e);
        }
    }

    @Override
    public void delete(final String eventId, final Long userId) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = ds.getDatabaseConnection()) {
            pst = conn.prepareStatement("DELETE FROM event_bookings WHERE event_id = ? AND user_id = ?");
            pst.setString(1, eventId);
            pst.setLong(2, userId);
            int executeUpdate = pst.executeUpdate();

            if (executeUpdate == 0) {
                throw new ResourceNotFoundException("Could not delete the requested booking.");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception while trying to delete event booking", e);
        }
    }

    /**
     * Acquire a globally unique database lock.
     * This method will block until the lock is released.
     * Any locks must be released manually.
     *
     * @param resourceId - the unique id for the object to be locked.
     */
    @Override
    public void acquireDistributedLock(final String resourceId) throws SegueDatabaseException {
        // generate 32 bit CRC based on table id and resource id so that is is more likely to be unique globally.
        CRC32 crc = new CRC32();
        crc.update((TABLE_NAME + resourceId).getBytes());

        // acquire lock
        try (Connection conn = ds.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select pg_advisory_lock(?)");
            pst.setLong(1, crc.getValue());
            pst.executeQuery();
        } catch (SQLException e) {
            String msg = String.format(
                "Unable to acquire lock for event (%s).", resourceId);
            log.error(msg);
            throw new SegueDatabaseException(msg);
        }
    }

    /**
     * Release a globally unique database lock.
     * This method will release a previously acquired lock.
     *
     * @param resourceId - the unique id for the object to be locked.
     */
    @Override
    public void releaseDistributedLock(final String resourceId) throws SegueDatabaseException {
        // generate 32 bit CRC based on table id and resource id so that is is more likely to be unique globally.
        CRC32 crc = new CRC32();
        crc.update((TABLE_NAME + resourceId).getBytes());

        // acquire lock
        try (Connection conn = ds.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select pg_advisory_unlock(?)");
            pst.setLong(1, crc.getValue());
            pst.executeQuery();
        } catch (SQLException e) {
            String msg = String.format(
                "Unable to release lock for event (%s).", resourceId);
            log.error(msg);
            throw new SegueDatabaseException(msg);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBookings#iterate()
     */
    @Override
    public EventBooking findBookingByEventAndUser(final String eventId, final Long userId)
            throws SegueDatabaseException {
        Validate.notBlank(eventId);

        try (Connection conn = ds.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select * FROM event_bookings WHERE event_id = ? AND user_id = ?");
            pst.setString(1, eventId);
            pst.setLong(2, userId);
            ResultSet results = pst.executeQuery();

            EventBooking result = null;
            int count = 0;
            while (results.next()) {
                result = buildPgEventBooking(results);
                count++;
            }

            if (count == 1) {
                return result;
            } else if (count == 0) {
                throw new ResourceNotFoundException("Unable to locate the booking you requested.");
            } else {
                String msg = String.format(
                        "Found more than one event booking that matches event id (%s) and user id (%s).", eventId,
                        userId);
                log.error(msg);
                throw new SegueDatabaseException(msg);
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBookings#iterate()
     */
    @Override
    public Iterable<EventBooking> findAll() throws SegueDatabaseException {
        try (Connection conn = ds.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select * FROM event_bookings");

            ResultSet results = pst.executeQuery();
            List<EventBooking> returnResult = Lists.newArrayList();
            while (results.next()) {
                returnResult.add(buildPgEventBooking(results));
            }
            return returnResult;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBookings#iterate()
     */
    @Override
    public Iterable<EventBooking> findAllByEventId(final String eventId) throws SegueDatabaseException {
        return this.findAllByEventIdAndStatus(eventId, (BookingStatus) null);
    }

    /**
     * Find all bookings for a given event with a given status.
     * <p>
     * Useful for finding all on a waiting list or confirmed.
     *
     * @param eventId - the event of interest.
     * @param status  - The event status that should match in the bookings returned. Can be null
     * @return an iterable with all the events matching the criteria.
     * @throws SegueDatabaseException - if an error occurs.
     */
    @Override
    public Iterable<EventBooking> findAllByEventIdAndStatus(final String eventId, @Nullable final BookingStatus status) throws SegueDatabaseException {
        Validate.notBlank(eventId);

        try (Connection conn = ds.getDatabaseConnection()) {
            PreparedStatement pst;
            StringBuilder sb = new StringBuilder();
            sb.append("Select * FROM event_bookings WHERE event_id = ?");

            if (status != null) {
                sb.append(" AND status = ?");
            }

            pst = conn.prepareStatement(sb.toString());
            pst.setString(1, eventId);
            if (status != null) {
                pst.setString(2, status.name());
            }

            ResultSet results = pst.executeQuery();

            List<EventBooking> returnResult = Lists.newArrayList();
            while (results.next()) {
                returnResult.add(buildPgEventBooking(results));
            }

            return returnResult;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public Iterable<EventBooking> findAllByUserId(final Long userId) throws SegueDatabaseException {
        Validate.notNull(userId);

        try (Connection conn = ds.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("Select * FROM event_bookings WHERE user_id = ?");
            pst.setLong(1, userId);
            ResultSet results = pst.executeQuery();

            List<EventBooking> returnResult = Lists.newArrayList();
            while (results.next()) {
                returnResult.add(buildPgEventBooking(results));

            }
            return returnResult;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * Create a pgEventBooking from a results set.
     * 
     * Assumes there is a result to read.
     * 
     * @param results
     *            - the results to convert
     * @return a new PgEventBooking
     * @throws SQLException
     *             - if an error occurs.
     */
    private PgEventBooking buildPgEventBooking(final ResultSet results) throws SQLException, SegueDatabaseException {
        return new PgEventBooking(ds, results.getLong("id"), results.getLong("user_id"),
                results.getString("event_id"), BookingStatus.valueOf(results.getString("status")), results.getTimestamp("created"), results.getTimestamp("updated"), results.getObject("additional_booking_information"));
    }
}
