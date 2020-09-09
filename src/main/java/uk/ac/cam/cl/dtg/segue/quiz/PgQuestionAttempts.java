/*
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * @author sac92
 *
 */
public class PgQuestionAttempts implements IQuestionAttemptManager {
    private static final Logger log = LoggerFactory.getLogger(PgQuestionAttempts.class);
            
    private final PostgresSqlDb database;
    private final ObjectMapper objectMapper;

    /**
     * @param ds
     *            - data source
     * @param objectMapper
     *            - for mapping between DO and DTO
     */
    @Inject
    public PgQuestionAttempts(final PostgresSqlDb ds, final ContentMapper objectMapper) {
        this.database = ds;
        this.objectMapper = objectMapper.getSharedContentObjectMapper();
    }
    
    @Override
    public void registerAnonymousQuestionAttempt(final String userId, final String questionPageId,
            final String fullQuestionId, final QuestionValidationResponse questionAttempt)
            throws SegueDatabaseException {

        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            Map<String, Map<String, List<QuestionValidationResponse>>> userAttempts = this.getAnonymousQuestionAttempts(userId);

            if (null == userAttempts) {
                userAttempts = new HashMap<>();
            }

            userAttempts.computeIfAbsent(questionPageId, k -> new HashMap<>());

            userAttempts.get(questionPageId).computeIfAbsent(fullQuestionId, k -> new ArrayList<>());

            userAttempts.get(questionPageId).get(fullQuestionId).add(questionAttempt);

            // TODO: This might be able to be simplified by using json_insert
            pst = conn.prepareStatement("UPDATE temporary_user_store " +
                    "SET temporary_app_data = jsonb_set(temporary_app_data, " +
                    "?::text[], ?::text::jsonb) WHERE id = ?;");
            pst.setString(1, "{questionAttempts}");
            pst.setString(2, objectMapper.writeValueAsString(userAttempts));
            pst.setString(3, userId);

            log.debug(pst.toString());
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save question attempt.");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to process json exception", e);
        }
    }

    /**
     * getAnonymousQuestionAttempts.
     * @param anonymousId
     *            to lookup
     * @return the question pageId --> full questionId --> list of responses. (or null if no data)
     */
    @Override
    public Map<String, Map<String, List<QuestionValidationResponse>>> getAnonymousQuestionAttempts(
            final String anonymousId) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("SELECT temporary_app_data->'questionAttempts' AS question_attempts from temporary_user_store where id = ?;"
                    , Statement.RETURN_GENERATED_KEYS);

            pst.setString(1, anonymousId);

            ResultSet resultSet = pst.executeQuery();
            // are there any results
            if (!resultSet.isBeforeFirst()) {
                return Maps.newHashMap();
            }

            resultSet.next();

            // We need to try and generate QuestionValidationResponses in the correct object structure - Apologies for the hideousness
            Map<String, Map<String, List<Object>>> questionAttemptsFromDB
                    = objectMapper.readValue(resultSet.getString("question_attempts"), Map.class);
            Map<String, Map<String, List<QuestionValidationResponse>>> result = Maps.newHashMap();

            for (Map.Entry<String, Map<String, List<Object>>> questionAttemptsForPage : questionAttemptsFromDB.entrySet()){

                Map<String, List<QuestionValidationResponse>> questionAttemptsForQuestion = Maps.newHashMap();
                for (Map.Entry<String, List<Object>> submap : questionAttemptsForPage.getValue().entrySet()){
                    List<QuestionValidationResponse> listOfuestionValidationResponses = Lists.newArrayList();
                    questionAttemptsForQuestion.put(submap.getKey(), listOfuestionValidationResponses);

                    for (Object o : submap.getValue()) {
                        listOfuestionValidationResponses
                                .add(objectMapper.convertValue(o, QuestionValidationResponse.class));
                    }
                }

                result.put(questionAttemptsForPage.getKey(), questionAttemptsForQuestion);
            }

            return result;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (IOException e) {
            throw new SegueDatabaseException("Unable to process json exception", e);
        }
    }

    @Override
    public void registerQuestionAttempt(final Long userId, final String questionPageId, final String fullQuestionId,
            final QuestionValidationResponse questionAttempt) throws SegueDatabaseException {

        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("INSERT INTO question_attempts(user_id, "
                    + "question_id, question_attempt, correct, \"timestamp\")"
                    + " VALUES (?, ?, ?::text::jsonb, ?, ?);", Statement.RETURN_GENERATED_KEYS);

            pst.setLong(1, userId);
            pst.setString(2, fullQuestionId);
            pst.setString(3, objectMapper.writeValueAsString(questionAttempt));

            if (questionAttempt.isCorrect() != null) {
                pst.setBoolean(4, questionAttempt.isCorrect());
            } else {
                pst.setNull(4, java.sql.Types.NULL);
            }
            pst.setTimestamp(5, new java.sql.Timestamp(questionAttempt.getDateAttempted().getTime()));

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save question attempt.");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to process json exception", e);
        }
    }

    @Override
    public Map<String, Map<String, List<QuestionValidationResponse>>> getQuestionAttempts(final Long userId)
            throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("SELECT * FROM question_attempts WHERE user_id = ? ORDER BY \"timestamp\" ASC");
            pst.setLong(1, userId);

            ResultSet results = pst.executeQuery();

            Map<String, Map<String, List<QuestionValidationResponse>>> mapOfQuestionAttemptsByPage = Maps.newHashMap();

            while (results.next()) {
                QuestionValidationResponse questionAttempt = objectMapper.readValue(
                        results.getString("question_attempt"), QuestionValidationResponse.class);
                String questionPageId = questionAttempt.getQuestionId().split("\\|")[0];
                String questionId = questionAttempt.getQuestionId();


                Map<String, List<QuestionValidationResponse>> attemptsForThisQuestionPage = mapOfQuestionAttemptsByPage
                        .get(questionPageId);

                if (null == attemptsForThisQuestionPage) {
                    attemptsForThisQuestionPage = Maps.newHashMap();
                    mapOfQuestionAttemptsByPage.put(questionPageId, attemptsForThisQuestionPage);
                }

                List<QuestionValidationResponse> listOfResponses = attemptsForThisQuestionPage.get(questionId);
                if (null == listOfResponses) {
                    listOfResponses = Lists.newArrayList();
                    attemptsForThisQuestionPage.put(questionId, listOfResponses);
                }

                listOfResponses.add(questionAttempt);
            }

            return mapOfQuestionAttemptsByPage;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (IOException e) {
            throw new SegueDatabaseException("Exception while parsing json", e);
        }
    }

    @Override
    public List<String> getMostRecentQuestionPageAttempts(final Long userId, final Integer limit)
            throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("SELECT SUBSTRING(question_id, 1, STRPOS(question_id, '|') - 1) AS question_page_id, MAX(\"timestamp\") AS latest_timestamp " +
                    "FROM question_attempts WHERE user_id = ? " +
                    "GROUP BY question_page_id " +
                    " ORDER BY latest_timestamp DESC LIMIT ?");
            pst.setLong(1, userId);
            pst.setInt(2, limit);

            ResultSet results = pst.executeQuery();
            List<String> questionPageIds = new ArrayList<>();
            while (results.next()) {
                questionPageIds.add(results.getString("question_page_id"));
            }
            return questionPageIds;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public List<String> getUnsolvedQuestions(final Long userId)
            throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("SELECT question_id from question_attempts WHERE user_id = ? " +
                    "GROUP BY question_id " +
                    "HAVING bool_or(correct) = false");
            pst.setLong(1, userId);

            ResultSet results = pst.executeQuery();
            List<String> questionIds = new ArrayList<>();
            while (results.next()) {
                questionIds.add(results.getString("question_id"));
            }
            return questionIds;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
    
    @Override
    public Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>>
            getQuestionAttemptsByUsersAndQuestionPrefix(final List<Long> userIds, final List<String> questionPageIds)
            throws SegueDatabaseException {
        Validate.notEmpty(questionPageIds);
        
        if (userIds.isEmpty()) {
            return Maps.newHashMap();
        }
        
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            StringBuilder query = new StringBuilder();
            query.append("SELECT id, user_id, question_id, correct, timestamp FROM question_attempts WHERE");
            
            // add all of the user ids we are interested in.
            if (!userIds.isEmpty()) {
                StringBuilder inParams = new StringBuilder();
                inParams.append("?");
                for (int i = 1; i < userIds.size(); i++) {
                    inParams.append(",?");
                }
    
                query.append(String.format(" user_id IN (%s)", inParams.toString()));
            }

            // add all of the question page ids we are interested in
            StringBuilder questionIdsSB = new StringBuilder();
            if (!questionPageIds.isEmpty()) {
                questionIdsSB.append("^(");
                questionIdsSB.append(questionPageIds.get(0));
                for (int i = 1; i < questionPageIds.size(); i++) {
                    questionIdsSB.append("|").append(questionPageIds.get(i));
                }
                
                questionIdsSB.append(")");
                
                query.append(" AND question_id ~ ?");
            }   
            
            query.append(" ORDER BY \"timestamp\" ASC");
            
            pst = conn.prepareStatement(query.toString());
            
            Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>> mapToReturn
                = Maps.newHashMap();
           
            int index = 1;
            for (int i = 0; i < userIds.size(); i++) {
                pst.setLong(index++, userIds.get(i));
                mapToReturn.put(userIds.get(i),
                        new HashMap<>());
            }

            pst.setString(index++, questionIdsSB.toString());
            
            ResultSet results = pst.executeQuery();
            while (results.next()) {
                LightweightQuestionValidationResponse partialQuestionAttempt = new QuestionValidationResponse();

                partialQuestionAttempt.setCorrect(results.getBoolean("correct"));
                partialQuestionAttempt.setQuestionId(results.getString("question_id"));
                partialQuestionAttempt.setDateAttempted(results.getTimestamp("timestamp"));

                String questionPageId = partialQuestionAttempt.getQuestionId().split("\\|")[0];
                String questionId = partialQuestionAttempt.getQuestionId();
                
                Map<String, Map<String, List<LightweightQuestionValidationResponse>>> mapOfQuestionAttemptsByPage
                    = mapToReturn.get(results.getLong("user_id"));

                Map<String, List<LightweightQuestionValidationResponse>> attemptsForThisQuestionPage =
                        mapOfQuestionAttemptsByPage.get(questionPageId);
                
                if (null == attemptsForThisQuestionPage) {
                    attemptsForThisQuestionPage = Maps.newHashMap();
                    mapOfQuestionAttemptsByPage.put(questionPageId, attemptsForThisQuestionPage);
                }
                
                List<LightweightQuestionValidationResponse> listOfResponses =
                        attemptsForThisQuestionPage.get(questionId);
                if (null == listOfResponses) {
                    listOfResponses = Lists.newArrayList();
                    attemptsForThisQuestionPage.put(questionId, listOfResponses);
                }

                listOfResponses.add(partialQuestionAttempt);
            }
            
            return mapToReturn;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
    * Merges any question data stored in the session (this will only happen for anonymous users).
    *
    * @param anonymousUserId
    *            - containing the question attempts.
    * @param registeredUserId
    *            - the account to merge with.
    * @throws SegueDatabaseException
    *             - if we are unable to locate the questions attempted by this user already.
    */
    @Override
    public void mergeAnonymousQuestionInformationWithRegisteredUserRecord(final String anonymousUserId,
            final Long registeredUserId) throws SegueDatabaseException {
        Validate.notNull(anonymousUserId, "Anonymous user must not be null when merging anonymousQuestion info");
        Validate.notNull(registeredUserId, "Registered user must not be null when merging anonymousQuestion info");

        Map<String, Map<String, List<QuestionValidationResponse>>> anonymouslyAnsweredQuestions = this
                .getAnonymousQuestionAttempts(anonymousUserId);

        if (anonymouslyAnsweredQuestions.isEmpty()) {
            return;
        }

        int count = 0;
        for (String questionPageId : anonymouslyAnsweredQuestions.keySet()) {
            for (String questionId : anonymouslyAnsweredQuestions.get(questionPageId).keySet()) {
                for (QuestionValidationResponse questionResponse : anonymouslyAnsweredQuestions.get(questionPageId)
                        .get(questionId)) {
                    this.registerQuestionAttempt(registeredUserId, questionPageId, questionId, questionResponse);
                    count++;
                }
            }
        }
        
        log.info(String.format("Merged anonymously answered questions (%s) with known user account (%s)", count,
                registeredUserId));
    }

    @Override
    public Map<Role, Long> getAnsweredQuestionRolesOverPrevious(TimeInterval timeInterval) throws
            SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("SELECT role, count(DISTINCT users.id) FROM question_attempts" +
                    " JOIN users ON user_id=users.id AND NOT deleted WHERE timestamp > now() - ? GROUP BY role");
            pst.setObject(1, timeInterval.getPGInterval());
            ResultSet results = pst.executeQuery();

            Map<Role, Long> resultsToReturn = Maps.newHashMap();
            while (results.next()) {
                resultsToReturn.put(Role.valueOf(results.getString("role")), results.getLong("count"));
            }
            return resultsToReturn;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public Map<Date, Long> getQuestionAttemptCountForUserByDateRange(final Date fromDate, final Date toDate,
                                                                     final Long userId, final Boolean perDay) throws SegueDatabaseException {
        Validate.notNull(fromDate);
        Validate.notNull(toDate);

        StringBuilder queryToBuild = new StringBuilder();
        queryToBuild.append("WITH filtered_attempts AS (SELECT * FROM question_attempts WHERE user_id = ?) ");

        // The following LEFT JOIN gives us months with no events in as required, but need count(id) not count(1) to
        // count actual logged events (where id strictly NOT NULL) in those months, and not count an extra '1' for
        // empty months where id is NULL by definition of the JOIN.
        queryToBuild.append("SELECT to_char(gen_date, 'YYYY-MM-DD'), count(id)");
        if (perDay != null && perDay) {
            queryToBuild.append(" FROM generate_series(date_trunc('day', ?::timestamp), ?, INTERVAL '1' DAY) m(gen_date)");
            queryToBuild.append(" LEFT OUTER JOIN filtered_attempts ON ( date_trunc('day', \"timestamp\") = date_trunc('day', gen_date) )");
        } else {
            queryToBuild.append(" FROM generate_series(date_trunc('month', ?::timestamp), ?, INTERVAL '1' MONTH) m(gen_date)");
            queryToBuild.append(" LEFT OUTER JOIN filtered_attempts ON ( date_trunc('month', \"timestamp\") = date_trunc('month', gen_date) )");
        }
        queryToBuild.append(" GROUP BY gen_date ORDER BY gen_date ASC;");

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement(queryToBuild.toString());
            pst.setLong(1, userId);
            pst.setTimestamp(2, new java.sql.Timestamp(fromDate.getTime()));
            pst.setTimestamp(3, new java.sql.Timestamp(toDate.getTime()));

            ResultSet results = pst.executeQuery();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

            Map<Date, Long> mapToReturn = Maps.newHashMap();
            while (results.next()) {
                mapToReturn.put(formatter.parse(results.getString("to_char")), results.getLong("count"));
            }

            return mapToReturn;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (ParseException e) {
            throw new SegueDatabaseException("Unable to parse date exception", e);
        }
    }
}
