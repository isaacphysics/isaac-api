/*
 * Copyright 2021 Raspberry Pi Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;
import java.util.Map;

public class PgQuizQuestionAttemptPersistenceManager implements IQuizQuestionAttemptPersistenceManager {
    private static final Logger log = LoggerFactory.getLogger(PgQuizQuestionAttemptPersistenceManager.class);

    private final ObjectMapper objectMapper;
    private final PostgresSqlDb database;

    /**
     * Creates a new quiz question attempt persistence manager.
     *
     * @param database
     *            - the database reference used for persistence.
     * @param objectMapper
     *            - the ContentMapper to get a Jackson ObjectMapper for persisting question answers.
     */
    @Inject
    public PgQuizQuestionAttemptPersistenceManager(final PostgresSqlDb database, final ContentMapper objectMapper) {
        this.database = database;
        this.objectMapper = objectMapper.getSharedContentObjectMapper();
    }

    @Override
    public void registerQuestionAttempt(Long quizAttemptId, QuestionValidationResponse questionResponse) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("INSERT INTO quiz_question_attempts(" +
                "quiz_attempt_id, question_id, question_attempt, correct, \"timestamp\")"
                + " VALUES (?, ?, ?::text::jsonb, ?, ?);", Statement.RETURN_GENERATED_KEYS);

            pst.setLong(1, quizAttemptId);
            pst.setString(2, questionResponse.getQuestionId());
            pst.setString(3, objectMapper.writeValueAsString(questionResponse));

            if (questionResponse.isCorrect() != null) {
                pst.setBoolean(4, questionResponse.isCorrect());
            } else {
                pst.setNull(4, Types.BOOLEAN);
            }
            pst.setTimestamp(5, new java.sql.Timestamp(questionResponse.getDateAttempted().getTime()));

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
    public Map<String, List<QuestionValidationResponse>> getAllAnswersForQuizAttempt(Long quizAttemptId) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("SELECT question_id, question_attempt FROM quiz_question_attempts" +
                " WHERE quiz_attempt_id = ? ORDER BY timestamp");

            pst.setLong(1, quizAttemptId);

            ResultSet results = pst.executeQuery();

            Map<String, List<QuestionValidationResponse>> resultsMap = Maps.newHashMap();
            while (results.next()) {
                String questionId = results.getString("question_id");

                List<QuestionValidationResponse> questionAttempts = resultsMap.computeIfAbsent(questionId, (ignore) -> Lists.newArrayList());

                QuestionValidationResponse questionAttempt = objectMapper.readValue(
                    results.getString("question_attempt"), QuestionValidationResponse.class);

                questionAttempts.add(questionAttempt);
            }

            return resultsMap;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to process json exception", e);
        }
    }

    @Override
    public Map<Long, Map<String, List<QuestionValidationResponse>>> getAllAnswersForQuizAssignment(Long quizAssignmentId) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("SELECT user_id, question_id, question_attempt FROM quiz_question_attempts" +
                " INNER JOIN quiz_attempts ON (quiz_attempts.id = quiz_question_attempts.quiz_attempt_id) WHERE quiz_assignment_id = ? ORDER BY quiz_attempt_id, timestamp");

            pst.setLong(1, quizAssignmentId);

            ResultSet results = pst.executeQuery();

            Map<Long, Map<String, List<QuestionValidationResponse>>> resultsMap = Maps.newHashMap();
            while (results.next()) {
                Long userId = results.getLong("user_id");
                String questionId = results.getString("question_id");

                Map<String, List<QuestionValidationResponse>> userAttemptsMap = resultsMap.compute(userId, (ignoreKey, ignoreValue) -> Maps.newHashMap());

                List<QuestionValidationResponse> questionAttempts = userAttemptsMap.computeIfAbsent(questionId, (ignore) -> Lists.newArrayList());

                QuestionValidationResponse questionAttempt = objectMapper.readValue(
                    results.getString("question_attempt"), QuestionValidationResponse.class);

                questionAttempts.add(questionAttempt);
            }

            return resultsMap;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to process json exception", e);
        }
    }
}
