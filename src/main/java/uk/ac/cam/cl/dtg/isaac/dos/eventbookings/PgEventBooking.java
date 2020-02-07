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

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
import org.postgresql.util.PGobject;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * Postgres specific implementation for event booking.
 *
 */
public class PgEventBooking implements EventBooking {
    private PostgresSqlDb ds;

    private Long bookingId;
    private Long userId;
    private Long reservedById;
    private String eventId;
    private BookingStatus bookingStatus;
    private Date created;
    private Date updated;
    private Map<String, String> additionalInformation;

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
    public PgEventBooking(final PostgresSqlDb ds, final Long bookingId, final Long userId, final Long reservedById, final String eventId,
            final BookingStatus bookingStatus, final Date created, final Date updated, final Object additionalInformation) throws SegueDatabaseException {
        this.ds = ds;
        this.bookingId = bookingId;
        this.userId = userId;
        this.reservedById = reservedById;
        this.eventId = eventId;
        this.bookingStatus = bookingStatus;
        this.updated = updated;
        this.created = created;
        if (additionalInformation != null) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                this.additionalInformation = this.convertFromJsonbToMap(additionalInformation);
            } catch (IOException e) {
                throw new SegueDatabaseException("Unable to convert object additionalInformation to Map.", e);
            }
        }

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
    public Long getReservedById() {
        return reservedById;
    }

    @Override
    public String getEventId() {
        return eventId;
    }

    @Override
    public BookingStatus getBookingStatus() { return bookingStatus; }

    @Override
    public Date getCreationDate() {
        return created;
    }

    @Override
    public Date getUpdateDate() {
        return updated;
    }

    @Override
    public Map<String, String> getAdditionalInformation() {
        return additionalInformation;
    }

    @Override
    public void setAdditionalInformation(Map<String, String> additionalInformation) {
        this.additionalInformation = additionalInformation;
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
                pst = conn.prepareStatement("SELECT * FROM event_bookings WHERE id = ?");
                pst.setLong(1, bookingId);
            } else if (userId != null && eventId != null) {
                pst = conn.prepareStatement("SELECT * FROM event_bookings WHERE user_id = ? AND event_id = ?");
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
                this.reservedById = results.getLong("reserved_by");
                this.bookingStatus = BookingStatus.valueOf(results.getString("booking_status"));
                this.created = results.getDate("created");
                this.updated = results.getDate("updated");

                Map<String, String> additionalInfoObject = this.convertFromJsonbToMap(results.getObject("additionalEventInformation"));

                this.additionalInformation = additionalInfoObject;
            }

            if (count == 0) {
                throw new ResourceNotFoundException("Unable to locate the Event booking you requested");
            }

        } catch (SQLException | IOException e) {
            throw new SegueDatabaseException("Unable to fully populate the Booking Details object.", e);
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

    private Map<String, String> convertFromJsonbToMap(Object objectToConvert) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        final String stringVersion = mapper.writeValueAsString(objectToConvert);
        Map<String, String> iterimResult = mapper.readValue(stringVersion, HashMap.class);

        // this check is here to see if the map coming in is actually a jsonb map, if not assume it doesn't need to be
        // unpacked
        if (iterimResult.containsKey("type") && iterimResult.get("type").equals("jsonb")) {
            return mapper.readValue(iterimResult.get("value"), HashMap.class);
        }

        return iterimResult;
    }
}
