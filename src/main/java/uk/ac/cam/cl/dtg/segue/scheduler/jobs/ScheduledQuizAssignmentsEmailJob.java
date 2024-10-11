package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.PgScheduledEmailManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizManager;
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.List;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class ScheduledQuizAssignmentsEmailJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(ScheduledQuizAssignmentsEmailJob.class);
    private final EmailService emailService;
    private final IQuizAssignmentPersistenceManager quizAssignmentPersistenceManager;
    private final QuizManager quizManager;
    private final PgScheduledEmailManager pgScheduledEmailManager;
    private final AbstractConfigLoader properties;

    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    public ScheduledQuizAssignmentsEmailJob() {
        Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
        emailService = injector.getInstance(EmailService.class);
        quizAssignmentPersistenceManager = injector.getInstance(IQuizAssignmentPersistenceManager.class);
        quizManager = injector.getInstance(QuizManager.class);
        pgScheduledEmailManager = injector.getInstance(PgScheduledEmailManager.class);
        properties = injector.getInstance(AbstractConfigLoader.class);
    }

    private void startSingleScheduledQuizAssignment(final QuizAssignmentDTO quizAssignment) {
        String emailKey = String.format("%d@scheduled_quiz_assignment", quizAssignment.getId());
        boolean sendAssignmentEmail;
        sendAssignmentEmail = this.pgScheduledEmailManager.commitToSchedulingEmail(emailKey);
        if (sendAssignmentEmail) {
            try {
                IsaacQuizDTO quiz = quizManager.findQuiz(quizAssignment.getQuizId());
                String quizURL = String.format("https://%s/test/assignment/%s?utm_source=notification-email", properties.getProperty(HOST_NAME), quizAssignment.getId());
                emailService.sendAssignmentEmailToGroup(quizAssignment, quiz, ImmutableMap.of("quizURL", quizURL),
                        "email-template-group-quiz-assignment");
            } catch (SegueDatabaseException | ContentManagerException e) {
                log.error("Exception while trying to send scheduled quiz assignment email", e);
            }
        }
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        try {
            List<QuizAssignmentDTO> assignments = this.quizAssignmentPersistenceManager.getAssignmentsScheduledForHour(context.getScheduledFireTime());
            assignments.forEach(this::startSingleScheduledQuizAssignment);
            log.info("Ran ScheduledQuizAssignmentsEmailJob");
        } catch (SegueDatabaseException e) {
            log.error("Failed get scheduled quizzes for ScheduledQuizAssignmentsEmailJob, cannot send emails", e);
            throw new JobExecutionException();
        }
    }
}
