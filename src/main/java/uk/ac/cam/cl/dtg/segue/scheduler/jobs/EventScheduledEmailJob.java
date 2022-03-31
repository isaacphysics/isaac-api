package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.ScheduledEmailManager;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;

public class EventScheduledEmailJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(EventScheduledEmailJob.class);
    private final ScheduledEmailManager scheduledEmailManager;

    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    public EventScheduledEmailJob() {
        Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
        scheduledEmailManager = injector.getInstance(ScheduledEmailManager.class);
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        scheduledEmailManager.sendReminderEmails();
        log.info("Ran EventScheudledEmailJob");
    }
}

