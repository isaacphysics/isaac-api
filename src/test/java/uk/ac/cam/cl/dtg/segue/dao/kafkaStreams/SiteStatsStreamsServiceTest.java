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
package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
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
import uk.ac.cam.cl.dtg.util.ClassVersionHash;


import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the SiteStatsStreamsService class.
 */
@PowerMockIgnore({"javax.ws.*"})
public class SiteStatsStreamsServiceTest {

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


        // SITE STATISTICS
        SiteStatisticsStreamsApplication.streamProcess(rawLoggedEvents[0]);

        driver = new ProcessorTopologyTestDriver(config, builder);

        String csvFile = "C:/dev/isaac-other-resources/kafka-streams-test.data";
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

            driver.process("topic_logged_events",
                    fields[0].getBytes(),
                    objectMapper.writeValueAsString(kafkaLogRecord).getBytes());
        }
    }




    @Test
    public void userDataTableUpdate_Test() throws Exception {

        HashMap<String, Map<String, Object>> testData = new HashMap<>();

        br = new BufferedReader(new FileReader("C:/dev/isaac-other-resources/kafka-streams-user-data-table-update.test"));
        while ((line = br.readLine()) != null) {

            String[] fields = line.split(csvSplitBy);

            Map<String, Object> userRecord = new ImmutableMap.Builder<String, Object>()
                    .put("user_id", fields[0])
                    .put("family_name", fields[1])
                    .put("given_name", fields[2])
                    .put("role", fields[3])
                    .put("date_of_birth", fields[4])
                    .put("gender", fields[5])
                    .put("registration_date", fields[6])
                    .put("school_id", fields[7])
                    .put("school_other", fields[8])
                    .put("default_level", fields[9])
                    .put("email_verification_status", fields[10])
                    .build();

            testData.put(fields[0], userRecord);
        }

        ReadOnlyKeyValueStore<String, JsonNode> store = driver.getKeyValueStore("store_user_data");
        KeyValueIterator<String, JsonNode> iter = store.all();

        while (iter.hasNext()) {

            KeyValue<String, JsonNode> entry = iter.next();

            String test = testData.get(entry.key).get("family_name").toString();

            assertTrue(testData.containsKey(entry.key)
                    && (testData.get(entry.key).get("family_name").toString().equals(entry.value.path("user_data").path("family_name").asText()))
                    && (testData.get(entry.key).get("given_name").toString().equals(entry.value.path("user_data").path("given_name").asText()))
                    && (testData.get(entry.key).get("gender").toString().equals(entry.value.path("user_data").path("gender").asText()))
                    && (testData.get(entry.key).get("role").toString().equals(entry.value.path("user_data").path("role").asText()))
            );
        }

    }



    @Test
    public void eventTypeCounts_Test() throws Exception {

        HashMap<String, Long> testData = new HashMap<>();

        br = new BufferedReader(new FileReader("C:/dev/isaac-other-resources/kafka-streams-log-event-counts.test"));
        while ((line = br.readLine()) != null) {

            String[] fields = line.split(csvSplitBy);
            testData.put(fields[0], Long.parseLong(fields[1]));
        }

        ReadOnlyKeyValueStore<String, Long> store = driver.getKeyValueStore("store_log_event_counts");
        KeyValueIterator<String, Long> iter = store.all();

        while (iter.hasNext()) {

            KeyValue<String, Long> entry = iter.next();
            assertTrue(testData.containsKey(entry.key) && testData.get(entry.key).equals(entry.value));
        }
    }


    @Test
    public void userLastSeen_Test() throws Exception {

        HashMap<String, JsonNode> testData = new HashMap<>();

        br = new BufferedReader(new FileReader("C:/dev/isaac-other-resources/kafka-streams-user-last-seen.test"));
        while ((line = br.readLine()) != null) {

            String[] fields = line.split(csvSplitBy);
            testData.put(fields[0], objectMapper.readTree(fields[1]));
        }

        ReadOnlyKeyValueStore<String, JsonNode> store = driver.getKeyValueStore("store_user_last_seen");
        KeyValueIterator<String, JsonNode> iter = store.all();

        while (iter.hasNext()) {

            KeyValue<String, JsonNode> entry = iter.next();
            Iterator<Map.Entry<String, JsonNode>> innerIter = entry.value.fields();

            assertTrue(testData.containsKey(entry.key));

            while (innerIter.hasNext()) {

                Map.Entry<String, JsonNode> innerEntry = innerIter.next();

                if (innerEntry.getValue().asText().equals("last_seen")) {
                    assertTrue(testData.get(entry.key).has("last_seen") && testData.get(entry.key).path("last_seen").equals(innerEntry.getValue()));
                } else {
                    assertTrue(testData.get(entry.key).has(innerEntry.getKey()) && testData.get(entry.key).path(innerEntry.getKey()).equals(innerEntry.getValue()));
                }
            }
        }

    }


    @Test
    public void streamsClassVersions_Test() {
        assertClassUnchanged(SiteStatisticsStreamsApplication.class,"8537da484ac395c4d42c9656652087e274047c8cf5ad1ca0bfd022b387b3221d");
    }




    private void assertClassUnchanged(Class c, String hash) {
        String newHash = ClassVersionHash.hashClass(c);
        assertEquals("Class '" + c.getSimpleName() + "' has changed - need up to update test and (possibly) Kafka streams application ID version number in SiteStatisticsStreamsApplication.\nNew class hash: " + newHash + "\n", newHash, hash);
    }
}
