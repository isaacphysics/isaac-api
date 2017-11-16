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
import com.google.api.client.util.Maps;
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
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.userAlerts.IAlertListener;
import uk.ac.cam.cl.dtg.segue.api.userAlerts.UserAlertsWebSocket;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlert;
import uk.ac.cam.cl.dtg.segue.dos.PgUserAlert;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.quiz.IQuestionAttemptManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Concrete Kafka streams processing application for generating user statistics
 *  @author Dan Underwood
 */
public class UserStatisticsStreamsApplication {

    private static final Logger log = LoggerFactory.getLogger(SiteStatisticsStreamsApplication.class);
    private static final Logger kafkaLog = LoggerFactory.getLogger("kafkaStreamsLogger");

    private static final Serializer<JsonNode> JsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> JsonDeserializer = new JsonDeserializer();
    private static final Serde<String> StringSerde = Serdes.String();
    private static final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(JsonSerializer, JsonDeserializer);
    private static final Serde<Long> LongSerde = Serdes.Long();

    private KafkaTopicManager kafkaTopicManager;
    private KafkaStreams streams;
    private KStreamBuilder builder = new KStreamBuilder();
    private Properties streamsConfiguration = new Properties();
    private IQuestionAttemptManager questionAttemptManager;

    private final String streamsAppNameAndVersion = "streamsapp_user_stats-v1.0";


    /**
     * Constructor
     * @param globalProperties
     *              - properties object containing global variables
     * @param kafkaTopicManager
     *              - manager for kafka topic administration
     */
    public UserStatisticsStreamsApplication(final PropertiesLoader globalProperties,
                                            final KafkaTopicManager kafkaTopicManager,
                                            final IQuestionAttemptManager questionAttemptManager) {

        this.kafkaTopicManager = kafkaTopicManager;
        this.questionAttemptManager = questionAttemptManager;

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
        kafkaTopicManager.ensureTopicExists(streamsAppNameAndVersion + "-localstore_user_daily_streaks-changelog", changelogConfigs);


        // raw logged events incoming data stream from kafka
        KStream<String, JsonNode> rawLoggedEvents = builder.stream(StringSerde, JsonSerde, "topic_logged_events")
                .filterNot(
                        (k, v) -> v.path("anonymous_user").asBoolean()
                );

        // process raw logged events
        streamProcess(rawLoggedEvents, questionAttemptManager);

        // need to make state stores queryable globally, as we often have 2 versions of API running concurrently, hence 2 streams app instances
        // aggregations are saved to a local state store per streams app instance and update a changelog topic in Kafka
        // we can use this changelog to populate a global state store for all streams app instances
        builder.globalTable(StringSerde, JsonSerde,streamsAppNameAndVersion + "-localstore_user_daily_streaks-changelog", "globalstore_user_daily_streaks");

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
    public static void streamProcess(KStream<String, JsonNode> rawStream,
                                     IQuestionAttemptManager questionAttemptManager) {

        // user question answer streaks
        rawStream
                .filter(
                        (userId, loggedEvent) -> loggedEvent.path("event_type")
                                .asText()
                                .equals("ANSWER_QUESTION")
                ).groupByKey()
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode streakRecord = JsonNodeFactory.instance.objectNode();

                            streakRecord.put("streak_start", 0);
                            streakRecord.put("streak_end", 0);
                            streakRecord.put("current_streak", 0);
                            streakRecord.put("largest_streak", 0);

                            return streakRecord;
                        },
                        // aggregator
                        (userId, latestEvent, streakRecord) -> {

                            // timestamp of streak start
                            Long streakStartTimestamp = streakRecord.path("streak_start").asLong();

                            // timestamp of streak end
                            Long streakEndTimestamp = streakRecord.path("streak_end").asLong();

                            // record of largest streak length
                            Integer largestStreak = streakRecord.path("largest_streak").asInt();

                            // timestamp of latest event
                            Long latestEventTimestamp = latestEvent.path("timestamp").asLong();

                            // question id
                            String questionId = latestEvent.path("event_details").path("questionId").asText();

                            // set up calendar objects for date comparisons
                            Calendar latest = Calendar.getInstance();
                            Calendar previous = Calendar.getInstance();
                            latest.setTimeInMillis(latestEventTimestamp);
                            previous.setTimeInMillis(streakEndTimestamp);

                            latest = roundDownToDay(latest);
                            previous = roundDownToDay(previous);

                            // if the latest event has happened on the same day as the previous event, don't continue
                            if (TimeUnit.MINUTES.convert(latest.getTimeInMillis() - previous.getTimeInMillis(), TimeUnit.MILLISECONDS) < 1) {
                                return streakRecord;
                            }

                            // if the user has already answered the question correctly, don't continue
                            List<Long> user = Lists.newArrayList();
                            List<String> questionPageId = Lists.newArrayList();
                            user.add(Long.parseLong(userId));
                            questionPageId.add(questionId.split("\\|")[0]);

                            try {
                                Map<Long, Map<String, Map<String, List<QuestionValidationResponse>>>> questionAttempts =
                                        questionAttemptManager.getQuestionAttemptsByUsersAndQuestionPrefix(user, questionPageId);

                                if (!questionAttempts.get(Long.parseLong(userId)).isEmpty()
                                        && questionAttempts.get(Long.parseLong(userId)).containsKey(questionId.split("\\|")[0])
                                        && questionAttempts.get(Long.parseLong(userId)).get(questionId.split("\\|")[0]).containsKey(questionId)) {

                                    for (QuestionValidationResponse response: questionAttempts.get(Long.parseLong(userId))
                                            .get(questionId.split("\\|")[0])
                                            .get(questionId)
                                            ) {

                                        Calendar current = Calendar.getInstance();
                                        current.setTimeInMillis(latestEventTimestamp);
                                        current.set(Calendar.MILLISECOND, 0);
                                        if (response.isCorrect()
                                                && response.getDateAttempted().getTime() != current.getTimeInMillis()
                                                && response.getDateAttempted().getTime() < current.getTimeInMillis()) {
                                            return streakRecord;
                                        }
                                    }

                                }

                            } catch (SegueDatabaseException e) {
                                e.printStackTrace();
                            }

                            Calendar streakStart = Calendar.getInstance();
                            streakStart.setTimeInMillis(streakStartTimestamp);

                            // if we have just started counting, or if the latest event arrived later than a day then reset
                            if (streakStartTimestamp == 0 ||
                                    (((streakEndTimestamp != 0 && TimeUnit.MINUTES.convert(latest.getTimeInMillis() - previous.getTimeInMillis(), TimeUnit.MILLISECONDS) > 2)))) {

                                ((ObjectNode) streakRecord).put("streak_start", latestEventTimestamp);
                                streakStart.setTimeInMillis(latestEventTimestamp);
                            }

                            ((ObjectNode) streakRecord).put("streak_end", latestEventTimestamp);

                            streakStart = roundDownToDay(streakStart);

                            Long daysSinceStart = TimeUnit.MINUTES.convert(latest.getTimeInMillis() - streakStart.getTimeInMillis(), TimeUnit.MILLISECONDS);
                            ((ObjectNode) streakRecord).put("current_streak", daysSinceStart);
                            if (daysSinceStart > largestStreak) {
                                ((ObjectNode) streakRecord).put("largest_streak", daysSinceStart);
                            }

                            IUserAlert alert = new PgUserAlert(null,
                                    Long.parseLong(userId),
                                    "streak:" + daysSinceStart,
                                    "/streaks",
                                    new Timestamp(System.currentTimeMillis()),
                                    null, null, null);

                            if (null != UserAlertsWebSocket.connectedSockets && UserAlertsWebSocket.connectedSockets.containsKey(Long.parseLong(userId))) {
                                for(IAlertListener listener : UserAlertsWebSocket.connectedSockets.get(Long.parseLong(userId))) {
                                    listener.notifyAlert(alert);
                                }
                            }

                            return streakRecord;
                        },
                        JsonSerde,
                        "localstore_user_daily_streaks"
                );

    }


    /**
     * Method to obtain the current user snapshot of activity stats
     *
     * @param userId id of the user
     * @return number of consecutive days a user has answered a question correctly
     */
    public Map<String, Object> getUserSnapshot(String userId) {

        Map<String, Object> userSnapshot = Maps.newHashMap();

        userSnapshot.put("dailyStreakRecord", 0);

        JsonNode streakRecord = streams
                .store("globalstore_user_daily_streaks", QueryableStoreTypes.<String, JsonNode>keyValueStore())
                .get(userId);

        Calendar tomorrowMidnight = roundDownToDay(Calendar.getInstance());
        tomorrowMidnight.add(Calendar.DAY_OF_YEAR, 1);

        if (streakRecord != null) {
            if (tomorrowMidnight.getTimeInMillis() - streakRecord.path("streak_end").asLong() > (1000 * 60 * 60 * 24)) {
                userSnapshot.put("dailyStreakRecord", streakRecord.path("current_streak").asInt());
            }
        }

        return userSnapshot;
    }


    /**
     * Function to take a calendar object and round down the timestamp to midnight
     *
     * @param calendar calendar object to modify
     * @return modified calendar object set to midnight
     */
    private static Calendar roundDownToDay(Calendar calendar) {

        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR, 0);

        return calendar;
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
