/*
 * Copyright 2016 James Sharkey
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
package uk.ac.cam.cl.dtg.segue.api.monitors;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

public class QuestionAttemptMisuseHandler implements IMisuseHandler {

    private static final Logger log = LoggerFactory.getLogger(QuestionAttemptMisuseHandler.class);

    private static final Integer SOFT_THRESHOLD = 10;
    private static final Integer HARD_THRESHOLD = 15;
    private static final Integer ACCOUNTING_INTERVAL = Constants.NUMBER_SECONDS_IN_FIFTEEN_MINUTES;

    private Integer overrideHardThreshold;


    /**
     * @param properties
     *            - so that we can look up properties set.
     */
    @Inject
    public QuestionAttemptMisuseHandler(final PropertiesLoader properties) {
        String overrideThresholdString = properties.getProperty(Constants.QUESTION_MISUSE_THRESHOLD_OVERRIDE);
        if (null != overrideThresholdString) {
            this.overrideHardThreshold = Integer.parseInt(overrideThresholdString);
        }
    }

    @Override
    public Integer getSoftThreshold() {
        return SOFT_THRESHOLD;
    }

    @Override
    public Integer getHardThreshold() {
        if (null != overrideHardThreshold) {
            return overrideHardThreshold;
        }
        return HARD_THRESHOLD;
    }

    @Override
    public Integer getAccountingIntervalInSeconds() {
        return ACCOUNTING_INTERVAL;
    }

    @Override
    public void executeSoftThresholdAction(final String message) {}

    @Override
    public void executeHardThresholdAction(final String message) {
        log.warn("Hard threshold limit: " + message);
    }
}
