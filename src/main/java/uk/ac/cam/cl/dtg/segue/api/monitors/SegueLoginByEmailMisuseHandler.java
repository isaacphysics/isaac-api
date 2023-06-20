/**
 * Copyright 2015 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
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

import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_TEN_MINUTES;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseLogValue;

/**
 * Handler to detect bruteforce login attempts.
 * <p>
 * Preventing users from overusing this endpoint is important as they may be trying to brute force someones password.
 */
public class SegueLoginByEmailMisuseHandler implements IMisuseHandler {
    private static final Logger log = LoggerFactory.getLogger(SegueLoginByEmailMisuseHandler.class);

    public final Integer SOFT_THRESHOLD;
    public final Integer HARD_THRESHOLD;
    public final Integer ACCOUNTING_INTERVAL;

    @Inject
    public SegueLoginByEmailMisuseHandler() {
        this(5, 10, NUMBER_SECONDS_IN_TEN_MINUTES);
    }

    @Inject
    public SegueLoginByEmailMisuseHandler(Integer softThreshold, Integer hardThreshold, Integer interval) {
        this.SOFT_THRESHOLD = softThreshold;
        this.HARD_THRESHOLD = hardThreshold;
        this.ACCOUNTING_INTERVAL = interval;
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
        log.warn("Soft threshold limit: " + sanitiseLogValue(message));
    }

    @Override
    public void executeHardThresholdAction(final String message) {
        log.warn("Hard threshold limit: " + sanitiseLogValue(message));
    }
}