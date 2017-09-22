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

package uk.ac.cam.cl.dtg.segue.dao.streams;

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
import org.apache.kafka.streams.kstream.*;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.streams.customAggregators.QuestionAnswerCounter;
import uk.ac.cam.cl.dtg.segue.dao.streams.customAggregators.QuestionAnswerInitializer;
import uk.ac.cam.cl.dtg.segue.dao.streams.customMappers.AugmentedQuestionDetailMapper;
import uk.ac.cam.cl.dtg.segue.dao.streams.customProcessors.ThresholdAchievedProcessor;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *  Static class to refactor out logic for derived streams.
 *  @author Dan Underwood
 */
public final class DerivedStreams {


    private static final Serializer<JsonNode> JsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> JsonDeserializer = new JsonDeserializer();
    private static final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(JsonSerializer, JsonDeserializer);
    private static final Serde<String> StringSerde = Serdes.String();
    private static final Serde<Long> LongSerde = Serdes.Long();

    private static Boolean userStatsInitialized = false;


    /**
     * Private, empty constructor (static utility class).
     */
    private DerivedStreams() {
    }


    /**
     * Function to filter incoming stream by logged event type.
     *
     * @param stream    incoming raw logged event data
     * @param eventType logged event type to filter by
     * @return filtered stream
     */
    public static KStream<String, JsonNode> filterByEventType(final KStream<String, JsonNode> stream, final String eventType) {

        return stream
                .filter(
                        (userId, loggedEvent) -> loggedEvent.path("event_type")
                                .asText()
                                .equals(eventType)
                );
    }


    /**
     * Stream processing of user achievements based on user activity captured by raw logged events.
     * Stream records are passed through various filters and transformations to feed downward streams.
     *
     * @param stream         incoming raw logged event data
     * @param contentManager
     * @param contentIndex
     * @return
     */
    public static KStream<String, JsonNode> userAchievements(final KStream<String, JsonNode> stream,
                                                             final IContentManager contentManager,
                                                             final String contentIndex,
                                                             final ThresholdAchievedProcessor achievementProcessor) {

        // user question attempts
        userQuestionAttempts(stream, achievementProcessor, contentManager, contentIndex);

        // user activity streaks
        userWeeklyStreaks(stream);

        // teacher assignment activity
        teacherAssignmentActivity(stream);

        return stream;
    }


    /**
     * Processing of teacher activity for creating and setting assignments
     *
     * @param stream incoming raw logged event data
     * @return
     */
    private static KStream<String, JsonNode> teacherAssignmentActivity(final KStream<String, JsonNode> stream) {

        KStream<String, JsonNode> setAssignments = filterByEventType(stream, "SET_NEW_ASSIGNMENT");

        return stream;
    }


    /**
     * Processing of user weekly streaks based on timestamped user logged events.
     *
     * @param stream incoming raw logged event data
     * @return
     */
    private static KStream<String, Long> userWeeklyStreaks(final KStream<String, JsonNode> stream) {

        return stream
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode streakRecord = JsonNodeFactory.instance.objectNode();

                            streakRecord.put("streak_start", 0);
                            streakRecord.put("streak_end", 0);

                            return streakRecord;
                        },
                        // aggregator
                        (userId, latestEvent, streakRecord) -> {
                            ObjectNode updatedStreakRecord = JsonNodeFactory.instance.objectNode();

                            // timestamp of streak start
                            Long streakStartTimestamp = streakRecord.path("streak_start").asLong();

                            // timestamp of streak end
                            Long streakEndTimestamp = streakRecord.path("streak_end").asLong();

                            // timestamp of latest event
                            Long latestEventTimestamp = latestEvent.path("event_details")
                                    .path("date_attempted").asLong();


                            if (streakStartTimestamp == 0 || TimeUnit.DAYS
                                    .convert(latestEventTimestamp - streakEndTimestamp,
                                            TimeUnit.MILLISECONDS) > 8) {

                                updatedStreakRecord.put("streak_start", latestEventTimestamp);
                                updatedStreakRecord.put("streak_end", latestEventTimestamp);

                            } else {
                                updatedStreakRecord.put("streak_start", streakStartTimestamp);
                                updatedStreakRecord.put("streak_end", latestEventTimestamp);
                            }

                            return updatedStreakRecord;
                        },
                        JsonSerde,
                        "store_user_streaks")

                .mapValues(
                        (jsonTimestamps) -> TimeUnit.DAYS
                                .convert(jsonTimestamps.path("streak_end").asLong() - jsonTimestamps.path("streak_start").asLong(),
                                        TimeUnit.MILLISECONDS) / 7
                )
                .toStream();

    }


    /**
     * Processes an incoming stream of logged events to update a number of internal state stores
     * useful for providing site usage statistics.
     *
     * @param stream - incoming logged events stream
     */
    public static void userStatistics(final KStream<String, JsonNode> stream) {

        if (userStatsInitialized)
            return;

        /**
         * process user data in local data stores
         * extract user record related events
         */
        KTable<String, JsonNode> userData = DerivedStreams.filterByEventType(stream, "CREATE_UPDATE_USER")
                // set up a local user data store
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode userRecord = JsonNodeFactory.instance.objectNode();

                            userRecord.put("user_id", "");
                            userRecord.put("user_data", "");

                            return userRecord;
                        },
                        // aggregator
                        (userId, userUpdateLogEvent, userRecord) -> {

                            ((ObjectNode) userRecord).put("user_id", userId);
                            ((ObjectNode) userRecord).put("user_data", userUpdateLogEvent.path("event_details"));

                            return userRecord;
                        },
                        JsonSerde,
                        "store_user_data"
                );

        // join user table to incoming event stream to get user data for stats processing
        KStream<String, JsonNode> userEvents = stream

                .join(
                        userData,
                        (logEventVal, userDataVal) -> {
                            ObjectNode joinedValueRecord = JsonNodeFactory.instance.objectNode();

                            joinedValueRecord.put("user_id", userDataVal.path("user_data").path("user_id"));
                            joinedValueRecord.put("user_role", userDataVal.path("user_data").path("role"));
                            joinedValueRecord.put("user_gender", userDataVal.path("user_data").path("gender"));
                            joinedValueRecord.put("event_type", logEventVal.path("event_type"));
                            joinedValueRecord.put("event_details", logEventVal.path("event_details"));
                            joinedValueRecord.put("timestamp", logEventVal.path("timestamp"));

                            return joinedValueRecord;
                        }
                );


        // maintain internal store of users' last seen times by log event type, and counts per event type
        KTable<String, JsonNode> userEventCounts = userEvents
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode countRecord = JsonNodeFactory.instance.objectNode();
                            countRecord.put("last_seen", 0);
                            return countRecord;
                        },
                        // aggregator
                        (userId, logEvent, countRecord) -> {

                            String eventType = logEvent.path("event_type").asText();
                            Timestamp stamp = new Timestamp(logEvent.path("timestamp").asLong());

                            if (!countRecord.has(eventType)) {
                                ObjectNode node = JsonNodeFactory.instance.objectNode();
                                node.put("count", 0);
                                node.put("latest", 0);
                                ((ObjectNode) countRecord).put(eventType, node);
                            }

                            Long count = countRecord.path(eventType).path("count").asLong();
                            ((ObjectNode) countRecord.path(eventType)).put("count", count + 1);
                            ((ObjectNode) countRecord.path(eventType)).put("latest", stamp.getTime());

                            ((ObjectNode) countRecord).put("last_seen", stamp.getTime());

                            return countRecord;
                        },
                        JsonSerde,
                        "store_user_last_seen"
                );

        /*userData
                .join(
                        userEventCounts,
                        (userDataVal, userEventCountsVal) -> {

                            ObjectNode joinedValueRecord = JsonNodeFactory.instance.objectNode();

                            joinedValueRecord.put("user_data", userDataVal.path("user_data"));
                            joinedValueRecord.put("user_last_seen_data", userEventCountsVal);

                            return (JsonNode) joinedValueRecord;
                        }
                ).through(StringSerde, JsonSerde, "topic_user_data", "store_augmented_user_data");*/


        // maintain internal store of log event type counts
        userEvents
                .map(
                        (k, v) -> {
                            return new KeyValue<String, JsonNode>(v.path("event_type").asText(), v);
                        }
                )
                .groupByKey(StringSerde, JsonSerde)
                .count("store_log_event_counts");



        /*// maintain internal store of log event counts per user type, per day
        userEvents
                .map(
                        (k, v) -> {

                            Timestamp stamp = new Timestamp(v.path("timestamp").asLong());
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(stamp);
                            cal.set(Calendar.HOUR_OF_DAY, 0);
                            cal.set(Calendar.MINUTE, 0);
                            cal.set(Calendar.SECOND, 0);
                            cal.set(Calendar.MILLISECOND, 0);

                            return new KeyValue<Long, JsonNode>(cal.getTimeInMillis(), v);
                        }
                )
                .groupByKey(LongSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode countRecord = JsonNodeFactory.instance.objectNode();
                            return countRecord;
                        },
                        // aggregator
                        (userId, logEvent, countRecord) -> {

                            String userRole = logEvent.path("user_role").asText();

                            if (!countRecord.has(userRole)) {
                                ObjectNode logEventTypes = JsonNodeFactory.instance.objectNode();
                                ((ObjectNode) countRecord).put(userRole, logEventTypes);
                            }

                            String eventType = logEvent.path("event_type").asText();

                            if (countRecord.path(userRole).has(eventType)) {

                                Long count = countRecord.path(userRole).path(eventType).asLong();
                                ((ObjectNode) countRecord.path(userRole)).put(eventType, count + 1);

                            } else {
                                ((ObjectNode) countRecord.path(userRole)).put(eventType, 1);
                            }

                            return countRecord;
                        },
                        JsonSerde,
                        "store_daily_log_events"
                );*/


        // streams initialized
        userStatsInitialized = true;
    }








    /**
     * Processing of user completed question attempts.
     *
     * @param stream         incoming raw logged event data
     * @param contentManager
     * @param contentIndex
     * @return
     */
    private static KStream<String, JsonNode> userQuestionAttempts(final KStream<String, JsonNode> stream,
                                                                          final ThresholdAchievedProcessor achievementProcessor,
                                                                          final IContentManager contentManager,
                                                                          final String contentIndex) {


        // stream of question *part* attempts
        KStream<String, JsonNode> questionPartAttempts = filterByEventType(stream, "ANSWER_QUESTION");


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

                            newValue.put("latest_attempt", value.path("latest_attempt"));
                            newValue.put("count", value.path("count"));

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










        // local state store for user question progress - structure mimics the user progress API endpoint response
        /*KTable<String, JsonNode> completedQuestionStore = completedQuestions
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {

                            ObjectNode correctByLevel = JsonNodeFactory.instance.objectNode();
                            ObjectNode correctByTag = JsonNodeFactory.instance.objectNode();

                            ObjectNode attemptsByLevel = JsonNodeFactory.instance.objectNode();
                            ObjectNode attemptsByTag = JsonNodeFactory.instance.objectNode();

                            ObjectNode unlockedAchievements = JsonNodeFactory.instance.objectNode();

                            ObjectNode userCompletedQuestionsRecord = JsonNodeFactory.instance.objectNode();
                            userCompletedQuestionsRecord.put("totalQuestionsAttempted", 0);
                            userCompletedQuestionsRecord.put("totalQuestionsCorrect", 0);
                            userCompletedQuestionsRecord.put("correctByTag", correctByTag);
                            userCompletedQuestionsRecord.put("correctByLevel", correctByLevel);

                            return userCompletedQuestionsRecord;
                        },
                        // aggregator
                        (userId, completedQuestion, completedQuestionsRecord) -> {

                            ((ObjectNode) completedQuestionsRecord).put("record_updated", completedQuestion.path("latest_attempt").asLong());

                            Long totalCompletedQuestions = completedQuestionsRecord.path("totalQuestionsCorrect").asLong();
                            ((ObjectNode) completedQuestionsRecord).put("totalQuestionsCorrect", totalCompletedQuestions + 1);


                            // update level counts
                            if (!completedQuestion.path("level").asText().equals("")) {
                                ((ObjectNode) completedQuestionsRecord).put("correctByLevel",
                                        updateCounters(completedQuestion.path("level"), completedQuestionsRecord.path("correctByLevel")));
                            }

                            //update tag counts
                            if (completedQuestion.path("tags").size() > 0) {

                                Iterator<JsonNode> elements = completedQuestion.path("tags").elements();

                                while (elements.hasNext()) {

                                    JsonNode tagElement = elements.next();

                                    ((ObjectNode) completedQuestionsRecord).put("correctByTag",
                                            updateCounters(tagElement, completedQuestionsRecord.path("correctByTag")));
                                }

                            }

                            return completedQuestionsRecord;
                        },
                        JsonSerde,
                        "store_user_completed_questions"
                );*/

        return stream;
    }


    /*private static JsonNode updateCounters(JsonNode countedValue, JsonNode counterNode) {

        String value = countedValue.asText();

        if (!counterNode.has(value)) {
            ((ObjectNode) counterNode).put(value, 0);
        }

        Long totalValueCount = counterNode.path(value).asLong();
        ((ObjectNode) counterNode).put(value, totalValueCount + 1);

        return counterNode;
    }*/
}

