/**
 * Copyright 2018 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.scheduler;

import static org.quartz.CronScheduleBuilder.cronSchedule;

import com.google.inject.Inject;
import jakarta.annotation.Nullable;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

public class SegueJobService implements ServletContextListener {

  private final Scheduler scheduler;

  private final List<SegueScheduledJob> allKnownJobs;
  private final List<SegueScheduledJob> localRegisteredJobs;
  private final List<SegueScheduledJob> jobsToRemove;

  private static final Logger log = LoggerFactory.getLogger(SegueJobService.class);

  /**
   * A job manager that can execute jobs on a schedule or by trigger.
   *
   * @param database     the Postgres database used as a job store.
   * @param allKnownJobs collection of jobs to register
   * @param jobsToRemove collection of possibly-existing jobs to remove/deregister
   */
  @Inject
  public SegueJobService(final PostgresSqlDb database, final List<SegueScheduledJob> allKnownJobs,
                         @Nullable final List<SegueScheduledJob> jobsToRemove) {
    this.allKnownJobs = allKnownJobs;
    this.jobsToRemove = jobsToRemove;
    this.localRegisteredJobs = new ArrayList<>();
    StdSchedulerFactory stdSchedulerFactory = new StdSchedulerFactory();

    try {
      scheduler = stdSchedulerFactory.getScheduler();
      if (!database.isReadOnlyReplica()) {
        initialiseService();
      } else {
        log.warn("Segue Job Service: Not starting due to readonly database.");
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
   * <p>
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

    scheduler.getContext()
        .remove(jobToRemove.getExecutableTask().getClass().getName(), jobToRemove.getExecutionContext());

    boolean deletionNeeded = scheduler.deleteJob(job.getKey());

    localRegisteredJobs.remove(jobToRemove);

    if (deletionNeeded) {
      log.info(String.format("Removed existing job: %s", jobToRemove.getJobKey()));
    } else {
      log.info(String.format("Skipping removal of non-registered job: %s", jobToRemove.getJobKey()));
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
   * <p>
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

    scheduler.getContext()
        .put(jobToRegister.getExecutableTask().getClass().getName(), jobToRegister.getExecutionContext());

    if (!scheduler.checkExists(job.getKey())) {
      scheduler.scheduleJob(job, cronTrigger);
      log.info(String.format("Registered job (%s) to segue job execution service. Current jobs registered (%s): ",
          jobToRegister.getJobKey(), localRegisteredJobs.size()));
    } else {
      // FIXME - this does not update the job details, e.g. if the SQL file name changes.
      scheduler.rescheduleJob(cronTrigger.getKey(), cronTrigger);
      log.info(String.format("Re-registered job (%s) to segue job execution service. Current jobs registered (%s): ",
          jobToRegister.getJobKey(), localRegisteredJobs.size()));
    }

    localRegisteredJobs.add(jobToRegister);
  }

  /**
   * Checks if the scheduler is already running.
   *
   * @return true if scheduler already started.
   */
  public boolean isStarted() {
    try {
      return scheduler.isStarted();
    } catch (SchedulerException e) {
      return false;
    }
  }

  /**
   * Attempt to register configured jobs, remove any jobs marked for removal, then start scheduler.
   */
  public synchronized void initialiseService() throws SchedulerException {
    if (!isStarted()) {
      this.registerScheduledJobs(allKnownJobs);
      this.removeScheduledJobs(jobsToRemove);

      scheduler.start();
      log.info("Segue Job Service started.");
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
