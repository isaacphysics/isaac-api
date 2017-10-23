package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.TopicConfig;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.inject.Inject;
import java.util.*;
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
     * @param configEntries specified configuration variables if topic requires creation
     */
    public void ensureTopicExists(final String name, final Collection<ConfigEntry> configEntries) {

        try {
            if (!adminClient.listTopics().names().get().contains(name)) {
                adminClient.createTopics(ImmutableList.of(new NewTopic(name, 1, Short.parseShort(globalProperties.getProperty("KAFKA_REPLICATION_FACTOR"))))).all().get();

                if (null != configEntries || !configEntries.isEmpty()) {

                    Map<ConfigResource, Config> updateConfig = new HashMap<ConfigResource, Config>();
                    ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, name);

                    Iterator<ConfigEntry> entries = configEntries.iterator();

                    while (entries.hasNext()) {
                        updateConfig.put(resource, new Config(Collections.singleton(entries.next())));
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
