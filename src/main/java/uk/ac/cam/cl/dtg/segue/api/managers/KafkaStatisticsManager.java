package uk.ac.cam.cl.dtg.segue.api.managers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.*;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.UserStatisticsStreamsApplication;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.SiteStatisticsStreamsApplication;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.util.locations.Location;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.VIEW_QUESTION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANSWER_QUESTION;

/**
 * KafkaStatisticsManager.
 * TODO like the old stats manager, this file is a mess... it needs refactoring. This at least provides a starting point.
 */
public class KafkaStatisticsManager implements IStatisticsManager {

    private UserAccountManager userManager;
    private ILogManager logManager;
    private GroupManager groupManager;
    private SchoolListReader schoolManager;
    private SiteStatisticsStreamsApplication statisticsStreamsApplication;
    private UserStatisticsStreamsApplication userStatisticsStreamsApplication;
    private IStatisticsManager oldStatisticsManager;


    private static final Logger log = LoggerFactory.getLogger(KafkaStatisticsManager.class);
    private static final String GENERAL_STATS = "GENERAL_STATS";
    private static final String SCHOOL_STATS = "SCHOOL_STATS";


    /**
     * Kafkaesque statistic manager.
     * @param userManager
     *            - to query user information
     * @param logManager
     *            - to query Log information
     * @param schoolManager
     *            - to query School information
     * @param groupManager
     *            - so that we can see how many groups we have site wide.
     * @param statisticsStreamsApplication
     *            - to query kafka state stores
     * @param statsManager
     *            - old stats manager injected in for non-kafkaized elements
     */
    @Inject
    public KafkaStatisticsManager(final UserAccountManager userManager, final ILogManager logManager,
                                  final SchoolListReader schoolManager, final GroupManager groupManager,
                                  final SiteStatisticsStreamsApplication statisticsStreamsApplication,
                                  final UserStatisticsStreamsApplication userStatisticsStreamsApplication,
                                  final StatisticsManager statsManager) {

        this.oldStatisticsManager = statsManager;

        this.statisticsStreamsApplication = statisticsStreamsApplication;
        this.userStatisticsStreamsApplication = userStatisticsStreamsApplication;
        this.logManager = logManager;
        this.userManager = userManager;
        this.groupManager = groupManager;
        this.schoolManager = schoolManager;

    }



    /**
     * Output general stats. This returns a Map of String to Object and is intended to be sent directly to a
     * serializable facade endpoint.
     *
     * @return Map<String, Object> (stat name, stat value)
     * @throws InvalidStateStoreException if there is a kafka data store error.
     * @throws SegueDatabaseException if there is a database error.
     */
    @Override
    public synchronized Map<String, Object> outputGeneralStatistics() throws InvalidStateStoreException, SegueDatabaseException {

        log.info("Calculating Kafka General Statistics");

        ImmutableMap.Builder<String, Object> ib = new ImmutableMap.Builder<>();

        Map<String, Object> gender = Maps.newHashMap();
        Map<String, Object> role = Maps.newHashMap();

        KeyValueIterator<String, JsonNode> it = statisticsStreamsApplication.getAllUsers();

        Integer userCount = 0;

        Integer maleCount = 0;
        Integer femaleCount = 0;
        Integer otherGenderCount = 0;

        Integer studentOrUnknownCount = 0;
        Integer teacherCount = 0;
        Integer adminCount = 0;
        Integer contentEditorCount = 0;
        Integer testerCount = 0;
        Integer staffCount = 0;
        Integer eventManagerCount = 0;

        Integer hasSchoolCount = 0;
        Integer hasNoSchoolCount = 0;
        Integer hasOtherSchoolCount = 0;

        Integer activeUsersSixMonths = 0;
        Integer activeUsersWeek = 0;
        Integer activeStudentsWeek = 0;
        Integer activeTeachersWeek = 0;
        Integer activeUsersMonth = 0;
        Integer activeUsersNinetyDays = 0;
        Integer activeStudentsMonth = 0;
        Integer activeTeachersMonth = 0;
        Integer activeStudentsNinetyDays = 0;
        Integer activeTeachersNinetyDays = 0;

        //Integer viewQuestionEvents = 0;
        //Integer answeredQuestionEvents = 0;
        Integer questionsAnsweredLastWeekTeachers = 0;
        Integer questionsAnsweredLastThirtyDaysTeachers = 0;
        Integer questionsAnsweredLastNinetyDaysTeachers = 0;
        Integer questionsAnsweredLastWeekStudents = 0;
        Integer questionsAnsweredLastThirtyDaysStudents = 0;
        Integer questionsAnsweredLastNinetyDaysStudents = 0;

        final int sevenDays = 7;
        final int thirtyDays = 30;
        final int ninetyDays = 90;
        final int sixMonthsInDays = 180;

        while (it.hasNext()) {

            JsonNode node = it.next().value;
            JsonNode userData = node.path("user_data");
            JsonNode lastSeenData = node.path("last_seen_data");

            if (node.path("user_id").asText().equals("")) {
                continue;
            }

            try {

                userCount++;

                String usrGender = userData.path("gender").asText();
                String usrRole = userData.path("role").asText();

                String usrSchoolId = userData.path("school_id").asText();
                String usrSchoolOther = userData.path("school_other").asText();

                Long lastSeen = lastSeenData.path("last_seen").asLong();


                // gender
                if (usrGender.equals(Gender.MALE.toString())) {
                    maleCount++;
                } else if (usrGender.equals(Gender.FEMALE.toString())) {
                    femaleCount++;
                } else {
                    otherGenderCount++;
                }

                // role
                if (usrRole.equals(Role.STUDENT.toString())) {
                    studentOrUnknownCount++;
                } else if (usrRole.equals(Role.ADMIN.toString())) {
                    adminCount++;
                } else if (usrRole.equals(Role.CONTENT_EDITOR.toString())) {
                    contentEditorCount++;
                } else if (usrRole.equals(Role.EVENT_MANAGER.toString())) {
                    eventManagerCount++;
                } else if (usrRole.equals(Role.TEACHER.toString())) {
                    teacherCount++;
                } else if (usrRole.equals(Role.STAFF.toString())) {
                    staffCount++;
                } else if (usrRole.equals(Role.TESTER.toString())) {
                    testerCount++;
                } else {
                    studentOrUnknownCount++;
                }


                // schools
                if ((usrSchoolId == null || usrSchoolId.equals(""))
                        && (usrSchoolOther.equals("") || usrSchoolOther == null)) {
                    hasNoSchoolCount++;
                } else {
                    hasSchoolCount++;
                    if (!usrSchoolOther.equals("")) {
                        hasOtherSchoolCount++;
                    }
                }

                // user activity
                if (userLastSeenNDays(lastSeen, sevenDays)) {
                    activeUsersSixMonths++;

                    if (usrRole.equals(Role.STUDENT.toString())) {
                        activeUsersWeek++;
                        activeUsersMonth++;
                        activeUsersNinetyDays++;
                        activeStudentsWeek++;
                        activeStudentsMonth++;
                        activeStudentsNinetyDays++;
                    }

                    if (usrRole.equals(Role.TEACHER.toString())) {
                        activeUsersWeek++;
                        activeUsersMonth++;
                        activeUsersNinetyDays++;
                        activeTeachersWeek++;
                        activeTeachersMonth++;
                        activeTeachersNinetyDays++;
                    }

                } else if (userLastSeenNDays(lastSeen, thirtyDays)) {
                    activeUsersSixMonths++;

                    if (usrRole.equals(Role.STUDENT.toString())) {
                        activeUsersMonth++;
                        activeUsersNinetyDays++;
                        activeStudentsMonth++;
                        activeStudentsNinetyDays++;
                    }

                    if (usrRole.equals(Role.TEACHER.toString())) {
                        activeUsersMonth++;
                        activeUsersNinetyDays++;
                        activeTeachersMonth++;
                        activeTeachersNinetyDays++;
                    }

                } else if (userLastSeenNDays(lastSeen, ninetyDays)) {
                    activeUsersSixMonths++;

                    if (usrRole.equals(Role.STUDENT.toString())) {
                        activeUsersNinetyDays++;
                        activeStudentsNinetyDays++;
                    }

                    if (usrRole.equals(Role.TEACHER.toString())) {
                        activeUsersNinetyDays++;
                        activeTeachersNinetyDays++;
                    }

                } else if (userLastSeenNDays(lastSeen, sixMonthsInDays)) {
                    activeUsersSixMonths++;
                }

                /*if (lastSeenData.has(VIEW_QUESTION)) {
                    viewQuestionEvents += lastSeenData.path(VIEW_QUESTION).path("count").asInt();;
                }*/

                if (lastSeenData.has(ANSWER_QUESTION)) {
                    //answeredQuestionEvents += lastSeenData.path(ANSWER_QUESTION).path("count").asInt();

                    if (userLastSeenNDays(lastSeenData.path(ANSWER_QUESTION).path("latest").asLong(), sevenDays)) {

                        if (usrRole.equals(Role.STUDENT.toString()))
                            questionsAnsweredLastWeekStudents++;

                        if (usrRole.equals(Role.TEACHER.toString()))
                            questionsAnsweredLastWeekTeachers++;
                    }

                    if (userLastSeenNDays(lastSeenData.path(ANSWER_QUESTION).path("latest").asLong(), thirtyDays)) {

                        if (usrRole.equals(Role.STUDENT.toString()))
                            questionsAnsweredLastThirtyDaysStudents++;

                        if (usrRole.equals(Role.TEACHER.toString()))
                            questionsAnsweredLastThirtyDaysTeachers++;
                    }

                    if (userLastSeenNDays(lastSeenData.path(ANSWER_QUESTION).path("latest").asLong(), ninetyDays)) {

                        if (usrRole.equals(Role.STUDENT.toString()))
                            questionsAnsweredLastNinetyDaysStudents++;

                        if (usrRole.equals(Role.TEACHER.toString()))
                            questionsAnsweredLastNinetyDaysTeachers++;
                    }

                }

            } catch (Exception e) {
                log.error("Error during querying kafka user store", e);
            }
        }

        gender.put(Gender.MALE.toString(), maleCount);
        gender.put(Gender.FEMALE.toString(), femaleCount);
        gender.put(Gender.OTHER.toString(), otherGenderCount);

        role.put(Role.STUDENT.toString(), studentOrUnknownCount);
        role.put(Role.ADMIN.toString(), adminCount);
        role.put(Role.CONTENT_EDITOR.toString(), contentEditorCount);
        role.put(Role.EVENT_MANAGER.toString(), eventManagerCount);
        role.put(Role.TEACHER.toString(), teacherCount);
        role.put(Role.STAFF.toString(), staffCount);
        role.put(Role.TESTER.toString(), testerCount);


        ib.put("totalUsers", userCount);
        ib.put("gender", gender);
        ib.put("role", role);
        ib.put("hasSchool", hasSchoolCount);
        ib.put("hasNoSchool", hasNoSchoolCount);
        ib.put("hasSchoolOther", hasOtherSchoolCount);

        ib.put("activeInLastSixMonths", activeUsersSixMonths);
        ib.put("activeUsersLastWeek", activeUsersWeek);
        ib.put("activeUsersLastThirtyDays", activeUsersMonth);
        ib.put("activeUsersLastNinetyDays", activeUsersNinetyDays);
        ib.put("activeTeachersLastWeek", activeTeachersWeek);
        ib.put("activeTeachersLastThirtyDays", activeTeachersMonth);
        ib.put("activeTeachersLastNinetyDays", activeTeachersNinetyDays);
        ib.put("activeStudentsLastWeek", activeStudentsWeek);
        ib.put("activeStudentsLastThirtyDays", activeStudentsMonth);
        ib.put("activeStudentsLastNinetyDays", activeStudentsNinetyDays);

        //ib.put("viewQuestionEvents", viewQuestionEvents);
        //ib.put("answeredQuestionEvents", answeredQuestionEvents);

        ib.put("viewQuestionEvents", getLogCount(VIEW_QUESTION));
        ib.put("answeredQuestionEvents", getLogCount(ANSWER_QUESTION));

        ib.put("questionsAnsweredLastWeekTeachers", questionsAnsweredLastWeekTeachers);
        ib.put("questionsAnsweredLastThirtyDaysTeachers", questionsAnsweredLastThirtyDaysTeachers);
        ib.put("questionsAnsweredLastNinetyDaysTeachers", questionsAnsweredLastNinetyDaysTeachers);
        ib.put("questionsAnsweredLastWeekStudents", questionsAnsweredLastWeekStudents);
        ib.put("questionsAnsweredLastThirtyDaysStudents", questionsAnsweredLastThirtyDaysStudents);
        ib.put("questionsAnsweredLastNinetyDaysStudents", questionsAnsweredLastNinetyDaysStudents);

        ib.put("groupCount", groupManager.getGroupCount());

        Map<String, Object> result = ib.build();

        log.info("Finished calculating Kafka General Statistics");

        return result;
    }



    /**
     * LogCount.
     *
     * @param logTypeOfInterest the log event that we care about.
     * @return the number of logs of that type (or an estimate).
     * @throws InvalidStateStoreException if there is a kafka data store error.
     */
    @Override
    public Long getLogCount(final String logTypeOfInterest) throws InvalidStateStoreException {
        return statisticsStreamsApplication.getLogCountByType(logTypeOfInterest);
    }



    /**
     * Get an overview of all school performance. This is for analytics / admin users.
     *
     * @return list of school to statistics mapping. The object in the map is another map with keys connections,
     *         numberActiveLastThirtyDays.
     *
     * @throws UnableToIndexSchoolsException if there is a problem getting school details.
     * @throws InvalidStateStoreException if there is a kafka data store error.
     * @throws SegueSearchException if there is a search exception.
     */
    @Override
    public List<Map<String, Object>> getSchoolStatistics() throws UnableToIndexSchoolsException, InvalidStateStoreException, SegueSearchException {

        List<Map<String, Object>> result = Lists.newArrayList();

        Map<School, List<RegisteredUserDTO>> map = getUsersBySchool();

        final String school = "school";
        final String connections = "connections";
        final String teachers = "teachers";
        final String numberActive = "numberActiveLastThirtyDays";
        final String teachersActive = "teachersActiveLastThirtyDays";
        final int thirtyDays = 30;

        try {

            for (Map.Entry<School, List<RegisteredUserDTO>> e : map.entrySet()) {

                int activeUsers = 0;
                int activeTeachers = 0;

                List<RegisteredUserDTO> teachersConnected = Lists.newArrayList();
                for (RegisteredUserDTO user : e.getValue()) {

                    if (user.getRole() != null && user.getRole().equals(Role.TEACHER))
                        teachersConnected.add(user);

                    if (userLastSeenNDays(user.getLastSeen().getTime(), thirtyDays)) {
                        activeUsers++;
                        if (user.getRole() != null && user.getRole().equals(Role.TEACHER))
                            activeTeachers++;
                    }
                }

                result.add(ImmutableMap.of(
                        school, e.getKey(),
                        connections, e.getValue().size(),
                        teachers, teachersConnected.size(),
                        numberActive, activeUsers,
                        teachersActive, activeTeachers
                ));
            }

            Collections.sort(result,
                    (o1, o2) -> {
                        if ((Integer) o1.get(numberActive) < (Integer) o2.get(numberActive)) {
                            return 1;
                        }

                        if ((Integer) o1.get(numberActive) > (Integer) o2.get(numberActive)) {
                            return -1;
                        }

                        return 0;
                    });

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return result;
    }


    /**
     * Get the number of users per school.
     *
     * @return A map of schools to integers (representing the number of registered users).
     * @throws UnableToIndexSchoolsException as per the description.
     * @throws InvalidStateStoreException if there is a kafka data store error.
     * @throws SegueSearchException as per the description.
     */
    @Override
    public Map<School, List<RegisteredUserDTO>> getUsersBySchool() throws UnableToIndexSchoolsException, InvalidStateStoreException, SegueSearchException {

        Map<School, List<RegisteredUserDTO>> usersBySchool = Maps.newHashMap();

        try {

            KeyValueIterator<String, JsonNode> it = statisticsStreamsApplication.getAllUsers();

            while (it.hasNext()) {

                JsonNode node = it.next().value;
                JsonNode userNode = node.path("user_data");
                JsonNode lastSeenData = node.path("last_seen_data");

                Long lastSeen = lastSeenData.path("last_seen").asLong();

                if (userNode.path("school_id").asText().isEmpty())
                    continue;

                RegisteredUserDTO user = new RegisteredUserDTO();
                user.setId(userNode.path("user_id").asLong());
                user.setGivenName(userNode.path("given_name").asText());
                user.setFamilyName(userNode.path("family_name").asText());
                user.setRole(Role.valueOf((!userNode.path("role").asText().isEmpty()) ? userNode.path("role").asText() : Role.STUDENT.toString()));
                user.setGender(Gender.valueOf((!userNode.path("gender").asText().isEmpty()) ? userNode.path("gender").asText() : Gender.OTHER.toString()));
                user.setRegistrationDate(new Timestamp(userNode.path("registration_date").asLong()));
                user.setLastSeen(new Timestamp(lastSeen));

                School s = schoolManager.findSchoolById(userNode.path("school_id").asText());
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

        } catch (IOException e) {
            log.error("Segue database error during school frequency calculation", e);
        }

        return usersBySchool;

    }



    /**
     * Find all users belonging to a given school.
     *
     * @param schoolId that we are interested in.
     * @return list of users.
     * @throws InvalidStateStoreException if there is a kafka data store error.
     * @throws UnableToIndexSchoolsException if the school list has not been indexed..
     */
    @Override
    public List<RegisteredUserDTO> getUsersBySchoolId(final String schoolId) throws InvalidStateStoreException, UnableToIndexSchoolsException, SegueSearchException {

        List<RegisteredUserDTO> users = Lists.newArrayList();

        KeyValueIterator<String, JsonNode> it = statisticsStreamsApplication.getAllUsers();

        while (it.hasNext()) {

            JsonNode node = it.next().value;
            JsonNode userNode = node.path("user_data");
            JsonNode lastSeenData = node.path("last_seen_data");

            Long lastSeen = lastSeenData.path("last_seen").asLong();

            if (userNode.path("school_id").asText().equals(schoolId)) {

                RegisteredUserDTO user = new RegisteredUserDTO();
                user.setId(userNode.path("user_id").asLong());
                user.setGivenName(userNode.path("given_name").asText());
                user.setFamilyName(userNode.path("family_name").asText());
                user.setRole(Role.valueOf((!userNode.path("role").asText().isEmpty()) ? userNode.path("role").asText() : Role.STUDENT.toString()));
                user.setGender(Gender.valueOf((!userNode.path("gender").asText().isEmpty()) ? userNode.path("gender").asText() : Gender.OTHER.toString()));
                user.setRegistrationDate(new Timestamp(userNode.path("registration_date").asLong()));
                user.setLastSeen(new Timestamp(lastSeen));

                users.add(user);
            }
        }

        return users;
    }


    /**
     * @return a list of userId's to last event timestamp.
     * @throws InvalidStateStoreException if there is a kafka data store error.
     */
    @Override
    public Map<String, Date> getLastSeenUserMap() throws InvalidStateStoreException {
        return getLastSeenUserMap("last_seen");
    }


    /**
     * @param qualifyingLogEvent the string event type that will be looked for.
     * @return a map of userId's to last event timestamp.
     * @throws InvalidStateStoreException if there is a kafka data store error.
     */
    @Override
    public Map<String, Date> getLastSeenUserMap(String qualifyingLogEvent) throws InvalidStateStoreException {
        return statisticsStreamsApplication.getLastLogDateForAllUsers(qualifyingLogEvent);
    }


    @Override
    public Map<String, Object> getUserQuestionInformation(RegisteredUserDTO userOfInterest) throws SegueDatabaseException, ContentManagerException {
        return oldStatisticsManager.getUserQuestionInformation(userOfInterest);
    }

    @Override
    public Map<String, Map<LocalDate, Long>> getEventLogsByDate(Collection<String> eventTypes, Date fromDate, Date toDate, boolean binDataByMonth) throws SegueDatabaseException {
        return oldStatisticsManager.getEventLogsByDate(eventTypes, fromDate, toDate, binDataByMonth);
    }

    @Override
    public Map<String, Map<LocalDate, Long>> getEventLogsByDateAndUserList(Collection<String> eventTypes, Date fromDate, Date toDate, List<RegisteredUserDTO> userList, boolean binDataByMonth) throws SegueDatabaseException {
        return oldStatisticsManager.getEventLogsByDateAndUserList(eventTypes, fromDate, toDate, userList, binDataByMonth);
    }

    @Override
    public Collection<RegisteredUserDTO> getNumberOfUsersActiveForLastNDays(Collection<RegisteredUserDTO> users, Map<String, Date> lastSeenUserMap, int daysFromToday) {
        return oldStatisticsManager.getNumberOfUsersActiveForLastNDays(users, lastSeenUserMap, daysFromToday);
    }

    @Override
    public Collection<Location> getLocationInformation(Date fromDate, Date toDate) throws SegueDatabaseException {
        return oldStatisticsManager.getLocationInformation(fromDate, toDate);
    }

    @Override
    public Map<String, Object> getDetailedUserStatistics(RegisteredUserDTO userOfInterest) {
        return userStatisticsStreamsApplication.getUserSnapshot(userOfInterest);
    }


    /**
     * Utility method for returning a boolean value specifying if a user has been seen within a given time frame.
     *
     * @param lastSeen - the last seen date of the user.
     * @param daysFromToday - the time period within which we want to check.
     * @return whether they were last seen in the specifie time window or not.
     */
    private Boolean userLastSeenNDays(Long lastSeen, int daysFromToday) {
        return lastSeen > System.currentTimeMillis() - daysFromToday * 24 * 60 * 60 * 1000L;
    }

}
