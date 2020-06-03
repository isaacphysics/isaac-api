package uk.ac.cam.cl.dtg.isaac.dao;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.name.Named;
import ma.glasnost.orika.MapperFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.*;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryWithEmailAddressDTO;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;

/**
 * EventBookingPersistenceManager.
 *
 */
public class EventBookingPersistenceManager {
    private static final Logger log = LoggerFactory.getLogger(EventBookingPersistenceManager.class);

    private final PostgresSqlDb database;
    private final EventBookings dao;
    private final UserAccountManager userManager;
    private final IContentManager contentManager;
    private final String contentIndex;

    /**
     * EventBookingPersistenceManager.
     * 
     * @param database
     *            - live connection
     * @param contentManager
     *            - for retrieving event content.
     * @param userManager
     *            - Instance of User Manager
     * @param objectMapper
     *            - Instance of objectmapper so we can deal with jsonb
     * @param dtoMapper
     *            - Instance of dtoMapper so we can deal with jsonb
     */
    @Inject
    public EventBookingPersistenceManager(final PostgresSqlDb database, final UserAccountManager userManager,
                                          final IContentManager contentManager, final ObjectMapper objectMapper, final MapperFacade dtoMapper, @Named(CONTENT_INDEX) final String contentIndex) {
        this.database = database;
        this.userManager = userManager;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.dao = new PgEventBookings(database, objectMapper);
    }

    /**
     * @param userId
     *            - user of interest.
     * @return events
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public List<EventBookingDTO> getEventsByUserId(final Long userId) throws SegueDatabaseException {
        return convertToDTO(Lists.newArrayList(dao.findAllByUserId(userId)));
    }

    /**
     * @param bookingId
     *            - of interest
     * @return event booking
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public EventBookingDTO getBookingById(final Long bookingId) throws SegueDatabaseException {
        return this.convertToDTO(new PgEventBooking(database, bookingId));
    }

    /**
     * Gets a specific event booking
     * @param eventId
     *            - of interest
     * @param userId
     *            - of interest
     * @return event booking or null if we can't find one.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public EventBookingDTO getBookingByEventIdAndUserId(final String eventId, final Long userId) throws SegueDatabaseException {
        return this.convertToDTO(dao.findBookingByEventAndUser(eventId, userId));
    }

	/**
     * Modify an existing event booking's status
     *
     * @param eventId - the id of the event
     * @param userId = the user who is registered against the event
     * @param reservingUserId - the user who is updating this booking to be a reservation
     * @param bookingStatus - the new booking status for this booking.
     * @return The newly updated event booking
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public EventBookingDTO updateBookingStatus(final String eventId, final Long userId, final Long reservingUserId, final BookingStatus bookingStatus, final Map additionalEventInformation) throws SegueDatabaseException {
        dao.updateStatus(eventId, userId, reservingUserId, bookingStatus, additionalEventInformation);
        return this.getBookingByEventIdAndUserId(eventId, userId);
    }

    /**
     * Modify an existing event booking's status
     *
     * @param eventId - the id of the event
     * @param userId = the user who is registered against the event
     * @param bookingStatus - the new booking status for this booking.
     * @return The newly updated event booking
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public EventBookingDTO updateBookingStatus(final String eventId, final Long userId, final BookingStatus bookingStatus, final Map additionalEventInformation) throws SegueDatabaseException {
        return updateBookingStatus(eventId, userId, null, bookingStatus, additionalEventInformation);
    }

    /**
     * Count all bookings in the database.
     *
     * @return count of event bookings
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public Long countAllBookings() throws SegueDatabaseException {
        return dao.countAllEventBookings();
    }

    /**
     * Get the current booking counts for the event specified.
     *
     * @param eventId - event specified
     * @param includeDeletedUsersInCounts - true if you want to include deleted users in the counts or not
     * @return Map of booking status, role to count
     * @throws SegueDatabaseException - if something is wrong with the database
     */
    public Map<BookingStatus, Map<Role, Long>> getEventBookingStatusCounts(final String eventId, final boolean includeDeletedUsersInCounts) throws SegueDatabaseException {
        return dao.getEventBookingStatusCounts(eventId, includeDeletedUsersInCounts);
    }

    /**
     * Get event bookings by an event id.
     * TODO - if an event disappears (either by being unpublished or being deleted, then this method will not pull back the event.
     *
     * @param eventId
     *            - of interest
     * @return event bookings
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public List<EventBookingDTO> getBookingsByEventId(final String eventId) throws SegueDatabaseException {
        try {
            ContentDTO c = this.contentManager.getContentById(this.contentManager.getCurrentContentSHA(), eventId);

            if (null == c) {
                return Lists.newArrayList();
            }

            if (c instanceof IsaacEventPageDTO) {
                return this.convertToDTO(Lists.newArrayList(dao.findAllByEventId(eventId)), (IsaacEventPageDTO) c);
            } else {
                log.error("Content object is not an event page.");
                throw new SegueDatabaseException("Content object is not an event page.");
            }
        } catch (ContentManagerException e) {
            log.error("Unable to create event booking dto.");
            throw new SegueDatabaseException("Unable to create event booking dto from DO.", e);
        }
    }

    /**
     * @param eventId
     *            - of interest
     * @param userId
     *            - user to book on to the event.
     * @param reservingId
     *            - user making the reservation
     * @param status
     *            - The status of the booking to create.
     * @return the newly created booking.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public EventBookingDTO createBooking(final String eventId, final Long userId, final Long reservingId, final BookingStatus status, final Map<String,String> additionalInformation) throws SegueDatabaseException {
        return this.convertToDTO(dao.add(eventId, userId, reservingId, status, additionalInformation));
    }

    /**
     * @param eventId
     *            - of interest
     * @param userId
     *            - user to book on to the event.
     * @param status
     *            - The status of the booking to create.
     * @return the newly created booking.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public EventBookingDTO createBooking(final String eventId, final Long userId, final BookingStatus status, final Map<String,String> additionalInformation) throws SegueDatabaseException {
        return this.convertToDTO(dao.add(eventId, userId, status, additionalInformation));
    }

    /**
     * This method only counts bookings that are confirmed.
     *
     * @param eventId
     *            - of interest
     * @param userId
     *            - of interest.
     * @return true if a booking exists and is in the confirmed state, false if not
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public boolean isUserBooked(final String eventId, final Long userId) throws SegueDatabaseException {
        try {
            final EventBooking bookingByEventAndUser = dao.findBookingByEventAndUser(eventId, userId);
            List<BookingStatus> bookedStatuses = Arrays.asList(
                    BookingStatus.CONFIRMED, BookingStatus.ATTENDED, BookingStatus.ABSENT);
            return bookingByEventAndUser != null && bookedStatuses.contains(bookingByEventAndUser.getBookingStatus());
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    public boolean isUserReserved(final String eventId, final Long userId) throws SegueDatabaseException {
        try {
            final EventBooking bookingByEventAndUser = dao.findBookingByEventAndUser(eventId, userId);
            return bookingByEventAndUser.getBookingStatus().equals(BookingStatus.RESERVED);
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    public boolean isUserInWaitingList(final String eventId, final Long userId) throws SegueDatabaseException {
        try {
            final EventBooking bookingByEventAndUser = dao.findBookingByEventAndUser(eventId, userId);
            return bookingByEventAndUser.getBookingStatus().equals(BookingStatus.WAITING_LIST);
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * @param eventId
     *            - event id
     * @param userId
     *            - user id
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public void deleteBooking(final String eventId, final Long userId) throws SegueDatabaseException {
        dao.delete(eventId, userId);
    }

    /**
     * @param userId
     *            - user id
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public void deleteAdditionalInformation(final Long userId) throws SegueDatabaseException {
        dao.deleteAdditionalInformation(userId);
    }

    /**
     * Acquire a globally unique database lock.
     *
     * This lock must be released manually.
     *
     * @param resourceId - the unique id for the object to be locked.
     * @throws SegueDatabaseException if there is a problem acquiring the lock
     */
    public void acquireDistributedLock(final String resourceId) throws SegueDatabaseException {
        dao.acquireDistributedLock(resourceId);
    }

    /**
     * Release a globally unique database lock.
     * This lock must be released manually.
     * @param resourceId - the unique id for the object to be locked.
     * @throws SegueDatabaseException if there is a problem releasing the lock
     */
    public void releaseDistributedLock(final String resourceId) throws SegueDatabaseException {
        dao.releaseDistributedLock(resourceId);
    }

    /**
     * @param eb
     *            - raw booking do
     * @param eventInformation
     *            - pre-fetched event information
     * @return event booking
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    private EventBookingDTO convertToDTO(final EventBooking eb, final IsaacEventPageDTO eventInformation)
            throws SegueDatabaseException {
        EventBookingDTO result = new EventBookingDTO();

        try {
            // Note: This will pull back deleted users for the purpose of the events system
            UserSummaryWithEmailAddressDTO user = userManager.convertToDetailedUserSummaryObject(userManager.getUserDTOById(eb
                    .getUserId(), true), UserSummaryWithEmailAddressDTO.class);
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
     * @param eb
     *            - raw booking do
     * @return event booking or null if it no longer exists.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    private EventBookingDTO convertToDTO(final EventBooking eb) throws SegueDatabaseException {
        try {
            ContentDTO c = this.contentManager.getContentById(this.contentManager.getCurrentContentSHA(),
                    eb.getEventId(), true);

            if (null == c) {
                // The event this booking relates to has disappeared so treat it as though it never existed.
                log.info(String.format("The event with id %s can no longer be found skipping...", eb.getEventId()));
                return null;
            }

            if (c instanceof IsaacEventPageDTO) {
                return this.convertToDTO(eb, (IsaacEventPageDTO) c);
            } else {
                log.error(String.format("Content object (%s) is not an event page.", c));
                throw new SegueDatabaseException("Content object is not an event page.");
            }
        } catch (ContentManagerException e) {
            log.error("Unable to create event booking dto.");
            throw new SegueDatabaseException("Unable to create event booking dto from DO.", e);
        }
    }

    /**
     * @param toConvert
     *            - the list of event bookings to convert to DTOs.
     * @return list of converted dtos
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    private List<EventBookingDTO> convertToDTO(final List<EventBooking> toConvert) throws SegueDatabaseException {
        List<EventBookingDTO> result = Lists.newArrayList();

        for (EventBooking e : toConvert) {
            EventBookingDTO augmentedBooking = convertToDTO(e);

            if (augmentedBooking != null) {
                result.add(augmentedBooking);
            }
        }

        return result;
    }

    /**
     * This expects the same event to be used for all.
     * 
     * @param toConvert
     *            - the list of event bookings to convert
     * @param eventDetails
     *            - event details.
     * @return list of converted dtos.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    private List<EventBookingDTO> convertToDTO(final List<EventBooking> toConvert, final IsaacEventPageDTO eventDetails)
            throws SegueDatabaseException {
        List<EventBookingDTO> result = Lists.newArrayList();

        for (EventBooking e : toConvert) {
            result.add(convertToDTO(e, eventDetails));
        }

        return result;
    }
}
