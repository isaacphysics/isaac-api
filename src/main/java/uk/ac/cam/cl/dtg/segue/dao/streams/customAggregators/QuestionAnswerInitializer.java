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

package uk.ac.cam.cl.dtg.isaac.kafka.customAggregators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.streams.kstream.Initializer;

/**
 * Created by du220 on 16/06/2017.
 */
public class QuestionAnswerInitializer implements Initializer<JsonNode> {

    private String countedMeasure;

    public QuestionAnswerInitializer(String countedMeasure) {
        this.countedMeasure = countedMeasure;
    }


    @Override
    public JsonNode apply() {

        ObjectNode countedMeasureRecord = JsonNodeFactory.instance.objectNode();

        countedMeasureRecord.put("userId", "");
        countedMeasureRecord.put("type", countedMeasure);
        countedMeasureRecord.put("count", 0);
        countedMeasureRecord.put("latestAttempt", "");

        return countedMeasureRecord;

    }


}
