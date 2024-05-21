package uk.ac.cam.cl.dtg.isaac.dao;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_CANNOT_CREATE_BOOKING_DTO;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_NOT_EVENT;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.ITransaction;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBooking;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBookings;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.PgEventBookings;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressAndGenderDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

public class EventBookingPersistenceManager {
  private static final Logger log = LoggerFactory.getLogger(EventBookingPersistenceManager.class);

  private final EventBookings dao;
  private final UserAccountManager userManager;
  private final GitContentManager contentManager;

  /**
   * EventBookingPersistenceManager.
   *
   * @param database       live connection
   * @param contentManager for retrieving event content.
   * @param userManager    Instance of User Manager
   * @param objectMapper   Instance of objectmapper so we can deal with jsonb
   */
  @Inject
  public EventBookingPersistenceManager(final PostgresSqlDb database, final UserAccountManager userManager,
                                        final GitContentManager contentManager, final ObjectMapper objectMapper) {
    this.userManager = userManager;
    this.contentManager = contentManager;
    this.dao = new PgEventBookings(database, objectMapper);
  }

  /**
   * Get a list of all event bookings for a specific user by their id.
   *
   * @param userId user of interest.
   * @return events
   * @throws SegueDatabaseException if an error occurs.
   */
  public List<DetailedEventBookingDTO> getEventsByUserId(final Long userId) throws SegueDatabaseException {
    return convertToDTO(Lists.newArrayList(dao.findAllByUserId(userId)));
  }

  /**
   * Get a list of all event reservations for a specific user by their id.
   *
   * @param userId user of interest.
   * @return events
   * @throws SegueDatabaseException if an error occurs.
   */
  public List<DetailedEventBookingDTO> getEventReservationsByUserId(final Long userId) throws SegueDatabaseException {
    return convertToDTO(Lists.newArrayList(dao.findAllReservationsByUserId(userId)));
  }

  /**
   * Get a specific event booking by its booking id.
   *
   * @param bookingId of interest
   * @return event booking
   * @throws SegueDatabaseException if an error occurs.
   */
  public DetailedEventBookingDTO getDetailedBookingById(final Long bookingId) throws SegueDatabaseException {
    return this.convertToDTO(dao.findBookingById(bookingId));
  }

  /**
   * Gets a specific event booking.
   *
   * @param eventId of interest
   * @param userId  of interest
   * @return event booking or null if we can't find one.
   * @throws SegueDatabaseException if an error occurs.
   */
  public DetailedEventBookingDTO getBookingByEventIdAndUserId(final String eventId, final Long userId)
      throws SegueDatabaseException {
    return this.convertToDTO(dao.findBookingByEventAndUser(eventId, userId));
  }

  /**
   * Modify an existing event booking's status.
   *
   * @param transaction                the database transaction to use
   * @param eventId                    the id of the event
   * @param userId                     the user who is registered against the event
   * @param reservingUserId            the user who is updating this booking to be a reservation
   * @param bookingStatus              the new booking status for this booking.
   * @param additionalEventInformation additional information required for the event.
   * @return The newly updated event booking
   * @throws SegueDatabaseException if an error occurs.
   */
  public DetailedEventBookingDTO updateBookingStatus(final ITransaction transaction, final String eventId,
                                                     final Long userId, final Long reservingUserId,
                                                     final BookingStatus bookingStatus,
                                                     final Map<String, String> additionalEventInformation)
      throws SegueDatabaseException {
    dao.updateStatus(transaction, eventId, userId, reservingUserId, bookingStatus, additionalEventInformation);
    return this.getBookingByEventIdAndUserId(eventId, userId);
  }

  /**
   * Modify an existing event booking's status.
   *
   * @param transaction                the database transaction to use
   * @param eventId                    the id of the event
   * @param userId                     the user who is registered against the event
   * @param bookingStatus              the new booking status for this booking.
   * @param additionalEventInformation additional information required for the event.
   * @return The newly updated event booking
   * @throws SegueDatabaseException if an error occurs.
   */
  public DetailedEventBookingDTO updateBookingStatus(final ITransaction transaction, final String eventId,
                                                     final Long userId, final BookingStatus bookingStatus,
                                                     final Map<String, String> additionalEventInformation)
      throws SegueDatabaseException {
    return updateBookingStatus(transaction, eventId, userId, null, bookingStatus, additionalEventInformation);
  }

  /**
   * Count all bookings in the database.
   *
   * @return count of event bookings
   * @throws SegueDatabaseException if an error occurs.
   */
  public Long countAllBookings() throws SegueDatabaseException {
    return dao.countAllEventBookings();
  }

  /**
   * Get the current booking counts for the event specified.
   *
   * @param eventId                     event specified
   * @param includeDeletedUsersInCounts true if you want to include deleted users in the counts or not
   * @return Map of booking status, role to count
   * @throws SegueDatabaseException if something is wrong with the database
   */
  public Map<BookingStatus, Map<Role, Integer>> getEventBookingStatusCounts(final String eventId,
                                                                         final boolean includeDeletedUsersInCounts)
      throws SegueDatabaseException {
    return dao.getEventBookingStatusCounts(eventId, includeDeletedUsersInCounts);
  }

  /**
   * Get event bookings by an event id.
   * WARNING: This pulls PII such as dietary info, email, and other stuff that should not (always) make it to end users.
   *
   * @param eventId of interest
   * @return event bookings
   * @throws SegueDatabaseException if an error occurs.
   */
  public List<DetailedEventBookingDTO> adminGetBookingsByEventId(final String eventId) throws SegueDatabaseException {
    try {
      ContentDTO c = this.contentManager.getContentById(eventId);

      if (null == c) {
        return Lists.newArrayList();
      }

      if (c instanceof IsaacEventPageDTO eventPageDTO) {
        return this.convertToDTO(Lists.newArrayList(dao.findAllByEventId(eventId)), eventPageDTO);
      } else {
        log.error(EXCEPTION_MESSAGE_NOT_EVENT);
        throw new SegueDatabaseException(EXCEPTION_MESSAGE_NOT_EVENT);
      }
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_CANNOT_CREATE_BOOKING_DTO);
      throw new SegueDatabaseException(EXCEPTION_MESSAGE_CANNOT_CREATE_BOOKING_DTO, e);
    }
  }

  /**
   * Get event bookings by an event id.
   * WARNING: This pulls PII such as dietary info, email, and other stuff that should not (always) make it to end users.
   *
   * @param eventId of interest
   * @return event bookings
   * @throws SegueDatabaseException if an error occurs.
   */
  public List<DetailedEventBookingDTO> adminGetBookingsByEventIdAndStatus(final String eventId,
                                                                          final BookingStatus status)
      throws SegueDatabaseException {
    try {
      ContentDTO c = this.contentManager.getContentById(eventId);

      if (null == c) {
        return Lists.newArrayList();
      }

      if (c instanceof IsaacEventPageDTO eventPageDTO) {
        return this.convertToDTO(Lists.newArrayList(dao.findAllByEventIdAndStatus(eventId, status)), eventPageDTO);
      } else {
        log.error(EXCEPTION_MESSAGE_NOT_EVENT);
        throw new SegueDatabaseException(EXCEPTION_MESSAGE_NOT_EVENT);
      }
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_CANNOT_CREATE_BOOKING_DTO);
      throw new SegueDatabaseException(EXCEPTION_MESSAGE_CANNOT_CREATE_BOOKING_DTO, e);
    }
  }

  /**
   * Get event bookings by event ids.
   * WARNING: This pulls PII such as dietary info, email, and other stuff that should not (always) make it to end users.
   *
   * @param eventIds of interest
   * @return event bookings
   * @throws SegueDatabaseException if an error occurs.
   */
  public Map<String, List<DetailedEventBookingDTO>> adminGetBookingsByEventIds(final List<String> eventIds)
      throws SegueDatabaseException {
    try {
      Map<String, IsaacEventPageDTO> eventDetails = new HashMap<>();

      for (String eventId : eventIds) {
        ContentDTO c = this.contentManager.getContentById(eventId);
        if (c instanceof IsaacEventPageDTO eventPage) {
          eventDetails.put(eventId, eventPage);
        }
      }

      Iterable<EventBooking> bookings = dao.findAllByEventIds(eventIds);
      Map<String, List<DetailedEventBookingDTO>> result = new HashMap<>();

      for (EventBooking e : bookings) {
        if (!result.containsKey(e.getEventId())) {
          result.put(e.getEventId(), new ArrayList<>());
        }
        result.get(e.getEventId()).add(convertToDTO(e, eventDetails.get(e.getEventId())));
      }
      return result;
    } catch (ContentManagerException e) {
      log.error("Unable to create event booking dto.");
      throw new SegueDatabaseException("Unable to create event booking dto from DO.", e);
    }
  }

  /**
   * Get event bookings by an event id.
   * NOTE: This is a patch to stop leaking PII (see adminGetBookingsByEventId).
   *
   * @param eventId of interest
   * @return event bookings
   * @throws SegueDatabaseException if an error occurs.
   */
  public List<DetailedEventBookingDTO> getBookingsByEventId(final String eventId) throws SegueDatabaseException {
    List<DetailedEventBookingDTO> bookings = adminGetBookingsByEventId(eventId);
    for (DetailedEventBookingDTO booking : bookings) {
      booking.setAdditionalInformation(null);
    }
    return bookings;
  }

  /**
   * Create a new event booking (on behalf of another user).
   *
   * @param transaction           the database transaction to use
   * @param eventId               of interest
   * @param userId                user to book on to the event.
   * @param reservingId           user making the reservation
   * @param status                The status of the booking to create.
   * @param additionalInformation additional information required for the event.
   * @return the newly created booking.
   * @throws SegueDatabaseException if an error occurs.
   */
  public EventBookingDTO createBooking(final ITransaction transaction, final String eventId, final Long userId,
                                       final Long reservingId, final BookingStatus status,
                                       final Map<String, String> additionalInformation) throws SegueDatabaseException {
    return this.convertToDTO(dao.add(transaction, eventId, userId, reservingId, status, additionalInformation));
  }

  /**
   * Create a new event booking.
   *
   * @param transaction           the database transaction to use
   * @param eventId               of interest
   * @param userId                user to book on to the event.
   * @param status                The status of the booking to create.
   * @param additionalInformation additional information required for the event.
   * @return the newly created booking.
   * @throws SegueDatabaseException if an error occurs.
   */
  public EventBookingDTO createBooking(final ITransaction transaction, final String eventId, final Long userId,
                                       final BookingStatus status, final Map<String, String> additionalInformation)
      throws SegueDatabaseException {
    return this.convertToDTO(dao.add(transaction, eventId, userId, status, additionalInformation));
  }

  /**
   * Check if the specified user is booked onto the specified event.
   * This method only counts bookings that are confirmed.
   *
   * @param eventId of interest
   * @param userId  of interest.
   * @return true if a booking exists and is in the confirmed state, false if not
   * @throws SegueDatabaseException if an error occurs.
   */
  public boolean isUserBooked(final String eventId, final Long userId) throws SegueDatabaseException {
    try {
      final EventBooking bookingByEventAndUser = dao.findBookingByEventAndUser(eventId, userId);
      List<BookingStatus> bookedStatuses =
          Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.ATTENDED, BookingStatus.ABSENT);
      return bookingByEventAndUser != null && bookedStatuses.contains(bookingByEventAndUser.getBookingStatus());
    } catch (ResourceNotFoundException e) {
      return false;
    }
  }

  /**
   * Delete a specified booking, identified by the user and event ids.
   *
   * @param transaction the database transaction to use
   * @param eventId     event id
   * @param userId      user id
   * @throws SegueDatabaseException if an error occurs.
   */
  public void deleteBooking(final ITransaction transaction, final String eventId, final Long userId)
      throws SegueDatabaseException {
    dao.delete(transaction, eventId, userId);
  }

  /**
   * Remove the additional booking information on all event bookings for the specified user.
   *
   * @param userId user id
   * @throws SegueDatabaseException if an error occurs.
   */
  public void deleteAdditionalInformation(final Long userId) throws SegueDatabaseException {
    dao.deleteAdditionalInformation(userId);
  }

  /**
   * Acquire a globally unique lock on an event for the duration of a transaction.
   *
   * @param transaction the database transaction to acquire the lock in.
   * @param resourceId  the ID of the event to be locked.
   */
  public void lockEventUntilTransactionComplete(final ITransaction transaction, final String resourceId)
      throws SegueDatabaseException {
    dao.lockEventUntilTransactionComplete(transaction, resourceId);
  }

  /**
   * Convert an EventBooking object to it's DTO counterpart, adding additional data from the provided EventPage
   * object where necessary.
   *
   * @param eb               raw booking do
   * @param eventInformation pre-fetched event information
   * @return event booking
   * @throws SegueDatabaseException if an error occurs.
   */
  private DetailedEventBookingDTO convertToDTO(final EventBooking eb, final IsaacEventPageDTO eventInformation)
      throws SegueDatabaseException {
    DetailedEventBookingDTO result = new DetailedEventBookingDTO();

    try {
      // Note: This will pull back deleted users for the purpose of the events system
      // Note: This will also pull in PII that should be of no interest to anyone
      // DANGER: The User DTO gets silently upgraded to one containing the email address here
      UserSummaryWithEmailAddressAndGenderDTO user =
          userManager.convertToUserSummary(userManager.getUserDTOById(eb.getUserId(), true),
              UserSummaryWithEmailAddressAndGenderDTO.class);
      result.setReservedById(eb.getReservedById());
      result.setUserBooked(user);
      result.setBookingId(eb.getId());
      result.setEventDate(eventInformation.getDate());
      result.setEventId(eventInformation.getId());
      result.setEventTitle(eventInformation.getTitle());
      result.setBookingDate(eb.getCreationDate());
      result.setUpdated(eb.getUpdateDate());
      result.setBookingStatus(eb.getBookingStatus());
      result.setAdditionalInformation(eb.getAdditionalInformation());

      return result;
    } catch (NoUserException e) {
      log.error("Unable to create event booking dto as user is unavailable");
      throw new SegueDatabaseException("Unable to create event booking dto as user is unavailable", e);
    }
  }

  /**
   * Convert an EventBooking object to it's DTO counterpart, fetching and the corresponding EventPage object to provide
   * additional data where necessary. Will instead return null if the event cannot be found.
   *
   * @param eb raw booking do
   * @return event booking or null if it no longer exists.
   * @throws SegueDatabaseException if an error occurs.
   */
  private DetailedEventBookingDTO convertToDTO(final EventBooking eb) throws SegueDatabaseException {
    try {
      ContentDTO c = this.contentManager.getContentById(eb.getEventId(), true);

      if (null == c) {
        // The event this booking relates to has disappeared so treat it as though it never existed.
        log.info("The event with id {} can no longer be found skipping...", eb.getEventId());
        return null;
      }

      if (c instanceof IsaacEventPageDTO eventPageDTO) {
        return this.convertToDTO(eb, eventPageDTO);
      } else {
        log.error("Content object ({}) is not an event page.", c);
        throw new SegueDatabaseException(EXCEPTION_MESSAGE_NOT_EVENT);
      }
    } catch (ContentManagerException e) {
      log.error(EXCEPTION_MESSAGE_CANNOT_CREATE_BOOKING_DTO);
      throw new SegueDatabaseException(EXCEPTION_MESSAGE_CANNOT_CREATE_BOOKING_DTO, e);
    }
  }

  /**
   * Convert a list of EventBooking objects to their DTO counterparts.
   * Any bookings for which the corresponding event cannot be found will be omitted from the returned list.
   *
   * @param toConvert the list of event bookings to convert to DTOs.
   * @return list of converted dtos
   * @throws SegueDatabaseException if an error occurs.
   */
  private List<DetailedEventBookingDTO> convertToDTO(final List<EventBooking> toConvert) throws SegueDatabaseException {
    List<DetailedEventBookingDTO> result = Lists.newArrayList();

    for (EventBooking e : toConvert) {
      DetailedEventBookingDTO augmentedBooking = convertToDTO(e);

      if (augmentedBooking != null) {
        result.add(augmentedBooking);
      }
    }

    return result;
  }

  /**
   * This expects the same event to be used for all.
   *
   * @param toConvert    the list of event bookings to convert
   * @param eventDetails event details.
   * @return list of converted dtos.
   * @throws SegueDatabaseException if an error occurs.
   */
  private List<DetailedEventBookingDTO> convertToDTO(final List<EventBooking> toConvert,
                                                     final IsaacEventPageDTO eventDetails)
      throws SegueDatabaseException {
    List<DetailedEventBookingDTO> result = Lists.newArrayList();

    for (EventBooking e : toConvert) {
      result.add(convertToDTO(e, eventDetails));
    }

    return result;
  }
}
