package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.test.ProcessorTopologyTestDriver;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.api.managers.IGameManager;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardCreationMethod;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.api.managers.IUserAccountManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userStatistics.UserStatisticsStreamsApplication;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.quiz.IQuestionAttemptManager;
import uk.ac.cam.cl.dtg.util.ClassVersionHash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by du220 on 19/01/2018.
 */
public class UserStatsStreamsServiceTest {

    private ProcessorTopologyTestDriver driver;
    private BufferedReader br = null;
    private String line = "";
    private String csvSplitBy = ";";

    private final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(new JsonSerializer(), new JsonDeserializer());
    private final Serde<String> StringSerde = Serdes.String();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<QuestionValidationResponse> qPartAttempts = Lists.newArrayList();
    private List<String> questionAttemptIds = Lists.newArrayList();


    /**
     * Initial configuration of tests.
     *
     * @throws Exception
     *             - test exception
     */
    @Before
    public final void setUp() throws Exception {

        // store the dummy log events - need these to set up dummy persistent data and to feed to kafka streams
        List<Map<String, Object>> events = Lists.newLinkedList();
        String csvFile = new File("").getAbsolutePath().concat("/src/test/resources/test_data/userStats/kafka-streams-user-snapshot-log.data");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        br = new BufferedReader(new FileReader(csvFile));
        while ((line = br.readLine()) != null) {

            String[] fields = line.split(csvSplitBy);

            JsonNode eventDetails = objectMapper.readTree(fields[4]);

            // make note of question attempts to set up dummy attempt data
            if (fields[2].equals("ANSWER_QUESTION")) {

                String questionId = eventDetails.path("questionId").asText();

                if (!questionAttemptIds.contains(questionId.split("\\|")[0])) {
                    questionAttemptIds.add(questionId.split("\\|")[0]);
                }

                qPartAttempts.add(new QuestionValidationResponse(questionId, null,
                        eventDetails.path("correct").asBoolean(), null,  null));
            }

            // build kafka record and store to process later
            Map<String, Object> kafkaLogRecord = new ImmutableMap.Builder<String, Object>()
                    .put("user_id", fields[0])
                    .put("anonymous_user", fields[1])
                    .put("event_type", fields[2])
                    .put("event_details_type", fields[3])
                    .put("event_details", eventDetails)
                    .put("ip_address", fields[5])
                    .put("timestamp", dateFormat.parse(fields[6]).getTime())
                    .build();

            events.add(kafkaLogRecord);
        }

        // set up mock objects
        IUserAccountManager dummyUserDb = createMock(IUserAccountManager.class);
        IQuestionAttemptManager dummyQuestionAttemptDb = createMock(IQuestionAttemptManager.class);
        ILogManager dummyLogManager = createMock(ILogManager.class);
        IGameManager dummyGameManager = createMock(IGameManager.class);

        // set up the dummy user
        Long testUserId = 1L;
        RegisteredUserDTO testUser = new RegisteredUserDTO("Test", "Testerson", null,
                null, new Date(), Gender.MALE, new Date(),"");
        testUser.setId(testUserId);
        testUser.setRole(Role.TEACHER);

        expect(dummyUserDb.getUserDTOById(testUserId)).andReturn(testUser).anyTimes();
        replay(dummyUserDb);

        // set up dummy question attempt data
        List<Long> userIds = Lists.newArrayList();
        userIds.add(testUserId);

        List<List<String>> attemptedQuestionPageIds = Lists.newArrayList();
        List<Map<Long, Map<String, Map<String, List<QuestionValidationResponse>>>>> expectedQuestionAttemptMaps = Lists.newArrayList();

        for (int i = 0; i < qPartAttempts.size(); i++) {
            expectedQuestionAttemptMaps.add(getTestQuestionAttempts(testUserId, qPartAttempts.get(i)));
            List<String> questionPageIdList = Lists.newArrayList();
            questionPageIdList.add(qPartAttempts.get(i).getQuestionId().split("\\|")[0]);
            attemptedQuestionPageIds.add(questionPageIdList);
        }

        expect(dummyQuestionAttemptDb.getQuestionAttemptsByUsersAndQuestionPrefix(userIds, attemptedQuestionPageIds.get(0))).andReturn(expectedQuestionAttemptMaps.get(0));
        expect(dummyQuestionAttemptDb.getQuestionAttemptsByUsersAndQuestionPrefix(userIds, attemptedQuestionPageIds.get(1))).andReturn(expectedQuestionAttemptMaps.get(1));
        expect(dummyQuestionAttemptDb.getQuestionAttemptsByUsersAndQuestionPrefix(userIds, attemptedQuestionPageIds.get(2))).andReturn(expectedQuestionAttemptMaps.get(2));
        expect(dummyQuestionAttemptDb.getQuestionAttemptsByUsersAndQuestionPrefix(userIds, attemptedQuestionPageIds.get(3))).andReturn(expectedQuestionAttemptMaps.get(3));

        replay(dummyQuestionAttemptDb);


        // set up dummy gameboard data
        GameboardItem gbi1 = new GameboardItem();
        gbi1.setId("gbi1");
        gbi1.setLevel(1);
        GameboardDTO gb1 = new GameboardDTO("98f5602b-cbc6-4786-b39f-88259e6d5172", null, Arrays.asList(gbi1), null, null, null, null, null, GameboardCreationMethod.BUILDER, null);

        GameboardItem gbi2 = new GameboardItem();
        gbi2.setId("gbi2");
        gbi2.setLevel(2);
        gbi2.setTags(Arrays.asList("phys_book_gcse"));
        GameboardDTO gb2 = new GameboardDTO("c6b4b02c-e6cb-4939-9790-c475f9fc8037", null, Arrays.asList(gbi2), null, null, null, null, null, GameboardCreationMethod.BUILDER, null);

        GameboardItem gbi3 = new GameboardItem();
        gbi3.setId("gbi3");
        gbi3.setLevel(3);
        GameboardDTO gb3 = new GameboardDTO("f9c5be15-4fc4-4fdf-ac79-0aa278bdcbf5", null, Arrays.asList(gbi3), null, null, null, null, null, GameboardCreationMethod.BUILDER, null);


        expect(dummyGameManager.getGameboard("98f5602b-cbc6-4786-b39f-88259e6d5172"))
                .andReturn(gb1)
                .atLeastOnce();
        expect(dummyGameManager.getGameboard("c6b4b02c-e6cb-4939-9790-c475f9fc8037"))
                .andReturn(gb2)
                .atLeastOnce();
        expect(dummyGameManager.getGameboard("f9c5be15-4fc4-4fdf-ac79-0aa278bdcbf5"))
                .andReturn(gb3)
                .atLeastOnce();

        replay(dummyGameManager);

        KStreamBuilder builder = new KStreamBuilder();
        Properties streamsConfiguration = new Properties();

        // kafka streaming config variables
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-test-app");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, StringSerde.getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, StringSerde.getClass().getName());

        StreamsConfig config = new StreamsConfig(streamsConfiguration);

        // raw logged events incoming data stream from kafka
        KStream<String, JsonNode>[] rawLoggedEvents = builder.stream(StringSerde, JsonSerde, "topic_logged_events")
                .branch(
                        (k, v) -> !v.path("anonymous_user").asBoolean(),
                        (k, v) -> v.path("anonymous_user").asBoolean()
                );

        // parallel log for anonymous events (may want to optimise how we do this later)
        rawLoggedEvents[1].to(StringSerde, JsonSerde, "topic_anonymous_logged_events");

        // get the streams logic which exists inside the "streamProcess" method
        Method method = UserStatisticsStreamsApplication.class.getDeclaredMethod("streamProcess",
                KStream.class, IQuestionAttemptManager.class, IUserAccountManager.class, IGameManager.class, ILogManager.class);
        method.setAccessible(true);
        method.invoke(null, rawLoggedEvents[0], dummyQuestionAttemptDb, dummyUserDb, dummyGameManager, dummyLogManager);


        driver = new ProcessorTopologyTestDriver(config, builder);

        for (Map<String, Object> event : events
             ) {
            driver.process("topic_logged_events",
                    event.get("user_id").toString().getBytes(),
                    objectMapper.writeValueAsString(event).getBytes());
        }
    }



    @Test
    public void teacherBadgeUpdate_Test() throws Exception {

        // set up the expected outcomes of the test
        HashMap<String, JsonNode> testData = new HashMap<>();
        String filePath = new File("").getAbsolutePath();
        br = new BufferedReader(new FileReader(filePath.concat("/src/test/resources/test_data/userStats/teacher-badges.test")));

        while ((line = br.readLine()) != null) {

            String[] fields = line.split(csvSplitBy);
            testData.put(fields[0], objectMapper.readTree(fields[1]));
        }

        // query the store to obtain calculated values
        ReadOnlyKeyValueStore<String, JsonNode> store = driver.getKeyValueStore("localstore_user_snapshot");
        KeyValueIterator<String, JsonNode> storeEntries = store.all();

        while (storeEntries.hasNext()) {

            KeyValue<String, JsonNode> storeEntry = storeEntries.next();

            JsonNode teacherRecord = storeEntry.value.path("teacher_record");
            Iterator<Map.Entry<String, JsonNode>> badges = teacherRecord.fields();

            while (badges.hasNext()) {
                Map.Entry<String, JsonNode> badge = badges.next();
                assertTrue(testData.containsKey(badge.getKey()) && testData.get(badge.getKey()).equals(badge.getValue()));
            }

            // Gameboard stats are separate
            Long gameboardCreateCount = storeEntry.value.path("gameboard_record").path("creations").path("builder").asLong();
            assertTrue(gameboardCreateCount.equals(testData.get("gameboards").asLong()));
        }
    }


    @Test
    public void dailyStreakUpdate_Test() throws Exception {

        // set up the expected outcomes of the test
        HashMap<String, JsonNode> testData = new HashMap<>();
        String filePath = new File("").getAbsolutePath();
        br = new BufferedReader(new FileReader(filePath.concat("/src/test/resources/test_data/userStats/streak-record.test")));

        while ((line = br.readLine()) != null) {

            String[] fields = line.split(csvSplitBy);
            testData.put(fields[0], objectMapper.readTree(fields[1]));
        }

        // query the store to obtain calculated values
        ReadOnlyKeyValueStore<String, JsonNode> store = driver.getKeyValueStore("localstore_user_snapshot");
        KeyValueIterator<String, JsonNode> storeEntries = store.all();

        while (storeEntries.hasNext()) {

            KeyValue<String, JsonNode> storeEntry = storeEntries.next();

            Long currentActivity = storeEntry.value.path("streak_record").path("current_activity").asLong();
            Long largestStreak = storeEntry.value.path("streak_record").path("largest_streak").asLong();

            assertTrue(currentActivity.equals(testData.get("current_activity").asLong()));
            assertTrue(largestStreak.equals(testData.get("largest_streak").asLong()));

        }

    }


    @Test
    public void streamsClassVersions_Test() throws Exception {
        assertClassUnchanged(UserStatisticsStreamsApplication.class,"d7e6ccb09a9212637317e067f7e2622426c4d0b8411c8e42206edce5556e3a67");
    }


    private void assertClassUnchanged(Class c, String hash) {
        String newHash = ClassVersionHash.hashClass(c);
        assertEquals("Class '" + c.getSimpleName() + "' has changed - need up to update test and (possibly) Kafka streams application ID version number in SiteStatisticsStreamsApplication.\nNew class hash: " + newHash + "\n", newHash, hash);
    }




    private Map<Long, Map<String, Map<String, List<QuestionValidationResponse>>>> getTestQuestionAttempts(Long testUserId, QuestionValidationResponse qPartResponse) {

        Map<Long, Map<String, Map<String, List<QuestionValidationResponse>>>> questionAttemptsByTestUser = Maps.newHashMap();
        questionAttemptsByTestUser.put(testUserId, Maps.newHashMap());

        String questionPageId = qPartResponse.getQuestionId().split("\\|")[0];
        List<QuestionValidationResponse> qPartResponses = Lists.newArrayList();
        qPartResponses.add(qPartResponse);

        Map<String, List<QuestionValidationResponse>> qParts = Maps.newHashMap();
        qParts.put(qPartResponse.getQuestionId(), qPartResponses);

        questionAttemptsByTestUser.get(testUserId).put(questionPageId, qParts);

        return questionAttemptsByTestUser;
    }

}
