/*
 * Copyright 2021 James Sharkey
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
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Handler to deal with email verification requests.
 * <p>
 * Preventing an IP address scanning many email addresses for account existence
 * by limiting reset requests on a per-IP basis.
 */
public class PasswordResetByIPMisuseHandler implements IMisuseHandler {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetByIPMisuseHandler.class);

    private final Integer softThreshold;
    private final Integer hardThreshold;
    private final Integer accountingInterval;

    private final PropertiesLoader properties;
    private final EmailManager emailManager;

    @Inject
    public PasswordResetByIPMisuseHandler(final EmailManager emailManager, final PropertiesLoader properties) {
        this(emailManager, properties, PASSWORD_RESET_BY_IP_DEFAULT_SOFT_THRESHOLD, PASSWORD_RESET_BY_IP_DEFAULT_HARD_THRESHOLD, NUMBER_SECONDS_IN_ONE_HOUR);
    }

    @Inject
    public PasswordResetByIPMisuseHandler(final EmailManager emailManager, final PropertiesLoader properties,
                                          final Integer softThreshold, final Integer hardThreshold, final Integer interval) {
        this.properties = properties;
        this.emailManager = emailManager;
        this.softThreshold = softThreshold;
        this.hardThreshold = hardThreshold;
        this.accountingInterval = interval;
    }

    @Override
    public Integer getSoftThreshold() {
        return softThreshold;
    }

    /*
     * (non-Javadoc)
     *
     * @see uk.ac.cam.cl.dtg.segue.api.managers.IMisuseEvent#getHardThreshold()
     */
    @Override
    public Integer getHardThreshold() {
        return hardThreshold;
    }

    @Override
    public Integer getAccountingIntervalInSeconds() {
        return accountingInterval;
    }

    @Override
    public void executeSoftThresholdAction(final String message) {
        final String subject = "Soft Threshold limit reached for IP Password Reset endpoint";
        EmailCommunicationMessage e = new EmailCommunicationMessage(
                properties.getProperty(Constants.SERVER_ADMIN_ADDRESS), subject, message, message, EmailType.ADMIN);
        emailManager.addSystemEmailToQueue(e);
        log.warn("Soft threshold limit: " + message);
    }

    @Override
    public void executeHardThresholdAction(final String message) {
        final String subject = "HARD Threshold limit reached for IP Password Reset endpoint";
        EmailCommunicationMessage e = new EmailCommunicationMessage(
                properties.getProperty(Constants.SERVER_ADMIN_ADDRESS), subject, message, message, EmailType.ADMIN);
        emailManager.addSystemEmailToQueue(e);
        log.error("Hard threshold limit: " + message);
    }

}
