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
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
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
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.KafkaTopicManager;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.customProcessors.ThresholdAchievedProcessor;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.derivedStreams.TeacherAssignmentActivity;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.derivedStreams.UserQuestionAttempts;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.derivedStreams.UserWeeklyStreaks;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.Properties;

/**
 * Concrete Kafka streams processing application for generating site statistics
 *  @author Dan Underwood
 */
public class UserAchievementStreamsApplication {

    private KafkaTopicManager kafkaTopicManager;
    private KafkaStreams streams;
    private PostgresSqlDb database;
    private IContentManager contentManager;
    private GameManager gameManager;
    private String contentIndex;
    private static final Serializer<JsonNode> JsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> JsonDeserializer = new JsonDeserializer();
    private static final Serde<String> StringSerde = Serdes.String();
    private static final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(JsonSerializer, JsonDeserializer);

    protected KStreamBuilder builder = new KStreamBuilder();
    protected Properties streamsConfiguration = new Properties();


    /**
     * Constructor
     * @param globalProperties
     *              - properties object containing global variables
     * @param kafkaTopicManager
     *              - manager for kafka topic administration
     */
    public UserAchievementStreamsApplication(final PropertiesLoader globalProperties,
                                             final KafkaTopicManager kafkaTopicManager,
                                             final PostgresSqlDb database,
                                             final IContentManager contentManager,
                                             final String contentIndex,
                                             final GameManager gameManager) {

        this.kafkaTopicManager = kafkaTopicManager;
        this.database = database;
        this.contentManager = contentManager;
        this.gameManager = gameManager;
        this.contentIndex = contentIndex;


        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "streamsapp_user-achievements-v-"
                + globalProperties.getProperty("SITE_STATS_STREAMS_APP_VERSION"));
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                globalProperties.getProperty("KAFKA_HOSTNAME") + ":" + globalProperties.getProperty("KAFKA_PORT"));
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, globalProperties.getProperty("KAFKA_STREAMS_STATE_DIR"));
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        streamsConfiguration.put(StreamsConfig.METADATA_MAX_AGE_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        streamsConfiguration.put(StreamsConfig.consumerPrefix(ConsumerConfig.METADATA_MAX_AGE_CONFIG), 60 * 1000);
        streamsConfiguration.put(StreamsConfig.producerPrefix(ProducerConfig.METADATA_MAX_AGE_CONFIG), 60 * 1000);

    }


    /**
     * Method to be called to start the streams application
     */
    public void start() {

        // ensure topics exist before attempting to consume
        kafkaTopicManager.ensureTopicExists("topic_logged_events");
        kafkaTopicManager.ensureTopicExists("topic_anonymous_logged_events");

        // raw logged events incoming data stream from kafka
        KStream<String, JsonNode>[] rawLoggedEvents = builder.stream(StringSerde, JsonSerde, "topic_logged_events")
                .branch(
                        (k, v) -> !v.path("anonymous_user").asBoolean(),
                        (k, v) -> v.path("anonymous_user").asBoolean()
                );

        // parallel log for anonymous events (may want to optimise how we do this later)
        rawLoggedEvents[1].to(StringSerde, JsonSerde, "topic_anonymous_logged_events");

        streamProcess(rawLoggedEvents[0]);

        // use the builder and the streams configuration we set to setup and start a streams object
        streams = new KafkaStreams(builder, streamsConfiguration);
        streams.start();

        // return when streams instance is initialized
        while (true) {

            if (streams.state().isRunning())
                break;
        }

    }

    /**
     * This method contains the logic that transforms the incoming stream
     * We keep this public to make it easy to unit test
     *
     * @param rawStream
     *          - the input stream
     */
    public void streamProcess(final KStream<String, JsonNode> rawStream) {

        // user question attempts
        ThresholdAchievedProcessor achievementProcessor = new ThresholdAchievedProcessor(database);
        UserQuestionAttempts.process(rawStream, achievementProcessor, contentManager, contentIndex, gameManager);

        // user activity streaks
        UserWeeklyStreaks.process(rawStream.filter(
                (userId, event) -> event.path("event_type").equals("ANSWER_QUESTION") || event.path("event_type").equals("SET_NEW_ASSIGNMENT")
        ), achievementProcessor);

        // teacher assignment activity
        TeacherAssignmentActivity.process(rawStream, gameManager, achievementProcessor);

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
