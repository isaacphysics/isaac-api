package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilMarkingResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.Date;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;


/**
 * Manager for Isaac Skills Practice app interactions.
 */
public class SkillsManager {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String hmacSecret;

    /**
     * Constructor.
     *
     * @param properties - application configuration
     */
    @Inject
    public SkillsManager(final AbstractConfigLoader properties) {
        this.hmacSecret = properties.getProperty(SKILLS_HMAC_SECRET);
    }

    /**
     * Parses and validates the raw JSON body from the external marking server.
     *
     * @param body - the raw JSON request body
     * @return the deserialised marking response
     * @throws InvalidMarkingResponseException if the body is missing or malformed
     */
    public AnvilMarkingResponseDTO parseResponse(final String body) throws InvalidMarkingResponseException {
        try {
            return objectMapper.readValue(body, AnvilMarkingResponseDTO.class);
        } catch (final JsonProcessingException e) {
            throw new InvalidMarkingResponseException("Invalid JSON object submitted");
        }
    }

    /**
     * Verifies that the HMAC in the DTO matches the expected signature of the payload.
     *
     * @param dto - the parsed marking response
     * @return whether the hmac was valid
     */
    public boolean isHmacValid(final AnvilMarkingResponseDTO dto) {
        String expected = UserAuthenticationManager.calculateHMAC(hmacSecret, dto.getPayload());
        return expected.equals(dto.getHmac());
    }

    /**
     * Validates the content of the signed payload string.
     *
     * @param payloadStr - the payload string from the marking response
     * @param userId     - the ID of the currently authenticated user
     * @throws InvalidMarkingResponseException if the payload is malformed, the user ID does not match,
     *                                         or the timestamp is outside the allowed window
     */
    public void validatePayload(final String payloadStr, final long userId) throws InvalidMarkingResponseException {
        try {
            AnvilPayloadDTO dto = objectMapper.readValue(payloadStr, AnvilPayloadDTO.class);
            if (dto.getUserId() != userId) {
                throw new InvalidMarkingResponseException("Payload user_id does not match session");
            }
            if (dto.getTimestamp().before(new Date(System.currentTimeMillis() - 300_000L))) {
                throw new InvalidMarkingResponseException("Payload timestamp is outside the allowed window");
            }
        } catch (final JsonProcessingException e) {
            throw new InvalidMarkingResponseException("Invalid payload");
        }
    }
}
