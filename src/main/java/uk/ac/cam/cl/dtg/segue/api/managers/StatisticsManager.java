/**
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

import org.apache.commons.lang3.Validate;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.LocationHistoryManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dos.LogEvent;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
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
public class StatisticsManager {
    private UserManager userManager;
    private ILogManager logManager;
    private SchoolListReader schoolManager;
    private ContentVersionController versionManager;
    private IContentManager contentManager;
    private GroupManager groupManager;
    
    private Cache<String, Object> statsCache;
    private LocationHistoryManager locationHistoryManager;

    private static final Logger log = LoggerFactory.getLogger(StatisticsManager.class);
    private static final String GENERAL_STATS = "GENERAL_STATS";
    private static final String SCHOOL_STATS = "SCHOOL_STATS";
    private static final String LOCATION_STATS = "LOCATION_STATS";
    private static final int STATS_EVICTION_INTERVAL_MINUTES = 720; // 12 hours
    

    /**
     * StatisticsManager.
     * 
     * @param userManager
     *            - to query user information
     * @param logManager
     *            - to query Log information
     * @param schoolManager
     *            - to query School information
     * @param versionManager
     *            - to query live version information
     * @param contentManager
     *            - to query content
     * @param locationHistoryManager
     *            - so that we can query our location database (ip addresses)
     * @param groupManager
     *            - so that we can see how many groups we have site wide.
     */
    @Inject
    public StatisticsManager(final UserManager userManager, final ILogManager logManager,
            final SchoolListReader schoolManager, final ContentVersionController versionManager,
            final IContentManager contentManager, final LocationHistoryManager locationHistoryManager,
            final GroupManager groupManager) {
        this.userManager = userManager;
        this.logManager = logManager;
        this.schoolManager = schoolManager;

        this.versionManager = versionManager;
        this.contentManager = contentManager;

        this.locationHistoryManager = locationHistoryManager;
        this.groupManager = groupManager;

        this.statsCache = CacheBuilder.newBuilder().expireAfterWrite(STATS_EVICTION_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .<String, Object> build();
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
        Map<String, Object> cachedOutput = (Map<String, Object>) this.statsCache.getIfPresent(GENERAL_STATS);
        if (cachedOutput != null) {
            log.debug("Using cached statistics.");
            return cachedOutput;
        } else {
            log.info("Calculating General Statistics");
        }

        List<RegisteredUserDTO> users = userManager.findUsers(new RegisteredUserDTO());
        ImmutableMap.Builder<String, Object> ib = new ImmutableMap.Builder<String, Object>();

        List<RegisteredUserDTO> male = Lists.newArrayList();
        List<RegisteredUserDTO> female = Lists.newArrayList();
        List<RegisteredUserDTO> unknownGender = Lists.newArrayList();
        ib.put("totalUsers", "" + users.size());

        List<RegisteredUserDTO> studentOrUnknownRole = Lists.newArrayList();
        List<RegisteredUserDTO> teacherRole = Lists.newArrayList();
        List<RegisteredUserDTO> adminStaffRole = Lists.newArrayList();
        List<RegisteredUserDTO> contentEditorStaffRole = Lists.newArrayList();
        List<RegisteredUserDTO> testerStaffRole = Lists.newArrayList();
        List<RegisteredUserDTO> eventManagerStaffRole = Lists.newArrayList();
        List<RegisteredUserDTO> hasSchool = Lists.newArrayList();
        List<RegisteredUserDTO> hasNoSchool = Lists.newArrayList();
        List<RegisteredUserDTO> hasOtherSchool = Lists.newArrayList();
        Map<String, Date> lastSeenMap = Maps.newHashMap();

        for (RegisteredUserDTO user : users) {
            if (user.getGender() == null) {
                unknownGender.add(user);
            } else {
                switch (user.getGender()) {
                    case MALE:
                        male.add(user);
                        break;
                    case FEMALE:
                        female.add(user);
                        break;
                    case OTHER:
                        unknownGender.add(user);
                        break;
                    default:
                        unknownGender.add(user);
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
                    case TESTER:
                    	testerStaffRole.add(user);
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

        ib.put("maleUsers", "" + male.size());
        ib.put("femaleUsers", "" + female.size());
        ib.put("unknownGenderUsers", "" + unknownGender.size());

        ib.put("adminUsers", "" + adminStaffRole.size());
        ib.put("contentEditorUsers", "" + contentEditorStaffRole.size());
        ib.put("eventManagerUsers", "" + eventManagerStaffRole.size());
        ib.put("teacherUsers", "" + teacherRole.size());
        ib.put("testerUsers", "" + testerStaffRole.size());
        ib.put("studentUsers", "" + studentOrUnknownRole.size());

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
        this.statsCache.put(GENERAL_STATS, result);

        return result;
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
            throws UnableToIndexSchoolsException, SegueDatabaseException {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cachedOutput = (List<Map<String, Object>>) this.statsCache.getIfPresent(SCHOOL_STATS);
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
        final int thirtyDays = 30;

        Map<String, Date> lastSeenUserMap = getLastSeenUserMap();
        List<Map<String, Object>> result = Lists.newArrayList();
        for (Entry<School, List<RegisteredUserDTO>> e : map.entrySet()) {
            RegisteredUserDTO prototype = new RegisteredUserDTO();
            prototype.setSchoolId(e.getKey().getUrn());

            List<RegisteredUserDTO> teachersConnected = Lists.newArrayList();
            for (RegisteredUserDTO user : e.getValue()) {
                if (user.getRole() != null && user.getRole().equals(Role.TEACHER)) {
                    teachersConnected.add(user);
                }
            }

            result.add(ImmutableMap.of(school, e.getKey(), connections, e.getValue().size(), teachers,
                    teachersConnected.size(), numberActive,
                    getNumberOfUsersActiveForLastNDays(e.getValue(), lastSeenUserMap, thirtyDays).size()));
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

        this.statsCache.put(SCHOOL_STATS, result);

        return result;
    }

    /**
     * Get the number of users per school.
     * 
     * @return A map of schools to integers (representing the number of registered users)
     * @throws UnableToIndexSchoolsException as per the description
     */
    public Map<School, List<RegisteredUserDTO>> getUsersBySchool() throws UnableToIndexSchoolsException {
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
    public List<RegisteredUserDTO> getUsersBySchoolId(final Long schoolId) throws ResourceNotFoundException,
            SegueDatabaseException, UnableToIndexSchoolsException {
        Validate.notNull(schoolId);

        List<RegisteredUserDTO> users = Lists.newArrayList();

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
     * @return a list of userId's to last event timestamp
     * @throws SegueDatabaseException 
     */
    public Map<String, Date> getLastSeenUserMap(final String qualifyingLogEvent) throws SegueDatabaseException {
        return this.convertFromLogEventToDateMap(this.logManager.getLastLogForAllUsers(qualifyingLogEvent));
    }

    /**
     * getUserQuestionInformation. Produces a map that contains information about the total questions attempted,
     * (correct and correct first time) "totalQuestionsAttempted", "totalCorrect",
     * "totalCorrectFirstTime","attemptsByTag", questionAttemptsByLevelStats.
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

        // get questions answered correctly.
        int questionsAnsweredCorrectly = 0;

        // get total questions attempted
        int totalQuestionsAttempted = 0;

        // get total questions answered first time correctly
        int questionsFirstTime = 0;

        Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsByUser = userManager
                .getQuestionAttemptsByUser(userOfInterest);

        // all relevant question page info
        for (Entry<String, Map<String, List<QuestionValidationResponse>>> questionPage : questionAttemptsByUser
                .entrySet()) {

            for (Entry<String, List<QuestionValidationResponse>> question : questionPage.getValue().entrySet()) {
                totalQuestionsAttempted++;

                for (int i = 0; question.getValue().size() > i; i++) {
                    // assumption that the order of the list is in chronological
                    // order

                    QuestionValidationResponse validationResponse = question.getValue().get(i);
                    if (validationResponse.isCorrect() != null && validationResponse.isCorrect() && i == 0) {
                        questionsFirstTime++;
                    }

                    if (validationResponse.isCorrect() != null && validationResponse.isCorrect()) {
                        questionsAnsweredCorrectly++;
                        break;
                    }
                }
            }
        }

        // TODO this stuff should be tidied up and put somewhere else
        Map<String, ContentDTO> questionMap = this.getQuestionMap(questionAttemptsByUser.keySet());

        Map<String, Integer> questionAttemptsByTagStats = Maps.newHashMap();
        Map<String, Integer> questionAttemptsByLevelStats = Maps.newHashMap();

        for (Entry<String, Map<String, List<QuestionValidationResponse>>> question 
                : questionAttemptsByUser.entrySet()) {
            // add the tags
            if (questionMap.get(question.getKey()) != null) {
                for (String tag : questionMap.get(question.getKey()).getTags()) {
                    if (questionAttemptsByTagStats.containsKey(tag)) {
                        questionAttemptsByTagStats.put(tag, questionAttemptsByTagStats.get(tag) + 1);
                    } else {
                        questionAttemptsByTagStats.put(tag, 1);
                    }
                }
            }

            ContentDTO questionContentDTO = questionMap.get(question.getKey());

            if (null == questionContentDTO) {
                continue;
            }

            String questionLevel = questionContentDTO.getLevel().toString();

            if (questionAttemptsByLevelStats.containsKey(questionLevel.toString())) {
                questionAttemptsByLevelStats.put(questionLevel.toString(),
                        questionAttemptsByLevelStats.get(questionLevel.toString()) + 1);
            } else {
                questionAttemptsByLevelStats.put(questionLevel.toString(), 1);
            }
        }

        ImmutableMap<String, Object> immutableMap = new ImmutableMap.Builder<String, Object>()
                .put("totalQuestionsAttempted", totalQuestionsAttempted)
                .put("totalCorrect", questionsAnsweredCorrectly).put("totalCorrectFirstTime", questionsFirstTime)
                .put("attemptsByTag", questionAttemptsByTagStats).put("attemptsByLevel", questionAttemptsByLevelStats)
                .put("userDetails", this.userManager.convertToUserSummaryObject(userOfInterest)).build();

        return immutableMap;
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
    public Map<String, Map<LocalDate, Integer>> getEventLogsByDate(final Collection<String> eventTypes,
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
    public Map<String, Map<LocalDate, Integer>> getEventLogsByDateAndUserList(final Collection<String> eventTypes,
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
     * getRecentLocationInformation.
     * 
     * @param threshold
     *            - the earliest date to include in the search.
     * @return the list of all locations we know about..
     * @throws SegueDatabaseException
     *             if we can't read from the database.
     */
    @SuppressWarnings("unchecked")
    public Collection<Location> getLocationInformation(final Date threshold) throws SegueDatabaseException {
        if (this.statsCache.getIfPresent(LOCATION_STATS) != null) {
            return (Set<Location>) this.statsCache.getIfPresent(LOCATION_STATS);
        }

        Set<Location> result = Sets.newHashSet();

        Map<String, Location> locationsFromHistory = locationHistoryManager.getLocationsByLastAccessDate(threshold,
                new Date());

        result.addAll(locationsFromHistory.values());

        this.statsCache.put(LOCATION_STATS, result);

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
                new ArrayList<String>(ids));

        fieldsToMap.put(immutableEntry(BooleanOperator.OR, TYPE_FIELDNAME),
                Arrays.asList(QUESTION_TYPE, FAST_TRACK_QUESTION_TYPE));

        // Search for questions that match the ids.
        ResultsWrapper<ContentDTO> findByFieldNames = contentManager.findByFieldNames(versionManager.getLiveVersion(),
                fieldsToMap, 0, ids.size());

        List<ContentDTO> questionsForGameboard = findByFieldNames.getResults();

        Map<String, ContentDTO> questionIdToQuestionMap = Maps.newHashMap();
        for (ContentDTO content : questionsForGameboard) {
            if (content != null) {
                questionIdToQuestionMap.put(content.getId(), content);
            }
        }

        return questionIdToQuestionMap;
    }

    /**
     * @param input
     *            - containing more information than necessary.
     * @return converted map
     */
    private Map<String, Date> convertFromLogEventToDateMap(final Map<String, LogEvent> input) {
        Map<String, Date> result = Maps.newHashMap();

        for (Entry<String, LogEvent> e : input.entrySet()) {
            result.put(e.getKey(), e.getValue().getTimestamp());
        }
        return result;
    }
}
