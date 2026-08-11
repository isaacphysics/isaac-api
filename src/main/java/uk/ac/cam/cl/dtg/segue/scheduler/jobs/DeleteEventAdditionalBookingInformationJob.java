package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.api.client.util.Maps;
import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.search.BooleanInstruction;
import uk.ac.cam.cl.dtg.segue.search.MatchInstruction;
import uk.ac.cam.cl.dtg.segue.search.RangeInstruction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class DeleteEventAdditionalBookingInformationJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(DeleteEventAdditionalBookingInformationJob.class);

    private final PostgresSqlDb database;
    private final GitContentManager contentManager;

    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    public DeleteEventAdditionalBookingInformationJob() {
        Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
        contentManager = injector.getInstance(GitContentManager.class);
        database = injector.getInstance(PostgresSqlDb.class);

    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        // Magic number
        Integer limit = 10000;
        Integer startIndex = 0;
        Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
        sortInstructions.put(DATE_FIELDNAME, SortOrder.DESC);

        BooleanInstruction instruction = new BooleanInstruction();
        instruction.must(new MatchInstruction(TYPE_FIELDNAME, EVENT_TYPE));

        // Start with all events with end dates in the past OR dates in the past, in case end date isn't set
        BooleanInstruction pastEventsInstruction = new BooleanInstruction();
        pastEventsInstruction.should(new RangeInstruction<Long>(ENDDATE_FIELDNAME).lessThanOrEqual(new Date().getTime()));
        pastEventsInstruction.should(new RangeInstruction<Long>(DATE_FIELDNAME).lessThanOrEqual(new Date().getTime()));
        instruction.must(pastEventsInstruction);

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime thirtyDaysAgo = now.plusDays(-30);
        try {
            ResultsWrapper<ContentDTO> results = this.contentManager.nestedMatchSearch(
                    instruction, startIndex, limit, null, sortInstructions);
            for (ContentDTO contentResult : results.getResults()) {
                if (contentResult instanceof IsaacEventPageDTO page) {
                    // Event end date (if present) > 30 days ago, else event date > 30 days ago
                    boolean endDate30DaysAgo = page.getEndDate() != null && page.getEndDate().toInstant().isBefore(thirtyDaysAgo.toInstant());
                    boolean noEndDateAndStartDate30DaysAgo = page.getEndDate() == null && page.getDate().toInstant().isBefore(thirtyDaysAgo.toInstant());
                    if (endDate30DaysAgo || noEndDateAndStartDate30DaysAgo) {
                        String query = "UPDATE event_bookings SET additional_booking_information=jsonb_set(jsonb_set(jsonb_set(jsonb_set(additional_booking_information,"
                            + " '{emergencyName}', '\"[REMOVED]\"'::JSONB, FALSE),"
                            + " '{emergencyNumber}', '\"[REMOVED]\"'::JSONB, FALSE),"
                            + " '{accessibilityRequirements}', '\"[REMOVED]\"'::JSONB, FALSE),"
                            + " '{medicalRequirements}', '\"[REMOVED]\"'::JSONB, FALSE),"
                            + " pii_removed=? "
                            + " WHERE event_id = ?"
                            + " AND additional_booking_information ??| array['emergencyName', 'emergencyNumber', 'accessibilityRequirements', 'medicalRequirements']"
                            + " AND pii_removed IS NULL";
                        try (Connection conn = database.getDatabaseConnection();
                             PreparedStatement pst = conn.prepareStatement(query);
                        ) {
                            // Check for additional info that needs removing, check if pii has already been removed, if
                            // so, don't re-remove
                            pst.setTimestamp(1, Timestamp.valueOf(now.toLocalDateTime()));
                            pst.setString(2, page.getId());

                            int affectedRows = pst.executeUpdate();
                            if (affectedRows > 0) {
                                log.info("Event ({}) had {} bookings which have been scrubbed of PII", page.getId(), affectedRows);
                            }
                        }
                    }
                }
            }
            log.info("Ran DeleteEventAdditionalBookingInformationJob");
        } catch (SQLException | ContentManagerException e) {
            log.error("Failed to delete event additional booking information: ", e);
        }
    }
}
