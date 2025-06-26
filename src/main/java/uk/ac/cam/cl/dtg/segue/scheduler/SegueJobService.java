/*
 * Copyright 2018 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.scheduler;

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
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import jakarta.annotation.Nullable;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.quartz.CronScheduleBuilder.cronSchedule;

public class SegueJobService implements ServletContextListener {

    private final Scheduler scheduler;

    private final List<SegueScheduledJob> allKnownJobs;
    private final List<SegueScheduledJob> localRegisteredJobs;
    private final List<SegueScheduledJob> jobsToRemove;

    private static final Logger log = LoggerFactory.getLogger(SegueJobService.class);

    /**
     * A job manager that can execute jobs on a schedule or by trigger.
     * @param database the Postgres database used as a job store.
     * @param allKnownJobs collection of jobs to register
     * @param jobsToRemove collection of possibly-existing jobs to remove/deregister
     * @param disableAutostart whether to prevent automatically starting Quartz
     */
    public SegueJobService(final PostgresSqlDb database, final List<SegueScheduledJob> allKnownJobs,
                           @Nullable final List<SegueScheduledJob> jobsToRemove,
                           final Boolean disableAutostart) {
        this.allKnownJobs = allKnownJobs;
        this.jobsToRemove = jobsToRemove;
        this.localRegisteredJobs = new ArrayList<>();
        StdSchedulerFactory stdSchedulerFactory = new StdSchedulerFactory();

        try {
            scheduler = stdSchedulerFactory.getScheduler();
            if (disableAutostart) {
                log.warn("Segue Job Service: Not starting, prevented by configuration.");
            } else if (database.isReadOnlyReplica()) {
                log.warn("Segue Job Service: Not starting, readonly database.");
            } else {
                initialiseService();
            }
        } catch (SchedulerException | SQLException e) {
            throw new RuntimeException("Segue Job Service: Failed to schedule quartz jobs or start scheduler! Aborting!", e);
        }
    }

    /**
     * Handy method to register a collection of Scheduled jobs.
     *
     * @param allJobs to register
     * @throws SchedulerException if something goes wrong
     */
    public void registerScheduledJobs(final Collection<SegueScheduledJob> allJobs) throws SchedulerException {
        for (SegueScheduledJob s : allJobs) {
            this.registerScheduleJob(s);
        }
    }

    /**
     * Remove an already-existing job from the Scheduler's list of scheduled jobs.
     *
     * If the job isn't already registered, this is safe and will have no effect.
     *
     * @param jobToRemove the job to remove
     * @throws SchedulerException if the job cannot be de-registered
     */
    public void removeScheduleJob(final SegueScheduledJob jobToRemove) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(jobToRemove.getExecutableTask().getClass())
                .withIdentity(jobToRemove.getJobKey(), jobToRemove.getJobGroupName())
                .setJobData(new JobDataMap(jobToRemove.getExecutionContext()))
                .withDescription(jobToRemove.getJobDescription()).build();

        scheduler.getContext().remove(jobToRemove.getExecutableTask().getClass().getName(), jobToRemove.getExecutionContext());

        boolean deletionNeeded = scheduler.deleteJob(job.getKey());

        localRegisteredJobs.remove(jobToRemove);

        if (deletionNeeded) {
            log.info("Removed existing job: {}", jobToRemove.getJobKey());
        } else {
            log.info("Skipping removal of non-registered job: {}", jobToRemove.getJobKey());
        }
    }

    public void removeScheduledJobs(final List<SegueScheduledJob> jobsToRemoveList) throws SchedulerException {
        if (jobsToRemoveList == null) {
            return;
        }
        for (SegueScheduledJob jobToRemove : jobsToRemoveList) {
            this.removeScheduleJob(jobToRemove);
        }
    }

    /**
     * Register a new job or update the trigger of an existing job.
     *
     * This currently does not alter the job details!
     *
     * @param jobToRegister the job to schedule
     * @throws SchedulerException if the job could not be registered
     */
    public void registerScheduleJob(final SegueScheduledJob jobToRegister) throws SchedulerException {
        CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity(jobToRegister.getJobKey() + "_trigger", jobToRegister.getJobGroupName())
                .withSchedule(cronSchedule(jobToRegister.getCronString())).build();

        JobDetail job = JobBuilder.newJob(jobToRegister.getExecutableTask().getClass())
                .withIdentity(jobToRegister.getJobKey(), jobToRegister.getJobGroupName())
                .setJobData(new JobDataMap(jobToRegister.getExecutionContext()))
                .withDescription(jobToRegister.getJobDescription()).build();

        scheduler.getContext().put(jobToRegister.getExecutableTask().getClass().getName(), jobToRegister.getExecutionContext());

        if (!scheduler.checkExists(job.getKey()) || !scheduler.checkExists(cronTrigger.getKey())) {
            scheduler.scheduleJob(job, cronTrigger);
            log.info("Registered Quartz job ({}). Current jobs registered ({}): ", jobToRegister.getJobKey(), localRegisteredJobs.size());
        } else {
            CronTrigger existingTrigger = (CronTrigger) scheduler.getTrigger(cronTrigger.getKey());
            if (!Objects.equals(existingTrigger.getCronExpression(), cronTrigger.getCronExpression())) {
                // FIXME - this does not update the job details, e.g. if the SQL file name changes.
                scheduler.rescheduleJob(cronTrigger.getKey(), cronTrigger);
                log.info("Re-registered Quartz job ({}). Current jobs registered ({}): ", jobToRegister.getJobKey(), localRegisteredJobs.size());
            } else {
                log.info("Skipping re-registering existing Quartz job ({}). Current jobs registered ({}): ", jobToRegister.getJobKey(), localRegisteredJobs.size());
            }
        }

        localRegisteredJobs.add(jobToRegister);
    }

    /**
     * Checks if the scheduler has been started already.
     *
     * @return true if scheduler has already started, even if it has subsequently been shut down.
     */
    public boolean wasStarted() {
        try {
            return scheduler.isStarted();
        } catch (SchedulerException e) {
            return false;
        }
    }

    /**
     * Checks if the scheduler has been shut down.
     *
     * @return true if scheduler shut down.
     */
    public boolean isShutdown() {
        try {
            return scheduler.isShutdown();
        } catch (SchedulerException e) {
            return false;
        }
    }

    /**
     *  Attempt to register configured jobs, remove any jobs marked for removal, then start scheduler.
     */
    public synchronized void initialiseService() throws SchedulerException {
        if (!wasStarted()) {
            this.registerScheduledJobs(allKnownJobs);
            this.removeScheduledJobs(jobsToRemove);

            scheduler.start();
            log.info("Segue Job Service started.");
        }
    }

    /**
     * Attempt to complete running jobs, then shut down the scheduler.
     * @throws SchedulerException on failure to terminate.
     */
    public synchronized void shutdownService() throws SchedulerException {
        if (wasStarted()) {
            log.info("Shutting down Segue Job Service");
            this.scheduler.shutdown(true);
        }
    }

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        try {
            log.info("Shutting down Segue Job Service");
            this.scheduler.shutdown(true);
        } catch (SchedulerException e) {
            log.error("Error while attempting to shutdown Segue Scheduler.", e);
        }
    }
}
