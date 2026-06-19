package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing the request received from the external Anvil marking server.
 */
public class AnvilMarkingRequestDTO {
    private final String payload;
    private final String hmac;

    /**
     * Constructor.
     *
     * @param payload - the signed payload from the marking server
     * @param hmac    - the HMAC-SHA256 hex digest authenticating the payload
     */
    @JsonCreator
    public AnvilMarkingRequestDTO(
            @JsonProperty(value = "payload", required = true) final String payload,
            @JsonProperty(value = "hmac", required = true) final String hmac) {
        this.payload = payload;
        this.hmac = hmac;
    }

    /**
     * Gets the payload.
     *
     * @return the signed payload string
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Gets the HMAC signature.
     *
     * @return the HMAC-SHA256 hex digest
     */
    public String getHmac() {
        return hmac;
    }
}
