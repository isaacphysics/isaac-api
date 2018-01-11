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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.userAlerts.IAlertListener;
import uk.ac.cam.cl.dtg.segue.api.userAlerts.UserAlertsWebSocket;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.IUserAlert;
import uk.ac.cam.cl.dtg.segue.dos.PgUserAlert;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.quiz.IQuestionAttemptManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static uk.ac.cam.cl.dtg.segue.api.Constants.LONGEST_STREAK_REACHED;

/**
 * Concrete Kafka streams processing application for generating user statistics
 *  @author Dan Underwood
 */
public class UserStatisticsStreamsApplication {

    private static final Logger log = LoggerFactory.getLogger(UserStatisticsStreamsApplication.class);
    private static final Logger kafkaLog = LoggerFactory.getLogger("kafkaStreamsLogger");

    private static final Serializer<JsonNode> JsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> JsonDeserializer = new JsonDeserializer();
    private static final Serde<String> StringSerde = Serdes.String();
    private static final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(JsonSerializer, JsonDeserializer);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private KafkaTopicManager kafkaTopicManager;
    private KafkaStreams streams;
    private KStreamBuilder builder = new KStreamBuilder();
    private Properties streamsConfiguration = new Properties();
    private IQuestionAttemptManager questionAttemptManager;
    private UserAccountManager userAccountManager;
    private ILogManager logManager;
    private String kafkaTopic;


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
                                            final IQuestionAttemptManager questionAttemptManager,
                                            final UserAccountManager userAccountManager,
                                            final ILogManager logManager) {


        this.kafkaTopicManager = kafkaTopicManager;
        this.questionAttemptManager = questionAttemptManager;
        this.userAccountManager = userAccountManager;
        this.logManager = logManager;
        this.kafkaTopic = globalProperties.getProperty("KAFKA_TOPIC_LOGGED_EVENTS");


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
        kafkaTopicManager.ensureTopicExists(kafkaTopic, loggedEventsConfigs);

        // local store changelog topics
        List<ConfigEntry> changelogConfigs = Lists.newLinkedList();
        changelogConfigs.add(new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT));
        kafkaTopicManager.ensureTopicExists(streamsAppNameAndVersion + "-localstore_user_snapshot-changelog", changelogConfigs);

        final AtomicLong lastLagLog = new AtomicLong(0);
        final AtomicBoolean wasLagging = new AtomicBoolean(true);

        // raw logged events incoming data stream from kafka
        KStream<String, JsonNode> rawLoggedEvents = builder.stream(StringSerde, JsonSerde, kafkaTopic)
                .filterNot(
                        (k, v) -> v.path("anonymous_user").asBoolean()
                ).peek(
                        (k,v) -> {
                            long lag = System.currentTimeMillis() - v.get("timestamp").asLong();
                            if (lag > 1000) {

                                if (System.currentTimeMillis() - lastLagLog.get() > 10000) {
                                    kafkaLog.info(String.format("User statistics stream lag: %.02f hours (%.03f s).", lag / 3600000.0, lag / 1000.0));
                                    lastLagLog.set(System.currentTimeMillis());
                                }

                            } else if (wasLagging.get()) {
                                wasLagging.set(false);
                                kafkaLog.info("Site statistics stream processing caught up.");
                            }
                        }
                );

        // process raw logged events
        streamProcess(rawLoggedEvents);

        // need to make state stores queryable globally, as we often have 2 versions of API running concurrently, hence 2 streams app instances
        // aggregations are saved to a local state store per streams app instance and update a changelog topic in Kafka
        // we can use this changelog to populate a global state store for all streams app instances
        builder.globalTable(StringSerde, JsonSerde,streamsAppNameAndVersion + "-localstore_user_snapshot-changelog", "globalstore_user_snapshot");

        // use the builder and the streams configuration we set to setup and start a streams object
        streams = new KafkaStreams(builder, streamsConfiguration);
        streams.start();

        // return when streams instance is initialized
        while (true) {

            if (streams.state().isCreatedOrRunning())
                break;
        }

        kafkaLog.info("User statistics streams application started.");

    }

    /**
     * This method contains the logic that transforms the incoming stream
     *
     * @param rawStream
     *          - the input stream
     */
    private void streamProcess(KStream<String, JsonNode> rawStream) {


        // user question answer streaks
        rawStream.groupByKey()
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode userSnapshot = JsonNodeFactory.instance.objectNode();

                            ObjectNode streakRecord = JsonNodeFactory.instance.objectNode();
                            streakRecord.put("streak_start", 0);
                            streakRecord.put("streak_end", 0);
                            streakRecord.put("largest_streak", 0);
                            streakRecord.put("current_activity", 0);
                            streakRecord.put("activity_threshold", 3);

                            userSnapshot.put("streak_record", streakRecord);

                            return userSnapshot;
                        },
                        // aggregator
                        (userId, latestEvent, userSnapshot) -> {

                            // first, make sure that if the user is not a student, we augment their snapshot with a teacher sub-record
                            if (!userSnapshot.has("teacher_record")) {
                                ObjectNode teacherRecord = JsonNodeFactory.instance.objectNode();
                                teacherRecord.put("groups_created", 0);
                                teacherRecord.put("boards_created", 0);
                                teacherRecord.put("assignments_set", 0);
                                teacherRecord.put("book_pages_set", 0);
                                teacherRecord.put("cpd_events_attended", 0);
                                ((ObjectNode) userSnapshot).put("teacher_record", teacherRecord);
                            }


                            // snapshot updates pertaining to question answer activity
                            if (latestEvent.path("event_type").asText().equals("ANSWER_QUESTION")) {

                                if (latestEvent.path("event_details").path("correct").asBoolean()) {
                                    ((ObjectNode) userSnapshot).put("streak_record", updateStreakRecord(userId, latestEvent, userSnapshot.path("streak_record")));
                                }
                            }

                            // snapshot updates pertaining to teacher activity
                            try {

                                if (!userAccountManager.getUserDTOById(Long.parseLong(userId)).getRole().equals(Role.STUDENT)) {

                                    if (latestEvent.path("event_type").asText().equals("CREATE_USER_GROUP")) {
                                        ((ObjectNode) userSnapshot).put("teacher_record", updateTeacherActivityRecord("groups_created", userSnapshot.path("teacher_record")));
                                    }

                                    if (latestEvent.path("event_type").asText().equals("SET_NEW_ASSIGNMENT")) {
                                        ((ObjectNode) userSnapshot).put("teacher_record", updateTeacherActivityRecord("assignments_set", userSnapshot.path("teacher_record")));
                                    }

                                }

                            } catch (NoUserException | SegueDatabaseException e) {
                                e.printStackTrace();
                            }


                            return userSnapshot;
                        },
                        JsonSerde,
                        "localstore_user_snapshot"
                );

    }




    /**
     * Method to obtain the current user snapshot of activity stats
     *
     * @param user the user we are interested in
     * @return number of consecutive days a user has answered a question correctly
     */
    public Map<String, Object> getUserSnapshot(RegisteredUserDTO user) {

        Map<String, Object> userSnapshot = Maps.newHashMap();
        Map<String, Object> streakRecord = Maps.newHashMap();
        streakRecord.put("currentStreak", 0);
        streakRecord.put("largestStreak", 0);
        streakRecord.put("currentActivity", 0);

        // get the persisted snapshot document
        JsonNode snapshotRecord = streams
                .store("globalstore_user_snapshot", QueryableStoreTypes.<String, JsonNode>keyValueStore())
                .get(String.valueOf(user.getId()));


        if (snapshotRecord != null) {

            // get daily streak information
            JsonNode streakNode = snapshotRecord.path("streak_record");

            Long streakEndTimestamp = streakNode.path("streak_end").asLong();

            if (streakEndTimestamp != 0) {

                // set up a streak increase deadline at midnight for the next day
                Calendar tomorrowMidnight = roundDownToDay(Calendar.getInstance());
                tomorrowMidnight.add(Calendar.DAY_OF_YEAR, 1);

                Long daysSinceStreakIncrease = TimeUnit.DAYS.convert(tomorrowMidnight.getTimeInMillis() - streakEndTimestamp, TimeUnit.MILLISECONDS);

                if (daysSinceStreakIncrease == 0) {

                    streakRecord.put("currentActivity", snapshotRecord.path("streak_record").path("current_activity").asInt());
                    streakRecord.put("currentStreak", TimeUnit.DAYS.convert(streakEndTimestamp - snapshotRecord.path("streak_record").path("streak_start").asLong(),
                            TimeUnit.MILLISECONDS));


                } else if (daysSinceStreakIncrease == 1) {

                    Integer curActivity = snapshotRecord.path("streak_record").path("current_activity").asInt();

                    if (curActivity != snapshotRecord.path("streak_record").path("activity_threshold").asInt()) {
                        streakRecord.put("currentActivity", snapshotRecord.path("streak_record").path("current_activity").asInt());
                    }

                    streakRecord.put("currentStreak", TimeUnit.DAYS.convert(streakEndTimestamp - snapshotRecord.path("streak_record").path("streak_start").asLong(),
                            TimeUnit.MILLISECONDS));

                    streakRecord.put("dailyStreakMessage", "Complete your daily question goal by the end of today to increase your streak!");
                }

                streakRecord.put("largestStreak", snapshotRecord.path("streak_record").path("largest_streak").asLong());
                userSnapshot.put("streakRecord", streakRecord);
            }

            // for all users who aren't students, augment the snapshot with additional teacher stats
            if (!user.getRole().equals(Role.STUDENT) && snapshotRecord.has("teacher_record")) {
                userSnapshot.put("teacherActivityRecord", snapshotRecord.path("teacher_record"));
            }
        }

        return userSnapshot;
    }



    /**
     *
     * @param activityType
     * @param teacherRecord
     * @return
     */
    private JsonNode updateTeacherActivityRecord(String activityType, JsonNode teacherRecord) {

        Integer count = teacherRecord.path(activityType).asInt();
        ((ObjectNode) teacherRecord).put(activityType, count + 1);

        return teacherRecord;
    }






    /**
     *We call this method to update the streak data for the user snapshot record
     *
     * @param userId id of the user we want to update
     * @param latestEvent json object describing the event which triggers the streak update
     * @param streakRecord the current snapshot of the streak record
     * @return the new updated streak record
     */
    private JsonNode updateStreakRecord(String userId, JsonNode latestEvent, JsonNode streakRecord) {

        // timestamp of streak start
        Long streakStartTimestamp = streakRecord.path("streak_start").asLong();

        // timestamp of streak end
        Long streakEndTimestamp = streakRecord.path("streak_end").asLong();

        // timestamp of latest event
        Long latestEventTimestamp = latestEvent.path("timestamp").asLong();

        // set up calendar objects for date comparisons
        Calendar latest = Calendar.getInstance();
        latest.setTimeInMillis(latestEventTimestamp);
        latest = roundDownToDay(latest);


        // 1) If the current activity threshold for the day has been reached
        if (streakRecord.path("current_activity").asLong() == streakRecord.path("activity_threshold").asLong()) {

            // if we are still on the same day, don't continue, otherwise we need to reset the daily activity count
            if (latest.getTimeInMillis() < streakEndTimestamp) {
                return streakRecord;
            } else {
                ((ObjectNode) streakRecord).put("current_activity", 0);
            }

        }


        // 2) We want to make sure the user hasn't answered the question part correctly before. If they have, don't continue
        String questionId = latestEvent.path("event_details").path("questionId").asText();
        List<Long> user = Lists.newArrayList();
        List<String> questionPageId = Lists.newArrayList();
        user.add(Long.parseLong(userId));
        questionPageId.add(questionId.split("\\|")[0]);

        try {
            Map<Long, Map<String, Map<String, List<QuestionValidationResponse>>>> questionAttempts =
                    questionAttemptManager.getQuestionAttemptsByUsersAndQuestionPrefix(user, questionPageId);

            // the following assumes that question attempts are always recorded in the question attempt table before they are logged as an event
            if (!questionAttempts.get(Long.parseLong(userId)).isEmpty()
                    && questionAttempts.get(Long.parseLong(userId)).containsKey(questionId.split("\\|")[0])
                    && questionAttempts.get(Long.parseLong(userId)).get(questionId.split("\\|")[0]).containsKey(questionId)) {

                Integer correctCount = 0;
                for (QuestionValidationResponse response: questionAttempts.get(Long.parseLong(userId))
                        .get(questionId.split("\\|")[0])
                        .get(questionId)
                        ) {

                    // count all the times the user has correctly attempted the question
                    if (response.isCorrect()) {
                        correctCount++;
                    }
                }

                // if the correct count is 1 then the recorded attempt corresponds to the current event
                // otherwise the user has attempted it correctly before, therefore we don't continue
                if (correctCount > 1) {
                    return streakRecord;
                }
            }

        } catch (SegueDatabaseException e) {
            e.printStackTrace();
        }


        // 3) If we have just started counting, or if the latest event arrived later than a day since the previous, reset the streak record and go no further
        if (streakStartTimestamp == 0 ||
                (((streakEndTimestamp != 0 && TimeUnit.DAYS.convert(latest.getTimeInMillis() - streakEndTimestamp, TimeUnit.MILLISECONDS) >= 1)))) {

            ((ObjectNode) streakRecord).put("streak_start", latest.getTimeInMillis());
            ((ObjectNode) streakRecord).put("streak_end", latest.getTimeInMillis());
            ((ObjectNode) streakRecord).put("current_activity", 0);
            streakStartTimestamp = latest.getTimeInMillis();
        }


        // 4) Increment the daily activity. If we've reached the daily activity threshold, increment the latest event time to the end of the day
        Long currentActivity = streakRecord.path("current_activity").asLong();
        ((ObjectNode) streakRecord).put("current_activity", currentActivity + 1);

        if (streakRecord.path("current_activity").asLong() == streakRecord.path("activity_threshold").asLong()) {
            latest.add(Calendar.DAY_OF_YEAR, 1);
            ((ObjectNode) streakRecord).put("streak_end", latest.getTimeInMillis());
        }


        // 5) Update largest streak count if days since start is greater than the recorded largest streak
        Long daysSinceStart = TimeUnit.DAYS.convert(latest.getTimeInMillis() - streakStartTimestamp, TimeUnit.MILLISECONDS);

        if (daysSinceStart > streakRecord.path("largest_streak").asLong()) {
            ((ObjectNode) streakRecord).put("largest_streak", daysSinceStart);

            // log the new longest streak record
            Map<String, Object> eventDetails = Maps.newHashMap();
            eventDetails.put("longestStreak", streakRecord.path("largest_streak").asLong());
            eventDetails.put("threshold", streakRecord.path("activity_threshold").asLong());
            eventDetails.put("streakType", "correctQuestionPartsPerDay");

            try {
                logManager.logInternalEvent(userAccountManager.getUserDTOById(Long.parseLong(userId)), LONGEST_STREAK_REACHED, eventDetails);
            } catch (NoUserException | SegueDatabaseException e) {
                e.printStackTrace();
            }
        }


        // 6) At this point we want to notify the user that their streak has increased
        Map<String, Object> notificationData = Maps.newHashMap();
        Map<String, Object> streakData = Maps.newHashMap();
        streakData.put("currentStreak", daysSinceStart);
        streakData.put("currentActivity", streakRecord.path("current_activity").asInt());

        if (streakRecord.path("current_activity").asLong() == streakRecord.path("activity_threshold").asLong()) {
            streakData.put("streakIncrease", true);
        }

        notificationData.put("streakData", streakData);

        try {
            // notify the user of a streak increase
            IUserAlert alert = new PgUserAlert(null,
                    Long.parseLong(userId),
                    objectMapper.writeValueAsString(notificationData),
                    "progress",
                    new Timestamp(System.currentTimeMillis()),
                    null, null, null);

            if (null != UserAlertsWebSocket.connectedSockets && UserAlertsWebSocket.connectedSockets.containsKey(Long.parseLong(userId))) {
                for(IAlertListener listener : UserAlertsWebSocket.connectedSockets.get(Long.parseLong(userId))) {
                    listener.notifyAlert(alert);
                }
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return streakRecord;
    }



    /**
     * Function to take a calendar object and round down the timestamp to midnight
     * for the same date
     *
     * @param calendar calendar object to modify
     * @return modified calendar object set to midnight
     */
    private static Calendar roundDownToDay(Calendar calendar) {

        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);

        return calendar;
    }

}
