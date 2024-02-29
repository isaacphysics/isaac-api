/**
 * Copyright 2021 Raspberry Pi Foundation
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

public class PgQuizQuestionAttemptPersistenceManager implements IQuizQuestionAttemptPersistenceManager {
  private final ObjectMapper objectMapper;
  private final PostgresSqlDb database;

  /**
   * Creates a new quiz question attempt persistence manager.
   *
   * @param database           - the database reference used for persistence.
   * @param contentMapperUtils - the ContentMapperUtils to get a Jackson ObjectMapper for persisting question answers.
   */
  @Inject
  public PgQuizQuestionAttemptPersistenceManager(final PostgresSqlDb database,
                                                 final ContentMapperUtils contentMapperUtils) {
    this.database = database;
    this.objectMapper = contentMapperUtils.getSharedContentObjectMapper();
  }

  @Override
  public void registerQuestionAttempt(final Long quizAttemptId, final QuestionValidationResponse questionResponse)
      throws SegueDatabaseException {

    String query =
        "INSERT INTO quiz_question_attempts(quiz_attempt_id, question_id, question_attempt, correct, \"timestamp\")"
            + " VALUES (?, ?, ?::text::jsonb, ?, ?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
    ) {
      pst.setLong(FIELD_REGISTER_ATTEMPT_ATTEMPT_ID, quizAttemptId);
      pst.setString(FIELD_REGISTER_ATTEMPT_QUESTION_ID, questionResponse.getQuestionId());
      pst.setString(FIELD_REGISTER_ATTEMPT_ATTEMPT_STRING, objectMapper.writeValueAsString(questionResponse));

      if (questionResponse.isCorrect() != null) {
        pst.setBoolean(FIELD_REGISTER_ATTEMPT_IS_CORRECT, questionResponse.isCorrect());
      } else {
        pst.setNull(FIELD_REGISTER_ATTEMPT_IS_CORRECT, Types.BOOLEAN);
      }
      pst.setTimestamp(FIELD_REGISTER_ATTEMPT_TIMESTAMP,
          new java.sql.Timestamp(questionResponse.getDateAttempted().getTime()));

      if (pst.executeUpdate() == 0) {
        throw new SegueDatabaseException("Unable to save quiz question attempt.");
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    } catch (JsonProcessingException e) {
      throw new SegueDatabaseException("Unable to process json exception", e);
    }
  }

  @Override
  public Map<String, List<QuestionValidationResponse>> getAllAnswersForQuizAttempt(final Long quizAttemptId)
      throws SegueDatabaseException {
    String query =
        "SELECT question_id, question_attempt FROM quiz_question_attempts WHERE quiz_attempt_id = ? ORDER BY timestamp";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_ATTEMPT_ANSWERS_ATTEMPT_ID, quizAttemptId);

      try (ResultSet results = pst.executeQuery()) {
        Map<String, List<QuestionValidationResponse>> resultsMap = Maps.newHashMap();
        while (results.next()) {
          String questionId = results.getString("question_id");

          List<QuestionValidationResponse> questionAttempts =
              resultsMap.computeIfAbsent(questionId, ignore -> Lists.newArrayList());

          QuestionValidationResponse questionAttempt = objectMapper.readValue(
              results.getString("question_attempt"), QuestionValidationResponse.class);

          questionAttempts.add(questionAttempt);
        }
        return resultsMap;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    } catch (JsonProcessingException e) {
      throw new SegueDatabaseException("Unable to process json exception", e);
    }
  }

  @Override
  public Map<Long, Map<String, List<QuestionValidationResponse>>> getAllAnswersForQuizAssignment(
      final Long quizAssignmentId) throws SegueDatabaseException {
    String query = "SELECT user_id, question_id, question_attempt FROM quiz_question_attempts"
        + " INNER JOIN quiz_attempts ON (quiz_attempts.id = quiz_question_attempts.quiz_attempt_id)"
        + " WHERE quiz_assignment_id = ? ORDER BY quiz_attempt_id, timestamp";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_ASSIGNMENT_ANSWERS_ASSIGNMENT_ID, quizAssignmentId);

      try (ResultSet results = pst.executeQuery()) {
        Map<Long, Map<String, List<QuestionValidationResponse>>> resultsMap = Maps.newHashMap();
        while (results.next()) {
          Long userId = results.getLong("user_id");
          String questionId = results.getString("question_id");

          Map<String, List<QuestionValidationResponse>> userAttemptsMap =
              resultsMap.computeIfAbsent(userId, ignoreKey -> Maps.newHashMap());

          List<QuestionValidationResponse> questionAttempts =
              userAttemptsMap.computeIfAbsent(questionId, ignoreKey -> Lists.newArrayList());

          QuestionValidationResponse questionAttempt = objectMapper.readValue(
              results.getString("question_attempt"), QuestionValidationResponse.class);

          questionAttempts.add(questionAttempt);
        }
        return resultsMap;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    } catch (JsonProcessingException e) {
      throw new SegueDatabaseException("Unable to process json exception", e);
    }
  }

  // Field Constants
  // registerQuestionAttempt
  private static final int FIELD_REGISTER_ATTEMPT_ATTEMPT_ID = 1;
  private static final int FIELD_REGISTER_ATTEMPT_QUESTION_ID = 2;
  private static final int FIELD_REGISTER_ATTEMPT_ATTEMPT_STRING = 3;
  private static final int FIELD_REGISTER_ATTEMPT_IS_CORRECT = 4;
  private static final int FIELD_REGISTER_ATTEMPT_TIMESTAMP = 5;

  // getAllAnswersForQuizAttempt
  private static final int FIELD_GET_ATTEMPT_ANSWERS_ATTEMPT_ID = 1;

  // getAllAnswersForQuizAssignment
  private static final int FIELD_GET_ASSIGNMENT_ANSWERS_ASSIGNMENT_ID = 1;

}
