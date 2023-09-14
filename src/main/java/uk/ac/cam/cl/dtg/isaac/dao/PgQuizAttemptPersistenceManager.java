/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dao;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.inject.Inject;
import jakarta.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ma.glasnost.orika.MapperFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.QuizAttemptDO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * This class is responsible for managing and persisting quiz attempts.
 */
public class PgQuizAttemptPersistenceManager implements IQuizAttemptPersistenceManager {
  private static final Logger log = LoggerFactory.getLogger(PgQuizAttemptPersistenceManager.class);

  private final MapperFacade mapper;
  private final PostgresSqlDb database;

  /**
   * Creates a new user data manager object.
   *
   * @param database
   *            - the database reference used for persistence.
   * @param mapper
   *            - An instance of an automapper that can be used for mapping to and from AssignmentDOs and DTOs.
   */
  @Inject
  public PgQuizAttemptPersistenceManager(final PostgresSqlDb database,
                                         final MapperFacade mapper) {
    this.database = database;
    this.mapper = mapper;
  }

  @Override
  public QuizAttemptDTO getByQuizAssignmentIdAndUserId(final Long quizAssignmentId, final Long userId)
      throws SegueDatabaseException {
    String query = "SELECT * FROM quiz_attempts WHERE quiz_assignment_id = ? AND user_id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_BY_ASSIGNMENT_AND_USER_ASSIGNMENT_ID, quizAssignmentId);
      pst.setLong(FIELD_GET_BY_ASSIGNMENT_AND_USER_USER_ID, userId);

      try (ResultSet results = pst.executeQuery()) {
        if (results.next()) {
          return this.convertToQuizAttemptDTO(this.convertFromSQLToQuizAttemptDO(results));
        } else {
          return null;
        }
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find quiz attempt by assignment and user", e);
    }
  }

  @Override
  public Long saveAttempt(final QuizAttemptDTO attempt) throws SegueDatabaseException {
    QuizAttemptDO attemptToSave = mapper.map(attempt, QuizAttemptDO.class);

    String query = "INSERT INTO quiz_attempts(user_id, quiz_id, quiz_assignment_id, start_date) VALUES (?, ?, ?, ?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
    ) {
      pst.setLong(FIELD_SAVE_ATTEMPT_USER_ID, attemptToSave.getUserId());
      pst.setString(FIELD_SAVE_ATTEMPT_QUIZ_ID, attemptToSave.getQuizId());

      if (attemptToSave.getQuizAssignmentId() != null) {
        pst.setLong(FIELD_SAVE_ATTEMPT_ASSIGNMENT_ID, attemptToSave.getQuizAssignmentId());
      } else {
        pst.setNull(FIELD_SAVE_ATTEMPT_ASSIGNMENT_ID, Types.BIGINT);
      }

      if (attemptToSave.getStartDate() != null) {
        pst.setTimestamp(FIELD_SAVE_ATTEMPT_START_DATE, new java.sql.Timestamp(attemptToSave.getStartDate().getTime()));
      } else {
        pst.setTimestamp(FIELD_SAVE_ATTEMPT_START_DATE, new java.sql.Timestamp(new Date().getTime()));
      }

      if (pst.executeUpdate() == 0) {
        throw new SegueDatabaseException("Unable to save assignment.");
      }

      long attemptId;
      try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          attemptId = generatedKeys.getLong(1);
        } else {
          throw new SQLException("Creating attempt failed, no ID obtained.");
        }

        log.debug("Saving Quiz Attempt... Quiz Attempt ID: " + attempt.getId() + " Db id : " + attemptId);
        return attemptId;
      }

    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public List<QuizAttemptDTO> getByQuizIdAndUserId(final String quizId, final Long userId)
      throws SegueDatabaseException {
    String query = "SELECT * FROM quiz_attempts WHERE quiz_id = ? AND user_id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_GET_BY_QUIZ_AND_USER_QUIZ_ID, quizId);
      pst.setLong(FIELD_GET_BY_QUIZ_AND_USER_USER_ID, userId);

      try (ResultSet results = pst.executeQuery()) {

        List<QuizAttemptDTO> listOfResults = Lists.newArrayList();
        while (results.next()) {
          listOfResults.add(this.convertToQuizAttemptDTO(this.convertFromSQLToQuizAttemptDO(results)));
        }
        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find quiz attempts by quiz id and user id", e);
    }
  }

  @Override
  public QuizAttemptDTO getById(final Long quizAttemptId) throws SegueDatabaseException {
    String query = "SELECT * FROM quiz_attempts WHERE id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_BY_ID_ATTEMPT_ID, quizAttemptId);

      try (ResultSet results = pst.executeQuery()) {
        if (results.next()) {
          return this.convertToQuizAttemptDTO(this.convertFromSQLToQuizAttemptDO(results));
        } else {
          throw new SQLException("No results");
        }
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find quiz attempt by id", e);
    }
  }

  @Override
  @Nullable
  public Date updateAttemptCompletionStatus(final Long quizAttemptId, final boolean newCompletionStatus)
      throws SegueDatabaseException {
    String query = "UPDATE quiz_attempts SET completed_date = ? WHERE id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      Date completedDate = newCompletionStatus ? new Date() : null;
      if (completedDate != null) {
        pst.setTimestamp(FIELD_UPDATE_ATTEMPT_COMPLETED_DATE, new java.sql.Timestamp(completedDate.getTime()));
      } else {
        pst.setNull(FIELD_UPDATE_ATTEMPT_COMPLETED_DATE, Types.TIMESTAMP);
      }
      pst.setLong(FIELD_UPDATE_ATTEMPT_ATTEMPT_ID, quizAttemptId);

      pst.executeUpdate();

      return completedDate;
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to mark quiz attempt complete", e);
    }
  }

  @Override
  public Set<Long> getCompletedUserIds(final Long assignmentId) throws SegueDatabaseException {
    String query = "SELECT user_id FROM quiz_attempts WHERE quiz_assignment_id = ? AND completed_date IS NOT NULL";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_COMPLETED_USER_IDS_ASSIGNMENT_ID, assignmentId);

      try (ResultSet results = pst.executeQuery()) {
        Set<Long> setOfResults = Sets.newHashSet();
        while (results.next()) {
          setOfResults.add(results.getLong("user_id"));
        }
        return setOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to get completed user ids for assignment: " + assignmentId, e);
    }
  }

  @Override
  public Map<Long, QuizAttemptDTO> getByQuizAssignmentIdsAndUserId(final List<Long> quizAssignmentIds,
                                                                   final Long userId)
      throws SegueDatabaseException {
    Map<Long, QuizAttemptDTO> mapOfResults = Maps.newHashMap();
    if (quizAssignmentIds.isEmpty()) {
      return mapOfResults; // IN condition below doesn't work with empty list.
    }
    // This is a nasty hack to make a prepared statement using the sql IN operator.
    StringBuilder builder = new StringBuilder();
    builder.append("(");
    for (int i = 0; i < quizAssignmentIds.size(); i++) {
      builder.append("?,");
    }
    String quizAssignmentIdsHoles = builder.deleteCharAt(builder.length() - 1).append(")").toString();
    String query = "SELECT quiz_attempts.* FROM quiz_attempts INNER JOIN quiz_assignments"
        + " ON quiz_attempts.quiz_assignment_id = quiz_assignments.id"
        + " WHERE quiz_attempts.user_id = ? AND quiz_assignments.id IN " + quizAssignmentIdsHoles;
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_BY_ASSIGNMENTS_AND_USER_USER_ID, userId);
      int i = FIELD_GET_BY_ASSIGNMENTS_AND_USER_ASSIGNMENTS_INITIAL_INDEX;
      for (Long quizAssignmentId : quizAssignmentIds) {
        pst.setLong(i++, quizAssignmentId);
      }

      try (ResultSet results = pst.executeQuery()) {
        while (results.next()) {
          mapOfResults.put(results.getLong("quiz_assignment_id"),
              this.convertToQuizAttemptDTO(this.convertFromSQLToQuizAttemptDO(results)));
        }
        return mapOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find quiz attempts by assignment ids for user id", e);
    }
  }

  @Override
  public List<QuizAttemptDTO> getFreeAttemptsByUserId(final Long userId) throws SegueDatabaseException {
    String query = "SELECT * FROM quiz_attempts WHERE quiz_attempts.user_id = ? AND quiz_assignment_id IS NULL";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_FREE_ATTEMPTS_BY_USER_USER_ID, userId);

      try (ResultSet results = pst.executeQuery()) {
        List<QuizAttemptDTO> listOfResults = Lists.newArrayList();
        while (results.next()) {
          listOfResults.add(this.convertToQuizAttemptDTO(this.convertFromSQLToQuizAttemptDO(results)));
        }
        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find free quiz attempts for user id", e);
    }
  }

  @Override
  public void deleteAttempt(final Long quizAttemptId) throws SegueDatabaseException {
    String query = "DELETE FROM quiz_attempts WHERE id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_DELETE_ATTEMPT_ATTEMPT_ID, quizAttemptId);

      pst.executeUpdate();
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to delete quiz attempt", e);
    }
  }

  /**
   * Convert from a Quiz Attempt DO to a Quiz Attempt DTO.
   *
   * @param attemptDO
   *            - to convert
   * @return Assignment DTO
   */
  private QuizAttemptDTO convertToQuizAttemptDTO(final QuizAttemptDO attemptDO) {
    return mapper.map(attemptDO, QuizAttemptDTO.class);
  }

  /**
   * Convert from an SQL result set to an Quiz Attempt DO.
   *
   * @param sqlResults set - assumed to be at the correct position.
   * @return The QuizAttemptDO
   * @throws SQLException if we cannot access a required field.
   */
  private QuizAttemptDO convertFromSQLToQuizAttemptDO(final ResultSet sqlResults) throws SQLException {
    Long quizAssignmentId = sqlResults.getLong("quiz_assignment_id");
    if (sqlResults.wasNull()) {
      quizAssignmentId = null;
    }

    Date startDate = new Date(sqlResults.getTimestamp("start_date").getTime());

    Date completedDate = null;
    if (sqlResults.getTimestamp("completed_date") != null) {
      completedDate = new Date(sqlResults.getTimestamp("completed_date").getTime());
    }

    return new QuizAttemptDO(sqlResults.getLong("id"), sqlResults.getLong("user_id"),
        sqlResults.getString("quiz_id"), quizAssignmentId,
        startDate, completedDate);
  }

  // Field Constants
  // getByQuizAssignmentIdAndUserId
  private static final int FIELD_GET_BY_ASSIGNMENT_AND_USER_ASSIGNMENT_ID = 1;
  private static final int FIELD_GET_BY_ASSIGNMENT_AND_USER_USER_ID = 2;

  // saveAttempt
  private static final int FIELD_SAVE_ATTEMPT_USER_ID = 1;
  private static final int FIELD_SAVE_ATTEMPT_QUIZ_ID = 2;
  private static final int FIELD_SAVE_ATTEMPT_ASSIGNMENT_ID = 3;
  private static final int FIELD_SAVE_ATTEMPT_START_DATE = 4;

  // getByQuizIdAndUserId
  private static final int FIELD_GET_BY_QUIZ_AND_USER_QUIZ_ID = 1;
  private static final int FIELD_GET_BY_QUIZ_AND_USER_USER_ID = 2;

  // getById
  private static final int FIELD_GET_BY_ID_ATTEMPT_ID = 1;

  // updateAttemptCompletionStatus
  private static final int FIELD_UPDATE_ATTEMPT_COMPLETED_DATE = 1;
  private static final int FIELD_UPDATE_ATTEMPT_ATTEMPT_ID = 2;

  // getCompletedUserIds
  private static final int FIELD_GET_COMPLETED_USER_IDS_ASSIGNMENT_ID = 1;

  // getByQuizAssignmentIdsAndUserId
  private static final int FIELD_GET_BY_ASSIGNMENTS_AND_USER_USER_ID = 1;
  private static final int FIELD_GET_BY_ASSIGNMENTS_AND_USER_ASSIGNMENTS_INITIAL_INDEX = 2;

  // getFreeAttemptsByUserId
  private static final int FIELD_GET_FREE_ATTEMPTS_BY_USER_USER_ID = 1;

  // deleteAttempt
  private static final int FIELD_DELETE_ATTEMPT_ATTEMPT_ID = 1;
}
