package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

/**
 * DTO representing the signed payload content from the external Anvil marking server.
 */
public class AnvilPayloadDTO {
    private final long userId;
    private final Date timestamp;

    /**
     * Constructor.
     *
     * @param userId    - the ID of the user who answered the question
     * @param timestamp - the datetime at which the response was signed
     */
    @JsonCreator
    public AnvilPayloadDTO(
            @JsonProperty(value = "user_id", required = true) final long userId,
            @JsonProperty(value = "timestamp", required = true) final Date timestamp) {
        this.userId = userId;
        this.timestamp = timestamp;
    }

    /**
     * Gets the user ID.
     *
     * @return the user ID
     */
    public long getUserId() {
        return userId;
    }

    /**
     * Gets the timestamp.
     *
     * @return the signing timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }
}
