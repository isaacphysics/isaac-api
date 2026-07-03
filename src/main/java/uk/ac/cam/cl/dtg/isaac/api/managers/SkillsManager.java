package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilMarkingRequestDTO;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.ISkillsAttemptManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.security.MessageDigest;
import java.util.Date;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Manager for Isaac Skills Practice app interactions.
 */
public class SkillsManager {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String hmacSecret;
    private final ISkillsAttemptManager skillsAttemptManager;

    /**
     * Constructor.
     *
     * @param properties          - application configuration
     * @param skillsAttemptManager - persistence manager for skills attempts
     */
    @Inject
    public SkillsManager(final AbstractConfigLoader properties, final ISkillsAttemptManager skillsAttemptManager) {
        this.hmacSecret = properties.getProperty(SKILLS_HMAC_SECRET);
        this.skillsAttemptManager = skillsAttemptManager;
    }

    /**
     * Verifies that the HMAC in the DTO matches the expected signature of the payload.
     *
     * @param dto - the parsed marking request
     * @return whether the hmac was valid
     */
    public boolean isHmacValid(final AnvilMarkingRequestDTO dto) {
        String expected = UserAuthenticationManager.calculateHMAC(hmacSecret, dto.getPayload());
        return MessageDigest.isEqual(expected.getBytes(), dto.getHmac().getBytes());
    }

    /**
     * Validates the content of the signed payload string.
     *
     * @param payloadStr - the payload string from the marking request
     * @param userId     - the ID of the currently authenticated user
     * @param appId      - the app ID from the URL, which must match the payload's skill_id
     * @throws InvalidAnvilMarkingRequestException if the payload is malformed or any validation fails
     */
    public AnvilPayloadDTO parsePayload(
        final String payloadStr, final long userId, final String appId
    ) throws InvalidAnvilMarkingRequestException {
        try {
            if (payloadStr.length() > 10 * 1024) {
                throw new InvalidAnvilMarkingRequestException("Payload too large", null);
            }

            AnvilPayloadDTO dto = objectMapper.readValue(payloadStr, AnvilPayloadDTO.class);
            if (dto.getUserId() != userId) {
                throw new InvalidAnvilMarkingRequestException("Payload user_id does not match session", null);
            }
            if (dto.getTimestamp().before(new Date(System.currentTimeMillis() - 300_000L))) {
                throw new InvalidAnvilMarkingRequestException("Payload timestamp is outside the allowed window", null);
            }
            if (!dto.getSkillId().equals(appId)) {
                throw new InvalidAnvilMarkingRequestException("Payload skill_id does not match app", null);
            }
            return dto;
        } catch (final JsonProcessingException e) {
            throw new InvalidAnvilMarkingRequestException("Invalid payload", e.getMessage());
        }
    }

    /**
     * Records a validated skills attempt.
     *
     * @param attempt - the validated payload DTO
     */
    public void recordAttempt(final AnvilPayloadDTO attempt) throws SegueDatabaseException {
        skillsAttemptManager.registerSkillsAttempt(attempt);
    }
}
