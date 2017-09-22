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

package uk.ac.cam.cl.dtg.segue.dao.streams.customAggregators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.streams.kstream.Aggregator;

/**
 * Created by du220 on 16/06/2017.
 */
public class QuestionAnswerCounter implements Aggregator<String, JsonNode, JsonNode> {

    public QuestionAnswerCounter() {

    }

    @Override
    public JsonNode apply(final String id, final JsonNode latestEvent, final JsonNode aggregatorRecord) {

        if (aggregatorRecord.path("user_id").asText().isEmpty()) {
            // annoyingly have to remove stringified escape character
            ((ObjectNode) aggregatorRecord).put("user_id", id.replace("\"", ""));
        }

        Long currentCount = aggregatorRecord.path("count").asLong();

        ((ObjectNode) aggregatorRecord).put("count", currentCount + 1);
        ((ObjectNode) aggregatorRecord).put("latest_attempt", latestEvent.path("latest_attempt").asText());

        return aggregatorRecord;
    }

}
