/**
 * Copyright 2015 Stephen Cummins
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicationMessage;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.Inject;

/**
 * Handler to detect users storing large amounts of data in our log database.
 * 
 * Preventing users from overusing this endpoint is important as they may be trying to brute force someones password.
 * 
 * NOTE: The accounting unit for this handler is bytes.
 */
public class LogEventMisuseHandler implements IMisuseHandler {
    private static final Logger log = LoggerFactory.getLogger(LogEventMisuseHandler.class);

    public static final Integer SOFT_THRESHOLD = 700000; // bytes
    public static final Integer ACCOUNTING_INTERVAL = Constants.NUMBER_SECONDS_IN_ONE_DAY;

    private final PropertiesLoader properties;
    private final EmailManager emailManager;

    /**
     * @param emailManager
     *            - so we can send e-mails if the threshold limits have been reached.
     * @param properties
     *            - so that we can look up properties set.
     */
    @Inject
    public LogEventMisuseHandler(final EmailManager emailManager, final PropertiesLoader properties) {
        this.properties = properties;
        this.emailManager = emailManager;
    }

    @Override
    public Integer getSoftThreshold() {
        return SOFT_THRESHOLD;
    }

    @Override
    public Integer getHardThreshold() {
        return Constants.MAX_LOG_REQUEST_BODY_SIZE_IN_BYTES;
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
        final String subject = "HARD Threshold limit reached for: LogEventMisuseHandler -- Log Data Requests too large";

        EmailCommunicationMessage e = new EmailCommunicationMessage(null,
                properties.getProperty(Constants.SERVER_ADMIN_ADDRESS), subject, message, message, EmailType.ADMIN,
                null, null, null);

        try {
			emailManager.addSystemEmailToQueue(e);
		} catch (SegueDatabaseException e1) {
			log.error("Database access error when attempting to send hard threshold limit warnings: " 
								+ e1.getMessage());
		}
        log.warn("Hard threshold limit: " + message);
    }
}
