/**
 * Copyright 2015 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dos.eventbookings;

import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import jakarta.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.CRC32;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.ITransaction;
import uk.ac.cam.cl.dtg.isaac.dos.PgTransaction;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * PgEventBookings.
 * <br>
 * Postgres aware EventBookings.
 */
public class PgEventBookings implements EventBookings {
  private static final Logger log = LoggerFactory.getLogger(PgEventBookings.class);
  private final PostgresSqlDb ds;

  private final ObjectMapper objectMapper;
  private static final String TABLE_NAME = "event_bookings";

  /**
   * @param ds     connection to the database.
   * @param mapper object mapper
   */
  public PgEventBookings(final PostgresSqlDb ds, final ObjectMapper mapper) {
    this.ds = ds;
    this.objectMapper = mapper;
  }

  @Override
  public EventBooking add(final ITransaction transaction, final String eventId, final Long userId,
                          final Long reserveById,
                          final BookingStatus status, final Map<String, String> additionalEventInformation)
      throws SegueDatabaseException {
    if (!(transaction instanceof PgTransaction)) {
      throw new SegueDatabaseException("Incorrect database transaction class type!");
    }

    return addEventBooking((PgTransaction) transaction, eventId, userId, reserveById, status,
        Objects.requireNonNullElseGet(additionalEventInformation, Maps::newHashMap));
  }

  @Override
  public EventBooking add(final ITransaction transaction, final String eventId, final Long userId,
                          final BookingStatus status,
                          final Map<String, String> additionalEventInformation) throws SegueDatabaseException {
    return add(transaction, eventId, userId, null, status, additionalEventInformation);
  }

  private PgEventBooking addEventBooking(
      final PgTransaction transaction, final String eventId, final Long userId, final Long reserveById,
      final BookingStatus status, final Map<String, String> additionalEventInformation) throws SegueDatabaseException {
    String query = "INSERT INTO event_bookings (id, user_id, reserved_by, event_id, status, created, updated,"
        + " additional_booking_information) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?::text::jsonb)";
    Connection conn = transaction.getConnection();
    try (PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
      Date creationDate = new Date();
      pst.setLong(FIELD_ADD_BOOKING_USER_ID, userId);
      if (reserveById == null) {
        pst.setNull(FIELD_ADD_BOOKING_RESERVED_BY, Types.INTEGER);
      } else {
        pst.setLong(FIELD_ADD_BOOKING_RESERVED_BY, reserveById);
      }
      pst.setString(FIELD_ADD_BOOKING_EVENT_ID, eventId);
      pst.setString(FIELD_ADD_BOOKING_STATUS, status.name());
      pst.setTimestamp(FIELD_ADD_BOOKING_CREATED, new java.sql.Timestamp(creationDate.getTime()));
      pst.setTimestamp(FIELD_ADD_BOOKING_UPDATED, new java.sql.Timestamp(creationDate.getTime()));
      pst.setString(FIELD_ADD_BOOKING_ADDITIONAL_INFORMATION,
          objectMapper.writeValueAsString(additionalEventInformation));
      if (pst.executeUpdate() == 0) {
        throw new SegueDatabaseException("Unable to save event booking.");
      }

      try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          Long id = generatedKeys.getLong(1);
          return new PgEventBooking(id, userId, reserveById, eventId, status, creationDate, creationDate,
              additionalEventInformation);
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
  public void updateStatus(final ITransaction transaction, final String eventId, final Long userId,
                           final Long reservingUserId,
                           final BookingStatus status, final Map<String, String> additionalEventInformation)
      throws SegueDatabaseException {
    if (additionalEventInformation != null && reservingUserId != null) {
      updateBookingStatus(transaction, eventId, userId, reservingUserId, status, additionalEventInformation);
    } else if (additionalEventInformation != null) {
      updateBookingStatus(transaction, eventId, userId, status, additionalEventInformation);
    } else if (reservingUserId != null) {
      updateBookingStatus(transaction, eventId, userId, reservingUserId, status);
    } else {
      updateBookingStatus(transaction, eventId, userId, status);
    }
  }

  /**
   * Update a booking with additional booking information and a reserving user.
   *
   * @param transaction                - the database transaction to use
   * @param eventId                    - the id of the event
   * @param userId                     - the id of the user booked on to the event
   * @param reservingUserId            - the id of the user making the reservation
   * @param status                     - the new status to change the booking to
   * @param additionalEventInformation - additional information required for the event
   * @throws SegueDatabaseException - if the database goes wrong.
   * @see #updateStatus
   */
  private void updateBookingStatus(final ITransaction transaction, final String eventId, final Long userId,
                                   final Long reservingUserId, final BookingStatus status,
                                   final Map<String, String> additionalEventInformation) throws SegueDatabaseException {
    if (!(transaction instanceof PgTransaction)) {
      throw new SegueDatabaseException("Incorrect database transaction class type!");
    }

    String query =
        "UPDATE event_bookings SET status = ?, updated = ?, additional_booking_information = ?::text::jsonb,"
            + " reserved_by = ? WHERE event_id = ? AND user_id = ?;";
    Connection conn = ((PgTransaction) transaction).getConnection();
    try (PreparedStatement pst = conn.prepareStatement(query)) {
      pst.setString(FIELD_UPDATE_BOOKING_STATUS, status.name());
      pst.setTimestamp(FIELD_UPDATE_BOOKING_UPDATED, new java.sql.Timestamp(new Date().getTime()));
      pst.setString(FIELD_UPDATE_BOOKING_BOTH_ADDITIONAL_INFORMATION,
          objectMapper.writeValueAsString(additionalEventInformation));
      pst.setLong(FIELD_UPDATE_BOOKING_BOTH_RESERVED_BY, reservingUserId);
      pst.setString(FIELD_UPDATE_BOOKING_BOTH_EVENT_ID, eventId);
      pst.setLong(FIELD_UPDATE_BOOKING_BOTH_USER_ID, userId);

      int executeUpdate = pst.executeUpdate();

      if (executeUpdate == 0) {
        throw new ResourceNotFoundException("Could not update the requested booking.");
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception while trying to update event booking", e);
    } catch (JsonProcessingException e) {
      throw new SegueDatabaseException("Unable to convert json to string for persistence.", e);
    }
  }

  /**
   * Update a booking with a reserving user but without additional booking information.
   *
   * @param transaction     - the database transaction to use
   * @param eventId         - the id of the event
   * @param userId          - the id of the user booked on to the event
   * @param reservingUserId - the id of the user making the reservation
   * @param status          - the new status to change the booking to
   * @throws SegueDatabaseException - if the database goes wrong.
   * @see #updateStatus
   */
  private void updateBookingStatus(final ITransaction transaction, final String eventId, final Long userId,
                                   final Long reservingUserId, final BookingStatus status)
      throws SegueDatabaseException {
    if (!(transaction instanceof PgTransaction)) {
      throw new SegueDatabaseException("Incorrect database transaction class type!");
    }
    String query =
        "UPDATE event_bookings SET status = ?, updated = ?, reserved_by = ? WHERE event_id = ? AND user_id = ?;";
    Connection conn = ((PgTransaction) transaction).getConnection();
    try (PreparedStatement pst = conn.prepareStatement(query)) {
      pst.setString(FIELD_UPDATE_BOOKING_STATUS, status.name());
      pst.setTimestamp(FIELD_UPDATE_BOOKING_UPDATED, new java.sql.Timestamp(new Date().getTime()));
      pst.setLong(FIELD_UPDATE_BOOKING_SINGLE_RESERVED_BY, reservingUserId);
      pst.setString(FIELD_UPDATE_BOOKING_SINGLE_EVENT_ID, eventId);
      pst.setLong(FIELD_UPDATE_BOOKING_SINGLE_USER_ID, userId);
      int executeUpdate = pst.executeUpdate();

      if (executeUpdate == 0) {
        throw new ResourceNotFoundException("Could not update the requested booking.");
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception while trying to update event booking", e);
    }
  }

  /**
   * Update a booking with additional booking information but without a reserving user.
   *
   * @param transaction                - the database transaction to use
   * @param eventId                    - the id of the event
   * @param userId                     - the id of the user booked on to the event
   * @param status                     - the new status to change the booking to
   * @param additionalEventInformation - additional information required for the event
   * @throws SegueDatabaseException - if the database goes wrong.
   * @see #updateStatus
   */
  private void updateBookingStatus(final ITransaction transaction, final String eventId, final Long userId,
                                   final BookingStatus status,
                                   final Map<String, String> additionalEventInformation) throws SegueDatabaseException {
    if (!(transaction instanceof PgTransaction)) {
      throw new SegueDatabaseException("Incorrect database transaction class type!");
    }

    String query =
        "UPDATE event_bookings SET status = ?, updated = ?, additional_booking_information = ?::text::jsonb"
            + " WHERE event_id = ? AND user_id = ?;";
    Connection conn = ((PgTransaction) transaction).getConnection();
    try (PreparedStatement pst = conn.prepareStatement(query)) {
      pst.setString(FIELD_UPDATE_BOOKING_STATUS, status.name());
      pst.setTimestamp(FIELD_UPDATE_BOOKING_UPDATED, new java.sql.Timestamp(new Date().getTime()));
      pst.setString(FIELD_UPDATE_BOOKING_SINGLE_ADDITIONAL_INFORMATION,
          objectMapper.writeValueAsString(additionalEventInformation));
      pst.setString(FIELD_UPDATE_BOOKING_SINGLE_EVENT_ID, eventId);
      pst.setLong(FIELD_UPDATE_BOOKING_SINGLE_USER_ID, userId);

      int executeUpdate = pst.executeUpdate();

      if (executeUpdate == 0) {
        throw new ResourceNotFoundException("Could not update the requested booking.");
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception while trying to update event booking", e);
    } catch (JsonProcessingException e) {
      throw new SegueDatabaseException("Unable to convert json to string for persistence.", e);
    }
  }

  /**
   * Update a booking without additional booking information or a reserving user.
   *
   * @param transaction - the database transaction to use
   * @param eventId     - the id of the event
   * @param userId      - the id of the user booked on to the event
   * @param status      - the new status to change the booking to
   * @throws SegueDatabaseException - if the database goes wrong.
   * @see #updateStatus
   */
  private void updateBookingStatus(final ITransaction transaction, final String eventId, final Long userId,
                                   final BookingStatus status) throws SegueDatabaseException {
    if (!(transaction instanceof PgTransaction)) {
      throw new SegueDatabaseException("Incorrect database transaction class type!");
    }

    String query = "UPDATE event_bookings SET status = ?, updated = ? WHERE event_id = ? AND user_id = ?;";
    Connection conn = ((PgTransaction) transaction).getConnection();
    try (PreparedStatement pst = conn.prepareStatement(query)) {
      pst.setString(FIELD_UPDATE_BOOKING_STATUS, status.name());
      pst.setTimestamp(FIELD_UPDATE_BOOKING_UPDATED, new java.sql.Timestamp(new Date().getTime()));
      pst.setString(FIELD_UPDATE_BOOKING_NONE_EVENT_ID, eventId);
      pst.setLong(FIELD_UPDATE_BOOKING_NONE_USER_ID, userId);

      int executeUpdate = pst.executeUpdate();

      if (executeUpdate == 0) {
        throw new ResourceNotFoundException("Could not update the requested booking.");
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception while trying to update event booking", e);
    }
  }

  @Override
  public void delete(final ITransaction transaction, final String eventId, final Long userId)
      throws SegueDatabaseException {
    if (!(transaction instanceof PgTransaction)) {
      throw new SegueDatabaseException("Incorrect database transaction class type!");
    }

    String query = "DELETE FROM event_bookings WHERE event_id = ? AND user_id = ?";
    Connection conn = ((PgTransaction) transaction).getConnection();
    try (PreparedStatement pst = conn.prepareStatement(query)) {
      pst.setString(FIELD_DELETE_EVENT_ID, eventId);
      pst.setLong(FIELD_DELETE_USER_ID, userId);
      int executeUpdate = pst.executeUpdate();

      if (executeUpdate == 0) {
        throw new ResourceNotFoundException("Could not delete the requested booking.");
      }

    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception while trying to delete event booking", e);
    }
  }

  @Override
  public void deleteAdditionalInformation(final Long userId) throws SegueDatabaseException {
    String query = "UPDATE event_bookings SET additional_booking_information = null WHERE user_id = ?;";
    try (Connection conn = ds.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_DELETE_ADDITIONAL_INFORMATION_USER_ID, userId);
      pst.executeUpdate();
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception while trying to expunge additional event information", e);
    }
  }

  /**
   * Acquire a globally unique lock on an event for the duration of a transaction.
   *
   * @param transaction - the database transaction to acquire the lock in.
   * @param resourceId  - the ID of the event to be locked.
   */
  @Override
  public void lockEventUntilTransactionComplete(final ITransaction transaction, final String resourceId)
      throws SegueDatabaseException {
    if (!(transaction instanceof PgTransaction)) {
      throw new SegueDatabaseException("Incorrect database transaction class type!");
    }

    // Generate 32-bit CRC based on table id and resource id so that it is more likely to be unique globally.
    CRC32 crc = new CRC32();
    crc.update((TABLE_NAME + resourceId).getBytes());

    Connection conn = ((PgTransaction) transaction).getConnection();
    try (PreparedStatement pst = conn.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
      pst.setLong(FIELD_TRANSACTION_LOCK_CRC, crc.getValue());
      log.debug(String.format("Attempting to acquire advisory transaction lock on %s (%s)", TABLE_NAME + resourceId,
          crc.getValue()));
      pst.executeQuery();
    } catch (SQLException e) {
      String msg = String.format("Unable to acquire lock for event (%s).", resourceId);
      log.error(msg);
      throw new SegueDatabaseException(msg);
    }
    log.debug(String.format("Acquired advisory transaction lock on %s (%s)", TABLE_NAME + resourceId, crc.getValue()));
  }

  @Override
  public EventBooking findBookingById(final Long bookingId) throws SegueDatabaseException {
    Validate.notNull(bookingId);

    String query = "SELECT * FROM event_bookings WHERE id = ?";
    try (Connection conn = ds.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_BY_ID_BOOKING_ID, bookingId);

      try (ResultSet results = pst.executeQuery()) {
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
          String msg = String.format("Found more than one event booking with ID (%s).", bookingId);
          log.error(msg);
          throw new SegueDatabaseException(msg);
        }
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
  public EventBooking findBookingByEventAndUser(final String eventId, final Long userId)
      throws SegueDatabaseException {
    Validate.notBlank(eventId);

    String query = "SELECT * FROM event_bookings WHERE event_id = ? AND user_id = ?";
    try (Connection conn = ds.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_GET_BY_EVENT_AND_USER_EVENT_ID, eventId);
      pst.setLong(FIELD_GET_BY_EVENT_AND_USER_USER_ID, userId);

      try (ResultSet results = pst.executeQuery()) {
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
              "Found more than one event booking that matches event id (%s) and user id (%s).",
              sanitiseExternalLogValue(eventId), userId);
          log.error(msg);
          throw new SegueDatabaseException(msg);
        }
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
  public Iterable<EventBooking> findAllByEventId(final String eventId) throws SegueDatabaseException {
    return this.findAllByEventIdAndStatus(eventId, (BookingStatus) null);
  }

  @Override
  public Long countAllEventBookings() throws SegueDatabaseException {
    String query = "SELECT COUNT(1) AS TOTAL FROM event_bookings";
    try (Connection conn = ds.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query);
         ResultSet results = pst.executeQuery()
    ) {
      results.next();
      return results.getLong("TOTAL");
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public Map<BookingStatus, Map<Role, Long>> getEventBookingStatusCounts(final String eventId,
                                                                         final boolean includeDeletedUsersInCounts)
      throws SegueDatabaseException {
    // Note this method joins at the db table mainly to allow inclusion of deleted users in the counts.
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT event_bookings.status, users.role, COUNT(event_bookings.id) FROM event_bookings \n"
        + "INNER JOIN users ON event_bookings.user_id = users.id\n"
        + "WHERE event_bookings.event_id=?"
    );

    if (!includeDeletedUsersInCounts) {
      sb.append(" AND users.deleted = 'f'\n");
    }

    sb.append(" GROUP BY event_bookings.status, users.role;");

    try (Connection conn = ds.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(sb.toString())
    ) {
      pst.setString(FIELD_GET_STATUS_COUNTS_EVENT_ID, eventId);

      try (ResultSet results = pst.executeQuery()) {
        Map<BookingStatus, Map<Role, Long>> returnResult = Maps.newHashMap();
        while (results.next()) {
          BookingStatus bookingStatus = BookingStatus.valueOf(results.getString("status"));
          Role role = Role.valueOf(results.getString("role"));
          Long count = results.getLong("count");

          Map<Role, Long> roleCountMap = returnResult.getOrDefault(bookingStatus, Maps.newHashMap());
          roleCountMap.put(role, count);
          returnResult.put(bookingStatus, roleCountMap);
        }
        return returnResult;
      }
    } catch (SQLException e) {
      log.error("DB error ", e);
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  /**
   * Find all bookings for a given event with a given status.
   * <br>
   * Useful for finding all on a waiting list or confirmed.
   *
   * @param eventId - the event of interest.
   * @param status  - The event status that should match in the bookings returned. Can be null
   * @return an iterable with all the events matching the criteria.
   * @throws SegueDatabaseException - if an error occurs.
   */
  @Override
  public Iterable<EventBooking> findAllByEventIdAndStatus(final String eventId, @Nullable final BookingStatus status)
      throws SegueDatabaseException {
    Validate.notBlank(eventId);

    StringBuilder sb = new StringBuilder();
    sb.append("SELECT event_bookings.* FROM event_bookings JOIN users ON users.id=user_id WHERE event_id=?"
        + " AND NOT users.deleted");

    if (status != null) {
      sb.append(" AND status = ?");
    }

    try (Connection conn = ds.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(sb.toString())
    ) {
      pst.setString(FIELD_SGET_BY_EVENT_AND_STATUS_EVENT_ID, eventId);
      if (status != null) {
        pst.setString(FIELD_SGET_BY_EVENT_AND_STATUS_STATUS, status.name());
      }

      try (ResultSet results = pst.executeQuery()) {
        List<EventBooking> returnResult = Lists.newArrayList();
        while (results.next()) {
          returnResult.add(buildPgEventBooking(results));
        }
        return returnResult;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public Iterable<EventBooking> findAllByUserId(final Long userId) throws SegueDatabaseException {
    Validate.notNull(userId);

    String query = "SELECT * FROM event_bookings WHERE user_id = ?";
    try (Connection conn = ds.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_BY_USER_ID_USER_ID, userId);

      try (ResultSet results = pst.executeQuery()) {
        List<EventBooking> returnResult = Lists.newArrayList();
        while (results.next()) {
          returnResult.add(buildPgEventBooking(results));
        }
        return returnResult;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public Iterable<EventBooking> findAllReservationsByUserId(final Long userId) throws SegueDatabaseException {
    Validate.notNull(userId);

    String query =
        "SELECT distinct on (event_id) * FROM event_bookings WHERE reserved_by = ? AND status != 'CANCELLED'";
    try (Connection conn = ds.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_RESERVATIONS_BY_USER_ID_RESERVED_BY, userId);

      try (ResultSet results = pst.executeQuery()) {
        List<EventBooking> returnResult = Lists.newArrayList();
        while (results.next()) {
          returnResult.add(buildPgEventBooking(results));
        }
        return returnResult;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  /**
   * Create a pgEventBooking from a results set.
   * <br>
   * Assumes there is a result to read.
   *
   * @param results - the results to convert
   * @return a new PgEventBooking
   * @throws SQLException - if an error occurs.
   */
  private PgEventBooking buildPgEventBooking(final ResultSet results) throws SQLException, SegueDatabaseException {
    return new PgEventBooking(
        results.getLong("id"),
        results.getLong("user_id"),
        results.getLong("reserved_by"),
        results.getString("event_id"),
        BookingStatus.valueOf(results.getString("status")),
        results.getTimestamp("created"),
        results.getTimestamp("updated"),
        results.getObject("additional_booking_information")
    );
  }

  // Field Constants
  // add
  private static final int FIELD_ADD_BOOKING_USER_ID = 1;
  private static final int FIELD_ADD_BOOKING_RESERVED_BY = 2;
  private static final int FIELD_ADD_BOOKING_EVENT_ID = 3;
  private static final int FIELD_ADD_BOOKING_STATUS = 4;
  private static final int FIELD_ADD_BOOKING_CREATED = 5;
  private static final int FIELD_ADD_BOOKING_UPDATED = 6;
  private static final int FIELD_ADD_BOOKING_ADDITIONAL_INFORMATION = 7;

  // updateBookingStatus - common fields
  private static final int FIELD_UPDATE_BOOKING_STATUS = 1;
  private static final int FIELD_UPDATE_BOOKING_UPDATED = 2;

  // updateBookingStatus - both additional information and reserving user
  private static final int FIELD_UPDATE_BOOKING_BOTH_ADDITIONAL_INFORMATION = 3;
  private static final int FIELD_UPDATE_BOOKING_BOTH_RESERVED_BY = 4;
  private static final int FIELD_UPDATE_BOOKING_BOTH_EVENT_ID = 5;
  private static final int FIELD_UPDATE_BOOKING_BOTH_USER_ID = 6;

  // updateBookingStatus - one of additional information or reserving user
  private static final int FIELD_UPDATE_BOOKING_SINGLE_ADDITIONAL_INFORMATION = 3;
  private static final int FIELD_UPDATE_BOOKING_SINGLE_RESERVED_BY = 3;
  private static final int FIELD_UPDATE_BOOKING_SINGLE_EVENT_ID = 4;
  private static final int FIELD_UPDATE_BOOKING_SINGLE_USER_ID = 5;

  // updateBookingStatus - neither additional information nor reserving user
  private static final int FIELD_UPDATE_BOOKING_NONE_EVENT_ID = 3;
  private static final int FIELD_UPDATE_BOOKING_NONE_USER_ID = 4;

  // delete
  private static final int FIELD_DELETE_EVENT_ID = 1;
  private static final int FIELD_DELETE_USER_ID = 2;

  // deleteAdditionalInformation
  private static final int FIELD_DELETE_ADDITIONAL_INFORMATION_USER_ID = 1;

  // lockEventUntilTransactionComplete
  private static final int FIELD_TRANSACTION_LOCK_CRC = 1;

  // findBookingById
  private static final int FIELD_GET_BY_ID_BOOKING_ID = 1;

  // findBookingByEventAndUser
  private static final int FIELD_GET_BY_EVENT_AND_USER_EVENT_ID = 1;
  private static final int FIELD_GET_BY_EVENT_AND_USER_USER_ID = 2;

  // getEventBookingStatusCounts
  private static final int FIELD_GET_STATUS_COUNTS_EVENT_ID = 1;

  // findAllByEventIdAndStatus
  private static final int FIELD_SGET_BY_EVENT_AND_STATUS_EVENT_ID = 1;
  private static final int FIELD_SGET_BY_EVENT_AND_STATUS_STATUS = 2;

  // findAllByUserId
  private static final int FIELD_GET_BY_USER_ID_USER_ID = 1;

  // findAllReservationsByUserId
  private static final int FIELD_GET_RESERVATIONS_BY_USER_ID_RESERVED_BY = 1;
}
