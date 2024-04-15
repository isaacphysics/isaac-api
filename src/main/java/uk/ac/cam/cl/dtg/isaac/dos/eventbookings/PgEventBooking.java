/**
 * Copyright 2015 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dos.eventbookings;

import static uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils.getSharedBasicObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * Postgres specific implementation for event booking.
 */
public class PgEventBooking implements EventBooking {
  private final Long bookingId;
  private final Long userId;
  private final Long reservedById;
  private final String eventId;
  private final BookingStatus bookingStatus;
  private final Instant created;
  private final Instant updated;
  private final Map<String, String> additionalInformation;

  /**
   * Full constructor will not populate the fields.
   *
   * @param bookingId             - unique identifier for this booking
   * @param userId                - user who is booked on to the event.
   * @param reservedById          - the id of the user who created the reservation
   * @param eventId               - event that the user is booked on to.
   * @param bookingStatus         - the status of the booking
   * @param created               - the date the booking was made.
   * @param updated               - the date the booking was last updated
   * @param additionalInformation - additional information to be stored with this booking e.g. dietary requirements.
   */
  public PgEventBooking(final Long bookingId, final Long userId, final Long reservedById, final String eventId,
                        final BookingStatus bookingStatus, final Instant created, final Instant updated,
                        final Object additionalInformation) throws SegueDatabaseException {
    this.bookingId = bookingId;
    this.userId = userId;
    this.reservedById = reservedById;
    this.eventId = eventId;
    this.bookingStatus = bookingStatus;
    this.updated = updated;
    this.created = created;
    if (additionalInformation != null) {
      try {
        this.additionalInformation = this.convertFromJsonbToMap(additionalInformation);
      } catch (IOException e) {
        throw new SegueDatabaseException("Unable to convert object additionalInformation to Map.", e);
      }
    } else {
      this.additionalInformation = null;
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
  public BookingStatus getBookingStatus() {
    return bookingStatus;
  }

  @Override
  public Instant getCreationDate() {
    return created;
  }

  @Override
  public Instant getUpdateDate() {
    return updated;
  }

  @Override
  public Map<String, String> getAdditionalInformation() {
    return additionalInformation;
  }

  private Map<String, String> convertFromJsonbToMap(final Object objectToConvert) throws IOException {
    ObjectMapper mapper = getSharedBasicObjectMapper();
    final String stringVersion = mapper.writeValueAsString(objectToConvert);
    Map<String, String> interimResult = mapper.readValue(stringVersion, HashMap.class);

    // this check is here to see if the map coming in is actually a jsonb map, if not assume it doesn't need to be
    // unpacked
    if (interimResult.containsKey("type") && interimResult.get("type").equals("jsonb")) {
      return mapper.readValue(interimResult.get("value"), HashMap.class);
    }

    return interimResult;
  }
}
