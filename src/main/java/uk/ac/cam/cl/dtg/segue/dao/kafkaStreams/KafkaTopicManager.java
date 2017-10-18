package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    /**
     * Method to check if a topic exists inside Kafka, and to create it if not
     *
     * @param name name of the topic
     * @param retentionMillis required retention period if topic requires creation
     */
    public void ensureTopicExists(final String name, final long retentionMillis) {

        try {
            if (!adminClient.listTopics().names().get().contains(name)) {
                adminClient.createTopics(ImmutableList.of(new NewTopic(name, 1, Short.parseShort(globalProperties.getProperty("KAFKA_REPLICATION_FACTOR"))))).all().get();

                if (retentionMillis != 0) {

                    Map<ConfigResource, Config> updateConfig = new HashMap<ConfigResource, Config>();
                    ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);

                    if (retentionMillis == -2) {

                        ConfigEntry cleanupPolicy = new ConfigEntry(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
                        updateConfig.put(resource, new Config(Collections.singleton(cleanupPolicy)));

                    } else {

                        // create a new entry for updating the retention.ms value on the same topic
                        ConfigEntry retentionEntry = new ConfigEntry(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retentionMillis));
                        updateConfig.put(resource, new Config(Collections.singleton(retentionEntry)));

                    }

                    AlterConfigsResult alterConfigsResult = adminClient.alterConfigs(updateConfig);
                    alterConfigsResult.all();
                }
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
