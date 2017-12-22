package uk.ac.cam.cl.dtg.segue.dao.kafkaStreams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.streams.kstream.Initializer;

/**
 * Initializer class to specify structure of user snapshot document stored in kafka state store
 */
public class UserStatisticsSnapshotInitializer implements Initializer<JsonNode> {
    @Override
    public JsonNode apply() {

        ObjectNode userSnapshot = JsonNodeFactory.instance.objectNode();

        /*USER STREAK DATA*/
        ObjectNode streakRecord = JsonNodeFactory.instance.objectNode();
        streakRecord.put("streak_start", 0);
        streakRecord.put("streak_end", 0);
        streakRecord.put("largest_streak", 0);
        streakRecord.put("current_activity", 0);
        streakRecord.put("activity_threshold", 3);

        userSnapshot.put("streak_record", streakRecord);

        return userSnapshot;
    }
}
