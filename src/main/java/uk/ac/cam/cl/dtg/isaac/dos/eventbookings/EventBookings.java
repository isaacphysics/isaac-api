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
package uk.ac.cam.cl.dtg.isaac.dos.eventbookings;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import javax.annotation.Nullable;
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
     * @param eventId
     *            - the event id
     * @param userId
     *            - the user id
     * @param status
     *            - the initial status of the booking.
     * @param additionalInformation - additional information required for the event.
     * @return the newly created booking
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    EventBooking add(final String eventId, final Long userId, final BookingStatus status, final Map<String,String> additionalInformation) throws SegueDatabaseException;

	/**
     * updateStatus.
     *
     * @param eventId - the id of the event
     * @param userId - the id of the user booked on to the event
     * @param status - the new status to change the booking to
     * @param additionalEventInformation - additional information required for the event if null it will be unmodified.
     * @return the newly updated event booking.
     * @throws SegueDatabaseException - if the database goes wrong.
     */
    void updateStatus(final String eventId, final Long userId, final BookingStatus status, Map<String, String> additionalEventInformation) throws SegueDatabaseException;

    /**
     * Remove booking from the database.
     * 
     * @param eventId
     *            - the event id
     * @param userId
     *            - the user id
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    void delete(final String eventId, final Long userId) throws SegueDatabaseException;

    /**
     * Acquire a globally unique database lock.
     * This lock must be released manually.
     * @param resourceId - the unique id for the object to be locked.
     */
    void acquireDistributedLock(String resourceId) throws SegueDatabaseException;

    /**
     * Release a globally unique database lock.
     * This method will release a previously acquired lock.
     *
     * @param resourceId - the unique id for the object to be locked.
     */
    void releaseDistributedLock(String resourceId) throws SegueDatabaseException;

    /**
     * get all events.
     * 
     * @return an iterable with all events in it.
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    Iterable<EventBooking> findAll() throws SegueDatabaseException;

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
}
