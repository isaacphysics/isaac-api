package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;

public class ScheduledAssignmentEmailJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(ScheduledAssignmentEmailJob.class);
    private final EmailService emailService;

    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    public ScheduledAssignmentEmailJob() {
        Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
        emailService = injector.getInstance(EmailService.class);
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        //emailService.sendAssignmentEmailToGroup();
        log.info("Ran ScheduledAssignmentEmailJob");
    }
}
