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
package uk.ac.cam.cl.dtg.isaac.dto.eventbookings;

import java.util.Date;
import java.util.Map;

import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

/**
 * @author sac92
 *
 */
public class EventBookingDTO {
    private Long bookingId;

    private UserSummaryDTO userBooked;

    private UserSummaryDTO reservedBy;

    private String eventId;

    private String eventTitle;

    private Date eventDate;

    private BookingStatus bookingStatus;

    private Date lastUpdated;

    private Date bookingDate;

    private Map<String, String> additionalInformation;

    /**
     * EventBookingDTO.
     */
    public EventBookingDTO() {

    }

    /**
     * @param bookingId
     *            - id of the booking
     * @param userBooked
     *            - the user summary of the user booked on the event.
     * @param eventId
     *            - the event id
     * @param eventTitle
     *            - event title
     * @param eventDate
     *            - date of the event
     * @param bookingDate
     *            booking date.
     */
    public EventBookingDTO(final Long bookingId, final UserSummaryDTO userBooked, final UserSummaryDTO reservedBy,
                           final String eventId, final String eventTitle, final Date eventDate, final Date bookingDate,
                           final Date lastUpdated, final BookingStatus status) {
        this.bookingId = bookingId;
        this.userBooked = userBooked;
        this.reservedBy = reservedBy;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.eventDate = eventDate;
        this.bookingStatus = status;
        this.lastUpdated = lastUpdated;
        this.bookingDate = bookingDate;
    }

    /**
     * Gets the bookingId.
     * 
     * @return the bookingId
     */
    public Long getBookingId() {
        return bookingId;
    }

    /**
     * Sets the bookingId.
     * 
     * @param bookingId
     *            the bookingId to set
     */
    public void setBookingId(final Long bookingId) {
        this.bookingId = bookingId;
    }

    /**
     * Gets the userBooked.
     * 
     * @return the userBooked
     */
    public UserSummaryDTO getUserBooked() {
        return userBooked;
    }

    /**
     * Sets the userBooked.
     * 
     * @param userBooked
     *            the userBooked to set
     */
    public void setUserBooked(final UserSummaryDTO userBooked) {
        this.userBooked = userBooked;
    }

    /**
     * Gets the user who created the reservation.
     *
     * @return the user who created the reservation.
     */
    public UserSummaryDTO getReservedBy() {
        return reservedBy;
    }

    /**
     * Sets the user who created the reservation.
     *
     * @param reservedBy - the user who created the reservation.
     */
    public void setReservedBy(UserSummaryDTO reservedBy) {
        this.reservedBy = reservedBy;
    }

    /**
     * Gets the eventId.
     * 
     * @return the eventId
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets the eventId.
     * 
     * @param eventId
     *            the eventId to set
     */
    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets the eventTitle.
     * 
     * @return the eventTitle
     */
    public String getEventTitle() {
        return eventTitle;
    }

    /**
     * Sets the eventTitle.
     * 
     * @param eventTitle
     *            the eventTitle to set
     */
    public void setEventTitle(final String eventTitle) {
        this.eventTitle = eventTitle;
    }

    /**
     * Gets the eventDate.
     * 
     * @return the eventDate
     */
    public Date getEventDate() {
        return eventDate;
    }

    /**
     * Sets the eventDate.
     * 
     * @param eventDate
     *            the eventDate to set
     */
    public void setEventDate(final Date eventDate) {
        this.eventDate = eventDate;
    }

    /**
     * Gets the bookingDate.
     * 
     * @return the bookingDate
     */
    public Date getBookingDate() {
        return bookingDate;
    }

    /**
     * Sets the bookingDate.
     * 
     * @param bookingDate
     *            the bookingDate to set
     */
    public void setBookingDate(final Date bookingDate) {
        this.bookingDate = bookingDate;
    }

	/**
     * Get the lastUpdate date.
     * @return
     */
    public Date getUpdated() {
        return lastUpdated;
    }

	/**
     * Set the lastUpdate Date.
     * @param lastUpdated - date it was updated.
     */
    public void setUpdated(final Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

	/**
	 * Get the booking status.
     * @return the status of the booking.
     */
    public BookingStatus getBookingStatus() {
        return bookingStatus;
    }

	/**
     * Set the status of the booking.
     * @param bookingStatus - the status to set.
     */
    public void setBookingStatus(final BookingStatus bookingStatus) {
        this.bookingStatus = bookingStatus;
    }

	/**
     * Get additional event booking information.
     *
     * @return a map representing additional booking information needed to process the booking.
     */
    public Map<String, String> getAdditionalInformation() {
        return additionalInformation;
    }

	/**
     * Set the additional information for an event booking.
     *
     * @param additionalInformation
     */
    public void setAdditionalInformation(final Map<String, String> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }
}
