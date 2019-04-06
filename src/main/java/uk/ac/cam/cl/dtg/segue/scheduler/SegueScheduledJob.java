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
package uk.ac.cam.cl.dtg.segue.scheduler;

import org.quartz.Job;

import java.util.Map;

/**
 * Super class to support the capture of all variables needed to register a segue Scheduled Job
 */
public abstract class SegueScheduledJob {
    private String jobKey;
    private String jobGroupName;
    private String description;

    private String cronString;

    public SegueScheduledJob(String jobKey, String jobGroupName, String description, String cronString) {
        this.jobKey = jobKey;
        this.jobGroupName = jobGroupName;
        this.description = description;
        this.cronString = cronString;
    }

    /**
     * Key used to describe the job to the schedule cluster.
     *
     * Must be unique
     *
     * @return the id
     */
    public String getJobKey() {
        return jobKey;
    }

    /**
     * Group name
     *
     * Describes the group of jobs
     *
     * @return the group name
     */

    public String getJobGroupName() {
        return jobGroupName;
    }

    /**
     * CronString
     * This describes the cron trigger that will be used to periodically trigger this job.
     *
     * {@see http://www.quartz-scheduler.org/documentation/quartz-2.x/tutorials/crontrigger.html}
     *
     * @return the cron pattern
     */

    public String getCronString() {
        return cronString;
    }


    /**
     * Human readable description of the job
     *
     * @return string
     */
    public String getJobDescription() {
        return description;
    }

    /**
     * A map representing any serialisable values that will be available to the Quartz job during execution.
     *
     * @return may of values
     */
    public abstract Map<String, Object> getExecutionContext();

    /**
     * Quartz job that will be instantiated by quartz during job execution
     *
     * The execute method is used
     *
     * @return QuartzJob
     */
    public abstract Job getExecutableTask();

}
