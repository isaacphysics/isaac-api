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
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
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
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import java.text.DateFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * EventBookingManager.
 * This class is responsible for controlling event bookings throughout the platform.
 */
public class EventBookingManager {
    private static final Logger log = LoggerFactory.getLogger(EventBookingManager.class);

    private final EventBookingPersistenceManager bookingPersistenceManager;
    private final EmailManager emailManager;
    private final UserAssociationManager userAssociationManager;
    private final UserAccountManager userAccountManager;
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
                               final UserAccountManager userAccountManager,
                               final PropertiesLoader propertiesLoader,
                               final GroupManager groupManager) {
        this.bookingPersistenceManager = bookingPersistenceManager;
        this.emailManager = emailManager;
        this.userAssociationManager = userAssociationManager;
        this.userAccountManager = userAccountManager;
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
     * Count all bookings in the database.
     *
     * @return event bookings
     * @throws SegueDatabaseException - if an error occurs.
     */
    public Long getCountOfEventBookings() throws SegueDatabaseException {
        return this.bookingPersistenceManager.countAllBookings();
    }

    /**
     * @param eventId - of interest
     * @return event bookings
     * @throws SegueDatabaseException - if an error occurs.
     */
    public List<EventBookingDTO> getBookingByEventId(final String eventId) throws SegueDatabaseException {
        return this.bookingPersistenceManager.getBookingByEventId(eventId);
    }

    public EventBookingDTO getBookingByEventIdAndUserId(final String eventId, final Long userId) throws SegueDatabaseException {
        return this.bookingPersistenceManager.getBookingByEventIdAndUserId(eventId, userId);
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
     * Check if the registered user is able to manage the given event.
     * Event managers and admins can manage all events where as event leaders can only manage events for which they are
     * managers of the event's associated group.
     * @param user wishing to manage the event.
     * @param event to be managed.
     * @return either true or false if the user is able to manage the event.
     * @throws SegueDatabaseException if there is a problem with the database while retrieving associations or groups.
     */
    public boolean isUserAbleToManageEvent(RegisteredUserDTO user, IsaacEventPageDTO event) throws SegueDatabaseException {
        if (Arrays.asList(Role.EVENT_MANAGER, Role.ADMIN).contains(user.getRole())) {
            return true;
        }
        if (Role.EVENT_LEADER.equals(user.getRole())) {
            try {
                String eventGroupTokenString = event.getIsaacGroupToken();
                if (eventGroupTokenString == null || eventGroupTokenString.isEmpty()) {
                    return false;
                }
                AssociationToken eventGroupToken = userAssociationManager.lookupTokenDetails(user, eventGroupTokenString);
                UserGroupDTO eventGroup = groupManager.getGroupById(eventGroupToken.getGroupId());
                if (GroupManager.isOwnerOrAdditionalManager(eventGroup, user.getId())) {
                    return true;
                }
            } catch (InvalidUserAssociationTokenException e) {
                log.error("Event {} has an invalid user association token - ignoring", event.getId());
            }
        }
        return false;
    }

    /**
     * Check if the requesting/logged-in user is the one who made the reservation.
     *
     * @param user                  The logged in user
     * @param userOwningReservation The reserved user
     * @param event                 The event on which the reserved user is reserved
     * @return either true or false whether the requesting user made the reservation
     * @throws SegueDatabaseException if there is a problem retrieving the event and booking info
     */
    public boolean isReservationMadeByRequestingUser(RegisteredUserDTO user,
                                                     RegisteredUserDTO userOwningReservation,
                                                     IsaacEventPageDTO event)
            throws SegueDatabaseException {

        EventBookingDTO booking = this.getBookingByEventIdAndUserId(event.getId(), userOwningReservation.getId());
        Long reservingUserId = booking.getReservedById();
        return user.getId().equals(reservingUserId);
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
                                         final Map<String, String> additionalEventInformation,
                                         final BookingStatus status)
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

            // Send an email notifying the user (unless they are being added after the event for the sake of our records)
            Date bookingDate = new Date();
            if (event.getEndDate() == null || bookingDate.before(event.getEndDate())) {
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
                            Collections.singletonList(generateEventICSFile(event, booking)));
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
            Set<BookingStatus> upgradableStatuses = new HashSet<>(Arrays.asList(BookingStatus.CANCELLED, BookingStatus.RESERVED));
            if (this.hasBookingWithAnyOfStatuses(event.getId(), user.getId(), upgradableStatuses)) {
                // if the user has previously cancelled we should let them book again.
                booking = this.bookingPersistenceManager.updateBookingStatus(event.getId(), user.getId(),
                        BookingStatus.CONFIRMED, additionalEventInformation);
            } else {
                booking = this.bookingPersistenceManager.createBooking(event.getId(), user.getId(),
                        BookingStatus.CONFIRMED, additionalEventInformation);
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
                        Collections.singletonList(generateEventICSFile(event, booking)));

            } catch (ContentManagerException e) {
                log.error(String.format("Unable to send event email (%s) to user (%s)", event.getId(), user
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
     * Request a reservation for the given batch of users on the given event.
     * This method will allow teachers and group managers to "soft-book" students providing they do not exceed their
     * allocated limit.
     * There is no additional event information passed now, this can be filled in later when actual bookings are
     * requested.
     *
     * @param event - to reserve the user on
     * @param users - to reserve on the event
     * @return confirmation of reservation
     */
    public List<EventBookingDTO> requestReservations(final IsaacEventPageDTO event, final List<RegisteredUserDTO> users,
                                                     final RegisteredUserDTO reservingUser)
            throws EventDeadlineException, EmailMustBeVerifiedException, DuplicateBookingException,
            SegueDatabaseException, EventIsFullException, EventGroupReservationLimitException {

        // af599 TODO: Is it wise to do this before acquiring a database lock?
        for (RegisteredUserDTO user : users) {
            this.ensureValidBooking(event, user, true);
        }

        List<EventBookingDTO> reservations = new ArrayList<>();
        try {
            // Obtain an exclusive database lock to lock the event
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());

            // is there space on the event? Teachers don't count for student events.
            // work out capacity information for the event at this moment in time.
            // If there is no space, no reservations are made. Throw an exception and handle in EventsFacade.
            this.ensureCapacity(event, users);

            // Is the request for more reservations that this event allows?
            this.enforceReservationLimit(event, users, reservingUser);

            // IMPORTANT: Any non-ignorable exception past this point must roll back any reservation made thus far.
            for (RegisteredUserDTO user : users) {
                // attempt to book them on the event
                EventBookingDTO reservation;

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, EVENT_RESERVATION_CLOSE_INTERVAL_DAYS);
                Date reservationCloseDate = Stream.of(calendar.getTime(), event.getDate())
                        .min(Comparator.comparing(Date::getTime))
                        .orElseThrow(NoSuchElementException::new);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                Map<String, String> additionalEventInformation = new HashMap<>();
                additionalEventInformation.put("reservationCloseDate", dateFormat.format(reservationCloseDate));

                // attempt to book them on the event
                if (this.hasBookingWithStatus(event.getId(), user.getId(), BookingStatus.CANCELLED)) {
                    // if the user has previously cancelled we should let them book again.
                    reservation = this.bookingPersistenceManager.updateBookingStatus(event.getId(), user.getId(),
                            BookingStatus.RESERVED, additionalEventInformation);
                } else {
                    reservation = this.bookingPersistenceManager.createBooking(event.getId(), user.getId(), reservingUser.getId(),
                            BookingStatus.RESERVED, additionalEventInformation);
                }
                reservations.add(reservation);

                try {
                    emailManager.sendTemplatedEmailToUser(user,
                            // af599 TODO: Use the correct email template and parameters here.
                            // af599 TODO: Content team action.
                            emailManager.getEmailTemplateDTO("email-event-reservation-requested"),
                            new ImmutableMap.Builder<String, Object>()
                                    // af599 TODO Investigate flattening users
                                    .put("reservingUser.givenName", reservingUser.getGivenName())
                                    .put("reservingUser.familyName", reservingUser.getFamilyName())
                                    .put("contactUsURL", generateEventContactUsURL(event))
                                    .put("eventURL", String.format("https://%s/eventbooking/%s",
                                            propertiesLoader.getProperty(HOST_NAME), event.getId()))
                                    .put("event.emailEventDetails",
                                            event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                                    .put("event", event)
                                    .build(),
                            EmailType.SYSTEM,
                            // af599 TODO Maybe don't attach a calendar event file for a reservation
                            Collections.singletonList(generateEventICSFile(event, reservation)));

                } catch (SegueDatabaseException | ContentManagerException e) {
                    log.error(String.format("Unable to send event email (%s) to user (%s)",
                            event.getId(), user.getEmail()), e);
                }
            }
            return reservations;

        } catch (EventIsFullException | SegueDatabaseException | EventGroupReservationLimitException | NullPointerException e) {
            for (EventBookingDTO reservation : reservations) {
                try {
                    bookingPersistenceManager.deleteBooking(event.getId(), reservation.getUserBooked().getId());
                } catch (SegueDatabaseException f) {
                    // Tough luck?
                    log.error(String.format("Unable to roll back reservation (%s) on event (%s)",
                            reservation, event), f);
                }
            }
            throw e;
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

            Long numberOfPlaces = getPlacesAvailable(event);
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
                        null,
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
                                .put("event", event)
                                .build(),
                        EmailType.SYSTEM);

            } catch (ContentManagerException e) {
                log.error(String.format("Unable to send event email (%s) to user (%s)", event.getId(), user
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
     * @throws EventIsFullException         - No space on the event
     * @throws EventBookingUpdateException  - Unable to update the event booking.
     */
    public EventBookingDTO promoteFromWaitingListOrCancelled(final IsaacEventPageDTO event, final RegisteredUserDTO
            userDTO)
            throws SegueDatabaseException, EventBookingUpdateException, EventIsFullException {
        EventBookingDTO updatedStatus;
        try {
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());

            final EventBookingDTO eventBooking = this.bookingPersistenceManager.getBookingByEventIdAndUserId(
                    event.getId(), userDTO.getId());
            if (null == eventBooking) {
                throw new EventBookingUpdateException("Unable to promote a booking that doesn't exist.");
            }

            if (this.isUserBooked(event.getId(), userDTO.getId())) {
                throw new EventBookingUpdateException("Unable to promote a booking that is CONFIRMED already.");
            }

            final Long placesAvailable = this.getPlacesAvailable(event, true);
            if (placesAvailable != null && placesAvailable <= 0) {
                throw new EventIsFullException("The event you are attempting promote a booking for is at or "
                        + "over capacity.");
            }

            // probably want to send a waiting list promotion email.
            try {
                updatedStatus = this.bookingPersistenceManager
                        .updateBookingStatus(eventBooking.getEventId(), userDTO.getId(),
                                BookingStatus.CONFIRMED, eventBooking.getAdditionalInformation()
                        );

                // Send an email notifying the user (unless they are being promoted after the event for the sake of our records)
                Date promotionDate = new Date();
                if (event.getEndDate() == null || promotionDate.before(event.getEndDate())) {
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
                            Collections.singletonList(generateEventICSFile(event, updatedStatus)));
                }
            } catch (ContentManagerException e) {
                log.error(String.format("Unable to send event email (%s) to user (%s)", event.getId(),
                        userDTO.getEmail()), e);
                throw new EventBookingUpdateException("Unable to send event email, failed to update event booking");
            }
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
     * Allows an admin user to record the attendance of a booking as either attended or absent.
     *
     * @param event                 - The event in question.
     * @param userDTO               - The user whose booking should be updated.
     * @param attended              - Whether the user attended the event or not.
     * @return the updated booking.
     * @throws SegueDatabaseException       - Database error.
     * @throws EventBookingUpdateException  - Unable to update the event booking.
     */
    public EventBookingDTO recordAttendance(final IsaacEventPageDTO event, final RegisteredUserDTO
            userDTO, final boolean attended)
            throws SegueDatabaseException, EventBookingUpdateException {

        final EventBookingDTO eventBooking = this.bookingPersistenceManager.getBookingByEventIdAndUserId(
                event.getId(), userDTO.getId());
        if (null == eventBooking) {
            throw new EventBookingUpdateException("Unable to record attendance for booking that doesn't exist.");
        }

        BookingStatus attendanceStatus = attended ? BookingStatus.ATTENDED : BookingStatus.ABSENT;
        if (attendanceStatus.equals(eventBooking.getBookingStatus())) {
            throw new EventBookingUpdateException("Booking attendance is already registered.");
        }

        EventBookingDTO updatedStatus = this.bookingPersistenceManager.updateBookingStatus(eventBooking.getEventId(),
                userDTO.getId(), attendanceStatus, eventBooking.getAdditionalInformation());

        return updatedStatus;
    }

    /**
     * getPlacesAvailable.
     * This method is not threadsafe and will not acquire a lock.
     * It assumes that both WAITING_LIST and CONFIRMED bookings count towards capacity for all events apart from
     * WAITING_LIST_ONLY events where only confirmed bookings count.
     * <p>
     * This assumption allows waiting list bookings to be manually changed into CONFIRMED by event
     * managers without the possibility of someone creating a new booking to occupy the space after a confirmed
     * cancellation.
     * <p>
     * It also assumes teachers don't count on student events.
     *
     * @param event - the event we care about
     * @return the number of places available or Null if there is no limit. If a negative number would be returned
     * the method will only return 0. This allows for manual overbooking.
     * @throws SegueDatabaseException - if we cannot contact the database.
     */
    public Long getPlacesAvailable(final IsaacEventPageDTO event) throws SegueDatabaseException {
        if (EventStatus.WAITING_LIST_ONLY.equals(event.getEventStatus())) {
            return this.getPlacesAvailable(event, true);
        } else {
            return this.getPlacesAvailable(event, false);
        }
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
    private Long getPlacesAvailable(final IsaacEventPageDTO event, final boolean countOnlyConfirmed)
            throws SegueDatabaseException {
        boolean isStudentEvent = event.getTags().contains("student");
        Integer numberOfPlaces = event.getNumberOfPlaces();
        if (null == numberOfPlaces) {
            return null;
        }

        // include deleted users' bookings events only if the event is in the past so it doesn't mess with ability for new users to book on future events.
        boolean includeDeletedUsersInCounts = false;
        if (event.getDate() != null && event.getDate().before(new Date())) {
            includeDeletedUsersInCounts = true;
        }

        Map<BookingStatus, Map<Role, Long>> eventBookingStatusCounts = this.bookingPersistenceManager.getEventBookingStatusCounts(event.getId(), includeDeletedUsersInCounts);

        Long totalBooked = 0L;
        Long studentCount = 0L;

        if (eventBookingStatusCounts.get(BookingStatus.CONFIRMED) != null) {
            for (Map.Entry<Role, Long> roleLongEntry : eventBookingStatusCounts.get(BookingStatus.CONFIRMED).entrySet()) {
                if (Role.STUDENT.equals(roleLongEntry.getKey())) {
                    studentCount = roleLongEntry.getValue();
                }

                totalBooked = totalBooked + roleLongEntry.getValue();
            }
        }

        if (!countOnlyConfirmed && eventBookingStatusCounts.get(BookingStatus.WAITING_LIST) != null) {
            for (Map.Entry<Role, Long> roleLongEntry : eventBookingStatusCounts.get(BookingStatus.WAITING_LIST).entrySet()) {
                if (Role.STUDENT.equals(roleLongEntry.getKey())) {
                    studentCount = studentCount + roleLongEntry.getValue();
                }

                totalBooked = totalBooked + roleLongEntry.getValue();
            }
        }

        // capacity of the event
        if (isStudentEvent) {
            return numberOfPlaces - studentCount;
        }

        if (totalBooked > numberOfPlaces) {
            return 0L;
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

    public boolean isUserReserved(final String eventId, final Long userId) throws SegueDatabaseException {
        return this.bookingPersistenceManager.isUserReserved(eventId, userId);
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
     * Find out if a user has a booking with any of the given statuses.
     *
     * @param eventId       - of interest
     * @param userId        - of interest.
     * @param bookingStatuses - the statuses of the booking.
     * @return true if a waiting list booking exists false if not
     * @throws SegueDatabaseException - if an error occurs.
     */
    public boolean hasBookingWithAnyOfStatuses(final String eventId, final Long userId, final Set<BookingStatus> bookingStatuses)
            throws SegueDatabaseException {
        try {
            EventBookingDTO eb = this.bookingPersistenceManager.getBookingByEventIdAndUserId(eventId, userId);
            return null != eb && bookingStatuses.contains(eb.getBookingStatus());
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Return the booking status for the given user and event IDs.
     *
     * @param eventId - of interest
     * @param userId - of interest.
     * @return bookingStatus - the status of the booking.
     * @throws SegueDatabaseException - if an error occurs.
     */
    public BookingStatus getBookingStatus(final String eventId, final Long userId)
        throws SegueDatabaseException {
        try {
            EventBookingDTO eb = this.bookingPersistenceManager.getBookingByEventIdAndUserId(eventId, userId);
            return eb != null ? eb.getBookingStatus() : null;
        } catch (ResourceNotFoundException e) {
            return null;
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

        Long reservedById = null;
        try {
            // Obtain an exclusive database lock to lock the booking
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());
            reservedById = this.bookingPersistenceManager
                    .getBookingByEventIdAndUserId(event.getId(), user.getId()).getReservedById();
            BookingStatus previousBookingStatus = this.getBookingStatus(event.getId(), user.getId());
            this.bookingPersistenceManager.updateBookingStatus(event.getId(), user.getId(),
                    BookingStatus.CANCELLED,
                    null);

            // Reservations do not auto add users to the event's group, so no need to remove them.
            if (!previousBookingStatus.equals(BookingStatus.RESERVED)) {
                // auto remove them from the group
                this.removeUserFromEventGroup(event, user);
            }

            // Send an email notifying the user (unless they are being canceled after the event for the sake of our records)
            Date bookingCancellationDate = new Date();
            if (event.getEndDate() == null || bookingCancellationDate.before(event.getEndDate())) {
                if (previousBookingStatus.equals(BookingStatus.RESERVED) && reservedById != null) {
                    emailManager.sendTemplatedEmailToUser(user,
                            emailManager.getEmailTemplateDTO("email-event-reservation-cancellation-confirmed"),
                            new ImmutableMap.Builder<String, Object>()
                                    .put("contactUsURL", generateEventContactUsURL(event))
                                    .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                                    .put("event", event)
                                    .build(),
                            EmailType.SYSTEM);
                    // We may also want to send an email to userAccountManager.getUserDTOById(reservedById)
                } else {
                    emailManager.sendTemplatedEmailToUser(user,
                            emailManager.getEmailTemplateDTO("email-event-booking-cancellation-confirmed"),
                            new ImmutableMap.Builder<String, Object>()
                                    .put("contactUsURL", generateEventContactUsURL(event))
                                    .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                                    .put("event", event)
                                    .build(),
                            EmailType.SYSTEM);
                }
            }
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
     * Expunge additional information fields for all of a given user's bookings i.e. to remove PII.
     *
     * @param user  - user to unbook
     * @throws SegueDatabaseException - if an error occurs.
     */
    public void deleteUsersAdditionalInformationBooking(final RegisteredUserDTO user) throws SegueDatabaseException {
        this.bookingPersistenceManager.deleteAdditionalInformation(user.getId());
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
                    Collections.singletonList(generateEventICSFile(event, booking)));

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
        } else if (booking.getBookingStatus().equals(BookingStatus.RESERVED)) {
            // af599 TODO: Fill this in.
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
        Long numberOfPlaces = getPlacesAvailable(event);
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
     * Helper method to ensure a batch can be booked onto an event without violating space restrictions on the event.
     * <p>
     * If it does an exception will be thrown if a new booking wouldn't no exception will be thrown.
     *
     * @param event the event the user wants to book on to
     * @param users  the users who are trying to be booked onto the event.
     * @throws SegueDatabaseException - if an error occurs
     * @throws EventIsFullException   - if the event is full according to the event rules established.
     */
    private void ensureCapacity(final IsaacEventPageDTO event, final List<RegisteredUserDTO> users) throws
            SegueDatabaseException, EventIsFullException {
        final boolean isStudentEvent = event.getTags().contains("student");
        Long numberOfPlaces = getPlacesAvailable(event);
        if (numberOfPlaces != null) {
            long numberOfRequests = users.stream().filter(user -> !isStudentEvent || !Role.TEACHER.equals(user.getRole())).count();
            if (numberOfPlaces - numberOfRequests < 0) {
                throw  new EventIsFullException(String.format("Unable to book batch (%s) onto event (%s) as there are "
                        + "not enough places available", users, event.getId()));
            }
        }
    }

    private void enforceReservationLimit(final IsaacEventPageDTO event, final List<RegisteredUserDTO> users,
                                         final RegisteredUserDTO reservingUser)
            throws SegueDatabaseException, EventGroupReservationLimitException, NullPointerException {

        if (reservingUser == null) {
            throw new NullPointerException("Reserving user must be specified.");
        }

        List<EventBookingDTO> existingReservations = getBookingByEventId(event.getId()).stream()
                .filter(reservation -> {
                    Long reservedById = reservation.getReservedById();
                    return reservedById != null && reservedById.equals(reservingUser.getId());
                }).collect(Collectors.toList());
        long numberOfExistingReservations = existingReservations.size();
        final boolean isStudentEvent = event.getTags().contains("student");
        Integer groupReservationLimit = event.getGroupReservationLimit();
        // This should never be null
        if (groupReservationLimit != null) {
            long numberOfRequests = users.stream().filter(user -> !isStudentEvent || !Role.TEACHER.equals(user.getRole())).count();
            if (groupReservationLimit - numberOfExistingReservations - numberOfRequests < 0) {
                throw new EventGroupReservationLimitException(String.format("You can request a maximum of %d student "
                        + "reservations for event (%s)", numberOfRequests, event.getId()));
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

        if (this.isUserReserved(event.getId(), user.getId())) {
            throw new DuplicateBookingException(String.format("Unable to reserve onto event (%s) as user (%s) is"
                    + " already reserved on to it.", event.getId(), user.getEmail()));
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

            icalEvent.setOrganizer(new Organizer(propertiesLoader.getProperty(MAIL_NAME),
                    propertiesLoader.getProperty(EVENT_ADMIN_EMAIL)));
            icalEvent.setUid(String.format("%s@%s.%s", bookingDetails.getUserBooked().getId(),
                    event.getId(), propertiesLoader.getProperty(EVENT_ICAL_UID_DOMAIN)));
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
            if (event.getIsaacGroupToken() != null && !event.getIsaacGroupToken().isEmpty()) {
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
