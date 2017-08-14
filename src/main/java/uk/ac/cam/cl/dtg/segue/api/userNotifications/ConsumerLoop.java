package uk.ac.cam.cl.dtg.segue.api.userNotifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.eclipse.jetty.websocket.api.Session;

import java.io.IOException;
import java.util.*;

/**
 * Created by du220 on 21/06/2017.
 */
public class ConsumerLoop implements Runnable {

    private final KafkaConsumer<String, JsonNode> consumer;
    private final Collection<String> topics;
    private final ObjectMapper objectMapper;
    private final Session session;
    private final String userId;

    public ConsumerLoop(final Session session, final String userId,
                        final String topic, final ObjectMapper objectMapper) {

        Properties props = new Properties();

        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.deserializer", StringDeserializer.class.getName());
        props.put("value.deserializer", JsonDeserializer.class.getName());
        props.put("enable.auto.commit", "false");
        props.put("group.id", userId);


        this.objectMapper = objectMapper;
        this.consumer = new KafkaConsumer<String, JsonNode>(props);
        this.topics = new ArrayList<String>();
        this.topics.add(topic);
        this.session = session;
        this.userId = userId;

    }

    @Override
    public void run() {

        try {
            consumer.subscribe(topics);

            while (true) {

                ConsumerRecords<String, JsonNode> records = consumer.poll(1000);
                for (ConsumerRecord<String, JsonNode> record : records) {

                    if (record.key().matches(userId)) {

                        System.out.println(record.value());
                        session.getRemote().sendString(objectMapper.writeValueAsString(record.value()));
                    }
                }
            }
        } catch (WakeupException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally
        {
            consumer.close();
        }
    }

    public void shutdown() {
        consumer.wakeup();
    }

}

