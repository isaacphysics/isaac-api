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

package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.derivedStreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.customProcessors.ThresholdAchievedProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *  Static class to refactor out logic for assignment-based achievement derived streams.
 *  @author Dan Underwood
 */
public final class TeacherAssignmentActivity {

    private static final Logger log = LoggerFactory.getLogger(TeacherAssignmentActivity.class);

    private static final Serializer<JsonNode> JsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> JsonDeserializer = new JsonDeserializer();
    private static final Serde<String> StringSerde = Serdes.String();
    private static final Serde<Long> LongSerde = Serdes.Long();
    private static final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(JsonSerializer, JsonDeserializer);

    private TeacherAssignmentActivity(){}


    public static void process(KStream<String, JsonNode> stream,
                               GameManager gameManager,
                               ThresholdAchievedProcessor achievementProcessor) {

        KStream<String, JsonNode> setAssignments =
                stream.filter(
                        (userId, event) -> event.path("event_type").asText().equals("ANSWER_QUESTION")
                ).map(
                        (userId, event) -> {

                            String gameboardId = event.path("event_details").path("gameboardId").asText();
                            String owner;
                            ObjectNode transformedEventDetails = JsonNodeFactory.instance.objectNode();
                            ArrayNode levels = JsonNodeFactory.instance.arrayNode();
                            transformedEventDetails.put("timestamp", event.path("timestamp").asLong());

                            try {

                                GameboardDTO gameboard = gameManager.getGameboard(gameboardId);

                                if (gameboard.getOwnerUserId().equals(userId)) {
                                    owner = "custom";
                                } else {
                                    owner = "shared";
                                }

                                for (GameboardItem item: gameboard.getQuestions()
                                        ) {

                                    if (!levels.has(item.getLevel())) {
                                        levels.add(item.getLevel());
                                    }
                                }

                                transformedEventDetails.put("levels", levels);

                                return new KeyValue<>(userId + "-" + owner, transformedEventDetails);

                            } catch (SegueDatabaseException e) {
                                String message = "Error whilst trying to access the gameboard in the database.";
                                log.error(message, e);
                                return new KeyValue<>(userId, event);
                            }

                        }
                ).flatMap(
                        (userIdOwnerType, event) -> {

                            List<KeyValue<String, Long>> result = new ArrayList<>();
                            result.add(new KeyValue<>(userIdOwnerType + "-total", event.path("timestamp").asLong()));

                            Iterator<JsonNode> elements = event.path("levels").elements();

                            while (elements.hasNext()) {

                                JsonNode levelElement = elements.next();

                                result.add(new KeyValue<>(userIdOwnerType + "-" + levelElement.asText(), event.path("timestamp").asLong()));
                            }

                            return result;
                        }
                ).groupByKey(StringSerde, LongSerde)
                        .aggregate(
                                // initializer
                                () -> {
                                    ObjectNode setAssignmentRecord = JsonNodeFactory.instance.objectNode()
                                            .put("count", 0);

                                    return setAssignmentRecord;
                                },
                                // aggregator
                                (userIdOwnerTypeLevel, newAssignmentTimestamp, setAssignmentRecord) -> {

                                    Long count = setAssignmentRecord.path("count").asLong();
                                    ((ObjectNode) setAssignmentRecord).put("count", count + 1);
                                    ((ObjectNode) setAssignmentRecord).put("latest_attempt", newAssignmentTimestamp);

                                    return setAssignmentRecord;
                                },
                                JsonSerde,
                                "store_set_assignment_count"
                        ).toStream()
                        .map(
                                (userIdOwnerTypeLevel, countRecord) -> {

                                    String[] keyArray = userIdOwnerTypeLevel.split("-");

                                    if (keyArray[2].equals("total")) {
                                        ((ObjectNode) countRecord).put("type", "SET_" + keyArray[1].toUpperCase() + "_ASSIGNMENT_TOTAL");
                                    } else {
                                        ((ObjectNode) countRecord).put("type", "SET_" + keyArray[1].toUpperCase() + "_ASSIGNMENT_LEVEL_" + keyArray[2]);
                                    }

                                    return new KeyValue<>(keyArray[0], countRecord);
                                }
                        );

        // thresholds for achievement unlocking
        Integer[] thresholds = {1, 5, 10, 20, 30, 50, 75, 100, 125, 150, 200};

        // trigger process for records that match threshold (total questions answered)
        for (Integer threshold: thresholds
                ) {
            setAssignments.filter(
                    (k, v) -> v.path("count").asInt() == threshold
            ).process(
                    () -> achievementProcessor
            );
        }

    }

}
