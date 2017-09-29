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
import uk.ac.cam.cl.dtg.segue.dao.kafkaStreams.userAchievements.customProcessors.ThresholdAchievedProcessor;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 *  Static class to refactor out logic for user time-based activity achievement derived streams.
 *  @author Dan Underwood
 */
public final class UserWeeklyStreaks {

    private static final Serializer<JsonNode> JsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> JsonDeserializer = new JsonDeserializer();
    private static final Serde<String> StringSerde = Serdes.String();
    private static final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(JsonSerializer, JsonDeserializer);

    private UserWeeklyStreaks(){}

    private static enum Seasons {
        SPRING,
        SUMMER,
        AUTUMN,
        WINTER
    }

    /**
     * Processing of user weekly streaks based on timestamped user logged events.
     *
     * @param stream incoming raw logged event data
     * @return
     */
    public static void process(final KStream<String, JsonNode> stream, final ThresholdAchievedProcessor achievementProcessor) {

        stream
                .map(
                        (key, value) -> {

                            Long eventTimestamp = value.path("timestamp").asLong();
                            String season = getSeasonFromTimestamp(eventTimestamp);

                            return new KeyValue<>(key + "-" + season.toUpperCase() + "-" + value.path("event_type").asText().toUpperCase(), value);
                        }
                )
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode streakRecord = JsonNodeFactory.instance.objectNode();

                            streakRecord.put("streak_start", 0);
                            streakRecord.put("streak_end", 0);
                            streakRecord.put("largest_streak", 0);

                            return streakRecord;
                        },
                        // aggregator
                        (userId, latestEvent, streakRecord) -> {
                            ObjectNode updatedStreakRecord = JsonNodeFactory.instance.objectNode();

                            // timestamp of streak start
                            Long streakStartTimestamp = streakRecord.path("streak_start").asLong();

                            // timestamp of streak end
                            Long streakEndTimestamp = streakRecord.path("streak_end").asLong();

                            Integer largestStreak = streakRecord.path("largest_streak").asInt();

                            // timestamp of latest event
                            Long latestEventTimestamp = latestEvent.path("event_details")
                                    .path("date_attempted").asLong();

                            if (streakStartTimestamp == 0
                                    || (TimeUnit.DAYS.convert(latestEventTimestamp - streakEndTimestamp, TimeUnit.DAYS) > 8
                                    && !getSeasonFromTimestamp(latestEventTimestamp).equals(getSeasonFromTimestamp(streakEndTimestamp)))) {

                                updatedStreakRecord.put("streak_start", latestEventTimestamp);
                                updatedStreakRecord.put("streak_end", latestEventTimestamp);

                            } else {
                                updatedStreakRecord.put("streak_start", streakStartTimestamp);
                                updatedStreakRecord.put("streak_end", latestEventTimestamp);
                            }

                            if (TimeUnit.DAYS.convert(latestEventTimestamp - streakStartTimestamp, TimeUnit.DAYS) / 7 > largestStreak)
                                updatedStreakRecord.put("updated", true);

                            return updatedStreakRecord;
                        },
                        JsonSerde,
                        "store_user_streaks")
                .toStream()
                .filter(
                        (userId, streakRecord) -> streakRecord.path("updated").asBoolean()
                )
                .map(
                        (userIdSeasonEventTypeKey, streakRecord) -> {

                            ObjectNode achievementValue = JsonNodeFactory.instance.objectNode();

                            achievementValue.put("latest_attempt", streakRecord.path("streak_end").asLong());
                            achievementValue.put("count", streakRecord.path("largest_streak").asInt());

                            String[] keyArray = userIdSeasonEventTypeKey.split("-");

                            achievementValue.put("type", "ACTIVITY_STREAK_" + keyArray[1] + "_" + keyArray[2]);

                            return new KeyValue<>(keyArray[0], (JsonNode) achievementValue);
                        }
                ).process(
                () -> achievementProcessor
        );

                /*.mapValues(
                        (jsonTimestamps) -> TimeUnit.DAYS
                                .convert(jsonTimestamps.path("streak_end").asLong() - jsonTimestamps.path("streak_start").asLong(),
                                        TimeUnit.DAYS) / 7
                )*/
    }


    private static String getSeasonFromTimestamp(Long timestamp) {

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        switch (cal.get(Calendar.MONTH)) {

            case 8:case 9:case 10:
                return Seasons.AUTUMN.name();
            case 11:case 0:case 1:
                return Seasons.WINTER.name();
            case 2:case 3:case 4:
                return Seasons.SPRING.name();
            default:
                return Seasons.SUMMER.name();

        }
    }

}
