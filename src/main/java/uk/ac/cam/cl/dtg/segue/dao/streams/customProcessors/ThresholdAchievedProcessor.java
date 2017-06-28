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

package uk.ac.cam.cl.dtg.isaac.kafka.customProcessors;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.streams.processor.Processor;
import org.apache.kafka.streams.processor.ProcessorContext;
import uk.ac.cam.cl.dtg.isaac.kafka.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

/**
 *  Custom processor to handle instances of threshold passing for data counters
 *  @author Dan Underwood
 */
public class ThresholdAchievedProcessor implements Processor<String, JsonNode> {

    private ProcessorContext context;
    private PostgresSqlDb postgresDB;


    /**
     * Threshold achievement processor
     *
     * @param postgresDB
     *            client for postgres.
     */
    public ThresholdAchievedProcessor(PostgresSqlDb postgresDB) {
        this.postgresDB = postgresDB;
    }

    @Override
    public void init(ProcessorContext context) {
        this.context = context;

        // Here you would perform any additional initializations
        // such as setting up clients etc.
    }


    /**
     * Overridden Processor method to trigger process based on an event
     * This implementation reacts to a threshold value being achieved for a certain user statistic, and inserts a user's achievements record in postgres
     *
     * @param id
     *            - kafka topic record key, which in this particular case is the user_id
     * @param details
     *            - details of the threshold achieved
     */
    @Override
    public void process(String id, JsonNode details) {

        Long userId = Long.valueOf(id);
        String achievementId = details.path("type").asText();
        Long threshold = details.path("count").asLong();
        Long timestamp = details.path("latestAttempt").asLong();

        //handle the event

        PreparedStatement pst;
        try (Connection conn = postgresDB.getDatabaseConnection()) {

            pst = conn.prepareStatement(
                    "INSERT INTO user_achievements (user_id, achievement_id, threshold, timestamp)"
                    + " VALUES (?, ?, ?, ?);"
            );

            pst.setLong(1, userId);
            pst.setString(2, achievementId);
            pst.setLong(3, threshold);
            pst.setTimestamp(4, new java.sql.Timestamp(timestamp));

            if (pst.executeUpdate() == 0) {
                System.out.println("Unable to save question attempt.");
            }


        } catch (SQLException e) {
            System.out.println(e);
        }

    }

    @Override
    public void punctuate(long timestamp) {
        // Stays empty.  In this use case there would be no need for a periodical
        // action of this processor.
    }

    @Override
    public void close() {
        // Any code for clean up would go here.
        // This processor instance will not be used again after this call.
    }
}
