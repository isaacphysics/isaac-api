package uk.ac.cam.cl.dtg.isaac.dao;

import com.google.inject.Inject;
import org.postgresql.util.PSQLState;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateSkillsAttemptException;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.ISkillsAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** PostgreSQL-backed persistence for Anvil skills question attempts. */
public class PgSkillsAttemptPersistenceManager implements ISkillsAttemptPersistenceManager {
    private static final String MENTAL_MATHS_ID = "app_page_mental_maths_overall|0e184f9d-b619-4225-ac12-3c96d3c74046";

    private final PostgresSqlDb database;

    @Inject
    public PgSkillsAttemptPersistenceManager(final PostgresSqlDb database) {
        this.database = database;
    }

    @Override
    public void registerSkillsAttempt(final List<AnvilPayloadDTO> attempts)
            throws DuplicateSkillsAttemptException, SegueDatabaseException {
        String insertQuery = """
            INSERT INTO skills_question_attempts (
                id, user_id, skill_assignment_id, skill_id, subskill_id, question, question_attempt, marks,
                timestamp
            ) VALUES (?, ?, ?, ?, ?, (?::jsonb), (?::jsonb), ?, ?)""";
        try (var conn = database.getDatabaseConnection()) {
            conn.setAutoCommit(false);
            for (AnvilPayloadDTO attempt : attempts) {
                try (PreparedStatement pst = conn.prepareStatement(insertQuery)) {
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
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (final SQLException e) {
            if (PSQLState.UNIQUE_VIOLATION.getState().equals(e.getSQLState())) {
                throw new DuplicateSkillsAttemptException(null);
            }
            throw new SegueDatabaseException("Something went wrong saving the attempt.");
        }
    }

    @Override
    public Map<LocalDate, Long> getMentalMathsAttempts(final Long userId, final LocalDate from, final LocalDate to)
        throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement("""
                WITH dates(dt) AS (
                    SELECT generate_series(?, ?, INTERVAL '1' MONTH)::DATE
                ), attempts AS (
                    SELECT
                        DATE_TRUNC('month', timestamp::DATE) AS dt,
                        COUNT(1) AS cnt
                    FROM skills_question_attempts
                    WHERE user_id = ? AND timestamp >= ? AND skill_id = ?
                    GROUP BY dt
                )
                SELECT
                    dates.dt AS dt,
                    COALESCE(attempts.cnt, 0) AS cnt
                FROM dates
                LEFT JOIN attempts USING (dt)
                """
             )
        ) {
            pst.setObject(1, from);
            pst.setObject(2, to);
            pst.setLong(3, userId);
            pst.setObject(4, from);
            pst.setString(5, MENTAL_MATHS_ID);
            ResultSet results = pst.executeQuery();
            HashMap<LocalDate, Long> resultsMap = new HashMap<>();
            while (results.next()) {
                resultsMap.put(results.getObject("dt", LocalDate.class), results.getLong("cnt"));
            }
            return resultsMap;
        } catch (final SQLException e) {
            throw new SegueDatabaseException("Something went wrong querying the attempts.", e);
        }
    }
}
