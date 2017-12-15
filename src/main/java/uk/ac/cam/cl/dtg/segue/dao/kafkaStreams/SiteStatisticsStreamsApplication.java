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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Concrete Kafka streams processing application for generating site statistics
 *  @author Dan Underwood
 */
public class SiteStatisticsStreamsApplication {

    private static final Logger log = LoggerFactory.getLogger(SiteStatisticsStreamsApplication.class);
    private static final Logger kafkaLog = LoggerFactory.getLogger("kafkaStreamsLogger");

    private static final Serializer<JsonNode> JsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> JsonDeserializer = new JsonDeserializer();
    private static final Serde<String> StringSerde = Serdes.String();
    private static final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(JsonSerializer, JsonDeserializer);
    private static final Serde<Long> LongSerde = Serdes.Long();

    private KafkaTopicManager kafkaTopicManager;
    private UserAccountManager userAccountManager;
    private KafkaStreams streams;
    private KStreamBuilder builder = new KStreamBuilder();
    private Properties streamsConfiguration = new Properties();

    private final String streamsAppNameAndVersion = "streamsapp_site_stats-v1.43";


    /**
     * Constructor
     * @param globalProperties
     *              - properties object containing global variables
     * @param kafkaTopicManager
     *              - manager for kafka topic administration
     */
    public SiteStatisticsStreamsApplication(final PropertiesLoader globalProperties,
                                            final KafkaTopicManager kafkaTopicManager,
                                            final UserAccountManager userAccountManager) {

        this.kafkaTopicManager = kafkaTopicManager;
        this.userAccountManager = userAccountManager;

        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, streamsAppNameAndVersion);
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                globalProperties.getProperty("KAFKA_HOSTNAME") + ":" + globalProperties.getProperty("KAFKA_PORT"));
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, globalProperties.getProperty("KAFKA_STREAMS_STATE_DIR"));
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        streamsConfiguration.put(StreamsConfig.METADATA_MAX_AGE_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        streamsConfiguration.put(StreamsConfig.consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG), "earliest");
        streamsConfiguration.put(StreamsConfig.consumerPrefix(ConsumerConfig.METADATA_MAX_AGE_CONFIG), 60 * 1000);
        streamsConfiguration.put(StreamsConfig.producerPrefix(ProducerConfig.METADATA_MAX_AGE_CONFIG), 60 * 1000);

    }


    /**
     * Method to be called to start the streams application
     */
    public void start() {

        // ensure topics exist before attempting to consume

        // logged events
        List<ConfigEntry> loggedEventsConfigs = Lists.newLinkedList();
        loggedEventsConfigs.add(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(-1)));
        kafkaTopicManager.ensureTopicExists("topic_logged_events", loggedEventsConfigs);

        // local store changelog topics
        List<ConfigEntry> changelogConfigs = Lists.newLinkedList();
        changelogConfigs.add(new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT));

        kafkaTopicManager.ensureTopicExists(streamsAppNameAndVersion + "-localstore_user_data-changelog", changelogConfigs);
        kafkaTopicManager.ensureTopicExists(streamsAppNameAndVersion + "-localstore_user_last_seen-changelog", changelogConfigs);
        kafkaTopicManager.ensureTopicExists(streamsAppNameAndVersion + "-localstore_log_event_counts-changelog", changelogConfigs);


        // raw logged events incoming data stream from kafka
        KStream<String, JsonNode> rawLoggedEvents = builder.stream(StringSerde, JsonSerde, "topic_logged_events")
                .filterNot(
                        (k, v) -> v.path("anonymous_user").asBoolean()
                );

        // process raw logged events
        streamProcess(rawLoggedEvents);

        // need to make state stores queryable globally, as we often have 2 versions of API running concurrently, hence 2 streams app instances
        // aggregations are saved to a local state store per streams app instance and update a changelog topic in Kafka
        // we can use this changelog to populate a global state store for all streams app instances
        builder.globalTable(StringSerde, JsonSerde,streamsAppNameAndVersion + "-localstore_user_data-changelog", "globalstore_user_data");
        builder.globalTable(StringSerde, JsonSerde,streamsAppNameAndVersion + "-localstore_user_last_seen-changelog", "globalstore_user_last_seen");
        builder.globalTable(StringSerde, LongSerde,streamsAppNameAndVersion + "-localstore_log_event_counts-changelog", "globalstore_log_event_counts");

        // use the builder and the streams configuration we set to setup and start a streams object
        streams = new KafkaStreams(builder, streamsConfiguration);
        streams.start();

        // return when streams instance is initialized
        while (true) {

            if (streams.state().isCreatedOrRunning())
                break;
        }

        kafkaLog.info("Site statistics streams application started.");

    }

    /**
     * This method contains the logic that transforms the incoming stream
     * We keep this public and static to make it easy to unit test
     *
     * @param rawStream
     *          - the input stream
     */
    public void streamProcess(KStream<String, JsonNode> rawStream) {

        final AtomicLong lastLagLog = new AtomicLong(0);
        final AtomicBoolean wasLagging = new AtomicBoolean(true);

        // process user data in local data stores, extract user record related events
        KTable<String, JsonNode> userData = rawStream
                .peek(
                    (k,v) -> {
                        long lag = System.currentTimeMillis() - v.get("timestamp").asLong();
                        if (lag > 1000) {

                            if (System.currentTimeMillis() - lastLagLog.get() > 10000) {
                                kafkaLog.info(String.format("Site statistics stream lag: %.02f hours (%.03f s).", lag / 3600000.0, lag / 1000.0));
                                lastLagLog.set(System.currentTimeMillis());
                            }

                        } else if (wasLagging.get()) {
                            wasLagging.set(false);
                            kafkaLog.info("Site statistics stream processing caught up.");
                        }
                    }
                )
                .filter(
                        (userId, loggedEvent) -> loggedEvent.path("event_type")
                                .asText()
                                .equals("CREATE_UPDATE_USER")
                )
                // set up a local user data store
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode userRecord = JsonNodeFactory.instance.objectNode();

                            userRecord.put("user_id", "");
                            userRecord.put("user_data", "");
                            return userRecord;
                        },
                        // aggregator
                        (userId, userUpdateLogEvent, userRecord) -> {

                            try {
                                RegisteredUserDTO regUser = userAccountManager.getUserDTOById(Long.parseLong(userId));

                                ObjectNode userDetails = JsonNodeFactory.instance.objectNode()
                                        .put("user_id", regUser.getId())
                                        .put("family_name", regUser.getFamilyName())
                                        .put("given_name", regUser.getGivenName())
                                        .put("role", regUser.getRole().name())
                                        .put("date_of_birth", (regUser.getDateOfBirth() != null) ? regUser.getDateOfBirth().getTime() : 0)
                                        .put("gender", (regUser.getGender() != null) ? regUser.getGender().name() : "")
                                        .put("registration_date", regUser.getRegistrationDate().getTime())
                                        .put("school_id", (regUser.getSchoolId() != null) ? regUser.getSchoolId() : "")
                                        .put("school_other", (regUser.getSchoolOther() != null) ? regUser.getSchoolOther() : "");

                                ((ObjectNode) userRecord).put("user_id", userId);
                                ((ObjectNode) userRecord).put("user_data", userDetails);

                            } catch (NoUserException | SegueDatabaseException e) {
                                e.printStackTrace();
                            }

                            return userRecord;
                        },
                        JsonSerde,
                        "localstore_user_data"
                );

        // join user table to incoming event stream to get user data for stats processing
        KStream<String, JsonNode> userEvents = rawStream
                /*.map(
                        (userId, logEvent) -> {

                            ObjectNode newValueRecord = JsonNodeFactory.instance.objectNode();

                            try {

                                RegisteredUserDTO user = userManager.getUserDTOById(Long.parseLong(userId));

                                newValueRecord.put("user_id", user.getId());
                                newValueRecord.put("user_role", (user.getRole() != null) ? user.getRole().name() : "");
                                newValueRecord.put("user_gender", (user.getGender() != null) ? user.getGender().name() : "");
                            } catch (SegueDatabaseException e) {
                                log.error("Unable to access database", e);
                            } catch (NoUserException e) {
                                log.error("Unable to get user data from database", e);
                            }

                            newValueRecord.put("event_type", logEvent.path("event_type").asText());
                            newValueRecord.put("event_details", logEvent.path("event_details"));
                            newValueRecord.put("timestamp", logEvent.path("timestamp").asLong());

                            return new KeyValue<String, JsonNode>(userId, newValueRecord);
                        }
                );*/
                .join(
                        userData,
                        (logEventVal, userDataVal) -> {
                            ObjectNode joinedValueRecord = JsonNodeFactory.instance.objectNode();

                            joinedValueRecord.put("user_id", userDataVal.path("user_data").path("user_id").asInt());
                            joinedValueRecord.put("user_role", userDataVal.path("user_data").path("role").asText());
                            joinedValueRecord.put("user_gender", userDataVal.path("user_data").path("gender").asText());
                            joinedValueRecord.put("event_type", logEventVal.path("event_type").asText());
                            joinedValueRecord.put("event_details", logEventVal.path("event_details"));
                            joinedValueRecord.put("timestamp", logEventVal.path("timestamp").asLong());

                            return joinedValueRecord;
                        }
                );


        // maintain internal store of users' last seen times by log event type, and counts per event type
        userEvents
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode countRecord = JsonNodeFactory.instance.objectNode();
                            countRecord.put("last_seen", 0);
                            return countRecord;
                        },
                        // aggregator
                        (userId, logEvent, countRecord) -> {

                            String eventType = logEvent.path("event_type").asText();
                            Timestamp stamp = new Timestamp(logEvent.path("timestamp").asLong());

                            if (!countRecord.has(eventType)) {
                                ObjectNode node = JsonNodeFactory.instance.objectNode();
                                node.put("count", 0);
                                node.put("latest", 0);
                                ((ObjectNode) countRecord).put(eventType, node);
                            }

                            Long count = countRecord.path(eventType).path("count").asLong();
                            ((ObjectNode) countRecord.path(eventType)).put("count", count + 1);
                            ((ObjectNode) countRecord.path(eventType)).put("latest", stamp.getTime());

                            ((ObjectNode) countRecord).put("last_seen", stamp.getTime());

                            return countRecord;
                        },
                        JsonSerde,
                        "localstore_user_last_seen"
                );


        // maintain internal store of log event type counts
        userEvents
                .map(
                        (k, v) -> {
                            return new KeyValue<String, JsonNode>(v.path("event_type").asText(), v);
                        }
                )
                .groupByKey(StringSerde, JsonSerde)
                .count("localstore_log_event_counts");

    }


    /**
     * Returns single instance of streams service to dependants.
     * Useful for accessing state stores etc.
     *
     * @return streams
     *          - the single streams instance
     */
    public KafkaStreams getStream() {
        return streams;
    }
}
