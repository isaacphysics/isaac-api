/*
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.api.managers;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.inject.name.Named;
import org.apache.commons.lang3.Validate;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.LocationManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.util.locations.Location;
import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.api.client.util.Sets;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * StatisticsManager.
 * TODO this file is a mess... it needs refactoring.
 */
public class StatisticsManager implements IStatisticsManager {
    private UserAccountManager userManager;
    private ILogManager logManager;
    private SchoolListReader schoolManager;
    private final IContentManager contentManager;
    private final String contentIndex;
    private GroupManager groupManager;
    private QuestionManager questionManager;
    private GameManager gameManager;
    private IUserStreaksManager userStreaksManager;
    
    private Cache<String, Object> longStatsCache;
    private LocationManager locationHistoryManager;

    private static final Logger log = LoggerFactory.getLogger(StatisticsManager.class);
    private static final String GENERAL_STATS = "GENERAL_STATS";
    private static final String SCHOOL_STATS = "SCHOOL_STATS";
    private static final String LOCATION_STATS = "LOCATION_STATS";
    private static final int LONG_STATS_EVICTION_INTERVAL_MINUTES = 720; // 12 hours
    private static final long LONG_STATS_MAX_ITEMS = 20;

    
    /**
     * StatisticsManager.
     * 
     * @param userManager
     *            - to query user information
     * @param logManager
     *            - to query Log information
     * @param schoolManager
     *            - to query School information
     * @param contentManager
     *            - to query live version information
     * @param locationHistoryManager
     *            - so that we can query our location database (ip addresses)
     * @param groupManager
     *            - so that we can see how many groups we have site wide.
     * @param questionManager
     *            - so that we can see how many questions were answered.
     */
    @Inject
    public StatisticsManager(final UserAccountManager userManager, final ILogManager logManager,
            final SchoolListReader schoolManager, final IContentManager contentManager, @Named(CONTENT_INDEX) final String contentIndex,
                             final LocationManager locationHistoryManager, final GroupManager groupManager,
            final QuestionManager questionManager, final GameManager gameManager,
                             final IUserStreaksManager userStreaksManager) {
        this.userManager = userManager;
        this.logManager = logManager;
        this.schoolManager = schoolManager;

        this.contentManager = contentManager;
        this.contentIndex = contentIndex;

        this.locationHistoryManager = locationHistoryManager;
        this.groupManager = groupManager;
        this.questionManager = questionManager;
        this.gameManager = gameManager;
        this.userStreaksManager = userStreaksManager;

        this.longStatsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(LONG_STATS_EVICTION_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .maximumSize(LONG_STATS_MAX_ITEMS).build();
    }

    /**
     * Output general stats. This returns a Map of String to Object and is intended to be sent directly to a
     * serializable facade endpoint.
     * 
     * @return ImmutableMap<String, String> (stat name, stat value)
     * @throws SegueDatabaseException - if there is a database error.
     */
    public synchronized Map<String, Object> outputGeneralStatistics() 
            throws SegueDatabaseException {
        @SuppressWarnings("unchecked")
        Map<String, Object> cachedOutput = (Map<String, Object>) this.longStatsCache.getIfPresent(GENERAL_STATS);
        if (cachedOutput != null) {
            log.info("Using cached General Statistics");
            return cachedOutput;
        } else {
            log.info("Calculating General Statistics");
        }

        // get all the users
        List<RegisteredUserDTO> users = userManager.findUsers(new RegisteredUserDTO());
        ImmutableMap.Builder<String, Object> ib = new ImmutableMap.Builder<>();

        List<RegisteredUserDTO> male = Lists.newArrayList();
        List<RegisteredUserDTO> female = Lists.newArrayList();
        List<RegisteredUserDTO> otherGender = Lists.newArrayList();
        ib.put("totalUsers", "" + users.size());

        List<RegisteredUserDTO> studentOrUnknownRole = Lists.newArrayList();
        List<RegisteredUserDTO> teacherRole = Lists.newArrayList();
        List<RegisteredUserDTO> adminStaffRole = Lists.newArrayList();
        List<RegisteredUserDTO> contentEditorStaffRole = Lists.newArrayList();
        List<RegisteredUserDTO> testerRole = Lists.newArrayList();
        List<RegisteredUserDTO> staffRole = Lists.newArrayList();
        List<RegisteredUserDTO> eventManagerStaffRole = Lists.newArrayList();
        List<RegisteredUserDTO> hasSchool = Lists.newArrayList();
        List<RegisteredUserDTO> hasNoSchool = Lists.newArrayList();
        List<RegisteredUserDTO> hasOtherSchool = Lists.newArrayList();
        Map<String, Date> lastSeenMap = Maps.newHashMap();

        for (RegisteredUserDTO user : users) {
            if (user.getGender() == null) {
                otherGender.add(user);
            } else {
                switch (user.getGender()) {
                    case MALE:
                        male.add(user);
                        break;
                    case FEMALE:
                        female.add(user);
                        break;
                    case OTHER:
                        otherGender.add(user);
                        break;
                    default:
                        otherGender.add(user);
                        break;
                }

            }

            if (user.getRole() == null) {
                studentOrUnknownRole.add(user);
            } else {
                switch (user.getRole()) {
                    case STUDENT:
                        studentOrUnknownRole.add(user);
                        break;
                    case ADMIN:
                        adminStaffRole.add(user);
                        break;
                    case CONTENT_EDITOR:
                    	contentEditorStaffRole.add(user);
                        break;
                    case EVENT_MANAGER:
                    	eventManagerStaffRole.add(user);
                        break;
                    case TEACHER:
                        teacherRole.add(user);
                        break;
                    case STAFF:
                        staffRole.add(user);
                        break;
                    case TESTER:
                    	testerRole.add(user);
                        break;
                    default:
                        studentOrUnknownRole.add(user);
                        break;
                }
            }

            if (user.getSchoolId() == null && user.getSchoolOther() == null) {
                hasNoSchool.add(user);
            } else {
                hasSchool.add(user);
                if (user.getSchoolOther() != null) {
                    hasOtherSchool.add(user);
                }
            }

            if (user.getLastSeen() != null) {
                lastSeenMap.put(user.getId().toString(), user.getLastSeen());
            } else if (user.getRegistrationDate() != null) {
                lastSeenMap.put(user.getId().toString(), user.getRegistrationDate());
            }

        }

        Map<String, Object> gender = Maps.newHashMap();
        gender.put(Gender.MALE.toString(), male.size());
        gender.put(Gender.FEMALE.toString(), female.size());
        gender.put(Gender.OTHER.toString(), otherGender.size());
        ib.put("gender", gender);

        Map<String, Object> role = Maps.newHashMap();
        role.put(Role.ADMIN.toString(), adminStaffRole.size());
        role.put(Role.CONTENT_EDITOR.toString(), contentEditorStaffRole.size());
        role.put(Role.EVENT_MANAGER.toString(), eventManagerStaffRole.size());
        role.put(Role.TEACHER.toString(), teacherRole.size());
        role.put(Role.TESTER.toString(), testerRole.size());
        role.put(Role.STUDENT.toString(), studentOrUnknownRole.size());
        role.put(Role.STAFF.toString(), staffRole.size());
        ib.put("role", role);

        ib.put("viewQuestionEvents", "" + logManager.getLogCountByType(VIEW_QUESTION));
        ib.put("answeredQuestionEvents", "" + logManager.getLogCountByType(ANSWER_QUESTION));

        ib.put("hasSchool", "" + hasSchool.size());
        ib.put("hasNoSchool", "" + hasNoSchool.size());
        ib.put("hasSchoolOther", "" + hasOtherSchool.size());

        log.debug("Calculating general stats - 2. Last seen map");

        final int sevenDays = 7;
        final int thirtyDays = 30;
        final int sixMonthsInDays = 180;

        List<RegisteredUserDTO> nonStaffUsers = Lists.newArrayList();
        nonStaffUsers.addAll(teacherRole);
        nonStaffUsers.addAll(studentOrUnknownRole);

        ib.put("activeInLastSixMonths", this.getNumberOfUsersActiveForLastNDays(users, lastSeenMap, sixMonthsInDays)
                .size());
        
        ib.put("activeTeachersLastWeek",
                "" + this.getNumberOfUsersActiveForLastNDays(teacherRole, lastSeenMap, sevenDays).size());
        ib.put("activeTeachersLastThirtyDays",
                "" + this.getNumberOfUsersActiveForLastNDays(teacherRole, lastSeenMap, thirtyDays).size());

        ib.put("activeStudentsLastWeek",
                "" + this.getNumberOfUsersActiveForLastNDays(studentOrUnknownRole, lastSeenMap, sevenDays).size());
        ib.put("activeStudentsLastThirtyDays",
                "" + this.getNumberOfUsersActiveForLastNDays(studentOrUnknownRole, lastSeenMap, thirtyDays).size());

        ib.put("activeUsersLastWeek",
                "" + this.getNumberOfUsersActiveForLastNDays(nonStaffUsers, lastSeenMap, sevenDays).size());
        ib.put("activeUsersLastThirtyDays",
                "" + this.getNumberOfUsersActiveForLastNDays(nonStaffUsers, lastSeenMap, thirtyDays).size());

        Map<String, Date> lastSeenUserMapQuestions = this.getLastSeenUserMap(ANSWER_QUESTION);
        ib.put("questionsAnsweredLastWeekTeachers",
                "" + this.getNumberOfUsersActiveForLastNDays(teacherRole, lastSeenUserMapQuestions, sevenDays).size());
        ib.put("questionsAnsweredLastThirtyDaysTeachers",
                "" + this.getNumberOfUsersActiveForLastNDays(teacherRole, lastSeenUserMapQuestions, thirtyDays).size());

        ib.put("questionsAnsweredLastWeekStudents",
                ""
                        + this.getNumberOfUsersActiveForLastNDays(studentOrUnknownRole, lastSeenUserMapQuestions,
                                sevenDays).size());
        ib.put("questionsAnsweredLastThirtyDaysStudents",
                ""
                        + this.getNumberOfUsersActiveForLastNDays(studentOrUnknownRole, lastSeenUserMapQuestions,
                                thirtyDays).size());
        
        ib.put("groupCount", groupManager.getGroupCount());
        
        Map<String, Object> result = ib.build();
        this.longStatsCache.put(GENERAL_STATS, result);

        log.info("Finished calculating General Statistics");

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
     * Get an overview of all school performance. This is for analytics / admin users.
     * 
     * @return list of school to statistics mapping. The object in the map is another map with keys connections,
     *         numberActiveLastThirtyDays.
     * 
     * @throws UnableToIndexSchoolsException
     *             - if there is a problem getting school details.
     * @throws SegueDatabaseException
     *             - if there is a database exception.
     */
    public List<Map<String, Object>> getSchoolStatistics() 
            throws UnableToIndexSchoolsException, SegueDatabaseException, SegueSearchException {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cachedOutput = (List<Map<String, Object>>) this.longStatsCache
                .getIfPresent(SCHOOL_STATS);
        if (cachedOutput != null) {
            log.debug("Using cached statistics.");
            return cachedOutput;
        } else {
            log.info("Calculating School Statistics");
        }

        Map<School, List<RegisteredUserDTO>> map = getUsersBySchool();

        final String school = "school";
        final String connections = "connections";
        final String teachers = "teachers";
        final String numberActive = "numberActiveLastThirtyDays";
        final String teachersActive = "teachersActiveLastThirtyDays";
        final int thirtyDays = 30;

        Map<String, Date> lastSeenUserMap = getLastSeenUserMap();
        List<Map<String, Object>> result = Lists.newArrayList();
        for (Entry<School, List<RegisteredUserDTO>> e : map.entrySet()) {

            List<RegisteredUserDTO> teachersConnected = Lists.newArrayList();
            for (RegisteredUserDTO user : e.getValue()) {
                if (user.getRole() != null && user.getRole().equals(Role.TEACHER)) {
                    teachersConnected.add(user);
                }
            }

            result.add(ImmutableMap.of(
                school, e.getKey(),
                connections, e.getValue().size(),
                teachers, teachersConnected.size(),
                numberActive, getNumberOfUsersActiveForLastNDays(e.getValue(), lastSeenUserMap, thirtyDays).size(),
                teachersActive, getNumberOfUsersActiveForLastNDays(teachersConnected, lastSeenUserMap, thirtyDays).size()
            ));
        }

        Collections.sort(result, new Comparator<Map<String, Object>>() {
            /**
             * Descending numerical order
             */
            @Override
            public int compare(final Map<String, Object> o1, final Map<String, Object> o2) {

                if ((Integer) o1.get(numberActive) < (Integer) o2.get(numberActive)) {
                    return 1;
                }

                if ((Integer) o1.get(numberActive) > (Integer) o2.get(numberActive)) {
                    return -1;
                }

                return 0;
            }
        });

        this.longStatsCache.put(SCHOOL_STATS, result);

        return result;
    }

    /**
     * Get the number of users per school.
     * 
     * @return A map of schools to integers (representing the number of registered users)
     * @throws UnableToIndexSchoolsException as per the description
     */
    public Map<School, List<RegisteredUserDTO>> getUsersBySchool() throws UnableToIndexSchoolsException, SegueSearchException {
        List<RegisteredUserDTO> users;
        Map<School, List<RegisteredUserDTO>> usersBySchool = Maps.newHashMap();

        try {
            users = userManager.findUsers(new RegisteredUserDTO());

            for (RegisteredUserDTO user : users) {
                if (user.getSchoolId() == null) {
                    continue;
                }

                School s = schoolManager.findSchoolById(user.getSchoolId());
                if (s == null) {
                    continue;
                }

                if (!usersBySchool.containsKey(s)) {
                    List<RegisteredUserDTO> userList = Lists.newArrayList();
                    userList.add(user);
                    usersBySchool.put(s, userList);
                } else {
                    usersBySchool.get(s).add(user);
                }
            }

        } catch (SegueDatabaseException | IOException e) {
            log.error("Segue database error during school frequency calculation", e);
        }

        return usersBySchool;
    }

    /**
     * Find all users belonging to a given school.
     * 
     * @param schoolId
     *            - that we are interested in.
     * @return list of users.
     * @throws SegueDatabaseException
     *             - if there is a general database error
     * @throws ResourceNotFoundException
     *             - if we cannot locate the school requested.
     * @throws UnableToIndexSchoolsException
     *             - if the school list has not been indexed.
     */
    public List<RegisteredUserDTO> getUsersBySchoolId(final String schoolId) throws ResourceNotFoundException,
            SegueDatabaseException, UnableToIndexSchoolsException, SegueSearchException {
        Validate.notNull(schoolId);

        List<RegisteredUserDTO> users;

        School s;
        try {
            s = schoolManager.findSchoolById(schoolId);
        } catch (IOException e) {
            log.error("Unable to locate school based on id.", e);
            throw new ResourceNotFoundException("Unable to locate school based on id.");
        }

        if (null == s) {
            throw new ResourceNotFoundException("The school with the id provided cannot be found.");
        }

        RegisteredUserDTO prototype = new RegisteredUserDTO();
        prototype.setSchoolId(schoolId);

        users = userManager.findUsers(prototype);

        return users;
    }

    /**
     * @return a list of userId's to last event timestamp
     */
    public Map<String, Date> getLastSeenUserMap() {
        Map<String, Date> lastSeenMap = Maps.newHashMap();
        
        try {
            List<RegisteredUserDTO> users = userManager.findUsers(new RegisteredUserDTO());
            
            for (RegisteredUserDTO user : users) {
                if (user.getLastSeen() != null) {
                    lastSeenMap.put(user.getId().toString(), user.getLastSeen());
                } else if (user.getRegistrationDate() != null) {
                    lastSeenMap.put(user.getId().toString(), user.getRegistrationDate());
                }
            }
            
        } catch (SegueDatabaseException e) {
            log.error("Unable to get last seen user map", e);
        }
        
        return lastSeenMap;
    }

    /**
     * @param qualifyingLogEvent
     *            the string event type that will be looked for.
     * @return a map of userId's to last event timestamp
     * @throws SegueDatabaseException 
     */
    public Map<String, Date> getLastSeenUserMap(final String qualifyingLogEvent) throws SegueDatabaseException {
        return this.logManager.getLastLogDateForAllUsers(qualifyingLogEvent);
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

        int questionsAnsweredCorrectly = 0;
        int questionPartsAnsweredCorrectly = 0;
        int totalQuestionsAttempted = 0;
        int totalQuestionPartsAttempted = 0;
        Map<String, Integer> questionAttemptsByTagStats = Maps.newHashMap();
        Map<String, Integer> questionsCorrectByTagStats = Maps.newHashMap();
        Map<String, Integer> questionAttemptsByLevelStats = Maps.newHashMap();
        Map<String, Integer> questionsCorrectByLevelStats = Maps.newHashMap();
        Map<String, Integer> questionAttemptsByTypeStats = Maps.newHashMap();
        Map<String, Integer> questionsCorrectByTypeStats = Maps.newHashMap();

        Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsByUser = questionManager.getQuestionAttemptsByUser(userOfInterest);
        Map<String, ContentDTO> questionMap = this.getQuestionMap(questionAttemptsByUser.keySet());

        // Loop through each Question attempted:
        for (Entry<String, Map<String, List<QuestionValidationResponse>>> question : questionAttemptsByUser.entrySet()) {
            totalQuestionsAttempted++;

            boolean questionCorrect = true;  // Are all Parts of the Question correct?
            // Loop through each Part of the Question:
            // TODO - We might be able to avoid using a GameManager here!
            // The question page content object is questionMap.get(question.getKey()) and we could search this instead!
            for (QuestionDTO questionPart : gameManager.getAllMarkableQuestionPartsDFSOrder(question.getKey())) {

                boolean questionPartCorrect = false;  // Is this Part of the Question correct?
                // Has the user attempted this part of the question at all?
                if (question.getValue().containsKey(questionPart.getId())) {
                    totalQuestionPartsAttempted++;

                    // Loop through each attempt at the Question Part if they have attempted it:
                    for (QuestionValidationResponse validationResponse : question.getValue().get(questionPart.getId())) {

                        if (validationResponse.isCorrect() != null && validationResponse.isCorrect()) {
                            questionPartsAnsweredCorrectly++;
                            questionPartCorrect = true;
                            break;
                        }
                    }
                    // Type Stats - Count the attempt at the Question Part:
                    String questionPartType = questionPart.getType();
                    if (questionAttemptsByTypeStats.containsKey(questionPartType)) {
                        questionAttemptsByTypeStats.put(questionPartType, questionAttemptsByTypeStats.get(questionPartType) + 1);
                    } else {
                        questionAttemptsByTypeStats.put(questionPartType, 1);
                    }
                    // If this Question Part is correct, count this too:
                    if (questionPartCorrect) {
                        if (questionsCorrectByTypeStats.containsKey(questionPartType)) {
                            questionsCorrectByTypeStats.put(questionPartType, questionsCorrectByTypeStats.get(questionPartType) + 1);
                        } else {
                            questionsCorrectByTypeStats.put(questionPartType, 1);
                        }
                    }
                }

                // Correctness of whole Question: is the Question correct so far, and is this Question Part also correct?
                questionCorrect = questionCorrect && questionPartCorrect;
            }

            ContentDTO questionContentDTO = questionMap.get(question.getKey());
            if (null == questionContentDTO) {
                // We no longer have any information on this question!
                continue;
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
                if (questionCorrect) {
                    if (questionsCorrectByTagStats.containsKey(tag)) {
                        questionsCorrectByTagStats.put(tag, questionsCorrectByTagStats.get(tag) + 1);
                    } else {
                        questionsCorrectByTagStats.put(tag, 1);
                    }
                }
            }

            // Level Stats:
            String questionLevel = questionContentDTO.getLevel().toString();
            if (questionAttemptsByLevelStats.containsKey(questionLevel)) {
                questionAttemptsByLevelStats.put(questionLevel, questionAttemptsByLevelStats.get(questionLevel) + 1);
            } else {
                questionAttemptsByLevelStats.put(questionLevel, 1);
            }
            // If it's correct, count this globally and for the Question's level too:
            if (questionCorrect) {
                questionsAnsweredCorrectly++;
                if (questionsCorrectByLevelStats.containsKey(questionLevel)) {
                    questionsCorrectByLevelStats.put(questionLevel, questionsCorrectByLevelStats.get(questionLevel) + 1);
                } else {
                    questionsCorrectByLevelStats.put(questionLevel, 1);
                }
            }
        }

        Map<String, Object> questionInfo = Maps.newHashMap();

        questionInfo.put("totalQuestionsAttempted", totalQuestionsAttempted);
        questionInfo.put("totalQuestionsCorrect", questionsAnsweredCorrectly);
        questionInfo.put("totalQuestionPartsAttempted", totalQuestionPartsAttempted);
        questionInfo.put("totalQuestionPartsCorrect", questionPartsAnsweredCorrectly);
        questionInfo.put("attemptsByTag", questionAttemptsByTagStats);
        questionInfo.put("correctByTag", questionsCorrectByTagStats);
        questionInfo.put("attemptsByLevel", questionAttemptsByLevelStats);
        questionInfo.put("correctByLevel", questionsCorrectByLevelStats);
        questionInfo.put("attemptsByType", questionAttemptsByTypeStats);
        questionInfo.put("correctByType", questionsCorrectByTypeStats);
        questionInfo.put("userDetails", this.userManager.convertToUserSummaryObject(userOfInterest));

        return questionInfo;

    }

    /**
     * getEventLogsByDate.
     * 
     * @param eventTypes
     *            - of interest
     * @param fromDate
     *            - of interest
     * @param toDate
     *            - of interest
     * @param binDataByMonth
     *            - shall we group data by the first of every month?
     * @return Map of eventType --> map of dates and frequency
     * @throws SegueDatabaseException 
     */
    public Map<String, Map<LocalDate, Long>> getEventLogsByDate(final Collection<String> eventTypes,
            final Date fromDate, final Date toDate, final boolean binDataByMonth) throws SegueDatabaseException {
        return this.getEventLogsByDateAndUserList(eventTypes, fromDate, toDate, null, binDataByMonth);
    }

    /**
     * getEventLogsByDate.
     *
     * @param eventTypes
     *            - of interest
     * @param fromDate
     *            - of interest
     * @param toDate
     *            - of interest
     * @param userList
     *            - user prototype to filter events. e.g. user(s) with a particular id or role.
     * @param binDataByMonth
     *            - shall we group data by the first of every month?
     * @return Map of eventType --> map of dates and frequency
     * @throws SegueDatabaseException 
     */
    public Map<String, Map<LocalDate, Long>> getEventLogsByDateAndUserList(final Collection<String> eventTypes,
            final Date fromDate, final Date toDate, final List<RegisteredUserDTO> userList,
            final boolean binDataByMonth) throws SegueDatabaseException {
        Validate.notNull(eventTypes);

        return this.logManager.getLogCountByDate(eventTypes, fromDate, toDate, userList, binDataByMonth);
    }

    /**
     * Calculate the number of users from the list provided that meet the criteria.
     * 
     * @param users
     *            - collection of users to consider.
     * @param lastSeenUserMap
     *            - The map of user event data. UserId --> last event date.
     * @param daysFromToday
     *            - the number of days from today that should be included in the calculation e.g. 7 would be the last
     *            week's data.
     * @return a collection containing the users who meet the criteria
     */
    public Collection<RegisteredUserDTO> getNumberOfUsersActiveForLastNDays(final Collection<RegisteredUserDTO> users,
            final Map<String, Date> lastSeenUserMap, final int daysFromToday) {

        Set<RegisteredUserDTO> qualifyingUsers = Sets.newHashSet();

        for (RegisteredUserDTO user : users) {
            Date eventDate = lastSeenUserMap.get(user.getId().toString());
            Calendar validInclusionTime = Calendar.getInstance();
            validInclusionTime.setTime(new Date());
            validInclusionTime.add(Calendar.DATE, -1 * Math.abs(daysFromToday));

            if (eventDate != null && eventDate.after(validInclusionTime.getTime())) {
                qualifyingUsers.add(user);
            }
        }

        return qualifyingUsers;
    }

    /**
     * getLocationInformation.
     *
     * @param fromDate
     *            - date to start search
     * @param toDate
     *            - date to end search
     * @return the list of all locations we know about..
     * @throws SegueDatabaseException
     *             if we can't read from the database.
     */
    @SuppressWarnings("unchecked")
    public Collection<Location> getLocationInformation(final Date fromDate, final Date toDate) throws SegueDatabaseException {
        SimpleDateFormat cacheFormat = new SimpleDateFormat("yyyyMMdd");
        String cacheDateTag =  cacheFormat.format(fromDate) + cacheFormat.format(toDate);
        if (this.longStatsCache.getIfPresent(LOCATION_STATS + cacheDateTag) != null) {
            return (Set<Location>) this.longStatsCache.getIfPresent(LOCATION_STATS + cacheDateTag);
        }

        Set<Location> result = Sets.newHashSet();

        Map<String, Location> locationsFromHistory = locationHistoryManager.getLocationsByLastAccessDate(fromDate,
                toDate);

        result.addAll(locationsFromHistory.values());

        this.longStatsCache.put(LOCATION_STATS + cacheDateTag, result);

        return result;
    }

    @Override
    public Map<String, Object> getDetailedUserStatistics(RegisteredUserDTO userOfInterest) {

        //user streak info
        Map<String, Object> userStreakRecord = userStreaksManager.getCurrentStreakRecord(userOfInterest);
        userStreakRecord.put("largestStreak", userStreaksManager.getHighestStreak(userOfInterest));

        return ImmutableMap.of("streakRecord", userStreakRecord);
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
                this.contentManager.getContentMatchingIds(this.contentIndex, ids, 0, ids.size());

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
