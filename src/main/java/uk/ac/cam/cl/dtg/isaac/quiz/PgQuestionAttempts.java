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
package uk.ac.cam.cl.dtg.isaac.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager.extractPageIdFromQuestionId;

/**
 * @author sac92
 *
 */
public class PgQuestionAttempts implements IQuestionAttemptManager {
    private static final Logger log = LoggerFactory.getLogger(PgQuestionAttempts.class);
    private static final int MAX_PAGE_IDS_TO_MATCH = 100;
            
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
        try (Connection conn = database.getDatabaseConnection()) {
            Map<String, Map<String, List<QuestionValidationResponse>>> userAttempts = this.getAnonymousQuestionAttempts(userId);

            if (null == userAttempts) {
                userAttempts = new HashMap<>();
            }

            userAttempts.computeIfAbsent(questionPageId, k -> new HashMap<>());

            userAttempts.get(questionPageId).computeIfAbsent(fullQuestionId, k -> new ArrayList<>());

            userAttempts.get(questionPageId).get(fullQuestionId).add(questionAttempt);

            String query = "UPDATE temporary_user_store SET temporary_app_data = " +
                    "jsonb_set(temporary_app_data, ?::text[], ?::text::jsonb) WHERE id = ?;";
            try (PreparedStatement pst = conn.prepareStatement(query)) {
                pst.setString(1, "{questionAttempts}");
                pst.setString(2, objectMapper.writeValueAsString(userAttempts));
                pst.setString(3, userId);

                if (pst.executeUpdate() == 0) {
                    throw new SegueDatabaseException("Unable to save question attempt.");
                }
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
        String query = "SELECT temporary_app_data->'questionAttempts' AS question_attempts from temporary_user_store where id = ?;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ) {
            pst.setString(1, anonymousId);

            try (ResultSet resultSet = pst.executeQuery()) {
                // are there any results
                if (!resultSet.isBeforeFirst()) {
                    return Maps.newHashMap();
                }
                resultSet.next();

                // We need to try and generate QuestionValidationResponses in the correct object structure - Apologies for the hideousness
                Map<String, Map<String, List<Object>>> questionAttemptsFromDB
                        = objectMapper.readValue(resultSet.getString("question_attempts"), Map.class);
                Map<String, Map<String, List<QuestionValidationResponse>>> result = Maps.newHashMap();

                for (Map.Entry<String, Map<String, List<Object>>> questionAttemptsForPage : questionAttemptsFromDB.entrySet()) {

                    Map<String, List<QuestionValidationResponse>> questionAttemptsForQuestion = Maps.newHashMap();
                    for (Map.Entry<String, List<Object>> submap : questionAttemptsForPage.getValue().entrySet()) {
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
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (IOException e) {
            throw new SegueDatabaseException("Unable to process json exception", e);
        }
    }

    @Override
    public void registerQuestionAttempt(final Long userId, final String questionPageId, final String fullQuestionId,
            final QuestionValidationResponse questionAttempt) throws SegueDatabaseException {

        String query = "INSERT INTO question_attempts(user_id, question_id, question_attempt, correct, \"timestamp\")" +
                " VALUES (?, ?, ?::text::jsonb, ?, ?);";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ) {
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
        String query = "SELECT * FROM question_attempts WHERE user_id = ? ORDER BY \"timestamp\" ASC";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);

            try (ResultSet results = pst.executeQuery()) {
                return resultsToMapOfValidationResponseByPageId(results);
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (IOException e) {
            throw new SegueDatabaseException("Exception while parsing json", e);
        }
    }

    @Override
    public Map<String, Map<String, List<QuestionValidationResponse>>> getQuestionAttempts(final Long userId, final String questionPageId)
            throws SegueDatabaseException {
        String query = "SELECT * FROM question_attempts WHERE user_id = ? AND question_id LIKE ? ORDER BY \"timestamp\" ASC";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);
            pst.setString(2, questionPageId.replace("_", "\\_") + "%");

            try (ResultSet results = pst.executeQuery()) {
                return resultsToMapOfValidationResponseByPageId(results);
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (IOException e) {
            throw new SegueDatabaseException("Exception while parsing json", e);
        }
    }

    public Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>> getLightweightQuestionAttemptsByUsers(final List<Long> userIds)
            throws SegueDatabaseException {

        if (userIds.isEmpty()) {
            return Maps.newHashMap();
        }

        try (Connection conn = database.getDatabaseConnection()) {
            StringBuilder query = new StringBuilder();
            query.append("SELECT id, user_id, question_id, correct, timestamp FROM question_attempts WHERE");

            // add all of the user ids we are interested in.
            if (!userIds.isEmpty()) {
                String inParams = userIds.stream().map(u -> "?").collect(Collectors.joining(", "));
                query.append(String.format(" user_id IN (%s)", inParams));
            }
            query.append(" ORDER BY \"timestamp\" ASC");

            try (PreparedStatement pst = conn.prepareStatement(query.toString())) {

                Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>> mapToReturn = Maps.newHashMap();

                int index = 1;
                for (Long userId : userIds) {
                    pst.setLong(index++, userId);
                    mapToReturn.put(userId, new HashMap<>());
                }

                try (ResultSet results = pst.executeQuery()) {
                    while (results.next()) {
                        LightweightQuestionValidationResponse partialQuestionAttempt = resultsToLightweightValidationResponse(results);

                        String questionPageId = extractPageIdFromQuestionId(partialQuestionAttempt.getQuestionId());
                        String questionId = partialQuestionAttempt.getQuestionId();
                        Long userId = results.getLong("user_id");

                        Map<String, Map<String, List<LightweightQuestionValidationResponse>>> mapOfQuestionAttemptsByPage
                                = mapToReturn.get(userId);

                        Map<String, List<LightweightQuestionValidationResponse>> attemptsForThisQuestionPage =
                                mapOfQuestionAttemptsByPage.computeIfAbsent(questionPageId, k -> Maps.newHashMap());

                        List<LightweightQuestionValidationResponse> listOfResponses =
                                attemptsForThisQuestionPage.computeIfAbsent(questionId, k -> Lists.newArrayList());

                        listOfResponses.add(partialQuestionAttempt);
                    }
                    return mapToReturn;
                }
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
    
    @Override
    public Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>>
        getMatchingLightweightQuestionAttempts(final List<Long> userIds, final List<String> allQuestionPageIds)
            throws SegueDatabaseException {
        if (allQuestionPageIds.isEmpty()) {
            log.error("Attempted to fetch group progress for an empty gameboard.");
            return Collections.emptyMap();
        }
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> uniquePageIds = allQuestionPageIds.stream().distinct().collect(Collectors.toList());
        if (uniquePageIds.size() > MAX_PAGE_IDS_TO_MATCH) {
            // The repeated "OR question_id LIKE ___" will get very inefficient beyond this:
            log.debug(String.format("Attempting to match too many (%s) question page IDs; returning all attempts for these users instead!", uniquePageIds.size()));
            return this.getLightweightQuestionAttemptsByUsers(userIds);
        }

        try (Connection conn = database.getDatabaseConnection()) {
            StringBuilder query = new StringBuilder();
            query.append("SELECT id, user_id, question_id, correct, timestamp FROM question_attempts WHERE");
            
            // add all of the user ids we are interested in.
            if (!userIds.isEmpty()) {
                String inParams = userIds.stream().map(u -> "?").collect(Collectors.joining(", "));
                query.append(String.format(" user_id IN (%s)", inParams));
            }

            // add all of the question page ids we are interested in
            String questionIdsOr = uniquePageIds.stream().map(q -> "question_id LIKE ?").collect(Collectors.joining(" OR "));
            query.append(" AND (").append(questionIdsOr).append(")");
            query.append(" ORDER BY \"timestamp\" ASC");
            
            try (PreparedStatement pst = conn.prepareStatement(query.toString())) {

                Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>> mapToReturn
                        = Maps.newHashMap();

                int index = 1;
                for (Long userId : userIds) {
                    pst.setLong(index++, userId);
                    mapToReturn.put(userId, new HashMap<>());
                }

                for (String pageId : uniquePageIds) {
                    // Using LIKE matching on part IDs; add % wildcard to end of each page ID, and ensure any underscores
                    // in the ID are escaped and not treated as wildcard characters themselves:
                    pst.setString(index++, pageId.replace("_", "\\_") + "%");
                }

                try (ResultSet results = pst.executeQuery()) {
                    while (results.next()) {
                        LightweightQuestionValidationResponse partialQuestionAttempt = resultsToLightweightValidationResponse(results);

                        String questionPageId = extractPageIdFromQuestionId(partialQuestionAttempt.getQuestionId());
                        String questionId = partialQuestionAttempt.getQuestionId();
                        Long userId = results.getLong("user_id");

                        Map<String, Map<String, List<LightweightQuestionValidationResponse>>> mapOfQuestionAttemptsByPage
                                = mapToReturn.get(userId);

                        Map<String, List<LightweightQuestionValidationResponse>> attemptsForThisQuestionPage
                                = mapOfQuestionAttemptsByPage.computeIfAbsent(questionPageId, k -> Maps.newHashMap());

                        List<LightweightQuestionValidationResponse> listOfResponses
                                = attemptsForThisQuestionPage.computeIfAbsent(questionId, k -> Lists.newArrayList());

                        listOfResponses.add(partialQuestionAttempt);
                    }
                    return mapToReturn;
                }
            }
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
    public Map<Role, Long> getAnsweredQuestionRolesOverPrevious(TimeInterval timeInterval) throws SegueDatabaseException {
        String query = "SELECT role, count(DISTINCT users.id) FROM question_attempts" +
                " JOIN users ON user_id=users.id AND NOT deleted WHERE timestamp > now() - ? GROUP BY role";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setObject(1, timeInterval.getPGInterval());

            try (ResultSet results = pst.executeQuery()) {
                Map<Role, Long> resultsToReturn = Maps.newHashMap();
                while (results.next()) {
                    resultsToReturn.put(Role.valueOf(results.getString("role")), results.getLong("count"));
                }
                return resultsToReturn;
            }
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

        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(queryToBuild.toString());
        ) {
            pst.setLong(1, userId);
            pst.setTimestamp(2, new java.sql.Timestamp(fromDate.getTime()));
            pst.setTimestamp(3, new java.sql.Timestamp(toDate.getTime()));

            try (ResultSet results = pst.executeQuery()) {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

                Map<Date, Long> mapToReturn = Maps.newHashMap();
                while (results.next()) {
                    mapToReturn.put(formatter.parse(results.getString("to_char")), results.getLong("count"));
                }
                return mapToReturn;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        } catch (ParseException e) {
            throw new SegueDatabaseException("Unable to parse date exception", e);
        }
    }

    private LightweightQuestionValidationResponse resultsToLightweightValidationResponse(final ResultSet results) throws SQLException {
        LightweightQuestionValidationResponse partialQuestionAttempt = new QuestionValidationResponse();

        partialQuestionAttempt.setCorrect(results.getBoolean("correct"));
        partialQuestionAttempt.setQuestionId(results.getString("question_id"));
        partialQuestionAttempt.setDateAttempted(results.getTimestamp("timestamp"));

        return partialQuestionAttempt;
    }

    private Map<String, Map<String, List<QuestionValidationResponse>>> resultsToMapOfValidationResponseByPageId(final ResultSet results) throws SQLException, JsonProcessingException {
        // Since we go to the effort of sorting the attempts in Postgres, use LinkedHashMap which is ordered:
        Map<String, Map<String, List<QuestionValidationResponse>>> mapOfQuestionAttemptsByPage = Maps.newLinkedHashMap();

        while (results.next()) {
            QuestionValidationResponse questionAttempt = objectMapper.readValue(
                    results.getString("question_attempt"), QuestionValidationResponse.class);
            String pageId = extractPageIdFromQuestionId(questionAttempt.getQuestionId());
            String questionId = questionAttempt.getQuestionId();

            Map<String, List<QuestionValidationResponse>> attemptsForThisQuestionPage
                    = mapOfQuestionAttemptsByPage.computeIfAbsent(pageId, k -> Maps.newLinkedHashMap());

            List<QuestionValidationResponse> listOfResponses
                    = attemptsForThisQuestionPage.computeIfAbsent(questionId, k -> Lists.newArrayList());

            listOfResponses.add(questionAttempt);
        }
        return mapOfQuestionAttemptsByPage;
    }
}
