package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.PgScheduledEmailManager;
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.isaac.dao.IAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.List;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

public class ScheduledAssignmentsEmailJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(ScheduledAssignmentsEmailJob.class);
    private final EmailService emailService;
    private final IAssignmentPersistenceManager assignmentPersistenceManager;
    private final GameManager gameManager;
    private final PgScheduledEmailManager pgScheduledEmailManager;
    private final AbstractConfigLoader properties;

    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    public ScheduledAssignmentsEmailJob() {
        Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
        emailService = injector.getInstance(EmailService.class);
        assignmentPersistenceManager = injector.getInstance(IAssignmentPersistenceManager.class);
        gameManager = injector.getInstance(GameManager.class);
        pgScheduledEmailManager = injector.getInstance(PgScheduledEmailManager.class);
        properties = injector.getInstance(AbstractConfigLoader.class);
    }

    private void startSingleScheduledAssignment(final AssignmentDTO assignment) {
        String emailKey = String.format("%d@scheduled_assignment", assignment.getId());
        boolean sendAssignmentEmail;
        sendAssignmentEmail = this.pgScheduledEmailManager.commitToSchedulingEmail(emailKey);
        if (sendAssignmentEmail) {
            try {
                GameboardDTO gameboard = this.gameManager.getGameboard(assignment.getGameboardId());
                final String gameboardURL = String.format("https://%s/assignment/%s", this.properties.getProperty(HOST_NAME),
                        gameboard.getId());
                this.emailService.sendAssignmentEmailToGroup(assignment, gameboard, ImmutableMap.of("gameboardURL", gameboardURL),
                        "email-template-group-assignment");
            } catch (SegueDatabaseException e) {
                log.error("Exception while trying to send scheduled assignment email", e);
            }
        }
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        try {
            List<AssignmentDTO> assignments = this.assignmentPersistenceManager.getAssignmentsScheduledForHour(context.getScheduledFireTime());
            assignments.forEach(this::startSingleScheduledAssignment);
            log.info("Ran ScheduledAssignmentsEmailJob");
        } catch (SegueDatabaseException e) {
            log.error("Failed get scheduled assignments for ScheduledAssignmentsEmailJob, cannot send emails", e);
            throw new JobExecutionException();
        }
    }
}
