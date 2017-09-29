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
import com.google.common.collect.Lists;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KStream;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.customMappers.AugmentedQuestionDetailMapper;
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.customProcessors.ThresholdAchievedProcessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 *  Static class to refactor out logic for user question-based achievement derived streams.
 *  @author Dan Underwood
 */
public final class UserQuestionAttempts {

    private static final Serializer<JsonNode> JsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> JsonDeserializer = new JsonDeserializer();
    private static final Serde<String> StringSerde = Serdes.String();
    private static final Serde<Long> LongSerde = Serdes.Long();
    private static final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(JsonSerializer, JsonDeserializer);

    private UserQuestionAttempts(){}

    public static void process(KStream<String, JsonNode> stream,
                               ThresholdAchievedProcessor achievementProcessor,
                               IContentManager contentManager,
                               String contentIndex,
                               GameManager gameManager) {



        // stream of question *part* attempts
        KStream<String, JsonNode> questionPartAttempts = stream.filter(
                (userId, event) -> event.path("event_type").asText().equals("ANSWER_QUESTION")
        );

        // need to determine if a user has answered all parts of a question correctly. This requires some transformations to the incoming stream...
        KStream<String, JsonNode> questionAttempts = questionPartAttempts

                // map the ["user_id"] key to a ["user_id"-"question_id"] key
                .map(
                        (userId, loggedEvent) -> {

                            String qPartId = loggedEvent.path("event_details")
                                    .path("questionId")
                                    .asText()
                                    .toLowerCase();

                            return new KeyValue<>(userId.toLowerCase() + "-" + qPartId.substring(0, qPartId.indexOf("|")), loggedEvent);
                        }
                )

                // group by the ["user_id"-"question_id"] and aggregate
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode userQuestionAttemptRecord = JsonNodeFactory.instance.objectNode();

                            userQuestionAttemptRecord.put("question_id", "");

                            ArrayNode qParts = JsonNodeFactory.instance.arrayNode();
                            userQuestionAttemptRecord.put("part_attempts_correct", qParts);

                            ObjectNode latestAttempt = JsonNodeFactory.instance.objectNode();
                            latestAttempt.put("timestamp", "");
                            userQuestionAttemptRecord.put("latest_attempt", latestAttempt);

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
                                            userIdQuestionId.length());

                            String partAttemptId = questionId
                                    .substring(questionId.indexOf("|") + 1, questionId.length());


                            ((ObjectNode) attemptRecord).put("question_id", questionPageId);
                            ((ObjectNode) attemptRecord.path("latest_attempt"))
                                    .put("timestamp", questionAttempt.path("timestamp").asLong())
                                    .put("first_time_correct", false);


                            Iterator<JsonNode> elements = attemptRecord.path("part_attempts_correct").elements();
                            Boolean elementExists = false;

                            while (elements.hasNext()) {

                                JsonNode node = elements.next();

                                if (node.path("part_id").asText().matches(partAttemptId)) {
                                    elementExists = true;

                                    if (questionAttempt.path("event_details").path("correct").asBoolean()) {
                                        Long correctCount =  node.path("correct_count").asLong();

                                        if (correctCount == 0)
                                            ((ObjectNode) attemptRecord.path("latest_attempt")).put("first_time_correct", true);

                                        ((ObjectNode) node).put("correct_count", correctCount + 1);
                                    }
                                    ((ObjectNode) attemptRecord.path("latest_attempt")).put("new_part_attempt", false);

                                    break;
                                }
                            }

                            if (!elementExists) {

                                ObjectNode qPartCounts = JsonNodeFactory.instance.objectNode();
                                qPartCounts.put("part_id", partAttemptId);
                                if (questionAttempt.path("event_details").path("correct").asBoolean()) {
                                    qPartCounts.put("correct_count", 1);
                                    ((ObjectNode) attemptRecord.path("latest_attempt")).put("first_time_correct", true);
                                } else {
                                    qPartCounts.put("correct_count", 0);
                                }

                                ((ArrayNode) attemptRecord.path("part_attempts_correct")).add(qPartCounts);

                                ((ObjectNode) attemptRecord.path("latest_attempt")).put("new_part_attempt", true);
                            }

                            return attemptRecord;

                        },
                        JsonSerde,
                        "store_user_question_attempt_count"
                ).toStream()

                // map the (["user_id"-"question_id"], question_attempt_details) key-value pair to (user_id, question_attempt_details)
                .map(
                        (userIdQId, jsonDoc) -> new KeyValue<>(userIdQId.substring(0, userIdQId.indexOf("-")), jsonDoc)
                )

                // get additional question details (levels, tags, etc.)
                .mapValues(new AugmentedQuestionDetailMapper(contentManager, contentIndex));



        // stream of events where user has answered a whole question for the first time
        KStream<String, JsonNode> completedQuestions = questionAttempts.filter(
                (userId, questionAttempt) -> questionAttempt.path("event").asText().equals("question_completed")
        );

        // tags we are interested in monitoring for badges
        ArrayList<String> tags = Lists.newArrayList();
        tags.add("mechanics");
        tags.add("waves");
        tags.add("fields");
        tags.add("circuits");
        tags.add("chemphysics");

        KStream<String, JsonNode> correctCountStream = completedQuestions
                .flatMap(
                        (key, value) -> {

                            List<KeyValue<String, Long>> result = new ArrayList<>();
                            result.add(new KeyValue<>(key + "-total", value.path("latest_attempt").asLong()));
                            result.add(new KeyValue<>(key + "-level-" + value.path("level").asText(), value.path("latest_attempt").asLong()));

                            Iterator<JsonNode> elements = value.path("tags").elements();

                            while (elements.hasNext()) {

                                JsonNode tagElement = elements.next();

                                if (tags.contains(tagElement.asText()))
                                    result.add(new KeyValue<>(key + "-tag-" + tagElement.asText(), value.path("latest_attempt").asLong()));
                            }

                            return result;
                        }
                )
                .groupByKey(StringSerde, LongSerde)
                .aggregate(
                        // initializer
                        () -> {

                            ObjectNode userCorrectQuestionsRecord = JsonNodeFactory.instance.objectNode();
                            userCorrectQuestionsRecord.put("count", 0);

                            return userCorrectQuestionsRecord;
                        },
                        (userIdQuestionType, correctQuestion, correctRecord) -> {

                            Long count = correctRecord.path("count").asLong();
                            ((ObjectNode) correctRecord).put("count", count + 1);
                            ((ObjectNode) correctRecord).put("latest_attempt", correctQuestion);

                            return correctRecord;
                        },
                        JsonSerde,
                        "store_user_correct_questions"

                ).toStream()
                .map(
                        (key, value) -> {

                            ObjectNode newValue = JsonNodeFactory.instance.objectNode();

                            newValue.put("latest_attempt", value.path("latest_attempt").asLong());
                            newValue.put("count", value.path("count").asInt());

                            String[] keyArray = key.split("-");

                            if (keyArray[1].equals("total")) {
                                newValue.put("type", "QUESTIONS_ANSWERED_TOTAL");
                            } else if (keyArray[1].equals("level")) {
                                newValue.put("type", "QUESTIONS_ANSWERED_LEVEL_" + keyArray[2]);
                            } else if (keyArray[1].equals("tag")) {
                                newValue.put("type", "QUESTIONS_ANSWERED_TAG_" + keyArray[2].toUpperCase());
                            }

                            return new KeyValue<>(keyArray[0], newValue);
                        }
                );

        /** here is where we work out whether users have met certain thresholds for unlocking achievements.
         *  first define achievement thresholds, then filter all derived streams based on these and trigger the "unlocking process" to
         *  record the achievement for the user
         */
        // thresholds for achievement unlocking
        Integer[] thresholds = {1, 5, 10, 20, 30, 50, 75, 100, 125, 150, 200};

        // trigger process for records that match threshold (total questions answered)
        for (Integer threshold: thresholds
                ) {

            correctCountStream.filter(
                    (k, v) -> v.path("count").asInt() == threshold
            ).process(
                    () -> achievementProcessor
            );
        }
    }


}
