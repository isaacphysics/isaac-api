package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by du220 on 11/10/2017.
 */
public class KafkaLoggingManagerTest {

    @Test
    public void consumer_Test() {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "tsdrfdffsest");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 100);

        KafkaConsumer<String, String> loggedEventsConsumer = new KafkaConsumer<String, String>(props);
        ArrayList<String> topics = Lists.newArrayList();
        topics.add("topic_logged_events_test");

        try {
            loggedEventsConsumer.subscribe(topics);

            Boolean running = true;

            while (running) {

                ConsumerRecords<String, String> records = loggedEventsConsumer.poll(1000);
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(record);
                }
                running = false;
            }

        } catch (KafkaException e) {
            e.printStackTrace();
        } finally {
            loggedEventsConsumer.close();
        }

    }

}
