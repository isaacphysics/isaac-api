package uk.ac.cam.cl.dtg.segue.dao;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Future;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.prism.impl.Disposer;
import org.apache.commons.lang3.Validate;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dos.LogEvent;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.serialization.StringSerializer;


import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by du220 on 27/04/2017.
 */
public class KafkaLoggingProducer extends LoggingEventHandler {
    private static final Logger log = LoggerFactory.getLogger(PgLogManager.class);

    private KafkaProducer<String, String> producer;
    private LocationManager locationManager;
    private final ObjectMapper objectMapper;


    public KafkaLoggingProducer(final String kafkaHost, final String kafkaPort, final LocationManager locationManager, final ObjectMapper objectMapper) {

        this.locationManager = locationManager;
        this.objectMapper = objectMapper;

        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaHost + ":" + kafkaPort);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        producer = new KafkaProducer<>(props);

    }


    public void handleEvent(AbstractSegueUserDTO user, final HttpServletRequest httpRequest, String eventType, Object eventDetails) {

        Validate.notNull(user);
        try {
            if (user instanceof RegisteredUserDTO) {
                this.publishLogEvent(((RegisteredUserDTO) user).getId().toString(), null, eventType, eventDetails,
                        getClientIpAddr(httpRequest));
            } else {
                this.publishLogEvent(null, ((AnonymousUserDTO) user).getSessionId(), eventType, eventDetails,
                        getClientIpAddr(httpRequest));
            }

        } catch (JsonProcessingException e) {
            log.error("Unable to serialize eventDetails as json string", e);
        } catch (SegueDatabaseException e) {
            log.error("Unable to save log event to the database", e);
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


        LogEvent logEvent = this.buildLogEvent(userId, anonymousUserId, eventType, eventDetails, ipAddress);

        ProducerRecord producerRecord = new ProducerRecord<String, String>("loggedeventsTest", logEvent.getUserId(),
                String.format("{\"user_id\":\"%s\", \"anonymous_user\":\"%s\", \"event_type\":\"%s\", \"event_details_type\":\"%s\", \"event_details\":%s, \"ip_address\":\"%s\"}",
                        logEvent.getUserId(),
                        objectMapper.writeValueAsString(logEvent.isAnonymousUser()),
                        logEvent.getEventType(),
                        logEvent.getEventDetailsType(),
                        objectMapper.writeValueAsString(logEvent.getEventDetails()),
                        logEvent.getIpAddress()));

        try {
            producer.send(producerRecord);

        } catch (Throwable throwable) {
            System.out.println(throwable.getStackTrace());
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



    /**
     * Extract client ip address.
     *
     * Solution retrieved from:
     * http://stackoverflow.com/questions/4678797/how-do-i-get-the-remote-address-of-a-client-in-servlet
     *
     * @param request
     *            - to attempt to extract a valid Ip from.
     * @return string representation of the client's ip address.
     */
    private static String getClientIpAddr(final HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
