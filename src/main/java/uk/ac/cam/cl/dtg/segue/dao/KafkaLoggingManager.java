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
package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.Validate;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.KafkaTopicManager;
import uk.ac.cam.cl.dtg.segue.database.KafkaStreamsProducer;
import uk.ac.cam.cl.dtg.segue.dos.LogEvent;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;

import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.RequestIPExtractor;

import javax.servlet.http.HttpServletRequest;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;


/**
 * Kafka logging listener
 *
 * This class implements the logging event handler interface to listen to log events, and publishes them to a kafka topic
 */
public class KafkaLoggingManager extends LoggingEventHandler {
    private static final Logger log = LoggerFactory.getLogger(KafkaLoggingManager.class);

    private static final Set ignoredEvents = ImmutableSet.of(SEND_EMAIL, CONTACT_US_FORM_USED, EMAIL_VERIFICATION_REQUEST_RECEIVED,
            PASSWORD_RESET_REQUEST_RECEIVED, PASSWORD_RESET_REQUEST_SUCCESSFUL);

    private KafkaStreamsProducer kafkaProducer;
    private LocationManager locationManager;
    private final ObjectMapper objectMapper;
    private final String kafkaHost;
    private final String kafkaPort;


    @Inject
    public KafkaLoggingManager(final KafkaStreamsProducer kafkaProducer,
                               final LocationManager locationManager,
                               final ObjectMapper objectMapper,
                               @Named(Constants.KAFKA_HOSTNAME) final String kafkaHost,
                               @Named(Constants.KAFKA_PORT) final String kafkaPort,
                               KafkaTopicManager kafkaTopicManager) {

        this.kafkaProducer = kafkaProducer;
        this.locationManager = locationManager;
        this.objectMapper = objectMapper;
        this.kafkaHost = kafkaHost;
        this.kafkaPort = kafkaPort;

        // ensure topics exist before attempting to consume
        // logged events
        List<ConfigEntry> loggedEventsConfigs = Lists.newLinkedList();
        loggedEventsConfigs.add(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(-1)));
        kafkaTopicManager.ensureTopicExists("topic_logged_events", loggedEventsConfigs);

        // anonymous logged events
        List<ConfigEntry> anonLoggedEventsConfigs = Lists.newLinkedList();
        anonLoggedEventsConfigs.add(new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(7200000)));
        kafkaTopicManager.ensureTopicExists("topic_anonymous_logged_events", anonLoggedEventsConfigs);
    }


    public void handleEvent(AbstractSegueUserDTO user, final HttpServletRequest httpRequest, String eventType, Object eventDetails) {

        Validate.notNull(user);
        try {
            if (user instanceof RegisteredUserDTO) {
                this.publishLogEvent(((RegisteredUserDTO) user).getId().toString(), null, eventType, eventDetails,
                        (httpRequest != null) ? RequestIPExtractor.getClientIpAddr(httpRequest) : null);
            } else {
                this.publishLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType, eventDetails,
                        (httpRequest != null) ? RequestIPExtractor.getClientIpAddr(httpRequest) : null);
            }

        } catch (JsonProcessingException e) {
            log.error("Unable to serialize eventDetails as json string", e);
        } catch (SegueDatabaseException e) {
            log.error("Unable to save log event to the database", e);
        }

    }

    @Override
    public void transferLogEventsToRegisteredUser(final String oldUserId, final String newUserId) {

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,  kafkaHost + ":" + kafkaPort);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, oldUserId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        ArrayList<String> topics = Lists.newArrayList();
        topics.add("topic_anonymous_logged_events");

        log.debug(String.format("Kafka Test Transfer Log Events - OldUser (%s) NewUser (%s) - About to poll", oldUserId, newUserId));
        int totalRecordCount = 0;
        try (KafkaConsumer<String, JsonNode> loggedEventsConsumer = new KafkaConsumer<String, JsonNode>(props)) {
            loggedEventsConsumer.subscribe(topics);

            Boolean running = true;
            Long now = new Date().getTime();

            while (running) {
                ConsumerRecords<String, JsonNode> records = loggedEventsConsumer.poll(1000);
                totalRecordCount += records.count();

                if (records.count() == 0) {
                    running = false;
                    log.debug(String.format("Kafka Test Transfer Log Events - exiting after reaching end of topic. Read %s records", totalRecordCount));
                }

                for (ConsumerRecord<String, JsonNode> record : records) {
                    Long ts = record.value().path("timestamp").asLong();

                    if (record.value().path("user_id").asText().equals(oldUserId)) {

                        if (now < ts) {
                            running = false;
                            log.info(String.format("Kafka Test Transfer Log Events - exiting after reaching current time. Read %s records.", totalRecordCount));
                        }

                        Map<String, Object> kafkaLogRecord = new ImmutableMap.Builder<String, Object>()
                                .put("user_id", newUserId)
                                .put("anonymous_user", false)
                                .put("event_type", record.value().path("event_type").asText())
                                .put("event_details_type", record.value().path("event_details_type").asText())
                                .put("event_details", record.value().path("event_details"))
                                .put("ip_address", (record.value().path("ip_address").asText() != null) ? record.value().path("ip_address").asText() : "")
                                .put("timestamp", ts.toString())
                                .build();

                        // producerRecord contains the name of the kafka topic we are publishing to, followed by the message to be sent.
                        ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>("topic_logged_events", newUserId,
                                objectMapper.writeValueAsString(kafkaLogRecord));

                        kafkaProducer.send(producerRecord);
                    }
                }
            }

        } catch (KafkaException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            log.error("Could not process Json document: " + e);
        }

    }

    /**
     * log an event in the database.
     *
     * @param userId
     *            -
     * @param anonymousUserId
     *            -
     * @param eventType
     *            -
     * @param eventDetails
     *            -
     * @param ipAddress
     *            -
     * @throws JsonProcessingException
     *             - if we are unable to serialize the eventDetails as a string.
     * @throws SegueDatabaseException - if we cannot persist the event in the database.
     */
    private void publishLogEvent(final String userId, final String anonymousUserId, final String eventType,
                                 final Object eventDetails, final String ipAddress) throws JsonProcessingException, SegueDatabaseException {

        if (ignoredEvents.contains(eventType)) {
            return;
        }

        LogEvent logEvent = this.buildLogEvent(userId, anonymousUserId, eventType, eventDetails, ipAddress);

        Map<String, Object> kafkaLogRecord = new ImmutableMap.Builder<String, Object>()
                .put("user_id", logEvent.getUserId())
                .put("anonymous_user", logEvent.isAnonymousUser())
                .put("event_type", logEvent.getEventType())
                .put("event_details_type", logEvent.getEventDetailsType())
                .put("event_details", logEvent.getEventDetails())
                .put("ip_address", (logEvent.getIpAddress() != null) ? logEvent.getIpAddress() : "")
                .put("timestamp", logEvent.getTimestamp())
                .build();

        // producerRecord contains the name of the kafka topic we are publishing to, followed by the message to be sent.
        ProducerRecord producerRecord = new ProducerRecord<String, String>("topic_logged_events", logEvent.getUserId(),
                objectMapper.writeValueAsString(kafkaLogRecord));

        try {
            kafkaProducer.send(producerRecord);

        } catch (KafkaException kex) {
            kex.printStackTrace();
        }
    }


    /**
     * Generate a logEvent object.
     *
     * @param userId
     *            - owner user id
     * @param anonymousUserId
     *            - id to use if not logged in
     * @param eventType
     *            - the type of event that has occurred
     * @param eventDetails
     *            - event details if further details are required.
     * @param ipAddress
     *            - the ip address of the client making the request
     * @return a log event.
     * @throws JsonProcessingException
     *             - if we cannot process the json
     * @throws SegueDatabaseException
     *             - If we cannot record the ip address location information
     */
    private LogEvent buildLogEvent(final String userId, final String anonymousUserId, final String eventType,
                                   final Object eventDetails, final String ipAddress) throws JsonProcessingException, SegueDatabaseException {
        if (null == userId && null == anonymousUserId) {
            throw new IllegalArgumentException("UserId or anonymousUserId must be set.");
        }

        LogEvent logEvent = new LogEvent();

        if (null != userId) {
            logEvent.setUserId(userId);
            logEvent.setAnonymousUser(false);
        } else {
            logEvent.setUserId(anonymousUserId);
            logEvent.setAnonymousUser(true);
        }

        logEvent.setEventType(eventType);

        if (eventDetails != null) {
            logEvent.setEventDetailsType(eventDetails.getClass().getCanonicalName());
            logEvent.setEventDetails(eventDetails);
        }

        if (ipAddress != null) {
            logEvent.setIpAddress(ipAddress.split(",")[0]);

            try {
                // split based on the fact that we usually get ip addresses of the form
                // [user_ip], [balancer/gateway_ip]
                locationManager.refreshLocation(ipAddress.split(",")[0]);
            } catch (SegueDatabaseException | IOException e1) {
                log.error("Unable to record location information for ip Address: " + ipAddress, e1);
            }
        }

        logEvent.setTimestamp(new Date());

        return logEvent;
    }

}
