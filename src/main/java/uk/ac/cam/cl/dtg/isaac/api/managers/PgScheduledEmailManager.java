package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;

public class PgScheduledEmailManager {
    private static final Logger log = LoggerFactory.getLogger(PgScheduledEmailManager.class);

    private final PostgresSqlDb database;

    /**
     * Creates a new user data manager object.
     *
     * @param database
     *            - the database reference used for persistence.
     */
    @Inject
    public PgScheduledEmailManager(final PostgresSqlDb database) {
        this.database = database;
    }

    public Boolean scheduledEmailAlreadySent(String emailKey) throws SegueDatabaseException {
        String query = "SELECT EXISTS(SELECT 1 FROM scheduled_emails WHERE email_id = ?)";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, emailKey);
            try (ResultSet results = pst.executeQuery()) {
                if (results.next()) {
                    return results.getBoolean(1);
                } else {
                    throw new SegueDatabaseException(String.format("Checking if '%s' was sent went wrong.", emailKey));
                }
            }
        } catch (SQLException e) {
            log.error("Failed to check if the scheduled email is already sent: ", e);
        }
        return true;
    }

    public void saveScheduledEmailSent(String emailKey) throws SegueDatabaseException {
        ZonedDateTime now = ZonedDateTime.now();
        String query = "INSERT INTO scheduled_emails(email_id, sent) VALUES (?, ?)";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, emailKey);
            pst.setTimestamp(2, Timestamp.valueOf(now.toLocalDateTime()));
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Email has already been sent.");
            }

        } catch (SQLException e) {
            log.error("Failed to add the scheduled email sent time: ", e);
        }
    }
}
