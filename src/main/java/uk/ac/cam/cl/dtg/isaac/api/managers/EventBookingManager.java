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

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.RoleNotAuthorisedException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Date;
import java.util.List;

/**
 * AssignmentManager.
 */
public class EventBookingManager {
    private static final Logger log = LoggerFactory.getLogger(EventBookingManager.class);

    private EventBookingPersistenceManager bookingPersistenceManager;

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
    public EventBookingDTO createBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user) throws SegueDatabaseException, DuplicateBookingException {
        // check if already booked
        if (this.isUserBooked(event.getId(), user.getId())) {
            throw new DuplicateBookingException(String.format("Unable to book onto event (%s) as user (%s) is already booked on to it.", event.getId(), user.getEmail()));
        }

        try {
            // Obtain an exclusive database lock to lock the booking
            this.bookingPersistenceManager.acquireDistributedLock(event.getId());
            return this.bookingPersistenceManager.createBooking(event.getId(), user.getId());
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
     * @return the newly created booking.
     * @throws SegueDatabaseException - if there is a database error
     * @throws EmailMustBeVerifiedException - if this method requires a validated e-mail address.
     * @throws DuplicateBookingException - Duplicate booking, only unique bookings.
     * @throws RoleNotAuthorisedException - You have to be a particular role
     * @throws EventIsFullException - No space on the event
     * @throws EventDeadlineException - The deadline for booking has passed.
	 */
    public EventBookingDTO requestBooking(final IsaacEventPageDTO event, final RegisteredUserDTO user) throws SegueDatabaseException, EmailMustBeVerifiedException, DuplicateBookingException, RoleNotAuthorisedException, EventIsFullException, EventDeadlineException {
        boolean isStudentEvent = event.getTags().contains("student");
        boolean isTeacherEvent = event.getTags().contains("teacher");
        boolean isVirtual = event.getTags().contains("virtual");

        final Date now = new Date();

        // check if the deadline has expired or not - if the end date has passed
        if (event.getBookingDeadline() != null && now.after(event.getBookingDeadline())
            || event.getEndDate() != null && now.after(event.getEndDate())
            || event.getDate() != null && now.after(event.getDate())) {
            throw new EventDeadlineException("The event deadline () has passed.");
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

            Integer numberOfPlaces = event.getNumberOfPlaces();
            if (null == numberOfPlaces) {
                // if the number of places is null just book on to it as there is no restriction
                return this.bookingPersistenceManager.createBooking(event.getId(), user.getId());
            }

            List<EventBookingDTO> getCurrentBookings = this.getBookingByEventId(event.getId());
            int teacherCount = 0;
            int studentCount = 0;
            int totalBooked = 0;

            for (EventBookingDTO booking : getCurrentBookings) {
                if (booking.getUserBooked().getRole().equals(Role.TEACHER)) {
                    teacherCount++;
                } else if (booking.getUserBooked().getRole() == null || booking.getUserBooked().getRole().equals(Role.STUDENT)) {
                    studentCount++;
                }
                totalBooked++;
            }

            // book teacher on to student event regardless of capacity as they don't count
            if (isStudentEvent && Role.TEACHER.equals(user.getRole())) {
                return this.bookingPersistenceManager.createBooking(event.getId(), user.getId());
            }

            if ((isStudentEvent && studentCount >= numberOfPlaces)
                ||
                totalBooked >= numberOfPlaces) {
                // sorry over booked - do something clever with waiting list.
                throw new EventIsFullException(String.format("Unable to book user (%s) onto event (%s) as it is full (%s/%s).", user.getEmail(), event.getId(), studentCount, event.getNumberOfPlaces()));
            }

            return this.bookingPersistenceManager.createBooking(event.getId(), user.getId());
        } finally {
            // release lock
            this.bookingPersistenceManager.releaseDistributedLock(event.getId());
        }
    }

	/**
     * getPlacesAvailable.
     * This method is not threadsafe and will not acquire a lock.
     *
     * @param event - the event we care about
     * @return the number of places available or Null if there is no limit.
     */
    public Integer getPlacesAvailable(final IsaacEventPageDTO event) throws SegueDatabaseException {
        boolean isStudentEvent = event.getTags().contains("student");
        boolean isTeacherEvent = event.getTags().contains("teacher");
        boolean isVirtual = event.getTags().contains("virtual");

        // or use stored procedure that can fail?
        Integer numberOfPlaces = event.getNumberOfPlaces();
        if (null == numberOfPlaces) {
            return null;
        }

        List<EventBookingDTO> getCurrentBookings = this.getBookingByEventId(event.getId());

        int studentCount = 0;
        int totalBooked = 0;

        for (EventBookingDTO booking : getCurrentBookings) {
            if (booking.getUserBooked().getRole().equals(Role.STUDENT)) {
                studentCount++;
            }
            totalBooked++;
        }

        // capacity of the event
        if (isStudentEvent && studentCount >= numberOfPlaces) {
            return numberOfPlaces - studentCount;
        }

        if (totalBooked > numberOfPlaces) {
            return 0;
        }

        return numberOfPlaces - totalBooked;
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
        return this.bookingPersistenceManager.isUserBooked(eventId, userId);
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
        try {
            // Obtain an exclusive database lock to lock the booking
            this.bookingPersistenceManager.acquireDistributedLock(eventId);
            this.bookingPersistenceManager.deleteBooking(eventId, userId);
        } finally {
            this.bookingPersistenceManager.releaseDistributedLock(eventId);
        }
    }
}