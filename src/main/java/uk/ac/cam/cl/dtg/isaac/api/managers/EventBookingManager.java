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
package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.RoleNotAuthorisedException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.InvalidUserAssociationTokenException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * AssignmentManager.
 */
public class EventBookingManager {
    private static final Logger log = LoggerFactory.getLogger(EventBookingManager.class);

    private final EventBookingPersistenceManager bookingPersistenceManager;

    private final GroupManager groupManager;
    private final EmailManager emailManager;
    private final UserAccountManager userManager;

    private final UserAssociationManager userAssociationManager;

    /**
     * AssignmentManager.
     *
     * @param bookingPersistenceManager
     *            - to allow bookings to be persisted in the database
     * @param groupManager
     *            - to allow communication with the group manager.
     * @param emailManager
     *            - email manager
     * @param userManager
     *            - the user manager object
     * @param userAssociationManager
     *            - the userAssociationManager manager object
     */
    @Inject
    public EventBookingManager(final EventBookingPersistenceManager bookingPersistenceManager, final GroupManager groupManager, final EmailManager emailManager, final UserAccountManager userManager,
                               final UserAssociationManager userAssociationManager) {
        this.bookingPersistenceManager = bookingPersistenceManager;
        this.groupManager = groupManager;
        this.emailManager = emailManager;
        this.userManager = userManager;
        this.userAssociationManager = userAssociationManager;
    }

    /**
     * @param userId
     *            - user of interest.
     * @return events
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public List<EventBookingDTO> getEventsByUserId(final Long userId) throws SegueDatabaseException {
        return this.bookingPersistenceManager.getEventsByUserId(userId);
    }

    /**
     * @param bookingId
     *            - of interest
     * @return event booking
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public EventBookingDTO getBookingById(final Long bookingId) throws SegueDatabaseException {
        return this.bookingPersistenceManager.getBookingById(bookingId);
    }

    /**
     * @return event bookings
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public List<EventBookingDTO> getAllBookings() throws SegueDatabaseException {
        return this.bookingPersistenceManager.getAllBookings();
    }

    /**
     * @param eventId
     *            - of interest
     * @return event bookings
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public List<EventBookingDTO> getBookingByEventId(final String eventId) throws SegueDatabaseException {
        return this.bookingPersistenceManager.getBookingByEventId(eventId);
    }

    /**
     * Create booking on behalf of a user.
     * This method will allow users to 'force' book a user onto an event.
     * @param event
     *            - of interest
     * @param user
     *            - user to book on to the event.
     * @return the newly created booking.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public EventBookingDTO createBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user, final Map<String, String> additionalEventInformation) throws SegueDatabaseException, DuplicateBookingException, EventIsFullException {
        // check if already booked
        if (this.isUserBooked(event.getId(), user.getId())) {
            throw new DuplicateBookingException(String.format("Unable to book onto event (%s) as user (%s) is already booked on to it.", event.getId(), user.getEmail()));
        }

        try {
            // Obtain an exclusive database lock to lock the booking
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());

            this.ensureCapacity(event, user);

            return this.bookingPersistenceManager.createBooking(event.getId(), user.getId(), BookingStatus.CONFIRMED, additionalEventInformation);
        } finally {
            // release lock.
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }
    }

    /**
     * Attempt to book onto an event.
     * This method will allow users to 'force' book a user onto an event.
     * @param event
     *            - of interest
     * @param user
     *            - user to book on to the event.
     * @param additionalEventInformation - any additional information for the event organisers (nullable)
     * @return the newly created booking.
     * @throws SegueDatabaseException - if there is a database error
     * @throws EmailMustBeVerifiedException - if this method requires a validated e-mail address.
     * @throws DuplicateBookingException - Duplicate booking, only unique bookings.
     * @throws RoleNotAuthorisedException - You have to be a particular role
     * @throws EventIsFullException - No space on the event
     * @throws EventDeadlineException - The deadline for booking has passed.
	 */
    public EventBookingDTO requestBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user, Map<String, String> additionalEventInformation) throws SegueDatabaseException, EmailMustBeVerifiedException, DuplicateBookingException, RoleNotAuthorisedException, EventIsFullException, EventDeadlineException {
        final boolean isStudentEvent = event.getTags().contains("student");
        final boolean isTeacherEvent = event.getTags().contains("teacher");

        final Date now = new Date();

        // check if the deadline has expired or not - if the end date has passed
        if (event.getBookingDeadline() != null && now.after(event.getBookingDeadline())
            || event.getEndDate() != null && now.after(event.getEndDate())
            || event.getDate() != null && now.after(event.getDate())) {
            throw new EventDeadlineException("The event deadline has passed.");
        }

        // check if already booked
        if (this.isUserBooked(event.getId(), user.getId())) {
            throw new DuplicateBookingException(String.format("Unable to book onto event (%s) as user (%s) is already booked on to it.", event.getId(), user.getEmail()));
        }

        if (isTeacherEvent && !Role.TEACHER.equals(user.getRole())) {
            throw new RoleNotAuthorisedException(String.format("Unable to book onto event (%s) as user (%s) must be a teacher.", event.getId(), user.getEmail()));
        }

        // must have verified email
        if (!EmailVerificationStatus.VERIFIED.equals(user.getEmailVerificationStatus())) {
            throw new EmailMustBeVerifiedException(String.format("Unable to book onto event (%s) without a verified email address for user (%s).",
                event.getId(), user.getEmail()));
        }

        // is there space on the event? Teachers don't count for student events.
        // work out capacity information for the event at this moment in time.
        try {
            // Obtain an exclusive database lock to lock the event
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());

            this.ensureCapacity(event, user);

            // attempt to book them on the event
            EventBookingDTO booking = null;

            // attempt to book them on the event
            if (this.hasBookingWithStatus(event.getId(), user.getId(), BookingStatus.CANCELLED)) {
                // if the user has previously cancelled we should let them book again.
                booking = this.bookingPersistenceManager.updateBookingStatus(event.getId(), user.getId(), BookingStatus.CONFIRMED, additionalEventInformation);
            } else {
                booking = this.bookingPersistenceManager.createBooking(event.getId(), user.getId(), BookingStatus.CONFIRMED, additionalEventInformation);
            }

            try {
                this.emailManager.sendEventWelcomeEmail(user, event);
            } catch (ContentManagerException e) {
                log.error(String.format("Unable to send welcome email (%s) to user (%s)", event.getId(), user.getEmail()), e);
            }

            // auto add them to the group and grant the owner permission
            if (event.getIsaacGroupToken() != null) {
                try {
                    this.userAssociationManager.createAssociationWithToken(event.getIsaacGroupToken(), user);
                } catch (InvalidUserAssociationTokenException e) {
                    log.error(String.format("Unable to auto add user (%s) using token (%s) as the token is invalid.", user.getEmail(), event.getIsaacGroupToken()));
                }
            }

            return booking;
        } finally {
            // release lock
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }
    }

    /**
     * Attempt to book onto an event.
     * This method will allow users to 'force' book a user onto an event.
     * @param event
     *            - of interest
     * @param user
     *            - user to book on to the event.
     * @return the newly created booking.
     * @throws SegueDatabaseException - if there is a database error
     * @throws EmailMustBeVerifiedException - if this method requires a validated e-mail address.
     * @throws DuplicateBookingException - Duplicate booking, only unique bookings.
     * @throws RoleNotAuthorisedException - You have to be a particular role
     * @throws EventIsFullException - No space on the event
     * @throws EventDeadlineException - The deadline for booking has passed.
     */
    public EventBookingDTO requestWaitingListBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user, final Map<String, String> additionalInformation) throws SegueDatabaseException, EmailMustBeVerifiedException, DuplicateBookingException, RoleNotAuthorisedException, EventDeadlineException, EventIsNotFullException {
        final boolean isStudentEvent = event.getTags().contains("student");
        final boolean isTeacherEvent = event.getTags().contains("teacher");

        final Date now = new Date();

        // check if if the end date has passed. Allowed to add to wait list after deadline.
        if (event.getEndDate() != null && now.after(event.getEndDate())
            || event.getDate() != null && now.after(event.getDate())) {
            throw new EventDeadlineException("The event deadline has passed.");
        }

        // check if already booked
        if (this.isUserBooked(event.getId(), user.getId()) || this.hasBookingWithStatus(event.getId(), user.getId(), BookingStatus.WAITING_LIST)) {
            throw new DuplicateBookingException(String.format("Unable to book onto event (%s) as user (%s) is already booked on to it.", event.getId(), user.getEmail()));
        }

        if (isTeacherEvent && !Role.TEACHER.equals(user.getRole())) {
            throw new RoleNotAuthorisedException(String.format("Unable to book onto event (%s) as user (%s) must be a teacher.", event.getId(), user.getEmail()));
        }

        // must have verified email
        if (!EmailVerificationStatus.VERIFIED.equals(user.getEmailVerificationStatus())) {
            throw new EmailMustBeVerifiedException(String.format("Unable to book onto event (%s) without a verified email address for user (%s).",
                event.getId(), user.getEmail()));
        }

        // is there space on the event? Teachers don't count for student events.
        // work out capacity information for the event at this moment in time.
        try {
            // Obtain an exclusive database lock to lock the event
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());

            Integer numberOfPlaces = getPlacesAvailable(event);
            if (numberOfPlaces != null) {
                // check the number of places - if some available then check if the event deadline has passed. If not throw error.
                if (numberOfPlaces > 0 && !(event.getBookingDeadline() != null && now.after(event.getBookingDeadline()))) {
                    throw new EventIsNotFullException("There are still spaces on this event. Please attempt to book on it.");
                }
            }

            EventBookingDTO booking = null;

            // attempt to book them on the event
            if (this.hasBookingWithStatus(event.getId(),user.getId(),BookingStatus.CANCELLED)) {
                // if the user has previously cancelled we should let them book again.
                booking = this.bookingPersistenceManager.updateBookingStatus(event.getId(), user.getId(), BookingStatus.WAITING_LIST, additionalInformation);
            } else {
                booking = this.bookingPersistenceManager.createBooking(event.getId(), user.getId(), BookingStatus.WAITING_LIST, additionalInformation);
            }

            try {
                this.emailManager.sendEventWaitingListEmail(user, event);
            } catch (ContentManagerException e) {
                log.error(String.format("Unable to send welcome email (%s) to user (%s)", event.getId(), user.getEmail()), e);
            }

            return booking;
        } finally {
            // release lock
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }
    }

    /**
     * Allows an admin user to promote someone from the waiting list to a confirmed booking.
     *
     * @param event - The event in question.
     * @param userDTO - The user whose booking should be updated
     * @return the updated booking.
     * @throws SegueDatabaseException
     * @throws EmailMustBeVerifiedException
     * @throws DuplicateBookingException
     * @throws RoleNotAuthorisedException
     * @throws EventDeadlineException
     * @throws EventIsNotFullException
     * @throws EventBookingUpdateException
	 */
    public EventBookingDTO promoteFromWaitingList(final IsaacEventPageDTO event, final RegisteredUserDTO userDTO, final Map<String, String> additionalInformation) throws SegueDatabaseException, EmailMustBeVerifiedException, DuplicateBookingException, RoleNotAuthorisedException, EventBookingUpdateException, EventIsFullException {
        this.bookingPersistenceManager.acquireDistributedLock(event.getId());

        final EventBookingDTO eventBooking = this.bookingPersistenceManager.getBookingByEventIdAndUserId(event.getId(), userDTO.getId());
        if (null == eventBooking) {
            throw new EventBookingUpdateException("Unable to promote a booking that doesn't exist.");
        }

        if (!BookingStatus.WAITING_LIST.equals(eventBooking.getBookingStatus())) {
            throw new EventBookingUpdateException("Unable to promote a booking that isn't on the waiting list.");
        }

        if (this.getPlacesAvailable(event, true) <=  0) {
            throw new EventIsFullException("The event you are attempting promote a booking for is at or over capacity.");
        }

        EventBookingDTO updatedStatus = null;

        // probably want to send a waiting list promotion email.
        try {
            this.emailManager.sendEventWelcomeEmailForWaitingListPromotion(userDTO, event);
            updatedStatus = this.bookingPersistenceManager.updateBookingStatus(eventBooking.getEventId(), userDTO.getId(), BookingStatus.CONFIRMED, additionalInformation);
        } catch (ContentManagerException e) {
            log.error(String.format("Unable to send welcome email (%s) to user (%s)", event.getId(), userDTO.getEmail()), e);
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
     *
     * This assumption allows waiting list bookings to be manually changed into CONFIRMED by event
     * managers without the possibility of someone creating a new booking to occupy the space.
     *
     * It also assumes teachers don't count on student events.
     *
     * @param event - the event we care about
     * @return the number of places available or Null if there is no limit. If a negative number would be returned
     * the method will only return 0. This allows for manual overbooking.
     */
    public Integer getPlacesAvailable(final IsaacEventPageDTO event) throws SegueDatabaseException {
        return this.getPlacesAvailable(event, false);
    }

    /**
     * getPlacesAvailable.
     * This method is not threadsafe and will not acquire a lock.
     *
     * It also assumes teachers don't count on student events.
     *
     * @param event - the event we care about
     * @param countOnlyConfirmed - if true only count confirmed bookings (i.e. ignore waiting list ones.
     * @return the number of places available or Null if there is no limit. If a negative number would be returned
     * the method will only return 0. This allows for manual overbooking.
     */
    public Integer getPlacesAvailable(final IsaacEventPageDTO event, final boolean countOnlyConfirmed) throws SegueDatabaseException {
        boolean isStudentEvent = event.getTags().contains("student");

        // or use stored procedure that can fail?
        Integer numberOfPlaces = event.getNumberOfPlaces();
        if (null == numberOfPlaces) {
            return null;
        }

        List<EventBookingDTO> getCurrentBookings = this.getBookingByEventId(event.getId());

        int studentCount = 0;
        int totalBooked = 0;

        for (EventBookingDTO booking : getCurrentBookings) {
            // TODO: In the future we may want to automatically promote users from the wait list?
            // This was not done initially as it was unclear whether event managers wanted to hand pick
            // wait list candidates to promote.

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
     * @param eventId
     *            - of interest
     * @param userId
     *            - of interest.
     * @return true if a booking exists false if not
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public boolean isUserBooked(final String eventId, final Long userId) throws SegueDatabaseException {
        return this.bookingPersistenceManager.isUserBooked(eventId, userId);
    }

    /**
     * Find out if a user has a booking with a given status.
     *
     * @param eventId
     *            - of interest
     * @param userId
     *            - of interest.
     * @param bookingStatus - the status of the booking.
     * @return true if a waitinglist booking exists false if not
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public boolean hasBookingWithStatus(final String eventId, final Long userId, final BookingStatus bookingStatus) throws SegueDatabaseException {
        try {
            EventBookingDTO eb = this.bookingPersistenceManager.getBookingByEventIdAndUserId(eventId, userId);
            if (null == eb) {
                return false;
            }
            return bookingStatus.equals(eb.getBookingStatus());
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    /**
     * Cancel a booking.
     *
     * Note: cancelled bookings no longer occupy space on an events capacity calculations.
     *
     * @param event
     *            - event
     * @param user
     *            - user to unbook
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public void cancelBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user) throws SegueDatabaseException, ContentManagerException {
        try {
            // Obtain an exclusive database lock to lock the booking
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());
            this.bookingPersistenceManager.updateBookingStatus(event.getId(), user.getId(), BookingStatus.CANCELLED, null);
            this.emailManager.sendEventCancellationEmail(user, event);
        } finally {
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }
    }

    /**
     * Delete a booking permanently.
     *
     * @param eventId
     *            - event id
     * @param userId
     *            - user id to unbook
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    public void deleteBooking(final String eventId, final Long userId) throws SegueDatabaseException {
        try {
            // Obtain an exclusive database lock to lock the booking
            this.bookingPersistenceManager.acquireDistributedLock(eventId);
            this.bookingPersistenceManager.deleteBooking(eventId, userId);
            // TODO: what do we do about people cancelling and those on a waiting list?
        } finally {
            this.bookingPersistenceManager.releaseDistributedLock(eventId);
        }
    }

	/**
	 * Helper method to ensure that that the booking would not violate space restrictions on the event.
     *
     * If it does an exception will be thrown if a new booking wouldn't no exception will be thrown.
     *
     * @param event the event the user wants to book on to
     * @param user the user who is trying to be booked onto the event.
     * @throws SegueDatabaseException - if an error occurs
     * @throws EventIsFullException - if the event is full according to the event rules established.
     */
    private void ensureCapacity(IsaacEventPageDTO event, RegisteredUserDTO user) throws SegueDatabaseException, EventIsFullException {
        final boolean isStudentEvent = event.getTags().contains("student");
        Integer numberOfPlaces = getPlacesAvailable(event);
        if (numberOfPlaces != null) {
            // teachers can book on student events and do not count towards capacity
            if ((isStudentEvent && !Role.TEACHER.equals(user.getRole()) && numberOfPlaces <= 0)
                || (!isStudentEvent && numberOfPlaces <= 0)) {
                throw new EventIsFullException(String.format("Unable to book user (%s) onto event (%s) as it is full.", user.getEmail(), event.getId()));
            }
        }
    }
}