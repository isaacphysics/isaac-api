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
package uk.ac.cam.cl.dtg.segue.database;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.StringSerializer;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.KafkaTopicManager;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

/**
 * KafkaProducer class.
 *
 * This represents an immutable wrapped client for a Kafka producer.
 */
public class KafkaStreamsProducer implements Closeable {

    private KafkaProducer<String, String> producer;
    private KafkaTopicManager topicManager;
    private Set<String> topicListCache = Collections.emptySet();

    public KafkaStreamsProducer(final String kafkaHost, final String kafkaPort, final KafkaTopicManager topicManager) {

        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, kafkaHost + ":" + kafkaPort);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 200);

        producer = new KafkaProducer<>(props);
        this.topicManager = topicManager;

        topicListCache = topicManager.listTopics();

    }

    public KafkaProducer getProducer() {
        return this.producer;
    }

    public void send(ProducerRecord<String, String> record) throws KafkaException {
        if (!topicListCache.contains(record.topic())) {
            topicManager.ensureTopicExists(record.topic(), 0);
            topicListCache = topicManager.listTopics();
        }
        producer.send(record);
    }

    @Override
    public void close() throws IOException {

        producer.close();

    }

}