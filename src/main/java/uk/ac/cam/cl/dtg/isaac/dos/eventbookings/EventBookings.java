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
     * @return the newly created booking
     * @throws SegueDatabaseException
     *             - if an error occurs.
     */
    EventBooking add(final String eventId, final Long userId) throws SegueDatabaseException;

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
