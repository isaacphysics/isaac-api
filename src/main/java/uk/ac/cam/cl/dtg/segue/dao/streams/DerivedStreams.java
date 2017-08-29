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


import java.sql.Timestamp;
import java.util.Calendar;

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



    /**
     * Processes an incoming stream of logged events to update a number of internal state stores
     * useful for providing site usage statistics.
     *
     * @param stream
     *          - incoming logged events stream
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
}

