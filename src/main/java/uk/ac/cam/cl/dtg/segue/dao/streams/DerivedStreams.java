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

package uk.ac.cam.cl.dtg.isaac.kafka.utilities;

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
import uk.ac.cam.cl.dtg.isaac.kafka.customMappers.AugmentedQuestionDetailMapper;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *  Static class to refactor out logic for derived streams
 *  @author Dan Underwood
 */
public final class DerivedStreams {

    private static final Serializer<JsonNode> jsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
    private static final Serde<JsonNode> jsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);
    private static final Serde<String> stringSerde = Serdes.String();
    private static final Serde<Long> longSerde = Serdes.Long();
    private static final JsonNodeFactory nodeFactory = JsonNodeFactory.instance;

    // private, empty constructor
    private DerivedStreams() {
    }

    public static KStream<String, JsonNode> FilterByEventType(KStream<String, JsonNode> stream, String eventType) {

        return stream
                .filter(
                        (userId, loggedEvent) -> loggedEvent.path("event_type")
                                .asText()
                                .equals(eventType)
                );
    }

    public static KStream<String, Long> UserWeeklyStreaks(KStream<String, JsonNode> stream) {

        return stream
                .groupByKey(stringSerde, jsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode streakRecord = JsonNodeFactory.instance.objectNode();

                            streakRecord.put("streakStart", 0);
                            streakRecord.put("streakEnd", 0);

                            return streakRecord;
                        },
                        // aggregator
                        (userId, latestEvent, streakRecord) -> {
                            ObjectNode updatedStreakRecord = JsonNodeFactory.instance.objectNode();

                            // timestamp of streak start
                            Long streakStartTimestamp = streakRecord.path("streakStart").asLong();

                            // timestamp of streak end
                            Long streakEndTimestamp = streakRecord.path("streakEnd").asLong();

                            // timestamp of latest event
                            Long latestEventTimestamp = latestEvent.path("event_details")
                                    .path("dateAttempted").asLong();


                            if (streakStartTimestamp == 0 || TimeUnit.SECONDS
                                    .convert(latestEventTimestamp - streakEndTimestamp,
                                            TimeUnit.MILLISECONDS) > 8 ) {

                                updatedStreakRecord.put("streakStart", latestEventTimestamp);
                                updatedStreakRecord.put("streakEnd", latestEventTimestamp);

                            }
                            else {
                                updatedStreakRecord.put("streakStart", streakStartTimestamp);
                                updatedStreakRecord.put("streakEnd", latestEventTimestamp);
                            }

                            return updatedStreakRecord;
                        },
                        jsonSerde,
                        "userStreaks")

                .mapValues(
                        (jsonTimestamps) -> TimeUnit.SECONDS
                                .convert(jsonTimestamps.path("streakEnd").asLong() - jsonTimestamps.path("streakStart").asLong(),
                                        TimeUnit.MILLISECONDS) / 7
                )
                .toStream();

    }

    public static KStream<String, JsonNode> UserQuestionAttemptsComplete(KStream<String, JsonNode> stream) {


        /** need to determine if a user has answered all parts of a question correctly. This requires some transformations to the incoming stream...
         * steps include:
         * 1) work out the number of question parts a user has answered correctly for a given question
         * 2) check if the user answer count for question parts matches the number of parts to that question
         *      i) fetch the question details, including level, tags, no. of question parts
         *      ii) if user question part count = no. of question parts -> correct = true, else false
         */
        return stream

                // filter for correctly answered question parts
                .filter(
                        (userId, loggedEvent) -> loggedEvent.path("event_details")
                                .path("correct")
                                .asBoolean()
                )

                // first map the <string, json> key-value pair into a <string, json> of ({userId}-{questionPartId}, jsonDetails) [the derived value is irrelevant really]
                .map(
                        (userId, loggedEvent) -> {

                            String qPartId = loggedEvent.path("event_details")
                                    .path("questionId")
                                    .asText()
                                    .toLowerCase();

                            return new KeyValue<>(userId.toLowerCase() + "-" + qPartId.substring(0, qPartId.indexOf("|")), loggedEvent);
                        }
                )
                // group by the {userId}-{questionPartId} for all correct attempts at a question part by a user, then count
                // this maintains counts for unique user question part attempts
                .groupByKey(stringSerde, jsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode userQuestionAttemptRecord = JsonNodeFactory.instance.objectNode();

                            userQuestionAttemptRecord.put("questionId", "");
                            userQuestionAttemptRecord.put("partAttemptsCorrect", "");
                            userQuestionAttemptRecord.put("latestAttempt", "");

                            return userQuestionAttemptRecord;
                        },
                        // aggregator
                        (userIdQuestionId, questionAttempt, attemptRecord) -> {

                            String questionId = questionAttempt.path("event_details")
                                    .path("questionId")
                                    .asText()
                                    .toLowerCase();

                            String questionPageId = userIdQuestionId
                                    .substring(userIdQuestionId.indexOf("-") + 1,
                                    (userIdQuestionId.length()));

                            String partAttemptId = questionId
                                    .substring(questionId.indexOf("|") + 1, questionId.length());

                            // timestamp of latest event
                            Long attemptDate = questionAttempt.path("event_details")
                                    .path("dateAttempted").asLong();

                            ObjectNode qPartCounts = nodeFactory.objectNode();
                            qPartCounts.put("questionPartId", partAttemptId);
                            qPartCounts.put("correctAttemptCount", 1);



                            if (attemptRecord.path("questionId").asText().isEmpty()) {

                                ((ObjectNode)attemptRecord).put("questionId", questionPageId);
                                ((ObjectNode)attemptRecord)
                                        .putArray("partAttemptsCorrect")
                                        .add(qPartCounts);

                            }
                            else {

                                Iterator<JsonNode> elements = attemptRecord.path("partAttemptsCorrect").elements();
                                Boolean elementExists = false;

                                while (elements.hasNext()) {

                                    JsonNode node = elements.next();

                                    if (node.path("questionPartId").asText().matches(partAttemptId)) {
                                        elementExists = true;
                                        Long currentCount = node.path("correctAttemptCount").asLong();
                                        ((ObjectNode)node).put("correctAttemptCount", currentCount + 1);
                                        break;
                                    }
                                }

                                if (!elementExists)
                                    ((ArrayNode)attemptRecord.path("partAttemptsCorrect")).add(qPartCounts);

                            }

                            ((ObjectNode)attemptRecord).put("latestAttempt", attemptDate);

                            return attemptRecord;

                        },
                        jsonSerde,
                        "userQuestionAttemptCount"
                ).toStream()

                /*.count("userQuestionPartCount")
                .toStream()

                // next need to map the {userId}-{questionPartId} in the previous aggregation into {userId}-{questionId}
                // then group by the {userId}-{questionId}, and count to obtain question parts answered by user for given questionId, into a <string, long> key-value pair
                .groupBy(
                        (userIdQPartId, count) -> userIdQPartId.substring(0, userIdQPartId.indexOf("|")), stringSerde, longSerde
                )
                .count("userTotalQuestionCount")
                .toStream()*/

                // finally map  the ({userId}-{questionId}, count) key-value pair back to a <string, json> of (userId, questionAttemptDetails), where questionAttemptDetails holds info on the questionId and parts attempted by the user
                .map(
                        (userIdQId, jsonDoc) -> new KeyValue<>(userIdQId.substring(0, userIdQId.indexOf("-")), jsonDoc)
                )

                // we then map the value onto a AugmentedQuestionDetail json object, which includes additional question details, and a flag stating if the user has the whole question correct
                .mapValues(new AugmentedQuestionDetailMapper());
    }

}
