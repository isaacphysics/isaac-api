package uk.ac.cam.cl.dtg.isaac.dao;

import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.ISkillsAttemptManager;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.SQLException;
import java.sql.Timestamp;

public class PgSkillsAttemptManager implements ISkillsAttemptManager {
    private final PostgresSqlDb database;

    @Inject
    public PgSkillsAttemptManager(final PostgresSqlDb database) {
        this.database = database;
    }

    @Override
    public void registerSkillsAttempt(final AnvilPayloadDTO attempt) {
        try (var conn = database.getDatabaseConnection();
             var pst = conn.prepareStatement("""
                 INSERT INTO skills_question_attempts (
                     id, user_id, skill_assignment_id, skill_id, subskill_id, question, question_attempt, marks,
                     timestamp
                 ) VALUES (?, ?, ?, ?, ?, (?::jsonb), (?::jsonb), ?, ?)""")) {
            pst.setString(1, attempt.getId().toString());
            pst.setLong(2, attempt.getUserId());
            pst.setString(3, attempt.getSkillAssignmentId());
            pst.setString(4, attempt.getSkillId());
            pst.setString(5, attempt.getSubskillId());
            pst.setString(6, attempt.getQuestion());
            pst.setString(7, attempt.getQuestionAttempt());
            pst.setInt(8, (Integer) attempt.getMarks());
            pst.setTimestamp(9, new Timestamp(attempt.getTimestamp().getTime()));
            pst.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to record skills attempt", e);
        }
    }
}
