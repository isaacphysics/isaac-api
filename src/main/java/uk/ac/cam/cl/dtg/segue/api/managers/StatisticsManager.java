/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.api.managers;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.services.ContentSummarizerService;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;
import uk.ac.cam.cl.dtg.isaac.dos.Difficulty;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TimeInterval.*;

/**
 * StatisticsManager.
 * TODO this file is a mess... it needs refactoring.
 */
public class StatisticsManager implements IStatisticsManager {
    private UserAccountManager userManager;
    private ILogManager logManager;
    private final GitContentManager contentManager;
    private GroupManager groupManager;
    private QuestionManager questionManager;
    private ContentSummarizerService contentSummarizerService;
    private IUserStreaksManager userStreaksManager;
    private static final Logger log = LoggerFactory.getLogger(StatisticsManager.class);
    private static final int PROGRESS_MAX_RECENT_QUESTIONS = 5;

    
    /**
     * StatisticsManager.
     * 
     * @param userManager
     *            - to query user information
     * @param logManager
     *            - to query Log information
     * @param contentManager
     *            - to query live version information
     * @param groupManager
     *            - so that we can see how many groups we have site wide.
     * @param questionManager
     *            - so that we can see how many questions were answered.
     * @param contentSummarizerService
     *            - to produce content summary objects
     */
    @Inject
    public StatisticsManager(final UserAccountManager userManager, final ILogManager logManager,
                             final GitContentManager contentManager, final GroupManager groupManager,
                             final QuestionManager questionManager, final ContentSummarizerService contentSummarizerService,
                             final IUserStreaksManager userStreaksManager) {
        this.userManager = userManager;
        this.logManager = logManager;

        this.contentManager = contentManager;

        this.groupManager = groupManager;
        this.questionManager = questionManager;
        this.contentSummarizerService = contentSummarizerService;
        this.userStreaksManager = userStreaksManager;
    }

    /**
     * Output general stats. This returns a Map of String to Object and is intended to be sent directly to a
     * serializable facade endpoint.
     * 
     * @return ImmutableMap<String, String> (stat name, stat value)
     * @throws SegueDatabaseException - if there is a database error.
     */
    public synchronized Map<String, Object> getGeneralStatistics()
            throws SegueDatabaseException {
        Map<String, Object> result = Maps.newHashMap();

        result.put("userGenders", userManager.getGenderCount());
        result.put("userRoles", userManager.getRoleCount());
        result.put("userSchoolInfo", userManager.getSchoolInfoStats());
        result.put("groupCount", this.groupManager.getGroupCount());

        result.put("viewQuestionEvents", logManager.getLogCountByType(IsaacServerLogType.VIEW_QUESTION.name()));
        result.put("answeredQuestionEvents", logManager.getLogCountByType(SegueServerLogType.ANSWER_QUESTION.name()));
        result.put("viewConceptEvents", logManager.getLogCountByType(IsaacServerLogType.VIEW_CONCEPT.name()));

        Map<String, Map<Role, Long>> rangedActiveUserStats = Maps.newHashMap();
        rangedActiveUserStats.put("sevenDays", userManager.getActiveRolesOverPrevious(SEVEN_DAYS));
        rangedActiveUserStats.put("thirtyDays", userManager.getActiveRolesOverPrevious(THIRTY_DAYS));
        rangedActiveUserStats.put("ninetyDays", userManager.getActiveRolesOverPrevious(NINETY_DAYS));
        rangedActiveUserStats.put("sixMonths", userManager.getActiveRolesOverPrevious(SIX_MONTHS));
        rangedActiveUserStats.put("twoYears", userManager.getActiveRolesOverPrevious(TWO_YEARS));
        result.put("activeUsersOverPrevious", rangedActiveUserStats);

        Map<String, Map<Role, Long>> rangedAnsweredQuestionStats = Maps.newHashMap();
        rangedAnsweredQuestionStats.put("sevenDays", questionManager.getAnsweredQuestionRolesOverPrevious(SEVEN_DAYS));
        rangedAnsweredQuestionStats.put("thirtyDays", questionManager.getAnsweredQuestionRolesOverPrevious(THIRTY_DAYS));
        rangedAnsweredQuestionStats.put("ninetyDays", questionManager.getAnsweredQuestionRolesOverPrevious(NINETY_DAYS));
        result.put("answeringUsersOverPrevious", rangedAnsweredQuestionStats);

        return result;
    }

    /**
     * LogCount.
     * 
     * @param logTypeOfInterest
     *            - the log event that we care about.
     * @return the number of logs of that type (or an estimate).
     * @throws SegueDatabaseException
     *             if there is a problem with the database.
     */
    public Long getLogCount(final String logTypeOfInterest) throws SegueDatabaseException {
        return this.logManager.getLogCountByType(logTypeOfInterest);
    }

    /**
     * getUserQuestionInformation. Produces a map that contains information about the total questions attempted,
     * (and those correct) "totalQuestionsAttempted", "totalCorrect",
     *  ,"attemptsByTag", questionAttemptsByLevelStats.
     * 
     * @param userOfInterest
     *            - the user you wish to compile statistics for.
     * @return gets high level statistics about the questions a user has completed.
     * @throws SegueDatabaseException
     *             - if something went wrong with the database.
     * @throws ContentManagerException
     *             - if we are unable to look up the content.
     */
    public Map<String, Object> getUserQuestionInformation(final RegisteredUserDTO userOfInterest)
            throws SegueDatabaseException, ContentManagerException {
        Validate.notNull(userOfInterest);

        // FIXME: there was a TODO here about tidying this up and moving it elsewhere.
        // It has been improved and tidied, but may still better belong elsewhere . . .

        int correctQuestions = 0;
        int attemptedQuestions = 0;
        int correctQuestionParts = 0;
        int attemptedQuestionParts = 0;
        int correctQuestionsThisAcademicYear = 0;
        int attemptedQuestionsThisAcademicYear = 0;
        int correctQuestionPartsThisAcademicYear = 0;
        int attemptedQuestionPartsThisAcademicYear = 0;
        Map<String, Integer> questionAttemptsByTagStats = Maps.newHashMap();
        Map<String, Integer> questionsCorrectByTagStats = Maps.newHashMap();
        Map<Stage, Map<Difficulty, Integer>> questionAttemptsByStageAndDifficultyStats = Maps.newHashMap();
        Map<Stage, Map<Difficulty, Integer>> questionsCorrectByStageAndDifficultyStats = Maps.newHashMap();
        Map<String, Integer> questionAttemptsByLevelStats = Maps.newHashMap();
        Map<String, Integer> questionsCorrectByLevelStats = Maps.newHashMap();
        Map<String, Integer> questionAttemptsByTypeStats = Maps.newHashMap();
        Map<String, Integer> questionsCorrectByTypeStats = Maps.newHashMap();
        List<ContentDTO> questionPagesNotComplete = Lists.newArrayList();
        Queue<ContentDTO> mostRecentlyAttemptedQuestionPages = new CircularFifoQueue<>(PROGRESS_MAX_RECENT_QUESTIONS);

        LocalDate now = LocalDate.now();
        LocalDate endOfAugustThisYear = LocalDate.of(now.getYear(), Month.AUGUST, 31);
        LocalDate endOfAugustLastYear = LocalDate.of(now.getYear() -1, Month.AUGUST, 31);
        LocalDate lastDayOfPreviousAcademicYear =
                now.isAfter(endOfAugustThisYear) ? endOfAugustThisYear : endOfAugustLastYear;

        Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsByUser = questionManager.getQuestionAttemptsByUser(userOfInterest);
        Map<String, ContentDTO> questionMap = this.getQuestionMap(questionAttemptsByUser.keySet());

        // Loop through each Question attempted:
        for (Entry<String, Map<String, List<QuestionValidationResponse>>> question : questionAttemptsByUser.entrySet()) {
            ContentDTO questionContentDTO = questionMap.get(question.getKey());
            if (null == questionContentDTO) {
                log.warn(String.format("Excluding missing question (%s) from user progress statistics for user (%s)!",
                        question.getKey(), userOfInterest.getId()));
                // We no longer have any information on this question, so we won't count it towards statistics!
                continue;
            }

            mostRecentlyAttemptedQuestionPages.add(questionContentDTO);  // Assumes questionAttemptsByUser is sorted!
            attemptedQuestions++;
            boolean questionIsCorrect = true;  // Are all Parts of the Question correct?
            LocalDate mostRecentCorrectQuestionPart = null;
            LocalDate mostRecentAttemptAtQuestion = null;
            // Loop through each Part of the Question:
            for (QuestionDTO questionPart : GameManager.getAllMarkableQuestionPartsDFSOrder(questionContentDTO)) {

                boolean questionPartIsCorrect = false;  // Is this Part of the Question correct?
                // Has the user attempted this part of the question at all?
                if (question.getValue().containsKey(questionPart.getId())) {
                    attemptedQuestionParts++;

                    LocalDate mostRecentAttemptAtThisQuestionPart = null;

                    // Loop through each attempt at the Question Part if they have attempted it:
                    for (QuestionValidationResponse validationResponse : question.getValue().get(questionPart.getId())) {
                        LocalDate dateAttempted = LocalDateTime.ofInstant(
                                validationResponse.getDateAttempted().toInstant(), ZoneId.systemDefault()).toLocalDate();
                        if (mostRecentAttemptAtThisQuestionPart == null || dateAttempted.isAfter(mostRecentAttemptAtThisQuestionPart)) {
                            mostRecentAttemptAtThisQuestionPart = dateAttempted;
                        }
                        if (validationResponse.isCorrect() != null && validationResponse.isCorrect()) {
                            correctQuestionParts++;
                            if (dateAttempted.isAfter(lastDayOfPreviousAcademicYear)) {
                                correctQuestionPartsThisAcademicYear++;
                                if (mostRecentCorrectQuestionPart == null || dateAttempted.isAfter(mostRecentCorrectQuestionPart)) {
                                    mostRecentCorrectQuestionPart = dateAttempted;
                                }
                            }
                            questionPartIsCorrect = true;
                            break; // early so that later attempts are ignored
                        }
                    }

                    // Type Stats - Count the attempt at the Question Part:
                    String questionPartType = questionPart.getType();
                    if (questionAttemptsByTypeStats.containsKey(questionPartType)) {
                        questionAttemptsByTypeStats.put(questionPartType, questionAttemptsByTypeStats.get(questionPartType) + 1);
                    } else {
                        questionAttemptsByTypeStats.put(questionPartType, 1);
                    }

                    if (mostRecentAttemptAtThisQuestionPart != null) {
                        if (mostRecentAttemptAtThisQuestionPart.isAfter(lastDayOfPreviousAcademicYear)) {
                            attemptedQuestionPartsThisAcademicYear++;
                        }

                        if (mostRecentAttemptAtQuestion == null || mostRecentAttemptAtThisQuestionPart.isAfter(mostRecentAttemptAtQuestion)) {
                            mostRecentAttemptAtQuestion = mostRecentAttemptAtThisQuestionPart;
                        }
                    }

                    // If this Question Part is correct, count this too:
                    if (questionPartIsCorrect) {
                        if (questionsCorrectByTypeStats.containsKey(questionPartType)) {
                            questionsCorrectByTypeStats.put(questionPartType, questionsCorrectByTypeStats.get(questionPartType) + 1);
                        } else {
                            questionsCorrectByTypeStats.put(questionPartType, 1);
                        }
                    }
                }

                // Correctness of whole Question: is the Question correct so far, and is this Question Part also correct?
                questionIsCorrect = questionIsCorrect && questionPartIsCorrect;
            }

            // Tag Stats - Loop through the Question's tags:
            for (String tag : questionContentDTO.getTags()) {
                // Count the attempt at the Question:
                if (questionAttemptsByTagStats.containsKey(tag)) {
                    questionAttemptsByTagStats.put(tag, questionAttemptsByTagStats.get(tag) + 1);
                } else {
                    questionAttemptsByTagStats.put(tag, 1);
                }
                // If it's correct, count this too:
                if (questionIsCorrect) {
                    if (questionsCorrectByTagStats.containsKey(tag)) {
                        questionsCorrectByTagStats.put(tag, questionsCorrectByTagStats.get(tag) + 1);
                    } else {
                        questionsCorrectByTagStats.put(tag, 1);
                    }
                }
            }

            // Stage and difficulty Stats
            // This is hideous, sorry
            if (questionContentDTO.getAudience() != null) {
                for (AudienceContext audience : questionContentDTO.getAudience()) {
                    // Check the question has both a stage and a difficulty
                    if (audience.getStage() != null && audience.getDifficulty() != null) {
                        Stage currentStage = audience.getStage().get(0);
                        Difficulty currentDifficulty = audience.getDifficulty().get(0);
                        // Count the attempt at the question
                        if (questionAttemptsByStageAndDifficultyStats.containsKey(currentStage)) {
                            if (questionAttemptsByStageAndDifficultyStats.get(currentStage).containsKey(currentDifficulty)) {
                                questionAttemptsByStageAndDifficultyStats.get(currentStage)
                                .put(currentDifficulty, questionAttemptsByStageAndDifficultyStats.get(currentStage).get(currentDifficulty) + 1);
                            } else {
                                questionAttemptsByStageAndDifficultyStats.get(currentStage).put(currentDifficulty, 1);
                            }
                        } else {
                            Map<Difficulty, Integer> newDifficultyMap = Maps.newHashMap();
                            newDifficultyMap.put(currentDifficulty, 1);
                            questionAttemptsByStageAndDifficultyStats.put(currentStage, newDifficultyMap);
                        }

                        // If correct, count this too:
                        if (questionIsCorrect) {
                            if (questionsCorrectByStageAndDifficultyStats.containsKey(currentStage)) {
                                if (questionsCorrectByStageAndDifficultyStats.get(currentStage).containsKey(currentDifficulty)) {
                                    questionsCorrectByStageAndDifficultyStats.get(currentStage)
                                    .put(currentDifficulty, questionsCorrectByStageAndDifficultyStats.get(currentStage).get(currentDifficulty) + 1);
                                } else {
                                    questionsCorrectByStageAndDifficultyStats.get(currentStage).put(currentDifficulty, 1);
                                }
                            } else {
                                Map<Difficulty, Integer> newDifficultyMap = Maps.newHashMap();
                                newDifficultyMap.put(currentDifficulty, 1);
                                questionsCorrectByStageAndDifficultyStats.put(currentStage, newDifficultyMap);
                            }
                        }
                    }
                }
            }

            // Level Stats:
            Integer questionLevelInteger = questionContentDTO.getLevel();
            String questionLevel;
            if (null == questionLevelInteger) {
                // There are questions on general pages which cannot have levels, must use a default value.
                questionLevel = "0";
            } else {
                questionLevel = questionLevelInteger.toString();
            }
            if (questionAttemptsByLevelStats.containsKey(questionLevel)) {
                questionAttemptsByLevelStats.put(questionLevel, questionAttemptsByLevelStats.get(questionLevel) + 1);
            } else {
                questionAttemptsByLevelStats.put(questionLevel, 1);
            }

            if (mostRecentAttemptAtQuestion != null && mostRecentAttemptAtQuestion.isAfter(lastDayOfPreviousAcademicYear)) {
                attemptedQuestionsThisAcademicYear++;
            }

            // If it's correct, count this globally and for the Question's level too:
            if (questionIsCorrect) {
                correctQuestions++;
                if (mostRecentCorrectQuestionPart != null && mostRecentCorrectQuestionPart.isAfter(lastDayOfPreviousAcademicYear)) {
                    correctQuestionsThisAcademicYear++;
                }
                if (questionsCorrectByLevelStats.containsKey(questionLevel)) {
                    questionsCorrectByLevelStats.put(questionLevel, questionsCorrectByLevelStats.get(questionLevel) + 1);
                } else {
                    questionsCorrectByLevelStats.put(questionLevel, 1);
                }
            } else if (questionPagesNotComplete.size() < PROGRESS_MAX_RECENT_QUESTIONS) {
                questionPagesNotComplete.add(questionContentDTO);
            }
        }

        // Collate all the information into the JSON response as a Map:
        Map<String, Object> questionInfo = Maps.newHashMap();
        List<ContentSummaryDTO> mostRecentlyAttemptedQuestionsList = mostRecentlyAttemptedQuestionPages
                .stream().map(contentSummarizerService::extractContentSummary).collect(Collectors.toList());
        Collections.reverse(mostRecentlyAttemptedQuestionsList);  // We want most-recent first order and streams cannot reverse.
        List<ContentSummaryDTO> questionsNotCompleteList = questionPagesNotComplete
                .stream().map(contentSummarizerService::extractContentSummary).collect(Collectors.toList());

        questionInfo.put("totalQuestionsAttempted", attemptedQuestions);
        questionInfo.put("totalQuestionsCorrect", correctQuestions);
        questionInfo.put("totalQuestionPartsAttempted", attemptedQuestionParts);
        questionInfo.put("totalQuestionPartsCorrect", correctQuestionParts);
        questionInfo.put("totalQuestionsCorrectThisAcademicYear", correctQuestionsThisAcademicYear);
        questionInfo.put("totalQuestionsAttemptedThisAcademicYear", attemptedQuestionsThisAcademicYear);
        questionInfo.put("totalQuestionPartsCorrectThisAcademicYear", correctQuestionPartsThisAcademicYear);
        questionInfo.put("totalQuestionPartsAttemptedThisAcademicYear", attemptedQuestionPartsThisAcademicYear);
        questionInfo.put("attemptsByTag", questionAttemptsByTagStats);
        questionInfo.put("correctByTag", questionsCorrectByTagStats);
        questionInfo.put("attemptsByStageAndDifficulty", questionAttemptsByStageAndDifficultyStats);
        questionInfo.put("correctByStageAndDifficulty", questionsCorrectByStageAndDifficultyStats);
        questionInfo.put("attemptsByLevel", questionAttemptsByLevelStats);
        questionInfo.put("correctByLevel", questionsCorrectByLevelStats);
        questionInfo.put("attemptsByType", questionAttemptsByTypeStats);
        questionInfo.put("correctByType", questionsCorrectByTypeStats);
        questionInfo.put("oldestIncompleteQuestions", questionsNotCompleteList);
        questionInfo.put("mostRecentQuestions", mostRecentlyAttemptedQuestionsList);
        questionInfo.put("userDetails", this.userManager.convertToUserSummaryObject(userOfInterest));

        return questionInfo;

    }

    @Override
    public Map<String, Object> getDetailedUserStatistics(RegisteredUserDTO userOfInterest) {

        // user streak info
        Map<String, Object> userStreakRecord = userStreaksManager.getCurrentStreakRecord(userOfInterest);
        userStreakRecord.put("largestStreak", userStreaksManager.getLongestStreak(userOfInterest));

        Map<String, Object> userWeeklyStreakRecord = userStreaksManager.getCurrentWeeklyStreakRecord(userOfInterest);
        userWeeklyStreakRecord.put("largestWeeklyStreak", userStreaksManager.getLongestWeeklyStreak(userOfInterest));

        Map<String, Object> result = Maps.newHashMap();
        result.put("dailyStreakRecord", userStreakRecord);
        result.put("weeklyStreakRecord", userWeeklyStreakRecord);

        return result;
    }

    /**
     * Utility method to get a load of question pages by id in one go.
     * 
     * @param ids
     *            to search for
     * @return map of id to content object.
     * @throws ContentManagerException
     *             - if something goes wrong.
     */
    private Map<String, ContentDTO> getQuestionMap(final Collection<String> ids) throws ContentManagerException {
        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

        fieldsToMap.put(immutableEntry(BooleanOperator.OR, ID_FIELDNAME + '.' + UNPROCESSED_SEARCH_FIELD_SUFFIX),
                new ArrayList<>(ids));

        fieldsToMap.put(immutableEntry(BooleanOperator.OR, TYPE_FIELDNAME),
                Arrays.asList(QUESTION_TYPE, FAST_TRACK_QUESTION_TYPE));

        // Search for questions that match the ids.
        ResultsWrapper<ContentDTO> allMatchingIds =
                this.contentManager.getUnsafeCachedContentDTOsMatchingIds(ids,
                        0, ids.size());

        List<ContentDTO> questionsForGameboard = allMatchingIds.getResults();

        Map<String, ContentDTO> questionIdToQuestionMap = Maps.newHashMap();
        for (ContentDTO content : questionsForGameboard) {
            if (content != null) {
                questionIdToQuestionMap.put(content.getId(), content);
            }
        }

        return questionIdToQuestionMap;
    }
}
