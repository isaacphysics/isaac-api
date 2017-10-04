/**
 * Copyright 2017 Dan Underwood
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
package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
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
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.SiteStatisticsStreamsApplication;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.UserAchievementStreamsApplication;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.customMappers.AugmentedQuestionDetailMapper;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.customProcessors.ThresholdAchievedProcessor;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.derivedStreams.TeacherAssignmentActivity;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.derivedStreams.UserQuestionAttempts;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.derivedStreams.UserWeeklyStreaks;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlerts;
import uk.ac.cam.cl.dtg.util.ClassVersionHash;


import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;

/**
 * Test class for the UserAchievementStreamsService class.
 */
@PowerMockIgnore({"javax.ws.*"})
public class userQuestionsAnsweredTest {

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
    @Inject
    public final void setUp(final PostgresSqlDb database,
                            final IContentManager contentManager,
                            @Named(CONTENT_INDEX) final String contentIndex,
                            IUserAlerts userAlerts) throws Exception {
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

        UserQuestionAttempts.process(rawLoggedEvents[0], new ThresholdAchievedProcessor(database, userAlerts), contentManager, contentIndex);

        driver = new ProcessorTopologyTestDriver(config, builder);


        String csvFile = "C:/dev/isaac-other-resources/kafka-streams-test.data";
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
                    .put("timestamp", fields[6])
                    .build();

            driver.process("topic_logged_events",
                    fields[0].getBytes(),
                    objectMapper.writeValueAsString(kafkaLogRecord).getBytes());
        }
    }

    private void assertClassUnchanged(Class c, String hash) {
        String newHash = ClassVersionHash.hashClass(c);
        assertEquals("Class '" + c.getSimpleName() + "' has changed - need up to update test and (possibly) Kafka streams application ID version number in SiteStatisticsStreamsApplication.\nNew class hash: " + newHash + "\n", newHash, hash);
    }

    @Test
    public void streamsClassVersions_Test() {

        // we should manually add any new classes here that get added to the uk.ac.cam.cl.dtg.dao.kafkaStreams.userAchievements package

        assertClassUnchanged(UserAchievementStreamsApplication.class,"fe8327260175f2480981741378127f4dff2558ab13178f354754c42a1aa1ded8");
        assertClassUnchanged(TeacherAssignmentActivity.class,"17588b63280580d45a0a5c8a8eeffa485cde9377dc5eeb985697a788ad331fd4");
        assertClassUnchanged(UserQuestionAttempts.class,"4b58648866912d16076cbdcf0bc4e3b57d5beb512253cea3e4d8bd8bed0b9de5");
        assertClassUnchanged(UserWeeklyStreaks.class,"ba3f134b960fc293e84609db505e02a831b451531ed3372be0a5c8c249d6ea23");
        assertClassUnchanged(ThresholdAchievedProcessor.class,"2b794835812b341d30c7d452c27dfd100255077eff26e810e654b5e1361f5bba");
        assertClassUnchanged(AugmentedQuestionDetailMapper.class,"4188da2d1fc1655660ff995b640ec4a6aa12ad85ce8b6af7b961dcf6defb46a5");
    }


}
