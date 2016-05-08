package uk.ac.cam.cl.dtg.isaac.dao;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBooking;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBookings;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.PgEventBooking;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.PgEventBookings;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

/**
 * EventBookingPersistenceManager.
 *
 */
public class EventBookingPersistenceManager {
    private static final Logger log = LoggerFactory.getLogger(EventBookingPersistenceManager.class);

    private final PostgresSqlDb database;
    private final EventBookings dao;
    private final UserAccountManager userManager;
    private final ContentVersionController versionManager;

    /**
     * EventBookingPersistenceManager.
     * 
     * @param database
     *            - live connection
     * @param versionManager
     *            - for retrieving event content.
     * @param userManager
     *            - Instance of User Manager
     */
    @Inject
    public EventBookingPersistenceManager(final PostgresSqlDb database, final UserAccountManager userManager,
            final ContentVersionController versionManager) {
        this.database = database;
        this.userManager = userManager;
        this.versionManager = versionManager;
        this.dao = new PgEventBookings(database);
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
     * @return event bookings
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public List<EventBookingDTO> getAllBookings() throws SegueDatabaseException {
        return this.convertToDTO(Lists.newArrayList(dao.findAll()));
    }

    /**
     * @param eventId
     *            - of interest
     * @return event bookings
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public List<EventBookingDTO> getBookingByEventId(final String eventId) throws SegueDatabaseException {
        try {
            ContentDTO c = versionManager.getContentManager().getContentById(versionManager.getLiveVersion(), eventId);
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
     * @return the newly created booking.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public EventBookingDTO createBooking(final String eventId, final Long userId) throws SegueDatabaseException {
        return this.convertToDTO(dao.add(eventId, userId));
    }

    /**
     * @param eventId
     *            - of interest
     * @param userId
     *            - of interest.
     * @return true if a booking exists false if not
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public boolean isUserBooked(final String eventId, final Long userId) throws SegueDatabaseException {
        try {
            return dao.findBookingByEventAndUser(eventId, userId) != null;
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
     * Acquire a globally unique database lock.
     * This lock must be released manually.
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
            UserSummaryDTO user = userManager.convertToUserSummaryObject(userManager.getUserDTOById(eb
                    .getUserId()));

            result.setBookingId(eb.getId());
            result.setEventDate(eventInformation.getDate());
            result.setEventId(eventInformation.getId());
            result.setEventTitle(eventInformation.getTitle());
            result.setBookingDate(eb.getCreationDate());
            result.setUserBooked(user);
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
            ContentDTO c = versionManager.getContentManager().getContentById(versionManager.getLiveVersion(),
                    eb.getEventId());

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
