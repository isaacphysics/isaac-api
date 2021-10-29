/*
 * Copyright 2019 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.ExternalAccountSynchronisationException;
import uk.ac.cam.cl.dtg.segue.api.managers.IExternalAccountManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicationMessage;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SyncMailjetUsersJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(SyncMailjetUsersJob.class);

    private final IExternalAccountManager externalAccountManager;
    private final EmailManager emailManager;
    private final PropertiesLoader properties;

    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    public SyncMailjetUsersJob() {
        Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
        emailManager = injector.getInstance(EmailManager.class);
        externalAccountManager = injector.getInstance(IExternalAccountManager.class);
        properties = injector.getInstance(PropertiesLoader.class);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            externalAccountManager.synchroniseChangedUsers();
            log.info("Success: synchronised users");
        } catch (ExternalAccountSynchronisationException e) {
            final String subject = "Failed to execute SyncMailjetUsersJob";
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter= new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            String exception = stringWriter.toString();
            EmailCommunicationMessage email = new EmailCommunicationMessage(properties.getProperty(Constants.SERVER_ADMIN_ADDRESS),
                    subject, exception, exception, EmailType.ADMIN);
            emailManager.addSystemEmailToQueue(email);
            log.error("Failed to synchronise users");
        }

    }
}
