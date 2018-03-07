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
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.IUserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka streams processing application for generating site statistics
 *  @author Dan Underwood
 */
public class SiteStatisticsStreamsApplication {

    private static final Logger log = LoggerFactory.getLogger(SiteStatisticsStreamsApplication.class);
    private static final Logger kafkaLog = LoggerFactory.getLogger("kafkaStreamsLogger");

    private static final Serde<String> StringSerde = Serdes.String();
    private static final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(new JsonSerializer(), new JsonDeserializer());
    private static final Serde<Long> LongSerde = Serdes.Long();

    private KafkaStreams streams;

    private final String streamsAppName = "streamsapp_site_stats";
    private final String streamsAppVersion = "v2.1";
    private static Long streamAppStartTime = System.currentTimeMillis();


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

        // set up streams app configuration
        Properties streamsConfiguration = new Properties();
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, streamsAppName + "-" + streamsAppVersion);
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                globalProperties.getProperty("KAFKA_HOSTNAME") + ":" + globalProperties.getProperty("KAFKA_PORT"));
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, globalProperties.getProperty("KAFKA_STREAMS_STATE_DIR"));
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.METADATA_MAX_AGE_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        streamsConfiguration.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, globalProperties.getProperty("KAFKA_REPLICATION_FACTOR"));
        streamsConfiguration.put(StreamsConfig.consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG), "earliest");
        streamsConfiguration.put(StreamsConfig.consumerPrefix(ConsumerConfig.METADATA_MAX_AGE_CONFIG), 45 * 1000);
        streamsConfiguration.put(StreamsConfig.producerPrefix(ProducerConfig.METADATA_MAX_AGE_CONFIG), 45 * 1000);
        streamsConfiguration.put(StreamsConfig.consumerPrefix(ConsumerConfig.MAX_POLL_RECORDS_CONFIG), 250);
        streamsConfiguration.put(StreamsConfig.consumerPrefix(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG), 60000);


        // ensure topics exist before attempting to consume

        // logged events
        List<ConfigEntry> loggedEventsConfigs = Lists.newLinkedList();
        loggedEventsConfigs.add(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(-1)));
        kafkaTopicManager.ensureTopicExists(Constants.KAFKA_TOPIC_LOGGED_EVENTS, loggedEventsConfigs);

        // local store changelog topics
        List<ConfigEntry> changelogConfigs = Lists.newLinkedList();
        changelogConfigs.add(new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT));

        // we set up the changelog topics ourselves so we can use them for global state store population (below)
        kafkaTopicManager.ensureTopicExists(streamsAppName + "-" + streamsAppVersion + "-localstore_user_data-changelog", changelogConfigs);
        kafkaTopicManager.ensureTopicExists(streamsAppName + "-" + streamsAppVersion + "-localstore_log_event_counts-changelog", changelogConfigs);

        final AtomicLong lastLagLog = new AtomicLong(0);
        final AtomicBoolean wasLagging = new AtomicBoolean(true);

        // raw logged events incoming data stream from kafka
        KStreamBuilder builder = new KStreamBuilder();
        KStream<String, JsonNode> rawLoggedEvents = builder.stream(StringSerde, JsonSerde, Constants.KAFKA_TOPIC_LOGGED_EVENTS)
                .filterNot(
                        (k, v) -> v.path("transferredFromAnonymous").asBoolean()
                )
                // log any lags in the streams processing
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
                );

        // process raw logged events
        streamProcess(rawLoggedEvents, userAccountManager);

        // need to make state stores queryable globally, as we often have 2 versions of API running concurrently, hence 2 streams app instances
        // aggregations are saved to a local state store per streams app instance and update a changelog topic in Kafka
        // we can use this changelog to populate a global state store for all streams app instances
        builder.globalTable(StringSerde, JsonSerde,streamsAppName + "-" + streamsAppVersion + "-localstore_user_data-changelog",
                "globalstore_user_data-" + streamsAppVersion);
        builder.globalTable(StringSerde, LongSerde,streamsAppName + "-" + streamsAppVersion + "-localstore_log_event_counts-changelog",
                "globalstore_log_event_counts-" + streamsAppVersion);

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
     *
     * @param rawStream
     *          - the input stream
     */
    private static void streamProcess(KStream<String, JsonNode> rawStream,
                                     IUserAccountManager userAccountManager) {

        // map the key-value pair to one where the key is always the user id
        KStream<String, JsonNode> mappedStream = rawStream
                .map(
                        (k, v) -> new KeyValue<>(v.path("user_id").asText(), v)
                );

        // process user data in local data stores, extract user record related events
        mappedStream
                .filterNot(
                        (k, v) -> v.path("anonymous_user").asBoolean()
                )
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode userRecord = JsonNodeFactory.instance.objectNode();

                            // set up user data attributes
                            userRecord.put("user_id", "");
                            userRecord.put("user_data", "");

                            // set up last seen attributes
                            ObjectNode lastSeenData = JsonNodeFactory.instance.objectNode();
                            lastSeenData.put("last_seen", 0);
                            userRecord.set("last_seen_data", lastSeenData);


                            return userRecord;
                        },
                        // aggregator
                        (userId, userUpdateLogEvent, userRecord) -> {

                            try {

                                String eventType = userUpdateLogEvent.path("event_type").asText();

                                if (eventType.equals("CREATE_UPDATE_USER")) {

                                    // we get the latest record from postgres to avoid passing sensitive info through Kafka
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
                                    ((ObjectNode) userRecord).set("user_data", userDetails);
                                }

                                // record user last seen data
                                JsonNode lastSeenData = userRecord.path("last_seen_data");
                                Timestamp stamp = new Timestamp(userUpdateLogEvent.path("timestamp").asLong());


                                if (!lastSeenData.has(eventType)) {
                                    ObjectNode node = JsonNodeFactory.instance.objectNode();
                                    node.put("count", 0);
                                    node.put("latest", 0);
                                    ((ObjectNode) lastSeenData).set(eventType, node);
                                }

                                Long count = lastSeenData.path(eventType).path("count").asLong();
                                ((ObjectNode) lastSeenData.path(eventType)).put("count", count + 1);
                                ((ObjectNode) lastSeenData.path(eventType)).put("latest", stamp.getTime());
                                ((ObjectNode) lastSeenData).put("last_seen", stamp.getTime());


                            } catch (NoUserException e) {
                                // don't want to clog up the logs with historical processing if we are ever doing a backfill
                                if (userUpdateLogEvent.path("timestamp").asLong() > streamAppStartTime) {
                                    log.error("User " + userId + " not found in Postgres DB while processing streams data!");
                                }
                                // returning a null record will ensure that it will be removed from the internal rocksDB store
                                return null;
                            } catch (NumberFormatException | SegueDatabaseException e) {
                                if (userUpdateLogEvent.path("timestamp").asLong() > streamAppStartTime) {
                                    log.error("Could not process user with id = " + userId + " in streams application.");
                                }
                                return null;
                            } catch (RuntimeException e) {
                                // streams app annoyingly dies if there is an uncaught runtime exception from any level, so we catch everything
                                e.printStackTrace();
                            }

                            return userRecord;
                        },
                        JsonSerde,
                        "localstore_user_data"
                );


        // maintain internal store of log event type counts
        mappedStream
                .map(
                        (k, v) -> new KeyValue<>(v.path("event_type").asText(), v)
                )
                .groupByKey(StringSerde, JsonSerde)
                .count("localstore_log_event_counts");
    }



    /**
     * Method to obtain current count for a particular type of log event.
     *
     * @param logEventType the type of event we want to get the count for.
     * @return the number of times this event has been logged.
     * @throws InvalidStateStoreException if there is a kafka data store error.
     */
    public Long getLogCountByType(String logEventType) throws InvalidStateStoreException {

        Long count = streams
                .store("globalstore_log_event_counts-" + streamsAppVersion,
                        QueryableStoreTypes.<String, Long>keyValueStore())
                .get(logEventType);

        return (count != null) ? count : Long.valueOf(0);
    }



    /**
     * Method to obtain a map of all the last logged times for each user for a particular log type.
     *
     * @param logEventType the string event type that will be looked for.
     * @return a map of userId's to last event timestamp.
     * @throws InvalidStateStoreException if there is a kafka data store error.
     */
    public Map<String, Date> getLastLogDateForAllUsers(String logEventType) throws InvalidStateStoreException {

        Map<String, Date> userMap = Maps.newHashMap();

        KeyValueIterator<String, JsonNode> allUsers = getAllUsers();

        while (allUsers.hasNext()) {
            KeyValue<String, JsonNode> record = allUsers.next();
            userMap.put(record.key,
                    new Date(record.value
                            .path("last_seen_data")
                            .path(logEventType)
                            .path("count").asLong()
                    )
            );
        }

        return userMap;
    }


    /**
     * Method to obtain an iterator over all user data records.
     *
     * @return iterator of the data records
     * @throws InvalidStateStoreException if there is a kafka data store error.
     */
    public KeyValueIterator<String, JsonNode> getAllUsers() throws InvalidStateStoreException {

        return streams
                .store("globalstore_user_data-" + streamsAppVersion,
                        QueryableStoreTypes.<String, JsonNode>keyValueStore())
                .all();
    }

}
