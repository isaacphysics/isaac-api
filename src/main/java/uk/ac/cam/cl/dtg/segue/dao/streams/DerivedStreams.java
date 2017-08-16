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
import java.util.Calendar;
import java.util.Iterator;
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
    private static final JsonNodeFactory NodeFactory = JsonNodeFactory.instance;

    private static Boolean UserStatsInitialized = false;
    private static Boolean UserWeeklyStreaksInitialized = false;


    /** Private, empty constructor (static utility class). */
    private DerivedStreams() {
    }




    /** Function to filter incoming stream by logged event type.
     *
     * @param stream incoming raw logged event data
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




    /** Stream processing of user achievements based on user activity captured by raw logged events.
     * Stream records are passed through various filters and transformations to feed downward streams.
     *
     * @param stream incoming raw logged event data
     * @param contentManager
     * @param contentIndex
     * @return
     */
    public static KStream<String, JsonNode> userAchievements(final KStream<String, JsonNode> stream,
                                                             final IContentManager contentManager,
                                                             final String contentIndex,
                                                             final ThresholdAchievedProcessor achievementProcessor) {

        // user correct question attempts
        userQuestionAttemptsComplete(stream, achievementProcessor, contentManager, contentIndex);

        // user activity streaks
        userWeeklyStreaks(stream);

        // teacher assignment activity
        teacherAssignmentActivity(stream);



        return stream;
    }



    /** Processing of teacher activity for creating and setting assignments
     *
     * @param stream incoming raw logged event data
     * @return
     */
    private static KStream<String, JsonNode> teacherAssignmentActivity(final KStream<String, JsonNode> stream) {

        KStream<String, JsonNode> setAssignments = filterByEventType(stream, "SET_NEW_ASSIGNMENT");





        return stream;
    }




    /** Processing of user weekly streaks based on timestamped user logged events.
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
                                            TimeUnit.MILLISECONDS) > 8 ) {

                                updatedStreakRecord.put("streak_start", latestEventTimestamp);
                                updatedStreakRecord.put("streak_end", latestEventTimestamp);

                            }
                            else {
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




    private static void processAchievement(final KStream<String, JsonNode> stream,
                                           final Integer threshold,
                                           final ThresholdAchievedProcessor achievementProcessor) {

        KStream<String, JsonNode> filteredStream = stream.filter(
                (k, v) -> v.path("count").asInt() == threshold
        );

        filteredStream.to(StringSerde, JsonSerde, "topic_user_achievements");

        filteredStream.process(
                () -> achievementProcessor
        );
    }






    /** Processing of user completed question attempts.
     *
     * @param stream incoming raw logged event data
     * @param contentManager
     * @param contentIndex
     * @return
     */
    private static KStream<String, JsonNode> userQuestionAttemptsComplete(final KStream<String, JsonNode> stream,
                                                                          final ThresholdAchievedProcessor achievementProcessor,
                                                                          final IContentManager contentManager,
                                                                          final String contentIndex) {


        // stream of question part attempts
        // filter logged events by "ANSWER_QUESTION" event type (this is a log of *part* attempts, not *whole question* attempts)
        KStream<String, JsonNode> questionPartAttempts = filterByEventType(stream, "ANSWER_QUESTION");

        // Publish the derived "question part attempt" stream to a kafka topic
        //questionPartAttempts.to(StringSerde, JsonSerde, "topic_question_part_attempts");


        /** need to determine if a user has answered all parts of a question correctly. This requires some transformations to the incoming stream...
         * steps include:
         * 1) work out the number of question parts a user has answered correctly for a given question
         * 2) check if the user answer count for question parts matches the number of parts to that question
         *      i) fetch the question details, including level, tags, no. of question parts
         *      ii) if user question part count = no. of question parts -> correct = true, else false
         */
        KStream <String, JsonNode> questionAttempts = questionPartAttempts

                // filter for correctly answered question parts
                .filter(
                        (userId, loggedEvent) -> loggedEvent.path("event_details")
                                .path("correct")
                                .asBoolean()
                )

                // first map the ["user_id"] key to a ["user_id"-"question_id"] key
                .map(
                        (userId, loggedEvent) -> {

                            String qPartId = loggedEvent.path("event_details")
                                    .path("question_id")
                                    .asText()
                                    .toLowerCase();

                            return new KeyValue<>(userId.toLowerCase() + "-" + qPartId.substring(0, qPartId.indexOf("|")), loggedEvent);
                        }
                )


                // Group by the ["user_id"-"question_id"] key (gives all correct attempts at a question part by each user), then aggregate
                // to maintain counts for unique user question part attempts
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode userQuestionAttemptRecord = JsonNodeFactory.instance.objectNode();

                            userQuestionAttemptRecord.put("question_id", "");
                            userQuestionAttemptRecord.put("part_attempts_correct", "");
                            userQuestionAttemptRecord.put("latest_attempt", "");

                            return userQuestionAttemptRecord;
                        },
                        // aggregator
                        (userIdQuestionId, questionAttempt, attemptRecord) -> {

                            String questionId = questionAttempt.path("event_details")
                                    .path("question_id")
                                    .asText()
                                    .toLowerCase();

                            String questionPageId = userIdQuestionId
                                    .substring(userIdQuestionId.indexOf("-") + 1,
                                            userIdQuestionId.length());

                            String partAttemptId = questionId
                                    .substring(questionId.indexOf("|") + 1, questionId.length());

                            // timestamp of latest event
                            Long attemptDate = questionAttempt.path("event_details")
                                    .path("date_attempted").asLong();

                            ObjectNode qPartCounts = NodeFactory.objectNode();
                            qPartCounts.put("question_part_id", partAttemptId);
                            qPartCounts.put("correct_attempt_count", 1);



                            if (attemptRecord.path("question_id").asText().isEmpty()) {

                                ((ObjectNode) attemptRecord).put("question_id", questionPageId);
                                ((ObjectNode) attemptRecord)
                                        .putArray("part_attempts_correct")
                                        .add(qPartCounts);

                            } else {

                                Iterator<JsonNode> elements = attemptRecord.path("part_attempts_correct").elements();
                                Boolean elementExists = false;

                                while (elements.hasNext()) {

                                    JsonNode node = elements.next();

                                    if (node.path("question_part_id").asText().matches(partAttemptId)) {
                                        elementExists = true;
                                        Long currentCount = node.path("correct_attempt_count").asLong();
                                        ((ObjectNode) node).put("correct_attempt_count", currentCount + 1);
                                        break;
                                    }
                                }

                                if (!elementExists)
                                    ((ArrayNode) attemptRecord.path("part_attempts_correct")).add(qPartCounts);

                            }

                            ((ObjectNode) attemptRecord).put("latest_attempt", attemptDate);

                            return attemptRecord;

                        },
                        JsonSerde,
                        "store_user_question_attempt_count"
                )
                .toStream()

                // finally map  the (["user_id"-"question_id"], question_attempt_details) key-value pair back to a <string, json> of (user_id, question_attempt_details),
                // where question_attempt_details holds the aggregated info on the question_id and parts attempted by the user
                .map(
                        (userIdQId, jsonDoc) -> new KeyValue<>(userIdQId.substring(0, userIdQId.indexOf("-")), jsonDoc)
                )

                // we then map the value onto a AugmentedQuestionDetail json object, which includes additional question details, and a flag stating if the user has the whole question correct
                .mapValues(new AugmentedQuestionDetailMapper(contentManager, contentIndex));




        // This is the "true" correct question attempt stream, which takes into account all parts of a question
        KStream<String, JsonNode> completedQuestions = questionAttempts.filter(
                (userId, questionAttempt) -> questionAttempt.path("correct")
                        .asBoolean()
        );

        // Publish the derived "completed question" stream to a kafka topic
        //completedQuestions.to(StringSerde, JsonSerde, "topic_completed_questions");

        // total correct attempts counter (user id, number of correct answers)
        KStream<String, JsonNode> correctTotalCountStream = completedQuestions
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        new QuestionAnswerInitializer("QUESTIONS_ANSWERED_TOTAL"),
                        // aggregator
                        new QuestionAnswerCounter(),
                        JsonSerde,
                        "store_user_correct_question_total"
                )
                .toStream();

        // branch main stream into separate level streams
        KStream<String, JsonNode>[] correctLevelCountStreams =
                new KStream[6];

        for (int i = 0; i < correctLevelCountStreams.length; i++) {
            final int level = i + 1;

            correctLevelCountStreams[i] = completedQuestions
                    .filter(
                            (k, v) -> v.path("level").asInt() == level
                    ).groupByKey(StringSerde, JsonSerde)
                    .aggregate(
                            // initializer
                            new QuestionAnswerInitializer("QUESTIONS_ANSWERED_LEVEL_" + level),
                            // aggregator
                            new QuestionAnswerCounter(),
                            JsonSerde,
                            "store_user_correct_question_level_" + level
                    )
                    .toStream();
        }


        /** branch main stream into separate tag streams
         * badges for tags need to be decided beforehand it seems...
         */
        /*String[] badgeTags = {"mechanics", "waves", "fields", "circuits"};
        KStream<String, JsonNode>[] correctTagCountStreams = new KStream[badgeTags.length];

        for (int i = 0; i < badgeTags.length; i++) {
            final int tag = i;

            correctTagCountStreams[i] = completedQuestions
                    .filter(
                            (k, v) -> v.path("tags").has(badgeTags[tag])
                    ).groupByKey(stringSerde, jsonSerde)
                    .aggregate(
                        // initializer
                        new QuestionAnswerInitializer("QUESTIONS_ANSWERED_TAG_" + badgeTags[tag]),
                        // aggregator
                        new QuestionAnswerCounter(),
                        jsonSerde,
                        "userCorrectTag" + badgeTags[tag]
                    )
                    .toStream();
        }*/




        /** here is where we work out whether users have met certain thresholds for unlocking achievements.
         *  first define achievement thresholds, then filter all derived streams based on these and trigger the "unlocking process" to
         *  record the achievement for the user
         */


        // thresholds for achievement unlocking
        Integer[] thresholds = {1, 5, 10, 20, 30, 50, 75, 100, 125, 150, 200};


        // trigger process for records that match threshold (total questions answered)
        for (Integer threshold: thresholds
                ) {

            processAchievement(correctTotalCountStream, threshold, achievementProcessor);

            // trigger process for records that match threshold (per level)
            for (KStream<String, JsonNode> correctLevelCountStream: correctLevelCountStreams
                    ) {
                processAchievement(correctLevelCountStream, threshold, achievementProcessor);
            }
        }

        return stream;

    }

}

