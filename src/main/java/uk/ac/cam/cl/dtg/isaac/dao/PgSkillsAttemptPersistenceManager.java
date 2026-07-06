package uk.ac.cam.cl.dtg.isaac.dao;

import com.google.inject.Inject;
import org.postgresql.util.PSQLState;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateSkillsAttemptException;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.ISkillsAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.SQLException;
import java.sql.Timestamp;

/** PostgreSQL-backed persistence for Anvil skills question attempts. */
public class PgSkillsAttemptPersistenceManager implements ISkillsAttemptPersistenceManager {
    private final PostgresSqlDb database;

    @Inject
    public PgSkillsAttemptPersistenceManager(final PostgresSqlDb database) {
        this.database = database;
    }

    @Override
    public void registerSkillsAttempt(final AnvilPayloadDTO attempt)
            throws DuplicateSkillsAttemptException, SegueDatabaseException {
        try (var conn = database.getDatabaseConnection();
             var pst = conn.prepareStatement("""
                 INSERT INTO skills_question_attempts (
                     id, user_id, skill_assignment_id, skill_id, subskill_id, question, question_attempt, marks,
                     timestamp
                 ) VALUES (?, ?, ?, ?, ?, (?::jsonb), (?::jsonb), ?, ?)""")) {
            pst.setObject(1, attempt.getId());
            pst.setLong(2, attempt.getUserId());
            pst.setString(3, attempt.getSkillAssignmentId());
            pst.setString(4, attempt.getSkillId());
            pst.setString(5, attempt.getSubskillId());
            pst.setString(6, attempt.getQuestion().toString());
            pst.setString(7, attempt.getQuestionAttempt().toString());
            pst.setInt(8, (Integer) attempt.getMarks());
            pst.setTimestamp(9, new Timestamp(attempt.getTimestamp().getTime()));
            pst.executeUpdate();
        } catch (final SQLException e) {
            if (PSQLState.UNIQUE_VIOLATION.getState().equals(e.getSQLState())) {
                throw new DuplicateSkillsAttemptException(null);
            }
            throw new SegueDatabaseException("Something went wrong saving the attempt.");
        }
    }
}
