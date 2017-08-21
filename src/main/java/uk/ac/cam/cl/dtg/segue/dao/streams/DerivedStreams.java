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
import org.apache.kafka.streams.kstream.*;
import java.util.List;


/**
 *  Static class to refactor out logic for derived streams.
 *  @author Dan Underwood
 */
public final class DerivedStreams {


    private static final Serializer<JsonNode> JsonSerializer = new JsonSerializer();
    private static final Deserializer<JsonNode> JsonDeserializer = new JsonDeserializer();
    private static final Serde<JsonNode> JsonSerde = Serdes.serdeFrom(JsonSerializer, JsonDeserializer);
    private static final Serde<String> StringSerde = Serdes.String();


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
     * Processing of user notifications based on log event triggers.
     * Notification messages are derived based on a log event type and sent to a notification topic as well
     * as a state store for (user) offline aggregation
     *
     * @param stream raw log event stream
     * @return stream
     */
    public static KStream<String, JsonNode> userNotifications(final KStream<String, JsonNode> stream) {

        String userNotificationTopic = "topic_user_notifications";
        String userNotificationStore = "store_user_notifications";


        // welcome message
        KStream<String, JsonNode> welcomeNotifications = filterByEventType(stream, "USER_REGISTRATION")
                .mapValues(
                        (value) -> {
                            ObjectNode userNotification = JsonNodeFactory.instance.objectNode();

                            userNotification.put("message", "Welcome to Isaac Physics!");
                            userNotification.put("status", "DELIVERED");
                            userNotification.put("type", value.path("event_type"));
                            userNotification.put("timestamp", value.path("timestamp"));

                            return (JsonNode) userNotification;
                        }
                );

        // write to the user notification topic while setting up a KStream instance based on that topic
        KStream<String, JsonNode> userNotificationStream = welcomeNotifications
                .through(StringSerde, JsonSerde, userNotificationTopic);

        // achievement unlocked
        KStream<String, JsonNode> test = filterByEventType(stream, "VIEW_GAMEBOARD_BY_ID")
                .mapValues(
                        (value) -> {
                            ObjectNode userNotification = JsonNodeFactory.instance.objectNode();

                            userNotification.put("message", "You have viewed a gameboard!");
                            userNotification.put("status", "DELIVERED");
                            userNotification.put("type", value.path("event_type"));
                            userNotification.put("timestamp", value.path("timestamp"));

                            return (JsonNode) userNotification;
                        }
                );

        test.to(StringSerde, JsonSerde, userNotificationTopic);

        // achievement unlocked
        KStream<String, JsonNode> achievementNotifications = filterByEventType(stream, "ACHIEVEMENT_UNLOCKED")
                .mapValues(
                        (value) -> {
                            ObjectNode userNotification = JsonNodeFactory.instance.objectNode();

                            userNotification.put("message", "You have unlocked an achievement!");
                            userNotification.put("status", "DELIVERED");
                            userNotification.put("type", value.path("event_type"));
                            userNotification.put("timestamp", value.path("timestamp"));

                            return (JsonNode) userNotification;
                        }
                );

        achievementNotifications.to(StringSerde, JsonSerde, userNotificationTopic);


        // notifications seen by user (perhaps could be done another way)
        KStream<String, JsonNode> userSeenNotifications = filterByEventType(stream, "VIEW_NOTIFICATIONS")
                .mapValues(
                        (value) -> {
                            ObjectNode userNotification = JsonNodeFactory.instance.objectNode();

                            // user has seen notifications, so sends flag to delete user notification state store records
                            userNotification.put("status", "RECEIVED");

                            return (JsonNode) userNotification;
                        }
                );



        // for all of the user notifications, aggregate into state store to persist them
        userNotificationStream
                .groupByKey(StringSerde, JsonSerde)
                .aggregate(
                        // initializer
                        () -> {
                            ObjectNode notificationsRecord = JsonNodeFactory.instance.objectNode();

                            notificationsRecord.putArray("notifications");
                            return notificationsRecord;

                        },
                        // aggregator
                        (userId, notificationEvent, userNotificationRecord) -> {

                            if (notificationEvent.path("status").asText().matches("DELIVERED")) {
                                ((ArrayNode) userNotificationRecord.path("notifications")).add(notificationEvent);
                            } else {

                                // remove records until only at most 10 most recent remain
                                while (((ArrayNode) userNotificationRecord.path("notifications")).size() > 10) {
                                    ((ArrayNode) userNotificationRecord.path("notifications")).remove(0);
                                }

                                //((ArrayNode) userNotificationRecord.path("notifications")).removeAll();
                            }

                            return userNotificationRecord;

                        },
                        JsonSerde,
                        userNotificationStore);


        return stream;
    }

}

