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

package uk.ac.cam.cl.dtg.segue.dao.streams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import uk.ac.cam.cl.dtg.segue.dao.streams.customAggregators.QuestionAnswerCounter;
import uk.ac.cam.cl.dtg.segue.dao.streams.customAggregators.QuestionAnswerInitializer;
import uk.ac.cam.cl.dtg.segue.dao.streams.customProcessors.ThresholdAchievedProcessor;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.io.IOException;
import java.util.Properties;

/**
 *  Kafka streams processing for Isaac user achievements
 *  @author Dan Underwood
 */
public class LoggedEventsListener implements ServletContextListener {

    private KafkaStreams streams;
    private PropertiesLoader globalProperties;
    private final PostgresSqlDb database;

    final static Serializer<JsonNode> jsonSerializer = new JsonSerializer();
    final static Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
    final static Serde<JsonNode> jsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);
    final static Serde<String> stringSerde = Serdes.String();
    static ThresholdAchievedProcessor achievementProcessor;


    @Inject
    public LoggedEventsListener(PostgresSqlDb database) {

        this.database = database;

        if (globalProperties == null) {
            try {
                globalProperties = new PropertiesLoader(System.getProperty("config.location"));
            } catch (IOException e) {
                //log.error("Error loading properties file.", e);
            }
        }
    }


    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {

        KStreamBuilder builder = new KStreamBuilder();
        Properties streamsConfiguration = new Properties();

        // set up kafka streaming variables
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, globalProperties.getProperty("KAFKA_STREAMS_APPNAME"));
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                globalProperties.getProperty("KAFKA_HOSTNAME") + ":" + globalProperties.getProperty("KAFKA_PORT"));

        streamsConfiguration.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        // in order to keep this example interactive.
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        // For illustrative purposes we disable record caches
        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");


        // raw logged_events incoming data stream from kafka
        KStream<String, JsonNode> rawLoggedEvents = builder.stream(stringSerde, jsonSerde, globalProperties.getProperty("RAW_LOG_STREAM"))
                .filter(
                        (k, v) -> !v.path("anonymous_user").asBoolean()
                );


        // stream of question part attempts
        // filter logged events by "ANSWER_QUESTION" event type (this is a log of *part* attempts, not *whole question* attempts)
        KStream<String, JsonNode> questionPartAttempts = DerivedStreams.FilterByEventType(rawLoggedEvents, "ANSWER_QUESTION");

        // Publish the derived "question part attempt" stream to a kafka topic
        questionPartAttempts.to(stringSerde, jsonSerde, globalProperties.getProperty("QPART_ATTEMPT_STREAMS"));


        /* USER WEEKLY FREQUENCY COUNTING */
        //KStream<String, Long> userWeeklyStreaks = DerivedStreams.UserWeeklyStreaks(questionPartAttempts);

        /* CORRECT QUESTION COUNTING */
        // simple derived stream of correct question part attempts
        // filter question part attempts by "correct" flag
        KStream <String, JsonNode> questionAttempts = DerivedStreams.UserQuestionAttemptsComplete(questionPartAttempts);

        // This is the "true" correct question attempt stream, which takes into account all parts of a question
        KStream<String, JsonNode> completedQuestions = questionAttempts.filter(
                (userId, questionAttempt) -> questionAttempt.path("correct")
                        .asBoolean()
        );

        // Publish the derived "completed question" stream to a kafka topic
        completedQuestions.to(stringSerde, jsonSerde, globalProperties.getProperty("COMPLETE_QUESTION_STREAM"));

        // total correct attempts counter (user id, number of correct answers)
        KStream<String, JsonNode> correctTotalCountStream = completedQuestions
                .groupByKey(stringSerde, jsonSerde)
                .aggregate(
                        // initializer
                        new QuestionAnswerInitializer("QUESTIONS_ANSWERED_TOTAL"),
                        // aggregator
                        new QuestionAnswerCounter(),
                        jsonSerde,
                        "userCorrectTotals"
                )
                .toStream();

        correctTotalCountStream.print();


        // branch main stream into separate level streams
        KStream<String, JsonNode>[] correctLevelCountStreams =
                new KStream[globalProperties.getProperty("QUESTION_LEVELS").split(",").length];

        for (int i = 0; i < correctLevelCountStreams.length; i++) {
            final int level = i + 1;

            correctLevelCountStreams[i] = completedQuestions
                    .filter(
                            (k, v) -> v.path("level").asInt() == level
                    ).groupByKey(stringSerde, jsonSerde)
                    .aggregate(
                            // initializer
                            new QuestionAnswerInitializer("QUESTIONS_ANSWERED_LEVEL_" + level),
                            // aggregator
                            new QuestionAnswerCounter(),
                            jsonSerde,
                            "userCorrectLevel" + level
                    )
                    .toStream();
        }

        /** branch main stream into separate tag streams
         * badges for tags need to be decided beforehand it seems...
         */
        /*String[] badgeTags = {"mechanics", "waves", "fields", "circuits"};
        KStream<String, JsonNode>[] correctTagCountStreams = new KStream[badgeTags.length];

        for (int i = 0; i < badgeTags.length; i++) {
            final int tag = i;

            correctTagCountStreams[i] = completedQuestions
                    .filter(
                            (k, v) -> v.path("tags").has(badgeTags[tag])
                    ).groupByKey(stringSerde, jsonSerde)
                    .aggregate(
                        // initializer
                        new QuestionAnswerInitializer("QUESTIONS_ANSWERED_TAG_" + badgeTags[tag]),
                        // aggregator
                        new QuestionAnswerCounter(),
                        jsonSerde,
                        "userCorrectTag" + badgeTags[tag]
                    )
                    .toStream();
        }*/




        /** here is where we work out whether users have met certain thresholds for unlocking achievements.
         *  first define achievement thresholds, then filter all derived streams based on these and trigger the "unlocking process" to
         *  record the achievement for the user
         */
        achievementProcessor = new ThresholdAchievedProcessor(database);

        // thresholds for achievement unlocking
        String[] strThresholds = globalProperties.getProperty("ACHIEVEMENT_THRESHOLDS").split(",");
        Integer[] thresholds = new Integer[strThresholds.length];

        for (int i = 0; i < thresholds.length; i++) {
            thresholds[i] = Integer.parseInt(strThresholds[i]);
        }

        // trigger process for records that match threshold (total questions answered)
        for (Integer threshold: thresholds
                ) {

            ProcessAchievement(correctTotalCountStream, threshold, globalProperties.getProperty("USER_ACHIEVEMENTS_STREAM"));

            // trigger process for records that match threshold (per level)
            for (KStream<String, JsonNode> correctLevelCountStream: correctLevelCountStreams
                    ) {
                ProcessAchievement(correctLevelCountStream, threshold, globalProperties.getProperty("USER_ACHIEVEMENTS_STREAM"));
            }
        }


        //use the builder and the streamsConfiguration we set to setup and start a streams object
        streams = new KafkaStreams(builder, streamsConfiguration);
        streams.cleanUp();
        streams.start();

    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {

        //shutdown on an interrupt
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

    }


    public void ProcessAchievement(KStream<String, JsonNode> stream, Integer threshold, String downwardStream) {

        KStream<String, JsonNode> filteredStream = stream.filter(
                (k, v) -> v.path("count").asInt() == threshold
        );

        filteredStream.to(stringSerde, jsonSerde, downwardStream);

        filteredStream.process(
                () -> achievementProcessor
        );

        // notification stream (to be refactored out at some point)
        filteredStream.mapValues(
                (achievementRecord) -> {

                    ObjectNode userNotificationRecord = JsonNodeFactory.instance.objectNode();

                    userNotificationRecord.put("notificationId", "achievementUnlocked");
                    userNotificationRecord.put("userId", achievementRecord.path("userId"));
                    userNotificationRecord.put("title", "Achievement Unlocked!");
                    userNotificationRecord.put("message", "You have unlocked an achievement!");
                    userNotificationRecord.put("timestamp", achievementRecord.path("latestAttempt"));

                    return (JsonNode)userNotificationRecord;
                }
        ).to(stringSerde, jsonSerde, globalProperties.getProperty("USER_NOTIFICATIONS_STREAM"));
    }
}
