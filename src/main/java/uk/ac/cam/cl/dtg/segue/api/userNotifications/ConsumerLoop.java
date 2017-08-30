package uk.ac.cam.cl.dtg.segue.api.userNotifications;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.AuthorisationFacade;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by du220 on 21/06/2017.
 */
public class ConsumerLoop implements Runnable {

    private final KafkaConsumer<String, JsonNode> consumer;
    private final Collection<String> topics;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean shutdown;
    private final CountDownLatch shutdownLatch;

    private final Session session;
    private final String userId;

    private final Logger log = LoggerFactory.getLogger(ConsumerLoop.class);


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
        this.shutdown = new AtomicBoolean(false);
        this.shutdownLatch = new CountDownLatch(1);

        this.topics.add(topic);
        this.session = session;
        this.userId = userId;

    }

    @Override
    public void run() {

        try {
            consumer.subscribe(topics);

            while (!shutdown.get()) {

                ConsumerRecords<String, JsonNode> records = consumer.poll(1000);
                for (ConsumerRecord<String, JsonNode> record : records) {

                    if (record.key().matches(userId)) {

                        ArrayList<JsonNode> notificationList = Lists.newArrayList();
                        Map<String, ArrayList<JsonNode>> notifications = Maps.newHashMap();

                        if (!record.value().path("status").asText().equals("RECEIVED")) {
                            notificationList.add(record.value());
                            notifications.put("notifications", notificationList);
                            session.getRemote().sendString(objectMapper.writeValueAsString(notifications));
                        }

                    }
                }
            }
        } catch (IOException e) {
            log.error("IO Exception", e);
        } finally {
            consumer.close();
            shutdownLatch.countDown();

        }
    }

    public void shutdown() {

        try {
            shutdown.set(true);
            shutdownLatch.await();
        } catch (InterruptedException e) {
            log.error(e.getMessage());
        }
    }

}

