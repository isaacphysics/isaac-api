package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilMarkingResponseDTO;

/**
 * Manager for Isaac Skills Practice app interactions.
 */
public class SkillsManager {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor.
     */
    @Inject
    public SkillsManager() { }

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
}
