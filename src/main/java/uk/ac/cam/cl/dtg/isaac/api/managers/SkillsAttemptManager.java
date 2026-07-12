package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilMarkingRequestDTO;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.ISkillsAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Manager for Isaac Skills Practice app attempts.
 */
public class SkillsAttemptManager {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final int MAX_PAYLOAD_LENGTH = 30 * 1024; // about 30 kilobytes for English payloads, even Unicode
    public static final long FIVE_MINUTES_IN_MILLIS = 300_000L;

    private final String hmacSecret;
    private final ISkillsAttemptPersistenceManager persistence;

    /**
     * Constructor.
     *
     * @param properties          - application configuration
     * @param skillsAttemptManager - persistence manager for skills attempts
     */
    @Inject
    public SkillsAttemptManager(
            final AbstractConfigLoader properties, final ISkillsAttemptPersistenceManager skillsAttemptManager) {
        this.hmacSecret = properties.getProperty(SKILLS_HMAC_SECRET);
        this.persistence = skillsAttemptManager;
    }

    /**
     * Verifies that the HMAC in the DTO matches the expected signature of the payload.
     *
     * @param dto - the parsed marking request
     * @return whether the hmac was valid
     */
    public boolean isAttemptHmacValid(final AnvilMarkingRequestDTO dto) {
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
    public List<AnvilPayloadDTO> parseAttemptsPayload(
        final String payloadStr, final long userId, final String appId
    ) throws InvalidAnvilMarkingRequestException {
        try {
            if (payloadStr.length() > MAX_PAYLOAD_LENGTH) {
                throw new InvalidAnvilMarkingRequestException("Payload too large.", null);
            }

            List<AnvilPayloadDTO> dtos = objectMapper.readValue(payloadStr, new TypeReference<>() {});
            if (dtos.stream().anyMatch(dto -> dto.getUserId() != userId)) {
                throw new InvalidAnvilMarkingRequestException("Payload user_id does not match session.", null);
            }
            if (dtos.stream().anyMatch(
                    dto -> dto.getTimestamp().before(new Date(System.currentTimeMillis() - FIVE_MINUTES_IN_MILLIS)))) {
                throw new InvalidAnvilMarkingRequestException("Payload timestamp is outside the allowed window.", null);
            }
            if (dtos.stream().anyMatch(dto -> !dto.getSkillId().equals(appId))) {
                throw new InvalidAnvilMarkingRequestException("Payload skill_id does not match app.", null);
            }
            return dtos;
        } catch (final JsonProcessingException e) {
            throw new InvalidAnvilMarkingRequestException("Invalid payload.", e.getMessage());
        }
    }

    /**
     * Records a validated skills attempt.
     *
     * @param attempt - the validated payload DTO
     */
    public void recordAttempt(final List<AnvilPayloadDTO> attempt)
            throws DuplicateSkillsAttemptException, SegueDatabaseException {
        persistence.registerSkillsAttempt(attempt);
    }

    /**
     * Returns a users attempts in the mental maths skills app.
     *
     * @param from - return attempts starting from this date (inclusive)
     *
     * @param to - return attempts until this date (exclusive)
     */
    public Map<LocalDate, Long> getMentalMathsAttempts(final LocalDate from, final LocalDate to)
        throws SegueDatabaseException {
        return persistence.getMentalMathsAttempts(from, to);
    }
}
