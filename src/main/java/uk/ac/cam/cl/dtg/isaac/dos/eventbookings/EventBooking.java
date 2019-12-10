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

import java.util.Date;
import java.util.Map;

/**
 * EventBooking.
 * 
 * Database aware EventBooking.
 *
 */
public interface EventBooking {

    /**
     * Getter for booking Id.
     * 
     * @return booking Id
     */
    Long getId();

    /**
     * Getter for User Id.
     * 
     * @return User Id
     */
    Long getUserId();

    /**
     * Getter for the Id of the user who made the reservation.
     *
     * @return User Id
     */
    Long getReservedBy();

    /**
     * Getter for event id.
     * 
     * @return event id
     */
    String getEventId();

	/**
     * Gets the current status of the booking.
     *
     * @return booking status
     */
    BookingStatus getBookingStatus();

	/**
     * Get the date that this booking was last updated.
     *
     * @return the date that an update or booking creation was made.
     */
    Date getUpdateDate();

    /**
     * Getter for Creation date.
     * 
     * @return creation date
     */
    Date getCreationDate();

    Map<String, String> getAdditionalInformation();

    void setAdditionalInformation(Map<String, String> additionalInformation);
}
