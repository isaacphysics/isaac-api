package uk.ac.cam.cl.dtg.isaac.dto.eventbookings;

import java.util.Map;

public class DetailedEventBookingDTO extends EventBookingDTO {
  private Map<String, String> additionalInformation;

  public DetailedEventBookingDTO() {

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
   * @param additionalInformation a Map of additional information to associate with an event booking
   */
  public void setAdditionalInformation(final Map<String, String> additionalInformation) {
    this.additionalInformation = additionalInformation;
  }
}
