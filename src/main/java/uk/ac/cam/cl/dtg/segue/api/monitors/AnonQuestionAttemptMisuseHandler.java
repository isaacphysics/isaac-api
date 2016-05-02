/**
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
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicationMessage;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

public class AnonQuestionAttemptMisuseHandler implements IMisuseHandler {

    private static final Logger log = LoggerFactory.getLogger(AnonQuestionAttemptMisuseHandler.class);

    private static final Integer SOFT_THRESHOLD = 3;
    private static final Integer HARD_THRESHOLD = 5;
    private static final Integer ACCOUNTING_INTERVAL = Constants.NUMBER_SECONDS_IN_FIVE_MINUTES;

    /**
     */
    @Inject
    public AnonQuestionAttemptMisuseHandler() {
    }

    @Override
    public Integer getSoftThreshold() {
        return SOFT_THRESHOLD;
    }

    @Override
    public Integer getHardThreshold() {
        return HARD_THRESHOLD;
    }

    @Override
    public Integer getAccountingIntervalInSeconds() {
        return ACCOUNTING_INTERVAL;
    }

    @Override
    public void executeSoftThresholdAction(String message) {
//        log.warn("Soft threshold limit reached for AnonQuestionAttemptMisuseHandler " + message);
    }

    @Override
    public void executeHardThresholdAction(String message) {
        log.warn("Hard threshold limit reached for AnonQuestionAttemptMisuseHandler: " + message);
    }
}
