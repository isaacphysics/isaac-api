/**
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

import static uk.ac.cam.cl.dtg.segue.api.Constants.ANONYMOUS_SESSION_DURATION_IN_MINUTES;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;

/**
 * @author sac92
 *
 */
public class PgQuestionAttempts implements IQuestionAttemptManager {
    private static final Logger log = LoggerFactory.getLogger(PgQuestionAttempts.class);
            
    private final PostgresSqlDb database;
    private final ObjectMapper objectMapper;

    // cache of anonymousUserId --> Map of questionPageId --> Map of questionId --> List of Validation responses
    private final Cache<String, Map<String, Map<String, List<QuestionValidationResponse>>>> 
        anonymousQuestionAttemptsCache;

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
        anonymousQuestionAttemptsCache = CacheBuilder.newBuilder()
                .expireAfterAccess(ANONYMOUS_SESSION_DURATION_IN_MINUTES, TimeUnit.MINUTES)
                .<String, Map<String, Map<String, List<QuestionValidationResponse>>>> build();
    }
    
    @Override
    public void registerAnonymousQuestionAttempt(final String userId, final String questionPageId,
            final String fullQuestionId, final QuestionValidationResponse questionAttempt)
            throws SegueDatabaseException {
        Map<String, Map<String, List<QuestionValidationResponse>>> userAttempts = anonymousQuestionAttemptsCache
                .getIfPresent(userId);

        if (null == userAttempts) {
            userAttempts = new HashMap<String, Map<String, List<QuestionValidationResponse>>>();
            anonymousQuestionAttemptsCache.put(userId, userAttempts);
        }

        if (userAttempts.get(questionPageId) == null) {
            userAttempts.put(questionPageId, new HashMap<String, List<QuestionValidationResponse>>());
        }

        if (userAttempts.get(questionPageId).get(fullQuestionId) == null) {
            userAttempts.get(questionPageId).put(fullQuestionId, new ArrayList<QuestionValidationResponse>());
        }

        userAttempts.get(questionPageId).get(fullQuestionId).add(questionAttempt);
    }

    /**
     * 
     * @param anonymousId
     *            to lookup
     * @return the question pageId --> full questionId --> list of responses. (or null if no data)
     */
    @Override
    public Map<String, Map<String, List<QuestionValidationResponse>>> getAnonymousQuestionAttempts(
            final String anonymousId) {
        if (this.anonymousQuestionAttemptsCache.getIfPresent(anonymousId) != null) {
            return this.anonymousQuestionAttemptsCache.getIfPresent(anonymousId);
        } else {
            return Maps.newHashMap();
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
            pst = conn.prepareStatement("Select * FROM question_attempts WHERE user_id = ? ORDER BY \"timestamp\" ASC");
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

        // delete the session attribute as merge has completed.
        this.anonymousQuestionAttemptsCache.invalidate(anonymousUserId);
    }
}
