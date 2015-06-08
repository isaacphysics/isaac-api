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
package uk.ac.cam.cl.dtg.segue.api.managers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseHandler;
import uk.ac.cam.cl.dtg.segue.comm.CommunicationException;
import uk.ac.cam.cl.dtg.segue.comm.ICommunicator;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Handler to deal with token ownership requests.
 * 
 * Preventing users from overusing this endpoint is important as some email address information is exposed
 * for identity verification purposes.
 *
 */
public class TokenOwnerLookupMisuseHandler implements IMisuseHandler {
    private static final Logger log = LoggerFactory.getLogger(TokenOwnerLookupMisuseHandler.class);
    
    public static final Integer SOFT_THRESHOLD = 50;
    public static final Integer HARD_THRESHOLD = 200;
    public static final Integer ACCOUNTING_INTERVAL = 86400;

    private ICommunicator communicator;
    private PropertiesLoader properties;
    
    /**
     * @param communicator
     *            - so we can send e-mails if the threshold limits have been reached.
     * @param properties
     *            - so that we can look up properties set.
     */
    @Inject
    public TokenOwnerLookupMisuseHandler(final ICommunicator communicator,
            final PropertiesLoader properties) {
        this.communicator = communicator;
        this.properties = properties;
    }
    
    @Override
    public Integer getSoftThreshold() {
        return SOFT_THRESHOLD;
    }

    /* (non-Javadoc)
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
        final String subject = "Soft Threshold limit reached for TokenOwnershipRequest endpoint";
        try {
            communicator.sendMessage(properties.getProperty(Constants.MAIL_FROM_ADDRESS), 
                    properties.getProperty(Constants.MAIL_FROM_ADDRESS), subject, message);
            log.warn("Soft threshold limit reached" + message);
        } catch (CommunicationException e) {
            log.error("Unable to send email to alert admin of soft threshold warning.", e);
        }
    }

    @Override
    public void executeHardThresholdAction(final String message) {
        final String subject = "HARD Threshold limit reached for TokenOwnershipRequest endpoint";
        try {
            communicator.sendMessage(properties.getProperty(Constants.MAIL_FROM_ADDRESS), 
                    properties.getProperty(Constants.MAIL_FROM_ADDRESS), subject, message);
            log.warn("Hard threshold limit reached" + message);
        } catch (CommunicationException e) {
            log.error("Unable to send email to alert admin of soft threshold warning.", e);
        }
    }
}
