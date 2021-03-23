package uk.ac.cam.cl.dtg.segue.dao.users;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.UserExternalAccountChanges;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
 * This class is responsible for managing and persisting user data.
 */
public class PgExternalAccountPersistenceManager implements IExternalAccountDataManager {
    private static final Logger log = LoggerFactory.getLogger(PgExternalAccountPersistenceManager.class);

    private final PostgresSqlDb database;

    /**
     * Creates a new user data manager object.
     *
     * @param database - the database reference used for persistence.
     */
    @Inject
    public PgExternalAccountPersistenceManager(final PostgresSqlDb database) {
        this.database = database;
    }

    @Override
    public List<UserExternalAccountChanges> getRecentlyChangedRecords() throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement(
            "SELECT id, provider_user_identifier, email, role, given_name, deleted, email_verification_status, " +
                "       news_prefs.preference_value AS news_emails, events_prefs.preference_value AS events_emails " +
                "FROM users " +
                "    LEFT OUTER JOIN user_preferences AS news_prefs ON users.id = news_prefs.user_id AND news_prefs.preference_type='EMAIL_PREFERENCE' AND news_prefs.preference_name='NEWS_AND_UPDATES' " +
                "    LEFT OUTER JOIN user_preferences AS events_prefs ON users.id = events_prefs.user_id AND events_prefs.preference_type='EMAIL_PREFERENCE' AND events_prefs.preference_name='EVENTS' " +
                "    LEFT OUTER JOIN external_accounts ON users.id=external_accounts.user_id AND provider_name='MailJet' " +
                "WHERE (users.last_updated >= provider_last_updated OR news_prefs.last_updated >= provider_last_updated " +
                "           OR events_prefs.last_updated >= provider_last_updated OR provider_last_updated IS NULL)");

            ResultSet results = pst.executeQuery();

            List<UserExternalAccountChanges> listOfResults = Lists.newArrayList();

            while (results.next()) {
                listOfResults.add(buildUserExternalAccountChanges(results));
            }

            return listOfResults;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public void updateProviderLastUpdated(final Long userId) throws SegueDatabaseException {

        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("UPDATE external_accounts SET provider_last_updated=? WHERE user_id=? AND provider_name='MailJet';");
            pst.setTimestamp(1, new Timestamp(new Date().getTime()));
            pst.setLong(2, userId);

            pst.executeUpdate();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception on update ", e);
        }
    }

    @Override
    public void updateExternalAccount(final Long userId, final String providerUserIdentifier) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            // Upsert the value in, using Postgres 9.5 syntax 'ON CONFLICT DO UPDATE ...'
            pst = conn.prepareStatement("INSERT INTO external_accounts(user_id, provider_name, provider_user_identifier) "
                    + " VALUES (?, 'MailJet', ?)"
                    + " ON CONFLICT (user_id, provider_name) DO UPDATE"
                    + " SET provider_user_identifier=excluded.provider_user_identifier");
            pst.setLong(1, userId);
            pst.setString(2, providerUserIdentifier);

            pst.executeUpdate();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception on upsert ", e);
        }
    }

    private UserExternalAccountChanges buildUserExternalAccountChanges(final ResultSet results) throws SQLException {
        return new UserExternalAccountChanges(
            results.getLong("id"),
            results.getString("provider_user_identifier"),
            results.getString("email"),
            Role.valueOf(results.getString("role")),
            results.getString("given_name"),
            results.getBoolean("deleted"),
            EmailVerificationStatus.valueOf(results.getString("email_verification_status")),
            results.getBoolean("news_emails"),
            results.getBoolean("events_emails")
        );

    }
}