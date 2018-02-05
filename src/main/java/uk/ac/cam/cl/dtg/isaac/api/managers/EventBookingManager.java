/*
 * Copyright 2015 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.api.managers;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.io.TimezoneAssignment;
import biweekly.property.Organizer;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBooking;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailAttachment;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.InvalidUserAssociationTokenException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.AssociationToken;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.text.DateFormat;

import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_TIME_LOCALITY;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

/**
 * EventBookingManager.
 * This class is responsible for controlling event bookings throughout the platform.
 */
public class EventBookingManager {
    private static final Logger log = LoggerFactory.getLogger(EventBookingManager.class);

    private final EventBookingPersistenceManager bookingPersistenceManager;
    private final EmailManager emailManager;
    private final UserAssociationManager userAssociationManager;
    private final PropertiesLoader propertiesLoader;
    private final GroupManager groupManager;

    /**
     * EventBookingManager.
     *
     * @param bookingPersistenceManager - to allow bookings to be persisted in the database
     * @param emailManager              - email manager
     * @param userAssociationManager    - the userAssociationManager manager object
     */
    @Inject
    public EventBookingManager(final EventBookingPersistenceManager bookingPersistenceManager,
                               final EmailManager emailManager,
                               final UserAssociationManager userAssociationManager,
                               final PropertiesLoader propertiesLoader,
                               final GroupManager groupManager) {
        this.bookingPersistenceManager = bookingPersistenceManager;
        this.emailManager = emailManager;
        this.userAssociationManager = userAssociationManager;
        this.propertiesLoader = propertiesLoader;
        this.groupManager = groupManager;
    }

    /**
     * This will get all bookings for a given user.
     *
     * @param userId - user of interest.
     * @return events
     * @throws SegueDatabaseException - if an error occurs.
     */
    public Map<String, BookingStatus> getAllEventStatesForUser(final Long userId) throws SegueDatabaseException {
        final ImmutableMap.Builder<String, BookingStatus> bookingStatusBuilder = new ImmutableMap.Builder<>();

        for (EventBookingDTO booking : this.bookingPersistenceManager.getEventsByUserId(userId)) {
            bookingStatusBuilder.put(booking.getEventId(), booking.getBookingStatus());
        }

        return bookingStatusBuilder.build();
    }

    /**
     * @param bookingId - of interest
     * @return event booking
     * @throws SegueDatabaseException - if an error occurs.
     */
    public EventBookingDTO getBookingById(final Long bookingId) throws SegueDatabaseException {
        return this.bookingPersistenceManager.getBookingById(bookingId);
    }

    /**
     * @return event bookings
     * @throws SegueDatabaseException - if an error occurs.
     */
    public List<EventBookingDTO> getAllBookings() throws SegueDatabaseException {
        return this.bookingPersistenceManager.getAllBookings();
    }

    /**
     * @param eventId - of interest
     * @return event bookings
     * @throws SegueDatabaseException - if an error occurs.
     */
    public List<EventBookingDTO> getBookingByEventId(final String eventId) throws SegueDatabaseException {
        return this.bookingPersistenceManager.getBookingByEventId(eventId);
    }

    /**
     * Utility method to provide a count of the number of bookings on a given event with a given status.
     *
     * @param eventId the event id to look up
     * @param status  - the status of bookings we are interested in
     * @return the total bookings matching the criteria provided.
     * @throws SegueDatabaseException if we cannot get the booking.
     */
    public Long countNumberOfBookingsWithStatus(final String eventId, final BookingStatus status)
            throws SegueDatabaseException {
        Long v = 0L;
        for (EventBookingDTO eb : this.bookingPersistenceManager.getBookingByEventId(eventId)) {
            if (status.equals(eb.getBookingStatus())) {
                v++;
            }
        }
        return v;
    }

    /**
     * Create booking on behalf of a user.
     * This method will allow users to be booked onto an event providing there is space. If there is no space the user
     * will simply be added to the waiting list.
     *
     * @param event - of interest
     * @param user  - user to book on to the event.
     * @param additionalEventInformation - any additional information for the event organisers (nullable)
     * @return the newly created booking.
     * @throws SegueDatabaseException    - if an error occurs.
     * @throws DuplicateBookingException - Duplicate booking, only unique bookings.
     */
    public EventBookingDTO createBookingOrAddToWaitingList(final IsaacEventPageDTO event, final RegisteredUserDTO user,
                                         final Map<String, String> additionalEventInformation)
            throws SegueDatabaseException, DuplicateBookingException {
        // check if already booked
        if (this.isUserBooked(event.getId(), user.getId())) {
            throw new DuplicateBookingException(String.format("Unable to book onto event (%s) as user (%s) is already"
                    + " booked on to it.", event.getId(), user.getEmail()));
        }

        // if an event admin wants to add a user to a waiting list only event they will need to promote them afterwards.
        EventBookingDTO booking;
        if (EventStatus.WAITING_LIST_ONLY.equals(event.getEventStatus())) {
            try {
                booking =  this.createBooking(event, user, additionalEventInformation, BookingStatus.WAITING_LIST);
            } catch (EventIsFullException e1) {
                throw new RuntimeException("Creating a waiting list booking should never throw an event is full exception " +
                        "- something went terribly wrong for this to have happened", e1);
            }
            return booking;
        }

        // attempt to create a confirmed booking for the user.
        try {
            booking = this.createBooking(event, user, additionalEventInformation, BookingStatus.CONFIRMED);

        } catch (EventIsFullException e) {
            // book the user on the waiting list instead as the event is full
            try {
                booking =  this.createBooking(event, user, additionalEventInformation, BookingStatus.WAITING_LIST);
            } catch (EventIsFullException e1) {
                throw new RuntimeException("Creating a waiting list booking should never throw an event is full exception " +
                        "- something went terribly wrong for this to have happened", e1);
            }
        }

        return booking;
    }

    /**
     * Create booking on behalf of a user.
     * This method will allow users to be booked onto an event providing there is space. No other rules are applied.
     * This is likely to be only for admin users.
     *
     * This method will not enforce some of the restrictions such as event deadlines and email verification
     *
     * @param event - of interest
     * @param user  - user to book on to the event.
     * @param additionalEventInformation - any additional information for the event organisers (nullable)
     * @param status - the booking status to create i.e. CONFIRMED, WAITING_LIST etc.
     * @return the newly created booking.
     * @throws SegueDatabaseException    - if an error occurs.
     * @throws EventIsFullException      - No space on the event
     * @throws DuplicateBookingException - Duplicate booking, only unique bookings.
     */
    public EventBookingDTO createBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user,
                                         final Map<String, String> additionalEventInformation, BookingStatus status)
            throws SegueDatabaseException, DuplicateBookingException, EventIsFullException {
        // check if already booked
        if (this.isUserBooked(event.getId(), user.getId())) {
            throw new DuplicateBookingException(String.format("Unable to book onto event (%s) as user (%s) is already"
                    + " booked on to it.", event.getId(), user.getEmail()));
        }

        EventBookingDTO booking = null;
        try {
            // Obtain an exclusive database lock to lock the booking
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());

            if (BookingStatus.CONFIRMED.equals(status)) {
                this.ensureCapacity(event, user);
            }

            booking = this.bookingPersistenceManager.createBooking(event.getId(), user.getId(), status,
                    additionalEventInformation);

            if (BookingStatus.CONFIRMED.equals(status)) {
                emailManager.sendTemplatedEmailToUser(user,
                        emailManager.getEmailTemplateDTO("email-event-booking-confirmed"),
                        new ImmutableMap.Builder<String, Object>()
                                .put("contactUsURL", generateEventContactUsURL(event))
                                .put("authorizationLink", String.format("https://%s/account?authToken=%s",
                                        propertiesLoader.getProperty(HOST_NAME), event.getIsaacGroupToken()))
                                .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                                .put("event", event)
                                .build(),
                        EmailType.SYSTEM,
                        Arrays.asList(generateEventICSFile(event, booking)));

            } else if (BookingStatus.WAITING_LIST.equals(status)) {
                emailManager.sendTemplatedEmailToUser(user,
                        emailManager.getEmailTemplateDTO("email-event-waiting-list-addition-notification"),
                        new ImmutableMap.Builder<String, Object>()
                                .put("contactUsURL", generateEventContactUsURL(event))
                                .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                                .put("event", event)
                                .build(),
                        EmailType.SYSTEM);
            }
        
        } catch (ContentManagerException e) {
            log.error(String.format("Unable to send booking confirmation email (%s) to user (%s)", event.getId(), user
                    .getEmail()), e);

        } finally {
            // release lock.
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }

        // auto add them to the group and grant the owner permission
        if (BookingStatus.CONFIRMED.equals(status) && event.getIsaacGroupToken() != null
                && !event.getIsaacGroupToken().isEmpty()) {
            try {
                this.userAssociationManager.createAssociationWithToken(event.getIsaacGroupToken(), user);
            } catch (InvalidUserAssociationTokenException e) {
                log.error(String.format("Unable to auto add user (%s) using token (%s) as the token is invalid.",
                        user.getEmail(), event.getIsaacGroupToken()));
            }
        }

        return booking;
    }

    /**
     * Attempt to book onto an event.
     * This method will allow attempt to book a onto an event if the rules are not broken.
     *
     * @param event                      - of interest
     * @param user                       - user to book on to the event.
     * @param additionalEventInformation - any additional information for the event organisers (nullable)
     * @return the newly created booking.
     * @throws SegueDatabaseException       - if there is a database error
     * @throws EmailMustBeVerifiedException - if this method requires a validated e-mail address.
     * @throws DuplicateBookingException    - Duplicate booking, only unique bookings.
     * @throws EventIsFullException         - No space on the event
     * @throws EventDeadlineException       - The deadline for booking has passed.
     */
    public EventBookingDTO requestBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user,
                                          final Map<String, String> additionalEventInformation)
            throws SegueDatabaseException, EmailMustBeVerifiedException, DuplicateBookingException,
            EventIsFullException, EventDeadlineException {
        this.ensureValidBooking(event, user, true);

        try {
            // Obtain an exclusive database lock to lock the event
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());

            // is there space on the event? Teachers don't count for student events.
            // work out capacity information for the event at this moment in time.
            this.ensureCapacity(event, user);

            // attempt to book them on the event
            EventBookingDTO booking;

            // attempt to book them on the event
            if (this.hasBookingWithStatus(event.getId(), user.getId(), BookingStatus.CANCELLED)) {
                // if the user has previously cancelled we should let them book again.
                booking = this.bookingPersistenceManager.updateBookingStatus(event.getId(), user.getId(),
                        BookingStatus.CONFIRMED, additionalEventInformation);
            } else {
                booking = this.bookingPersistenceManager.createBooking(event.getId(), user.getId(), BookingStatus
                        .CONFIRMED, additionalEventInformation);
            }

            try {
                emailManager.sendTemplatedEmailToUser(user,
                        emailManager.getEmailTemplateDTO("email-event-booking-confirmed"),
                        new ImmutableMap.Builder<String, Object>()
                                .put("contactUsURL", generateEventContactUsURL(event))
                                .put("authorizationLink", String.format("https://%s/account?authToken=%s",
                                        propertiesLoader.getProperty(HOST_NAME), event.getIsaacGroupToken()))
                                .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                                .put("event", event)
                                .build(),
                        EmailType.SYSTEM,
                        Arrays.asList(generateEventICSFile(event, booking)));

            } catch (ContentManagerException e) {
                log.error(String.format("Unable to send welcome email (%s) to user (%s)", event.getId(), user
                        .getEmail()), e);
            }

            // auto add them to the group and grant the owner permission
            if (event.getIsaacGroupToken() != null && !event.getIsaacGroupToken().isEmpty()) {
                try {
                    this.userAssociationManager.createAssociationWithToken(event.getIsaacGroupToken(), user);
                } catch (InvalidUserAssociationTokenException e) {
                    log.error(String.format("Unable to auto add user (%s) using token (%s) as the token is invalid.",
                            user.getEmail(), event.getIsaacGroupToken()));
                }
            }

            return booking;
        } finally {
            // release lock
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }
    }

    /**
     * Attempt to book onto the waiting list for an event.
     *
     * @param event                 - of interest
     * @param user                  - user to book on to the event.
     * @param additionalInformation additional information to be stored with this booking e.g. dietary requirements.
     * @return the newly created booking.
     * @throws SegueDatabaseException       - if there is a database error
     * @throws EmailMustBeVerifiedException - if this method requires a validated e-mail address.
     * @throws DuplicateBookingException    - Duplicate booking, only unique bookings.
     * @throws EventIsNotFullException      - There is space on the event
     * @throws EventDeadlineException       - The deadline for booking has passed.
     */
    public EventBookingDTO requestWaitingListBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user,
                                                     final Map<String, String> additionalInformation) throws
            SegueDatabaseException, EmailMustBeVerifiedException, DuplicateBookingException,
            EventDeadlineException, EventIsNotFullException {
        final Date now = new Date();

        this.ensureValidBooking(event, user, false);

        if (this.hasBookingWithStatus(event.getId(), user.getId(), BookingStatus.WAITING_LIST)) {
            throw new DuplicateBookingException(String.format("Unable to book onto event (%s) as user (%s) is already"
                    + " booked on to it.", event.getId(), user.getEmail()));
        }

        try {
            // Obtain an exclusive database lock to lock the event
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());

            Integer numberOfPlaces = getPlacesAvailable(event);
            if (numberOfPlaces != null) {
                // check the number of places - if some available then check if the event deadline has passed. If not
                // throw error.
                if (!EventStatus.WAITING_LIST_ONLY.equals(event.getEventStatus()) && numberOfPlaces > 0 && !(event.getBookingDeadline() != null
                        && now.after(event.getBookingDeadline()))) {
                    throw new EventIsNotFullException("There are still spaces on this event. Please attempt to book "
                            + "on it.");
                }
            }

            EventBookingDTO booking;

            // attempt to book them on the waiting list of the event.
            if (this.hasBookingWithStatus(event.getId(), user.getId(), BookingStatus.CANCELLED)) {
                // if the user has previously cancelled we should let them book again.
                booking = this.bookingPersistenceManager.updateBookingStatus(event.getId(),
                        user.getId(),
                        BookingStatus.WAITING_LIST,
                        additionalInformation);
            } else {
                booking = this.bookingPersistenceManager.createBooking(event.getId(),
                        user.getId(),
                        BookingStatus.WAITING_LIST,
                        additionalInformation);
            }

            // auto add them to the group and grant the owner permission - only if this event is a special wait list only event.
            if (event.getIsaacGroupToken() != null && !event.getIsaacGroupToken().isEmpty()
                    && EventStatus.WAITING_LIST_ONLY.equals(event.getEventStatus())) {
                try {
                    this.userAssociationManager.createAssociationWithToken(event.getIsaacGroupToken(), user);
                } catch (InvalidUserAssociationTokenException e) {
                    log.error(String.format("Unable to auto add user (%s) using token (%s) as the token is invalid.",
                            user.getEmail(), event.getIsaacGroupToken()));
                }
            }

            try {
                emailManager.sendTemplatedEmailToUser(user,
                        emailManager.getEmailTemplateDTO("email-event-waiting-list-addition-notification"),
                        new ImmutableMap.Builder<String, Object>()
                                .put("contactUsURL", generateEventContactUsURL(event))
                                .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                                .put("event", event)
                                .build(),
                        EmailType.SYSTEM);

            } catch (ContentManagerException e) {
                log.error(String.format("Unable to send welcome email (%s) to user (%s)", event.getId(), user
                        .getEmail()), e);
            }

            return booking;
        } finally {
            // release lock
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }
    }

    /**
     * Allows an admin user to promote someone from the waiting list or cancelled booking to a confirmed booking.
     *
     * @param event                 - The event in question.
     * @param userDTO               - The user whose booking should be updated
     * @return the updated booking.
     * @throws SegueDatabaseException       - if there is a database error
     * @throws EmailMustBeVerifiedException - if this method requires a validated e-mail address.
     * @throws DuplicateBookingException    - Duplicate booking, only unique bookings.
     * @throws EventIsFullException         - No space on the event
     * @throws EventBookingUpdateException  - Unable to update the event booking.
     */
    public EventBookingDTO promoteFromWaitingListOrCancelled(final IsaacEventPageDTO event, final RegisteredUserDTO
            userDTO)
            throws SegueDatabaseException, EmailMustBeVerifiedException,
            DuplicateBookingException, EventBookingUpdateException, EventIsFullException {

        this.bookingPersistenceManager.acquireDistributedLock(event.getId());

        final EventBookingDTO eventBooking = this.bookingPersistenceManager.getBookingByEventIdAndUserId(
                event.getId(), userDTO.getId());
        if (null == eventBooking) {
            throw new EventBookingUpdateException("Unable to promote a booking that doesn't exist.");
        }

        if (BookingStatus.CONFIRMED.equals(eventBooking.getBookingStatus())) {
            throw new EventBookingUpdateException("Unable to promote a booking that is CONFIRMED already.");
        }

        final Integer placesAvailable = this.getPlacesAvailable(event, true);
        if (placesAvailable != null && placesAvailable <= 0) {
            throw new EventIsFullException("The event you are attempting promote a booking for is at or "
                    + "over capacity.");
        }

        EventBookingDTO updatedStatus;

        // probably want to send a waiting list promotion email.
        try {
            updatedStatus = this.bookingPersistenceManager.updateBookingStatus(eventBooking.getEventId(), userDTO
                    .getId(), BookingStatus.CONFIRMED, eventBooking.getAdditionalInformation());

            emailManager.sendTemplatedEmailToUser(userDTO,
                    emailManager.getEmailTemplateDTO("email-event-booking-waiting-list-promotion-confirmed"),
                    new ImmutableMap.Builder<String, Object>()
                            .put("contactUsURL", generateEventContactUsURL(event))
                            .put("authorizationLink", String.format("https://%s/account?authToken=%s",
                                    propertiesLoader.getProperty(HOST_NAME), event.getIsaacGroupToken()))
                            .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                            .put("event", event)
                            .build(),
                    EmailType.SYSTEM,
                    Arrays.asList(generateEventICSFile(event, updatedStatus)));

        } catch (ContentManagerException e) {
            log.error(String.format("Unable to send welcome email (%s) to user (%s)", event.getId(),
                    userDTO.getEmail()), e);
            throw new EventBookingUpdateException("Unable to send welcome email, failed to update event booking");
        } finally {
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }

        // auto add them to the group and grant the owner permission
        if (event.getIsaacGroupToken() != null && !event.getIsaacGroupToken().isEmpty()) {
            try {
                this.userAssociationManager.createAssociationWithToken(event.getIsaacGroupToken(), userDTO);
            } catch (InvalidUserAssociationTokenException e) {
                log.error(String.format("Unable to auto add user (%s) using token (%s) as the token is invalid.",
                        userDTO.getEmail(), event.getIsaacGroupToken()));
            }
        }

        return updatedStatus;
    }

    /**
     * getPlacesAvailable.
     * This method is not threadsafe and will not acquire a lock.
     * It assumes that both WAITING_LIST and CONFIRMED bookings count towards capacity.
     * <p>
     * This assumption allows waiting list bookings to be manually changed into CONFIRMED by event
     * managers without the possibility of someone creating a new booking to occupy the space.
     * <p>
     * It also assumes teachers don't count on student events.
     *
     * @param event - the event we care about
     * @return the number of places available or Null if there is no limit. If a negative number would be returned
     * the method will only return 0. This allows for manual overbooking.
     * @throws SegueDatabaseException - if we cannot contact the database.
     */
    public Integer getPlacesAvailable(final IsaacEventPageDTO event) throws SegueDatabaseException {
        return this.getPlacesAvailable(event, false);
    }

    /**
     * getPlacesAvailable.
     * This method is not threadsafe and will not acquire a lock.
     * <p>
     * It also assumes teachers don't count on student events.
     *
     * @param event              - the event we care about
     * @param countOnlyConfirmed - if true only count confirmed bookings (i.e. ignore waiting list ones.
     * @return the number of places available or Null if there is no limit. If a negative number would be returned
     * the method will only return 0. This allows for manual overbooking.
     * @throws SegueDatabaseException - if we cannot contact the database.
     */
    private Integer getPlacesAvailable(final IsaacEventPageDTO event, final boolean countOnlyConfirmed)
            throws SegueDatabaseException {
        boolean isStudentEvent = event.getTags().contains("student");
        Integer numberOfPlaces = event.getNumberOfPlaces();
        if (null == numberOfPlaces) {
            return null;
        }

        List<EventBookingDTO> getCurrentBookings = this.getBookingByEventId(event.getId());

        int studentCount = 0;
        int totalBooked = 0;
        for (EventBookingDTO booking : getCurrentBookings) {
            // don't count cancelled bookings
            if (BookingStatus.CANCELLED.equals(booking.getBookingStatus())) {
                continue;
            }

            if (countOnlyConfirmed && BookingStatus.WAITING_LIST.equals(booking.getBookingStatus())) {
                continue;
            }

            // we are still counting WAITING_LIST bookings as we want these to occupy space.

            if (booking.getUserBooked().getRole().equals(Role.STUDENT)) {
                studentCount++;
            }

            totalBooked++;
        }

        // capacity of the event
        if (isStudentEvent) {
            return numberOfPlaces - studentCount;
        }

        if (totalBooked > numberOfPlaces) {
            return 0;
        }

        return numberOfPlaces - totalBooked;
    }

    /**
     * Find out if a user is already booked on an event.
     *
     * @param eventId - of interest
     * @param userId  - of interest.
     * @return true if a booking exists false if not
     * @throws SegueDatabaseException - if an error occurs.
     */
    public boolean isUserBooked(final String eventId, final Long userId) throws SegueDatabaseException {
        return this.bookingPersistenceManager.isUserBooked(eventId, userId);
    }

    /**
     * Find out if a user has a booking with a given status.
     *
     * @param eventId       - of interest
     * @param userId        - of interest.
     * @param bookingStatus - the status of the booking.
     * @return true if a waiting list booking exists false if not
     * @throws SegueDatabaseException - if an error occurs.
     */
    public boolean hasBookingWithStatus(final String eventId, final Long userId, final BookingStatus bookingStatus)
            throws SegueDatabaseException {
        try {
            EventBookingDTO eb = this.bookingPersistenceManager.getBookingByEventIdAndUserId(eventId, userId);
            return null != eb && bookingStatus.equals(eb.getBookingStatus());
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Cancel a booking.
     * <p>
     * Note: cancelled bookings no longer occupy space on an events capacity calculations.
     *
     * @param event - event
     * @param user  - user to unbook
     * @throws SegueDatabaseException  - if a database error occurs.
     * @throws ContentManagerException - if a content error occurs.
     */
    public void cancelBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user)
            throws SegueDatabaseException, ContentManagerException {
        try {
            // Obtain an exclusive database lock to lock the booking
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());
            this.bookingPersistenceManager.updateBookingStatus(event.getId(), user.getId(),
                    BookingStatus.CANCELLED,
                    null);

            emailManager.sendTemplatedEmailToUser(user,
                    emailManager.getEmailTemplateDTO("email-event-booking-cancellation-confirmed"),
                    new ImmutableMap.Builder<String, Object>()
                            .put("contactUsURL", generateEventContactUsURL(event))
                            .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                            .put("event", event)
                            .build(),
                    EmailType.SYSTEM);

            // auto remove them from the group
            this.removeUserFromEventGroup(event, user);

        } finally {
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }
    }

    /**
     * Delete a booking permanently.
     *
     * @param event - event context
     * @param user  - user to unbook
     * @throws SegueDatabaseException - if an error occurs.
     */
    public void deleteBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user) throws SegueDatabaseException {
        try {
            // Obtain an exclusive database lock to lock the booking
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());
            this.bookingPersistenceManager.deleteBooking(event.getId(), user.getId());

            this.removeUserFromEventGroup(event, user);

        } finally {
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }
    }

    /**
     * This method will attempt to resend the last email that a user booked on an event should have received.
     * E.g. if their status is confirmed it would be a welcome email, if cancelled it would be a cancellation one.
     *
     * @param event - event that the user was booked on.
     * @param user - user to be emailed.
     */
    public void resendEventEmail(final IsaacEventPageDTO event, final RegisteredUserDTO user)
            throws SegueDatabaseException, ContentManagerException {
        EventBookingDTO booking
                = this.bookingPersistenceManager.getBookingByEventIdAndUserId(event.getId(), user.getId());

        if (booking.getBookingStatus().equals(BookingStatus.CONFIRMED)) {
            emailManager.sendTemplatedEmailToUser(user,
                    emailManager.getEmailTemplateDTO("email-event-booking-confirmed"),
                    new ImmutableMap.Builder<String, Object>()
                            .put("contactUsURL", generateEventContactUsURL(event))
                            .put("authorizationLink", String.format("https://%s/account?authToken=%s",
                                    propertiesLoader.getProperty(HOST_NAME), event.getIsaacGroupToken()))
                            .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                            .put("event", event)
                            .build(),
                    EmailType.SYSTEM,
                    Arrays.asList(generateEventICSFile(event, booking)));

        } else if (booking.getBookingStatus().equals(BookingStatus.CANCELLED)) {
            emailManager.sendTemplatedEmailToUser(user,
                    emailManager.getEmailTemplateDTO("email-event-booking-cancellation-confirmed"),
                    new ImmutableMap.Builder<String, Object>()
                            .put("contactUsURL", generateEventContactUsURL(event))
                            .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                            .put("event", event)
                            .build(),
                    EmailType.SYSTEM);

        } else if (booking.getBookingStatus().equals(BookingStatus.WAITING_LIST)) {
            emailManager.sendTemplatedEmailToUser(user,
                    emailManager.getEmailTemplateDTO("email-event-waiting-list-addition-notification"),
                    new ImmutableMap.Builder<String, Object>()
                            .put("contactUsURL", generateEventContactUsURL(event))
                            .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                            .put("event", event)
                            .build(),
                    EmailType.SYSTEM);
        } else {
            log.error("Unknown event booking status. Unable to select correct email.");
        }
    }

    /**
     * Helper method to ensure that that the booking would not violate space restrictions on the event.
     * <p>
     * If it does an exception will be thrown if a new booking wouldn't no exception will be thrown.
     *
     * @param event the event the user wants to book on to
     * @param user  the user who is trying to be booked onto the event.
     * @throws SegueDatabaseException - if an error occurs
     * @throws EventIsFullException   - if the event is full according to the event rules established.
     */
    private void ensureCapacity(final IsaacEventPageDTO event, final RegisteredUserDTO user) throws
            SegueDatabaseException, EventIsFullException {
        final boolean isStudentEvent = event.getTags().contains("student");
        Integer numberOfPlaces = getPlacesAvailable(event);
        if (numberOfPlaces != null) {
            // teachers can book on student events and do not count towards capacity
            if ((isStudentEvent && !Role.TEACHER.equals(user.getRole()) && numberOfPlaces <= 0)
                    || (!isStudentEvent && numberOfPlaces <= 0)) {
                throw new EventIsFullException(String.format("Unable to book user (%s) onto event (%s) as it is full"
                        + ".", user.getEmail(), event.getId()));
            }
        }
    }

    /**
     * Enforce business logic that is common to all event bookings / waiting list entries.
     *
     * @param event                  of interest
     * @param user                   user to book on to the event.
     * @param enforceBookingDeadline - whether or not to enforce the booking deadline of the event
     * @throws SegueDatabaseException       - if there is a database error
     * @throws EmailMustBeVerifiedException - if this method requires a validated e-mail address.
     * @throws DuplicateBookingException    - Duplicate booking, only unique bookings.
     * @throws EventDeadlineException       - The deadline for booking has passed.
     */
    private void ensureValidBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user, final boolean
            enforceBookingDeadline) throws SegueDatabaseException, EmailMustBeVerifiedException,
            DuplicateBookingException, EventDeadlineException {
        Date now = new Date();

        // check if if the end date has passed. Allowed to add to wait list after deadline.
        if (event.getEndDate() != null && now.after(event.getEndDate())
                || event.getDate() != null && now.after(event.getDate())) {
            throw new EventDeadlineException("The event is in the past.");
        }

        // if we are enforcing the booking deadline then enforce it.
        if (enforceBookingDeadline && event.getBookingDeadline() != null && now.after(event.getBookingDeadline())) {
            throw new EventDeadlineException("The booking deadline has passed.");
        }

        // check if already booked
        if (this.isUserBooked(event.getId(), user.getId())) {
            throw new DuplicateBookingException(String.format("Unable to book onto event (%s) as user (%s) is already"
                    + " booked on to it.", event.getId(), user.getEmail()));
        }

        // must have verified email
        if (!EmailVerificationStatus.VERIFIED.equals(user.getEmailVerificationStatus())) {
            throw new EmailMustBeVerifiedException(String.format("Unable to book onto event (%s) without a "
                            + "verified email address for user (%s).",
                    event.getId(),
                    user.getEmail()));
        }
    }

    /**
     * Helper method to generate an ics file for emailing to users who have booked on to an event.
     *
     * Note: This method may return null in the event we cannot communicate with a third party service.
     *
     * @param event - the event booked on
     * @param bookingDetails - the booking details.
     * @return email attachment containing an ics file.
     */
    private EmailAttachment generateEventICSFile(IsaacEventPageDTO event, EventBookingDTO bookingDetails) {

        try {
            // note this library will go out to a third part to get a sensible timezone value.
            TimezoneAssignment london = TimezoneAssignment.download(TimeZone.getTimeZone(DEFAULT_TIME_LOCALITY), false);

            ICalendar ical = new ICalendar();
            ical.getTimezoneInfo().setDefaultTimezone(london);

            VEvent icalEvent = new VEvent();
            icalEvent.setSummary(event.getTitle());
            icalEvent.setDateStart(event.getDate(), true);
            icalEvent.setDateEnd(event.getEndDate(), true);
            icalEvent.setDescription(event.getSubtitle());

            icalEvent.setOrganizer(new Organizer("Isaac Physics", "events@isaacphysics.org"));
            icalEvent.setUid(String.format("%s@%s.isaacphysics.org", bookingDetails.getUserBooked().getId(), event.getId()));
            icalEvent.setUrl(String.format("https://%s/events/%s",
                    propertiesLoader.getProperty(HOST_NAME), event.getId()));

            if (event.getLocation() != null && event.getAddress() != null) {
                icalEvent.setLocation(event.getLocation().getAddress().toString());
            }

            ical.addEvent(icalEvent);
            return new EmailAttachment("event.ics",
                    "text/calendar; charset=\"utf-8\"; method=PUBLISH", Biweekly.write(ical).go());
        } catch (IllegalArgumentException e) {
            log.error("Unable to generate ics file for event email", e);
            return null;
        }
    }

    /**
     * Helper to generate a url with a pre-generated subject field for the contact page
     * @param event - the event of interest
     * @return customised contactUs url for the event.
     */
    private String generateEventContactUsURL(IsaacEventPageDTO event){
        String defaultURL = String.format("https://%s/contact", propertiesLoader.getProperty(HOST_NAME));
        if (event.getDate() == null) {
            return defaultURL;
        }

        try {
            DateFormat shortDateFormatter = DateFormat.getDateInstance(DateFormat.SHORT);
            String location = event.getLocation() != null &&
                    event.getLocation().getAddress() != null &&
                    event.getLocation().getAddress().getAddressLine1() != null
                    ? event.getLocation().getAddress().getAddressLine1()
                    : "";

            String contactUsSubject = "Event - " + location + " - " + shortDateFormatter.format(event.getDate());

            return String.format("https://%s/contact?subject=%s",
                    propertiesLoader.getProperty(HOST_NAME),
                    URLEncoder.encode(contactUsSubject, java.nio.charset.StandardCharsets.UTF_8.toString()));

        } catch (UnsupportedEncodingException e) {
            log.error("Unable to encode url for contact us link, using default url instead", e);
            return defaultURL;
        }
    }

    /**
     * Helper method to undo the group association made when a user books on an event.
     * @param event context
     * @param user to remove
     * @throws SegueDatabaseException if there is a database error.
     */
    private void removeUserFromEventGroup(final IsaacEventPageDTO event, final RegisteredUserDTO user)
            throws SegueDatabaseException{
        try {
            // auto remove them from the group
            if (event.getIsaacGroupToken() != null) {
                AssociationToken associationToken = this.userAssociationManager.lookupTokenDetails(user, event.getIsaacGroupToken());
                UserGroupDTO group = this.groupManager.getGroupById(associationToken.getGroupId());
                if (group != null) {
                    this.groupManager.removeUserFromGroup(group, user);
                }
            }
        } catch (InvalidUserAssociationTokenException e) {
            log.error(String.format("Unable to auto remove user (%s) using token (%s) as the token is invalid.",
                    user.getEmail(), event.getIsaacGroupToken()));
        }
    }
}