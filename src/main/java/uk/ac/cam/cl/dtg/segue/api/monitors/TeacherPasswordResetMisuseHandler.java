/**
 * Copyright 2020 Connor Holloway
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

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Handler to deal with teacher password reset requests.
 * 
 * Separate from PasswordResetRequestMisuseHandler since teachers may need to reset the passwords of numerous
 * students within a short period of time.
 * 
 * @author Connor Holloway
 *
 */
public class TeacherPasswordResetMisuseHandler implements IMisuseHandler {

    private static final Logger log = LoggerFactory.getLogger(TeacherPasswordResetMisuseHandler.class);

    private static final Integer SOFT_THRESHOLD = 15;
    private static final Integer HARD_THRESHOLD = 30;
    private static final Integer ACCOUNTING_INTERVAL = NUMBER_SECONDS_IN_ONE_HOUR;

    /**
     *
     */
    @Inject
    public TeacherPasswordResetMisuseHandler() {

    }
    

    @Override
    public Integer getSoftThreshold() {
        return SOFT_THRESHOLD;
    }

    /*
     * (non-Javadoc)
     * 
     * @see uk.ac.cam.cl.dtg.segue.api.managers.IMisuseEvent#getHardThreshold()
     */
    @Override
    public Integer getHardThreshold() {
        return HARD_THRESHOLD;
    }

    @Override
    public Integer getAccountingIntervalInSeconds() {
        return ACCOUNTING_INTERVAL;
    }

    @Override
    public void executeSoftThresholdAction(final String message) {
        log.warn("Soft threshold limit: " + message);
    }

    @Override
    public void executeHardThresholdAction(final String message) {
        log.error("Hard threshold limit: " + message);
    }

}
