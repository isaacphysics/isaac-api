package uk.ac.cam.cl.dtg.isaac.dto.eventbookings;

import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryWithEmailAddressDTO;

import java.util.Map;

public class DetailedEventBookingDTO extends EventBookingDTO {
    private Map<String, String> additionalInformation;
    private UserSummaryWithEmailAddressDTO userBooked;

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
     * @param additionalInformation
     */
    public void setAdditionalInformation(final Map<String, String> additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    /**
     * Get userBooked on the event.
     *
     * @return a userSummaryWithEmailAddressDTO for the requested user.
     */
    public UserSummaryWithEmailAddressDTO getUserBooked() {
        return userBooked;
    }

    /**
     * Set the userBooked for an event.
     *
     * @param userBooked
     */
    public void setUserBooked(final UserSummaryWithEmailAddressDTO userBooked) {
        this.userBooked = userBooked;
    }
}
