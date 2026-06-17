package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.apache.commons.lang3.Validate;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * DTO representing the signed payload content from the external Anvil marking server.
 */
public class AnvilPayloadDTO {
    private final UUID id;
    private final Long userId;
    private final String skillAssignmentId;
    private final String skillId;
    private final String subskillId;
    private final Question question;
    private final String questionAttempt;
    private final Number marks;
    private final Date timestamp;

    /**
     * Constructor.
     *
     * @param id               - UUID identifying this attempt
     * @param userId           - the ID of the user who answered the question
     * @param skillAssignmentId - the skill assignment ID (currently always null)
     * @param skillId          - the skill ID, must match the app ID from the URL
     * @param subskillId       - the subskill ID
     * @param question         - the question content
     * @param questionAttempt  - the student's answer
     * @param marks            - the marks awarded (0 or 1)
     * @param timestamp        - the datetime at which the response was signed
     */
    @JsonCreator
    public AnvilPayloadDTO(
            @JsonSetter(nulls = Nulls.FAIL) @JsonProperty(value = "id", required = true) final UUID id,
            @JsonSetter(nulls = Nulls.FAIL) @JsonProperty(value = "user_id", required = true) final Long userId,
            @JsonProperty(value = "skill_assignment_id", required = true) final String skillAssignmentId,
            @JsonSetter(nulls = Nulls.FAIL) @JsonProperty(value = "skill_id", required = true) final String skillId,
            @JsonSetter(nulls = Nulls.FAIL) @JsonProperty(value = "subskill_id", required = true) final String subskillId,
            @JsonSetter(nulls = Nulls.FAIL) @JsonProperty(value = "question", required = true) final Question question,
            @JsonSetter(nulls = Nulls.FAIL) @JsonProperty(value = "question_attempt", required = true) final String questionAttempt,
            @JsonSetter(nulls = Nulls.FAIL) @JsonProperty(value = "marks", required = true) final Number marks,
            @JsonSetter(nulls = Nulls.FAIL) @JsonProperty(value = "timestamp", required = true) final Date timestamp) {
        Validate.isTrue(userId != 0);
        Validate.isTrue(skillAssignmentId == null);
        Validate.isTrue((marks instanceof Integer n) && (n == 0 || n == 1));
        Stream.of(skillId, subskillId, question.answer, question.text).forEach(Validate::notEmpty);

        this.id = id;
        this.userId = userId;
        this.skillAssignmentId = skillAssignmentId;
        this.skillId = skillId;
        this.subskillId = subskillId;
        this.question = question;
        this.questionAttempt = questionAttempt;
        this.marks = marks;
        this.timestamp = timestamp;
    }

    public Long getUserId() { return userId; }
    public String getSkillAssignmentId() { return skillAssignmentId; }
    public String getSkillId() { return skillId; }
    public String getSubskillId() { return subskillId; }
    public String getQuestionAttempt() { return questionAttempt; }
    public Number getMarks() { return marks; }
    public Date getTimestamp() { return timestamp; }

    public record Question(
        @JsonSetter(nulls = Nulls.FAIL) @JsonProperty(value = "text", required = true) String text,
        @JsonSetter(nulls = Nulls.FAIL) @JsonProperty(value = "answer", required = true) String answer
    ) {}
}
