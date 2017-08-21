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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.name.Named;
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
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.streams.customProcessors.ThresholdAchievedProcessor;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.Properties;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;

/** Kafka streams processing service for deriving data streams from Isaac logged events
 *
 *  @author Dan Underwood
 */
public class KafkaStreamsService {

    private KafkaStreams streams;
    private PropertiesLoader globalProperties;
    private final PostgresSqlDb database;
    private final IContentManager contentManager;
    private final String contentIndex;

    final static Serializer<JsonNode> jsonSerializer = new JsonSerializer();
    final static Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
    final static Serde<JsonNode> jsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);
    final static Serde<String> stringSerde = Serdes.String();
    static ThresholdAchievedProcessor achievementProcessor;



    /** Returns single instance of streams service to dependants.
     *
     * @return streams
     *          - the single streams instance
     */
    public KafkaStreams getStream() {
        return streams;
    }


    /**
     *
     * @param globalProperties
     * @param database
     * @param contentManager
     * @param contentIndex
     */
    @Inject
    public KafkaStreamsService(final PropertiesLoader globalProperties,
                               final PostgresSqlDb database,
                               final IContentManager contentManager,
                               @Named(CONTENT_INDEX) final String contentIndex)  {

        this.globalProperties = globalProperties;
        this.database = database;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;

        KStreamBuilder builder = new KStreamBuilder();
        Properties streamsConfiguration = new Properties();

        // kafka streaming config variables
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, globalProperties.getProperty("KAFKA_STREAMS_APPNAME"));
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                globalProperties.getProperty("KAFKA_HOSTNAME") + ":" + globalProperties.getProperty("KAFKA_PORT"));
        streamsConfiguration.put(StreamsConfig.KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        streamsConfiguration.put(StreamsConfig.METADATA_MAX_AGE_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.consumerPrefix(ConsumerConfig.METADATA_MAX_AGE_CONFIG), 60 * 1000);
        streamsConfiguration.put(StreamsConfig.producerPrefix(ProducerConfig.METADATA_MAX_AGE_CONFIG), 60 * 1000);


        // raw logged events incoming data stream from kafka
        KStream<String, JsonNode>[] rawLoggedEvents = builder.stream(stringSerde, jsonSerde, "topic_logged_events")
                .branch(
                        (k, v) -> !v.path("anonymous_user").asBoolean(),
                        (k, v) -> v.path("anonymous_user").asBoolean()
        );

        // parallel log for anonymous events (may want to optimise how we do this later)
        rawLoggedEvents[1].to(stringSerde, jsonSerde, "topic_anonymous_logged_events");


        // USER NOTIFICATIONS
        DerivedStreams.userNotifications(rawLoggedEvents[0]);


        //use the builder and the streams configuration we set to setup and start a streams object
        streams = new KafkaStreams(builder, streamsConfiguration);
        streams.cleanUp();
        streams.start();

        // return when streams instance is initialized
        while (true) {

            if (streams.state().isRunning())
                break;
        }

    }


}
