package uk.ac.cam.cl.dtg.segue.dao.users;

import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.dos.users.AccountDeletionToken;
import uk.ac.cam.cl.dtg.segue.dao.AbstractPgDataManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Postgres-based storage of AccountDeletionTokens.
 */
public class PgDeletionTokenPersistenceManager extends AbstractPgDataManager implements IDeletionTokenPersistenceManager {
    private final PostgresSqlDb database;

    /**
     * PgDeletionTokenDataManager.
     *
     * @param ds - the postgres datasource to use.
     */
    @Inject
    public PgDeletionTokenPersistenceManager(final PostgresSqlDb ds) {
        this.database = ds;
    }

    @Override
    public AccountDeletionToken getAccountDeletionToken(final Long userId) throws SegueDatabaseException {
        if (null == userId) {
            return null;
        }

        String query = "SELECT * FROM user_deletion_tokens WHERE user_id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);

            try (ResultSet results = pst.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                return buildAccountDeletionToken(results);
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public AccountDeletionToken saveAccountDeletionToken(final AccountDeletionToken tokenToSave) throws SegueDatabaseException {

        String query = "INSERT INTO user_deletion_tokens(user_id, token, token_expiry, created, last_updated)"
                + " VALUES (?, ?, ?, ?, ?) ON CONFLICT ON CONSTRAINT pk_user_deletion_tokens DO UPDATE"
                + " SET token=EXCLUDED.token, token_expiry=EXCLUDED.token_expiry, created=EXCLUDED.created,"
                + " last_updated=EXCLUDED.last_updated";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            // Persist any existing creation date:
            Date created = tokenToSave.getCreated();
            if (null == created) {
                created = new Date();
            }

            pst.setLong(1, tokenToSave.getUserId());
            pst.setString(2, tokenToSave.getToken());
            setValueHelper(pst, 3, tokenToSave.getTokenExpiry());
            setValueHelper(pst, 4, created);
            setValueHelper(pst, 5, new Date());

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save account deletion token!");
            }

            return tokenToSave;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    private AccountDeletionToken buildAccountDeletionToken(final ResultSet results) throws SQLException {

        return new AccountDeletionToken(results.getLong("user_id"),
                results.getString("token"),
                results.getTimestamp("token_expiry"),
                results.getTimestamp("created"),
                results.getTimestamp("last_updated")
        );
    }
}
