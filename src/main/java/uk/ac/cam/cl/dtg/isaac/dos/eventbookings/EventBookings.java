/*
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

import uk.ac.cam.cl.dtg.isaac.dos.ITransaction;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import jakarta.annotation.Nullable;
import java.util.Map;

/**
 * Representation of a collection of event bookings.
 * 
 * This should be a database aware object.
 * 
 * @author sac92
 */
public interface EventBookings {

    /**
     * Add booking to the database.
     *
     * @param transaction - the database transaction to use
     * @param eventId - the event id
     * @param userId - the user id
     * @param reservedById - the user id of who made the reservation (can be null)
     * @param status - the initial status of the booking.
     * @param additionalInformation - additional information required for the event.
     * @return the newly created booking
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    EventBooking add(ITransaction transaction, String eventId, Long userId, Long reservedById, BookingStatus status,
                     Map<String, String> additionalInformation) throws SegueDatabaseException;

    /**
     * Add booking to the database.
     *
     * @param transaction - the database transaction to use
     * @param eventId - the event id
     * @param userId - the user id
     * @param status - the initial status of the booking.
     * @param additionalInformation - additional information required for the event.
     * @return the newly created booking
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    EventBooking add(ITransaction transaction, String eventId, Long userId, BookingStatus status,
                     Map<String, String> additionalInformation) throws SegueDatabaseException;

	/**
     * updateStatus.
     *
     * @param transaction - the database transaction to use
     * @param eventId - the id of the event
     * @param userId - the id of the user booked on to the event
     * @param reservingUserId - the id of the user making the reservation
     * @param status - the new status to change the booking to
     * @param additionalEventInformation - additional information required for the event if null it will be unmodified.
     * @throws SegueDatabaseException - if the database goes wrong.
     */
    void updateStatus(ITransaction transaction, String eventId, Long userId, Long reservingUserId, BookingStatus status,
                      Map<String, String> additionalEventInformation) throws SegueDatabaseException;

    /**
     * Remove booking from the database.
     *
     * @param transaction
     *            - the database transaction to use
     * @param eventId
     *            - the event id
     * @param userId
     *            - the user id
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    void delete(ITransaction transaction, String eventId, Long userId) throws SegueDatabaseException;

    /**
     * Acquire a globally unique lock on an event for the duration of a transaction.
     *
     * @param transaction - the database transaction to acquire the lock in.
     * @param resourceId - the ID of the event to be locked.
     */
    void lockEventUntilTransactionComplete(ITransaction transaction, String resourceId) throws SegueDatabaseException;

    /**
     * Find all bookings for a given event.
     * 
     * @param eventId
     *            - the event of interest.
     * @return an iterable with all the events matching the criteria.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    Iterable<EventBooking> findAllByEventId(final String eventId) throws SegueDatabaseException;

    /**
     * countAllEventBookings.
     *
     * Note: This will include any with users who have been deleted.
     *
     * @return the current count of all event bookings.
     * @throws SegueDatabaseException if there is a problem accessing the db
     */
    Long countAllEventBookings() throws SegueDatabaseException;

    /**
     * For a given event provide a count of the current bookings and their statuses.
     *
     * @param eventId - the event id we care about
     * @param includeDeletedUsersInCounts - true will include deleted users in the numbers, false will not.
     * @return Map of booking status to number of bookings for the event.
     * @throws SegueDatabaseException - if there is a problem accessing the db
     */
    Map<BookingStatus, Map<Role, Long>> getEventBookingStatusCounts(String eventId, boolean includeDeletedUsersInCounts) throws SegueDatabaseException;

    /**
     * Find all bookings for a given event with a given status.
     *
     * Useful for finding all on a waiting list or confirmed.
     *
     * @param eventId
     *            - the event of interest.
     * @param status
     *            - The event status that should match in the bookings returned.
     * @return an iterable with all the events matching the criteria.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    Iterable<EventBooking> findAllByEventIdAndStatus(String eventId, @Nullable BookingStatus status) throws SegueDatabaseException;

    /**
     * Find all bookings for a given event.
     * 
     * @param userId
     *            - the user of interest.
     * @return an iterable with all the events matching the criteria.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    Iterable<EventBooking> findAllByUserId(final Long userId) throws SegueDatabaseException;


    /**
     * Find all event reservations by a given user.
     *
     * @param userId
     *            - the user of interest.
     * @return an iterable with all the events matching the criteria.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    Iterable<EventBooking> findAllReservationsByUserId(final Long userId) throws SegueDatabaseException;

    /**
     * Find an event booking by event and user id.
     * 
     * @param eventId
     *            - the event of interest.
     * @param userId
     *            - the user of interest.
     * @return the event or an error.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    EventBooking findBookingByEventAndUser(String eventId, Long userId) throws SegueDatabaseException;

    /**
     * Find an event booking by id.
     *
     * @param bookingId - the event ID of interest.
     * @return the booking or an error.
     * @throws SegueDatabaseException - if an error occurs.
     */
    EventBooking findBookingById(Long bookingId) throws SegueDatabaseException;

    /**
     * Expunge the additional information field for all bookings for a given user id.
     *
     * @param userId - user id
     */
    void deleteAdditionalInformation(Long userId) throws SegueDatabaseException;
}
