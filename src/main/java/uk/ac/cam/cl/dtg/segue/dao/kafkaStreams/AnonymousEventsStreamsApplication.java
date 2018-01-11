package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams;

import com.fasterxml.jackson.databind.JsonNode;
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
import org.apache.kafka.streams.kstream.KStreamBuilder;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.List;
import java.util.Properties;

/**
 * Simple Kafka Streams application for filtering anonymous logged events into their own Kafka topic
 *  @author Dan Underwood
 */
public class AnonymousEventsStreamsApplication {

    private static final Serializer<JsonNode> JsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> JsonDeserializer = new JsonDeserializer();
    private static final Serde<String> StringSerde = Serdes.String();
    private static final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(JsonSerializer, JsonDeserializer);


    public AnonymousEventsStreamsApplication(final PropertiesLoader globalProperties,
                                            final KafkaTopicManager kafkaTopicManager) {

        String kafkaTopic = globalProperties.getProperty("KAFKA_TOPIC_LOGGED_EVENTS");
        KStreamBuilder builder = new KStreamBuilder();
        Properties streamsConfiguration = new Properties();

        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "streamsapp_anonymous_logged_events-v1.0");
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,
                globalProperties.getProperty("KAFKA_HOSTNAME") + ":" + globalProperties.getProperty("KAFKA_PORT"));
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, globalProperties.getProperty("KAFKA_STREAMS_STATE_DIR"));
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        streamsConfiguration.put(StreamsConfig.METADATA_MAX_AGE_CONFIG, 10 * 1000);
        streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        streamsConfiguration.put(StreamsConfig.consumerPrefix(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG), "latest");
        streamsConfiguration.put(StreamsConfig.consumerPrefix(ConsumerConfig.METADATA_MAX_AGE_CONFIG), 60 * 1000);
        streamsConfiguration.put(StreamsConfig.producerPrefix(ProducerConfig.METADATA_MAX_AGE_CONFIG), 60 * 1000);

        // ensure topics exist before attempting to consume

        // logged events
        List<ConfigEntry> loggedEventsConfigs = Lists.newLinkedList();
        loggedEventsConfigs.add(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(-1)));
        kafkaTopicManager.ensureTopicExists(kafkaTopic, loggedEventsConfigs);

        // anonymous logged events
        List<ConfigEntry> anonLoggedEventsConfigs = Lists.newLinkedList();
        anonLoggedEventsConfigs.add(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7200000)));
        kafkaTopicManager.ensureTopicExists("topic_anonymous_logged_events", anonLoggedEventsConfigs);


        // raw logged events incoming data stream from kafka
        builder.stream(StringSerde, JsonSerde, kafkaTopic)
                .filter(
                        (k, v) -> v.path("anonymous_user").asBoolean()
                ).to(StringSerde, JsonSerde, "topic_anonymous_logged_events");

        // use the builder and the streams configuration we set to setup and start a streams object
        KafkaStreams streams = new KafkaStreams(builder, streamsConfiguration);
        streams.start();

    }

}
