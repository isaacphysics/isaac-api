package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;

public class DeleteEventAdditionalBookingInformationOneYearJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(DeleteEventAdditionalBookingInformationOneYearJob.class);

    private final PostgresSqlDb database;

    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    public DeleteEventAdditionalBookingInformationOneYearJob() {
        Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
        database = injector.getInstance(PostgresSqlDb.class);

    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime oneYearAgo = now.plusYears(-1);
        try {

            try (Connection conn = database.getDatabaseConnection()) {
                PreparedStatement pst;
                // Check for additional info that needs removing, check if pii has already been removed, if
                // so, don't re-remove
                pst = conn.prepareStatement(
                        "UPDATE event_bookings SET additional_booking_information=jsonb_set(jsonb_set(jsonb_set(jsonb_set(additional_booking_information," +
                                " '{emergencyName}', '\"[REMOVED]\"'::JSONB, FALSE)," +
                                " '{emergencyNumber}', '\"[REMOVED]\"'::JSONB, FALSE)," +
                                " '{accessibilityRequirements}', '\"[REMOVED]\"'::JSONB, FALSE)," +
                                " '{medicalRequirements}', '\"[REMOVED]\"'::JSONB, FALSE)," +
                                " pii_removed=? " +
                                " WHERE created < ?" +
                                " AND additional_booking_information ??| array['emergencyName', 'emergencyNumber', 'accessibilityRequirements', 'medicalRequirements']" +
                                " AND pii_removed IS NULL");
                pst.setTimestamp(1, Timestamp.valueOf(now.toLocalDateTime()));
                pst.setTimestamp(2, Timestamp.valueOf(oneYearAgo.toLocalDateTime()));

                int affectedRows = pst.executeUpdate();
                if (affectedRows > 0) {
                    log.info(affectedRows + " bookings older than a year had additional booking information which have been scrubbed of PII");
                }
            }
            log.info("Ran DeleteEventAdditionalBookingInformationOneYearJob");
        } catch (SQLException e) {
            log.error("Failed to delete event additional booking information: ", e);
        }
    }
}
