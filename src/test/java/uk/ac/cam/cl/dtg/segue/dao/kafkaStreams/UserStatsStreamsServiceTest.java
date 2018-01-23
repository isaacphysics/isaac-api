package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.test.ProcessorTopologyTestDriver;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.segue.api.managers.IUserAccountManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.quiz.IQuestionAttemptManager;
import uk.ac.cam.cl.dtg.util.ClassVersionHash;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

/**
 * Created by du220 on 19/01/2018.
 */
public class UserStatsStreamsServiceTest {

    private ProcessorTopologyTestDriver driver;
    private BufferedReader br = null;
    private String line = "";
    private String csvSplitBy = ";";

    private final Serializer<JsonNode> jsonSerializer = new JsonSerializer();
    private final Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
    private final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);
    private final Serde<String> StringSerde = Serdes.String();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Initial configuration of tests.
     *
     * @throws Exception
     *             - test exception
     */
    @Before
    public final void setUp() throws Exception {

        IUserAccountManager dummyUserDb = createMock(IUserAccountManager.class);
        IQuestionAttemptManager dummyQuestionAttemptDb = createMock(IQuestionAttemptManager.class);
        ILogManager dummyLogManager = createMock(ILogManager.class);

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

        // initialise the mock DB for the user data
        Long testUser1Id = 1L;
        Long testUser2Id = 2L;

        RegisteredUserDTO testUser1 = new RegisteredUserDTO("TestingOne", "Test1", null,
                null, new Date(), Gender.MALE, new Date(),"");
        testUser1.setId(testUser1Id);
        testUser1.setRole(Role.STUDENT);

        RegisteredUserDTO testUser2change = new RegisteredUserDTO("TestingTwoChange", "Test2", null,
                null, new Date(), Gender.FEMALE, new Date(),"");
        testUser2change.setId(testUser2Id);
        testUser2change.setRole(Role.TEACHER);

        expect(dummyUserDb.getUserDTOById(testUser1Id)).andReturn(testUser1).atLeastOnce();
        expect(dummyUserDb.getUserDTOById(testUser2Id)).andReturn(testUser2change).atLeastOnce();
        replay(dummyUserDb);


        List<Map<String, Object>> events = Lists.newLinkedList();

        String csvFile = new File("").getAbsolutePath().concat("/src/test/resources/test_data/kafka-streams-test.data");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        br = new BufferedReader(new FileReader(csvFile));
        while ((line = br.readLine()) != null) {

            String[] fields = line.split(csvSplitBy);

            Map<String, Object> kafkaLogRecord = new ImmutableMap.Builder<String, Object>()
                    .put("user_id", fields[0])
                    .put("anonymous_user", fields[1])
                    .put("event_type", fields[2])
                    .put("event_details_type", fields[3])
                    .put("event_details", objectMapper.readTree(fields[4]))
                    .put("ip_address", fields[5])
                    .put("timestamp", dateFormat.parse(fields[6]).getTime())
                    .build();

            events.add(kafkaLogRecord);
        }


        // SITE STATISTICS
        UserStatisticsStreamsApplication.streamProcess(rawLoggedEvents[0], dummyUserDb, dummyQuestionAttemptDb, dummyLogManager);

        driver = new ProcessorTopologyTestDriver(config, builder);

        for (Map<String, Object> event : events
             ) {
            driver.process("topic_logged_events",
                    event.get("user_id").toString().getBytes(),
                    objectMapper.writeValueAsString(event).getBytes());
        }
    }



    @Test
    public void streamsClassVersions_Test() throws Exception {
        assertClassUnchanged(UserStatisticsStreamsApplication.class,"bb385eaf6cf51dcf9589ebe725c0e3d36f13e8e33af05d05394325af9f014a11");
    }


    private void assertClassUnchanged(Class c, String hash) {
        String newHash = ClassVersionHash.hashClass(c);
        assertEquals("Class '" + c.getSimpleName() + "' has changed - need up to update test and (possibly) Kafka streams application ID version number in SiteStatisticsStreamsApplication.\nNew class hash: " + newHash + "\n", newHash, hash);
    }

}
