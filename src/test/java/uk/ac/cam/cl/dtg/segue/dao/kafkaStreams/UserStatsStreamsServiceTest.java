package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.test.ProcessorTopologyTestDriver;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userStatistics.UserStatisticsStreamsApplication;
import uk.ac.cam.cl.dtg.util.ClassVersionHash;

import java.io.BufferedReader;

import static org.junit.Assert.assertEquals;

/**
 * Created by du220 on 19/01/2018.
 */
public class UserStatsStreamsServiceTest {

    /**
     * Initial configuration of tests.
     *
     * @throws Exception
     *             - test exception
     */
    @Before
    public final void setUp() throws Exception {

    }


    @Test
    public void streamsClassVersions_Test() throws Exception {
        assertClassUnchanged(UserStatisticsStreamsApplication.class,"213acaa77b41d4be52daef692e517d85a9975732b2ff0caf90d919478d3f6acc");
    }


    private void assertClassUnchanged(Class c, String hash) {
        String newHash = ClassVersionHash.hashClass(c);
        assertEquals("Class '" + c.getSimpleName() + "' has changed - need up to update test and (possibly) Kafka streams application ID version number in SiteStatisticsStreamsApplication.\nNew class hash: " + newHash + "\n", newHash, hash);
    }

}
