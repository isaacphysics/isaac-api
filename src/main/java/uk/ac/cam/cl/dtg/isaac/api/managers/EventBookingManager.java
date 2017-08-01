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
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailAttachment;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.InvalidUserAssociationTokenException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
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
                               final PropertiesLoader propertiesLoader) {
        this.bookingPersistenceManager = bookingPersistenceManager;
        this.emailManager = emailManager;
        this.userAssociationManager = userAssociationManager;
        this.propertiesLoader = propertiesLoader;
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
     * This method will allow users to be booked onto an event providing there is space. No other rules are applied.
     * This is likely to be only for admin users.
     *
     * @param event - of interest
     * @param user  - user to book on to the event.
     * @param additionalEventInformation - any additional information for the event organisers (nullable)
     * @return the newly created booking.
     * @throws SegueDatabaseException    - if an error occurs.
     * @throws EventIsFullException      - No space on the event
     * @throws DuplicateBookingException - Duplicate booking, only unique bookings.
     */
    public EventBookingDTO createBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user,
                                         final Map<String, String> additionalEventInformation)
            throws SegueDatabaseException, DuplicateBookingException, EventIsFullException {
        // check if already booked
        if (this.isUserBooked(event.getId(), user.getId())) {
            throw new DuplicateBookingException(String.format("Unable to book onto event (%s) as user (%s) is already"
                    + " booked on to it.", event.getId(), user.getEmail()));
        }

        try {
            // Obtain an exclusive database lock to lock the booking
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());

            this.ensureCapacity(event, user);

            return this.bookingPersistenceManager.createBooking(event.getId(), user.getId(), BookingStatus.CONFIRMED,
                    additionalEventInformation);
        } finally {
            // release lock.
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }
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
                                .put("myBookedEventsURL", String.format("https://%s/events?show_booked_only=true",
                                        propertiesLoader.getProperty(HOST_NAME)))
                                .put("myAssignmentsURL", String.format("https://%s/assignments",
                                        propertiesLoader.getProperty(HOST_NAME)))
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
            if (event.getIsaacGroupToken() != null) {
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
                if (numberOfPlaces > 0 && !(event.getBookingDeadline() != null
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

            try {
                emailManager.sendTemplatedEmailToUser(user,
                        emailManager.getEmailTemplateDTO("email-event-waiting-list-addition-notification"),
                        new ImmutableMap.Builder<String, Object>()
                                .put("myBookedEventsURL", String.format("https://%s/events?show_booked_only=true",
                                        propertiesLoader.getProperty(HOST_NAME)))
                                .put("myAssignmentsURL", String.format("https://%s/assignments",
                                        propertiesLoader.getProperty(HOST_NAME)))
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
     * @param additionalInformation additional information to be stored with this booking e.g. dietary requirements.
     * @return the updated booking.
     * @throws SegueDatabaseException       - if there is a database error
     * @throws EmailMustBeVerifiedException - if this method requires a validated e-mail address.
     * @throws DuplicateBookingException    - Duplicate booking, only unique bookings.
     * @throws EventIsFullException         - No space on the event
     * @throws EventBookingUpdateException  - Unable to update the event booking.
     */
    public EventBookingDTO promoteFromWaitingListOrCancelled(final IsaacEventPageDTO event, final RegisteredUserDTO
            userDTO, final Map<String, String> additionalInformation)
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
                    .getId(), BookingStatus.CONFIRMED, additionalInformation);

            emailManager.sendTemplatedEmailToUser(userDTO,
                    emailManager.getEmailTemplateDTO("email-event-booking-waiting-list-promotion-confirmed"),
                    new ImmutableMap.Builder<String, Object>()
                            .put("myBookedEventsURL", String.format("https://%s/events?show_booked_only=true",
                                    propertiesLoader.getProperty(HOST_NAME)))
                            .put("myAssignmentsURL", String.format("https://%s/assignments",
                                    propertiesLoader.getProperty(HOST_NAME)))
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

        } finally {
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }
    }

    /**
     * Delete a booking permanently.
     *
     * @param eventId - event id
     * @param userId  - user id to unbook
     * @throws SegueDatabaseException - if an error occurs.
     */
    public void deleteBooking(final String eventId, final Long userId) throws SegueDatabaseException {
        try {
            // Obtain an exclusive database lock to lock the booking
            this.bookingPersistenceManager.acquireDistributedLock(eventId);
            this.bookingPersistenceManager.deleteBooking(eventId, userId);
        } finally {
            this.bookingPersistenceManager.releaseDistributedLock(eventId);
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
                            .put("myBookedEventsURL", String.format("https://%s/events?show_booked_only=true",
                                    propertiesLoader.getProperty(HOST_NAME)))
                            .put("myAssignmentsURL", String.format("https://%s/assignments",
                                    propertiesLoader.getProperty(HOST_NAME)))
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
                            .put("myBookedEventsURL", String.format("https://%s/events?show_booked_only=true",
                                    propertiesLoader.getProperty(HOST_NAME)))
                            .put("myAssignmentsURL", String.format("https://%s/assignments",
                                    propertiesLoader.getProperty(HOST_NAME)))
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
     * @param event - the event booked on
     * @param bookingDetails - the booking details.
     * @return email attachment containing an ics file.
     */
    private EmailAttachment generateEventICSFile(IsaacEventPageDTO event, EventBookingDTO bookingDetails) {
        TimezoneAssignment london = TimezoneAssignment.download(TimeZone.getTimeZone(DEFAULT_TIME_LOCALITY), true);

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
}