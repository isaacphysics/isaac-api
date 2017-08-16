package uk.ac.cam.cl.dtg.segue.api.managers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.*;
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
import uk.ac.cam.cl.dtg.segue.dao.streams.KafkaStreamsService;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.util.locations.Location;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.VIEW_QUESTION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANSWER_QUESTION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;

/**
 * KafkaStatisticsManager.
 * TODO like the old stats manager, this file is a mess... it needs refactoring. This at least provides a starting point.
 */
public class KafkaStatisticsManager implements IStatisticsManager {

    private UserAccountManager userManager;
    private ILogManager logManager;
    private GroupManager groupManager;
    private SchoolListReader schoolManager;
    private KafkaStreamsService kafkaStreamsService;
    private IStatisticsManager oldStatisticsManager;

    private Cache<String, Object> longStatsCache;

    private static final Logger log = LoggerFactory.getLogger(KafkaStatisticsManager.class);
    private static final String GENERAL_STATS = "GENERAL_STATS";

    private static final int LONG_STATS_EVICTION_INTERVAL_MINUTES = 720; // 12 hours
    private static final long LONG_STATS_MAX_ITEMS = 20;

    @Inject
    public KafkaStatisticsManager(final UserAccountManager userManager, final ILogManager logManager,
                                  final SchoolListReader schoolManager, final GroupManager groupManager,
                                  final KafkaStreamsService kafkaStreamsService,
                                  final StatisticsManager statsManager) {

        this.oldStatisticsManager = statsManager;

        this.kafkaStreamsService = kafkaStreamsService;
        this.logManager = logManager;
        this.userManager = userManager;
        this.groupManager = groupManager;
        this.schoolManager = schoolManager;

        this.longStatsCache = CacheBuilder.newBuilder()
                .expireAfterWrite(LONG_STATS_EVICTION_INTERVAL_MINUTES, TimeUnit.MINUTES)
                .maximumSize(LONG_STATS_MAX_ITEMS).build();
    }


    @Override
    public synchronized Map<String, Object> outputGeneralStatistics() throws InvalidStateStoreException, SegueDatabaseException {

        @SuppressWarnings("unchecked")
        Map<String, Object> cachedOutput = (Map<String, Object>) this.longStatsCache.getIfPresent(GENERAL_STATS);
        if (cachedOutput != null) {
            log.debug("Using cached statistics.");
            return cachedOutput;
        } else {
            log.info("Calculating General Statistics");
        }


        ImmutableMap.Builder<String, Object> ib = new ImmutableMap.Builder<>();

        // get user records from local kafka store
        // this is much faster than accessing postgres
        ReadOnlyKeyValueStore<String, JsonNode> userStore = waitUntilStoreIsQueryable("store_user_data",
                    QueryableStoreTypes.<String, JsonNode>keyValueStore(),
                    kafkaStreamsService.getStream());


        Map<String, Object> gender = Maps.newHashMap();
        Map<String, Object> role = Maps.newHashMap();

        KeyValueIterator<String, JsonNode> it = userStore.all();

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
        Integer activeStudentsMonth = 0;
        Integer activeTeachersMonth = 0;

        final int sevenDays = 7;
        final int thirtyDays = 30;
        final int sixMonthsInDays = 180;

        while (it.hasNext()) {

            JsonNode userData = it.next().value.path("user_data");

            try {
                userCount++;

                String userId = userData.path("user_id").asText();
                String usrGender = userData.path("gender").asText();
                String usrRole = userData.path("role").asText();

                Integer usrSchoolId = userData.path("school_id").asInt();
                String usrSchoolOther = userData.path("school_other").asText();

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
                if ((usrSchoolId.toString().equals("0") || usrSchoolId.toString().equals("")) && usrSchoolOther.equals("")) {
                    hasNoSchoolCount++;
                } else {
                    hasSchoolCount++;
                    if (usrSchoolOther != "") {
                        hasOtherSchoolCount++;
                    }
                }

                // user activity
                if (userActiveLastNDays(userId, sevenDays)) {
                    activeUsersWeek++;
                    activeUsersMonth++;
                    activeUsersSixMonths++;

                    if (usrRole.equals(Role.STUDENT.toString())) {
                        activeStudentsWeek++;
                        activeStudentsMonth++;
                    }

                    if (usrRole.equals(Role.TEACHER.toString())) {
                        activeTeachersWeek++;
                        activeTeachersMonth++;
                    }

                } else if (userActiveLastNDays(userId, thirtyDays)) {
                    activeUsersMonth++;
                    activeUsersSixMonths++;

                    if (usrRole.equals(Role.STUDENT.toString()))
                        activeStudentsMonth++;

                    if (usrRole.equals(Role.TEACHER.toString()))
                        activeTeachersMonth++;

                } else if (userActiveLastNDays(userId, sixMonthsInDays)) {
                    activeUsersSixMonths++;
                }

            } catch (NullPointerException e) {
                System.out.println("EXCEPTION: " + userData);
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
        ib.put("activeTeachersLastWeek", activeTeachersWeek);
        ib.put("activeTeachersLastThirtyDays", activeTeachersMonth);
        ib.put("activeStudentsLastWeek", activeStudentsWeek);
        ib.put("activeStudentsLastThirtyDays", activeStudentsMonth);

        ib.put("viewQuestionEvents", getLogCount(VIEW_QUESTION));
        ib.put("answeredQuestionEvents", getLogCount(ANSWER_QUESTION));

        ib.put("questionsAnsweredLastWeekTeachers", getLogEventsLastNDays(ANSWER_QUESTION, sevenDays).get(Role.TEACHER.toString()));
        ib.put("questionsAnsweredLastThirtyDaysTeachers", getLogEventsLastNDays(ANSWER_QUESTION, thirtyDays).get(Role.TEACHER.toString()));
        ib.put("questionsAnsweredLastWeekStudents", getLogEventsLastNDays(ANSWER_QUESTION, sevenDays).get(Role.STUDENT.toString()));
        ib.put("questionsAnsweredLastThirtyDaysStudents", getLogEventsLastNDays(ANSWER_QUESTION, thirtyDays).get(Role.STUDENT.toString()));

        ib.put("groupCount", groupManager.getGroupCount());

        Map<String, Object> result = ib.build();
        this.longStatsCache.put(GENERAL_STATS, result);

        log.info("Finished calculating General Statistics");

        return result;
    }


    @Override
    public Long getLogCount(String logTypeOfInterest) throws InvalidStateStoreException {

        ReadOnlyKeyValueStore<String, Long> logEventCounts = waitUntilStoreIsQueryable("store_log_event_counts",
                QueryableStoreTypes.<String, Long>keyValueStore(),
                kafkaStreamsService.getStream());

        return (logEventCounts.get(logTypeOfInterest) != null) ? logEventCounts.get(logTypeOfInterest) : Long.valueOf(0);
    }


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

                    if (userActiveLastNDays(user.getId().toString(), thirtyDays))
                        activeUsers++;

                    if (user.getRole() != null && user.getRole().equals(Role.TEACHER)) {
                        teachersConnected.add(user);
                        if (userActiveLastNDays(user.getId().toString(), thirtyDays))
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

            //this.longStatsCache.put(SCHOOL_STATS, result);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public Map<School, List<RegisteredUserDTO>> getUsersBySchool() throws UnableToIndexSchoolsException, SegueSearchException {

        Map<School, List<RegisteredUserDTO>> usersBySchool = Maps.newHashMap();

        try {

            ReadOnlyKeyValueStore<String, JsonNode> userStore = waitUntilStoreIsQueryable("store_user_data",
                    QueryableStoreTypes.<String, JsonNode>keyValueStore(),
                    kafkaStreamsService.getStream());

            KeyValueIterator<String, JsonNode> it = userStore.all();

            while (it.hasNext()) {

                JsonNode userNode = it.next().value.path("user_data");

                if (userNode.path("school_id").asText().isEmpty())
                    continue;

                RegisteredUserDTO user = new RegisteredUserDTO();
                user.setId(userNode.path("user_id").asLong());
                user.setGivenName(userNode.path("given_name").asText());
                user.setFamilyName(userNode.path("family_name").asText());
                user.setRole(Role.valueOf((!userNode.path("role").asText().isEmpty()) ? userNode.path("role").asText() : Role.STUDENT.toString()));
                user.setGender(Gender.valueOf((!userNode.path("gender").asText().isEmpty()) ? userNode.path("gender").asText() : Gender.OTHER.toString()));
                user.setRegistrationDate(new Timestamp(userNode.path("registration_date").asLong()));

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

        }

        return usersBySchool;

    }

    @Override
    public List<RegisteredUserDTO> getUsersBySchoolId(String schoolId) throws SegueDatabaseException, UnableToIndexSchoolsException, SegueSearchException {

        List<RegisteredUserDTO> users = Lists.newArrayList();
        Map<String, Date> lastSeenUserMap = getLastSeenUserMap();

        ReadOnlyKeyValueStore<String, JsonNode> userStore = waitUntilStoreIsQueryable("store_user_data",
                QueryableStoreTypes.<String, JsonNode>keyValueStore(),
                kafkaStreamsService.getStream());

        KeyValueIterator<String, JsonNode> it = userStore.all();

        while (it.hasNext()) {

            JsonNode userNode = it.next().value.path("user_data");

            if (userNode.path("school_id").asText().equals(schoolId)) {

                RegisteredUserDTO user = new RegisteredUserDTO();
                user.setId(userNode.path("user_id").asLong());
                user.setGivenName(userNode.path("given_name").asText());
                user.setFamilyName(userNode.path("family_name").asText());
                user.setRole(Role.valueOf((!userNode.path("role").asText().isEmpty()) ? userNode.path("role").asText() : Role.STUDENT.toString()));
                user.setGender(Gender.valueOf((!userNode.path("gender").asText().isEmpty()) ? userNode.path("gender").asText() : Gender.OTHER.toString()));
                user.setRegistrationDate(new Timestamp(userNode.path("registration_date").asLong()));
                user.setLastSeen(lastSeenUserMap.get(userNode.path("user_id").asText()));

                users.add(user);
            }
        }

        return users;
    }

    @Override
    public Map<String, Date> getLastSeenUserMap() {

        return getLastSeenUserMap("OVERALL");
    }

    @Override
    public Map<String, Date> getLastSeenUserMap(String qualifyingLogEvent) {

        Map<String, Date> userMap = Maps.newHashMap();

        ReadOnlyKeyValueStore<String, JsonNode> userLastSeenStore = waitUntilStoreIsQueryable("store_user_last_seen",
                QueryableStoreTypes.<String, JsonNode>keyValueStore(),
                kafkaStreamsService.getStream());

        KeyValueIterator<String, JsonNode> it = userLastSeenStore.all();

        while (it.hasNext()) {
            KeyValue<String, JsonNode> record = it.next();
            userMap.put(record.key, new Date(record.value.path("OVERALL").asLong()));
        }

        return userMap;

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
    public Collection<Location> getLocationInformation(Date fromDate, Date toDate) throws SegueDatabaseException {
        return oldStatisticsManager.getLocationInformation(fromDate, toDate);
    }










    private Boolean userActiveLastNDays(String userId, int daysFromToday) {

        Long lastSeen = Long.valueOf(0);

        ReadOnlyKeyValueStore<String, JsonNode> userLastSeenStore = waitUntilStoreIsQueryable("store_user_last_seen",
                QueryableStoreTypes.<String, JsonNode>keyValueStore(),
                kafkaStreamsService.getStream());

        try {
            lastSeen = userLastSeenStore.get(userId).path("OVERALL").asLong();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }


        return lastSeen > System.currentTimeMillis() - daysFromToday * 24 * 60 * 60 * 1000L;
    }


    private Map<String, Long> getLogEventsLastNDays(String logEventType, int daysFromToday) {

        Map<String, Long> mapToReturn = Maps.newHashMap();

        Timestamp stamp = new Timestamp(System.currentTimeMillis());
        Calendar cal = Calendar.getInstance();
        cal.setTime(stamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        Long firstDate = cal.getTimeInMillis() - (daysFromToday * 24 * 60 * 60 * 1000L);

        ReadOnlyKeyValueStore<Long, JsonNode> dailyEvents = waitUntilStoreIsQueryable("store_daily_log_events",
                QueryableStoreTypes.<Long, JsonNode>keyValueStore(),
                kafkaStreamsService.getStream());

        KeyValueIterator<Long, JsonNode> dates = dailyEvents.range(firstDate, cal.getTimeInMillis());

        Long studentCount = Long.valueOf(0);
        Long teacherCount = Long.valueOf(0);

        while (dates.hasNext()) {

            JsonNode node = dates.next().value;

            studentCount += node.path("STUDENT").path(logEventType).asLong();
            teacherCount += node.path("TEACHER").path(logEventType).asLong();

        }

        mapToReturn.put("STUDENT", studentCount);
        mapToReturn.put("TEACHER", teacherCount);
        return mapToReturn;

    }


    private static <T> T waitUntilStoreIsQueryable(final String storeName,
                                                   final QueryableStoreType<T> queryableStoreType,
                                                   final KafkaStreams streams) {
        try {
            return streams.store(storeName, queryableStoreType);
        } catch (InvalidStateStoreException e) {
            throw new InvalidStateStoreException("State store '" + storeName + "' is unavailable!");
        }
    }

}
