package uk.ac.cam.cl.dtg.segue.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.legacy.PowerMockRunner;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * Created by du220 on 09/10/2017.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(KafkaStreamsProducer.class)
@PowerMockIgnore({"javax.ws.*"})
public class KafkaProducerTest {

    @Test
    public void kafkaProducer_checkForBadParameters_exceptionsShouldBeThrown() throws IOException {
        PowerMock.mockStatic(KafkaStreamsProducer.class);

        PowerMock.replay(KafkaStreamsProducer.class);

        try {
            new KafkaStreamsProducer(null, null, null);
            fail("KafkaStreamsProducer constructor was given null, but didn't throw an exception");
        } catch (NullPointerException e) {
            // Exception correctly thrown.
        } catch (Exception e) {
            fail("KafkaStreamsProducer constructor threw wrong exception type: " + e);
        }

    }

    @Test
    public void send_checkExceptionThrown() {

        ObjectMapper objectMapper = new ObjectMapper();

        KafkaStreamsProducer mock = EasyMock.createMock(KafkaStreamsProducer.class);

        Map<String, Object> kafkaLogRecord = new ImmutableMap.Builder<String, Object>()
                .put("testKey", "testValue")
                .build();

        /*try {
            ProducerRecord producerRecord = new ProducerRecord<String, String>(null, "10000000000000", objectMapper.writeValueAsString(kafkaLogRecord));
            mock.send(producerRecord);
            fail("KafkaStreamsProducer record was given null, but didn't throw an exception");
        } catch (IllegalArgumentException e) {
            // Exception correctly thrown.
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }*/

        try {
            ProducerRecord producerRecord = new ProducerRecord<String, String>("topic_test", null, objectMapper.writeValueAsString(kafkaLogRecord));
            mock.send(producerRecord);
            fail("KafkaStreamsProducer record was given null, but didn't throw an exception");
        } catch (IllegalArgumentException e) {
            // Exception correctly thrown.
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }



    }

}
