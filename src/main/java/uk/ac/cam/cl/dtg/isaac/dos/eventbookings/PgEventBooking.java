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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * @author sac92
 *
 */
public class PgEventBooking implements EventBooking {
    private PostgresSqlDb ds;
    private Long bookingId;
    private Long userId;
    private String eventId;
    private Date created;

    /**
     * Partial Constructor - the remaining fields will be populated.
     * 
     * @param ds
     *            - connection to the database.
     * @param bookingId
     *            - unique identifier for this booking
     * @throws SegueDatabaseException
     *             - if we cannot complete the operation.
     */
    public PgEventBooking(final PostgresSqlDb ds, final Long bookingId) throws SegueDatabaseException {
        this.ds = ds;
        this.bookingId = bookingId;
        this.populateBookingDetails();
    }

    /**
     * Full constructor will not populate the fields.
     * 
     * @param ds
     *            - connection to the database.
     * @param bookingId
     *            - unique identifier for this booking
     * @param userId
     *            - user who is booked on to the event.
     * @param eventId
     *            - event that the user is booked on to.
     * @param created
     *            - the date the booking was made.
     */
    public PgEventBooking(final PostgresSqlDb ds, final Long bookingId, final Long userId, final String eventId,
            final Date created) {
        this.ds = ds;
        this.bookingId = bookingId;
        this.userId = userId;
        this.eventId = eventId;
        this.created = created;
    }

    @Override
    public Long getId() {

        return bookingId;
    }

    @Override
    public Long getUserId() {
        return userId;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public Date getCreationDate() {
        return created;
    }

    /**
     * populateBookingDetails - will attempt to populate missing details.
     * 
     * @throws SegueDatabaseException
     *             - if we cannot complete the operation.
     */
    private void populateBookingDetails() throws SegueDatabaseException {
        if (isPopulated()) {
            // do not augment.
            return;
        }

        try (Connection conn = ds.getDatabaseConnection()) {
            PreparedStatement pst;
            if (bookingId != null) {
                pst = conn.prepareStatement("Select * FROM event_bookings WHERE id = ?");
                pst.setLong(1, bookingId);
            } else if (userId != null && eventId != null) {
                pst = conn.prepareStatement("Select * FROM event_bookings WHERE user_id = ? AND event_id = ?");
                pst.setLong(1, userId);
                pst.setString(2, eventId);
            } else {
                throw new IllegalArgumentException(
                        "You must provide an object with either a userId and eventId or a booking id.");
            }

            ResultSet results = pst.executeQuery();
            int count = 0;
            while (results.next()) {
                if (count > 1) {
                    throw new IllegalArgumentException("More than one result detected.");
                }
                count++;

                this.bookingId = results.getLong("id");
                this.eventId = results.getString("event_id");
                this.userId = results.getLong("user_id");
                this.created = results.getDate("created");
            }

            if (count == 0) {
                throw new ResourceNotFoundException("Unable to locate the Event booking you requested");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to fully populate the Booking Details object.");
        }
    }

    /**
     * Check if the event booking has been populated.
     * 
     * @return true if the fields are not null, false if one or more are null.
     */
    private boolean isPopulated() {
        if (bookingId != null && userId != null && eventId != null && created != null) {
            // do not augment.
            return true;
        }
        return false;
    }

}
