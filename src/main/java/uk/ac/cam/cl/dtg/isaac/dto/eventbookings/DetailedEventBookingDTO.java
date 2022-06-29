package uk.ac.cam.cl.dtg.isaac.dto.eventbookings;

import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;

import java.util.Map;

public class DetailedEventBookingDTO extends EventBookingDTO {
    private Map<String, String> additionalInformation;

    // Shadows the parent's userBooked field
    private UserSummaryWithEmailAddressDTO userBooked;

    public DetailedEventBookingDTO() {

    }

    /**
     * Gets the userBooked, with extra detail including the email address.
     *
     * @return the userBooked
     */
    public UserSummaryWithEmailAddressDTO getUserBooked() {
        return userBooked;
    }

    /**
     * Sets the userBooked with extra detail including the email address.
     *
     * @param userBooked
     *            the userBooked to set
     */
    public void setUserBooked(final UserSummaryWithEmailAddressDTO userBooked) {
        this.userBooked = userBooked;
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
