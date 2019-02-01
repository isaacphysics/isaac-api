/*
 * Copyright 2018 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.scheduler;

import com.google.inject.Inject;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.quartz.CronScheduleBuilder.cronSchedule;

public class SegueJobService implements ServletContextListener {

    private final Scheduler scheduler;

    private final List<SegueScheduledJob> localRegisteredJobs;

    private static final Logger log = LoggerFactory.getLogger(SegueJobService.class);

    /**
     * A job manager that can execute jobs on a schedule or by trigger.
     * @param staticallyConfiguredScheduledJobs Collection of statically configured jobs
     */
    @Inject
    public SegueJobService(Collection<SegueScheduledJob> staticallyConfiguredScheduledJobs) {
        this.localRegisteredJobs = new ArrayList<>();
        StdSchedulerFactory stdSchedulerFactory = new StdSchedulerFactory();

        try {
            scheduler = stdSchedulerFactory.getScheduler();

            // register statically configured jobs
            this.registerScheduledJobs(staticallyConfiguredScheduledJobs);

            scheduler.start();
        } catch (SchedulerException e) {
            throw new RuntimeException("Unable to initialise the scheduler", e);
        }
    }

    /**
     * Handy method to register a collection of Scheduled jobs
     * @param allJobs to register
     * @throws SchedulerException if something goes wrong
     */
    public void registerScheduledJobs(Collection<SegueScheduledJob> allJobs) throws SchedulerException {
        for (SegueScheduledJob s : allJobs) {
            this.registerScheduleJob(s);
        }
    }

    /**
     * Register or replace the trigger for a single scheduled job.
     *
     * @param jobToRegister
     *            add to the queue
     */
    public void registerScheduleJob(final SegueScheduledJob jobToRegister) throws SchedulerException {
        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity(jobToRegister.getJobKey() + "_trigger", jobToRegister.getJobGroupName())
                .withSchedule(cronSchedule(jobToRegister.getCronString())).build();

        JobDetail job = JobBuilder.newJob(jobToRegister.getExecutableTask().getClass())
                .withIdentity(jobToRegister.getJobKey(),jobToRegister.getJobGroupName())
                .setJobData(new JobDataMap(jobToRegister.getExecutionContext()))
                .withDescription(jobToRegister.getJobDescription()).build();

        scheduler.getContext().put(jobToRegister.getExecutableTask().getClass().getName(), jobToRegister.getExecutionContext());

        if(!scheduler.checkExists(job.getKey())) {
            scheduler.scheduleJob(job, cronTrigger);
            log.info(String.format("Registered job (%s) to segue job execution service. Current jobs registered (%s): ", jobToRegister.getJobKey(), localRegisteredJobs.size()));
        } else {
            scheduler.rescheduleJob(cronTrigger.getKey(), cronTrigger);
            log.info(String.format("Re-registered job (%s) to segue job execution service. Current jobs registered (%s): ", jobToRegister.getJobKey(), localRegisteredJobs.size()));
        }

        localRegisteredJobs.add(jobToRegister);
    }

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        log.info("Segue Job Service Initialised");
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        try {
            log.info("Shutting down segue scheduler");
            this.scheduler.shutdown(true);
        } catch (SchedulerException e) {
            log.error("Error while attempting to shutdown Segue Scheduler.", e);
        }
    }
}
