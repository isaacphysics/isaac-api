package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing the response received from the external Anvil marking server.
 */
public class AnvilMarkingResponseDTO {
    private final String payload;

    /**
     * Constructor.
     *
     * @param payload - the signed payload from the marking server
     */
    @JsonCreator
    public AnvilMarkingResponseDTO(@JsonProperty(value = "payload", required = true) final String payload) {
        this.payload = payload;
    }

    /**
     * Gets the payload.
     *
     * @return the signed payload string
     */
    public String getPayload() {
        return payload;
    }
}
