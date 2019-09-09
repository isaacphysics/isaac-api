/**
 * Copyright 2017 James Sharkey
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

public class IPQuestionAttemptMisuseHandler implements IMisuseHandler {

    private static final Logger log = LoggerFactory.getLogger(IPQuestionAttemptMisuseHandler.class);

    private static final Integer SOFT_THRESHOLD = 120;  // Two attempts per minute for an hour, or 24 anonymous users.
    private static final Integer HARD_THRESHOLD = 600;  // One every six seconds for an hour; far too high!
    private static final Integer ACCOUNTING_INTERVAL = Constants.NUMBER_SECONDS_IN_ONE_HOUR;

    private PropertiesLoader properties;
    private EmailManager emailManager;

    /**
     * @param emailManager
     *            - so we can send e-mails if the threshold limits have been reached.
     * @param properties
     *            - so that we can look up properties set.
     */
    @Inject
    public IPQuestionAttemptMisuseHandler(final EmailManager emailManager, final PropertiesLoader properties) {
        this.properties = properties;
        this.emailManager = emailManager;
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
    public void executeSoftThresholdAction(final String message) {
        log.warn("Too many requests from an IP Address: " + message);
    }

    @Override
    public void executeHardThresholdAction(final String message) {
        final String subject = "HARD Threshold limit reached for IP Address based Question Attempts!";
        EmailCommunicationMessage e = new EmailCommunicationMessage(properties.getProperty(Constants.SERVER_ADMIN_ADDRESS),
                subject, message, message, EmailType.ADMIN);
        try {
            emailManager.addSystemEmailToQueue(e);
        } catch (SegueDatabaseException e1) {
            log.error("Database error when attempting to send threshold limit warnings: " + e1.getMessage());
        }
        log.warn("Too many requests from an IP Address: " + message + " This may be a scripted attack!");
    }
}
