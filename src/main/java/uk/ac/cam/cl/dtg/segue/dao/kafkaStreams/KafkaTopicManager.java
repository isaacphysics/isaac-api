package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class KafkaTopicManager {

    private PropertiesLoader globalProperties;
    private AdminClient adminClient;

    @Inject
    public KafkaTopicManager(final PropertiesLoader globalProperties) {
        this.globalProperties = globalProperties;
        adminClient = AdminClient.create(ImmutableMap.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                globalProperties.getProperty("KAFKA_HOSTNAME") + ":" + globalProperties.getProperty("KAFKA_PORT")));
    }

    public void ensureTopicExists(final String name) {

        try {
            if (!adminClient.listTopics().names().get().contains(name)) {
                adminClient.createTopics(ImmutableList.of(new NewTopic(name, 1, Short.parseShort(globalProperties.getProperty("KAFKA_REPLICATION_FACTOR"))))).all().get();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            // TODO: If this failed, disable Kafka API-wide.
            e.printStackTrace();
        }


    }

    public Set<String> listTopics() {
        try {
            return adminClient.listTopics().names().get();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

}
