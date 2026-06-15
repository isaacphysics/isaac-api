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
             var pst = conn.prepareStatement(
                     "INSERT INTO skills_question_attempts (user_id, timestamp) VALUES (?, ?)")) {
            pst.setLong(1, attempt.getUserId());
            pst.setTimestamp(2, new Timestamp(attempt.getTimestamp().getTime()));
            pst.executeUpdate();
        } catch (final SQLException e) {
            throw new RuntimeException("Failed to record skills attempt", e);
        }
    }
}
