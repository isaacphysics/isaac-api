package uk.ac.cam.cl.dtg.segue.dao.userBadges;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.segue.api.managers.ITransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.PgTransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.ITransaction;
import uk.ac.cam.cl.dtg.segue.dos.PgTransaction;
import uk.ac.cam.cl.dtg.segue.dos.UserBadge;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.zip.CRC32;

/**
 * Created by du220 on 13/04/2018.
 */
public class PgUserBadgePersistenceManager implements IUserBadgePersistenceManager {

    private static final String TABLE_NAME = "user_badges";

    private PostgresSqlDb postgresSqlDb;
    private ObjectMapper mapper = new ObjectMapper();


    /**
     * Postgres specific database management for user badges
     *
     * @param postgresSqlDb pre-configured connection
     */
    @Inject
    public PgUserBadgePersistenceManager(PostgresSqlDb postgresSqlDb) {
        this.postgresSqlDb = postgresSqlDb;
    }

    @Override
    public UserBadge getBadge(RegisteredUserDTO user, UserBadgeManager.Badge badgeName,
                              ITransaction transaction) throws SegueDatabaseException {

        /*if (!(transaction instanceof PgTransaction)) {
            throw new SegueDatabaseException("Unable to get badge definition from database.");
        }*/

        try {

            PreparedStatement pst;
            //pst = ((PgTransaction)transaction).getConnection().prepareStatement("INSERT INTO user_badges (user_id, badge)" +
            pst = postgresSqlDb.getDatabaseConnection().prepareStatement("INSERT INTO user_badges (user_id, badge)" +
                    " VALUES (?, ?) ON CONFLICT (user_id, badge) DO UPDATE SET user_id = excluded.user_id " +
                    "WHERE user_badges.user_id = ? AND user_badges.badge = ? RETURNING *");

            pst.setLong(1, user.getId());
            pst.setString(2, badgeName.name());
            pst.setLong(3, user.getId());
            pst.setString(4, badgeName.name());

            ResultSet results = pst.executeQuery();
            results.next();

            return new UserBadge(user.getId(), badgeName, (results.getString("state") != null) ?
                    mapper.readTree(results.getString("state")) : null);

        } catch (SQLException | IOException e) {
            throw new SegueDatabaseException("Unable to get badge definition from database: " + e);
        }
    }

    @Override
    public void updateBadge(UserBadge badge, ITransaction transaction) throws SegueDatabaseException {

        /*if (!(transaction instanceof PgTransaction)) {
            throw new SegueDatabaseException("Unable to update database badge.");
        }*/

        try {

            PreparedStatement pst;
            pst = postgresSqlDb.getDatabaseConnection()//((PgTransaction)transaction).getConnection()
                    .prepareStatement("UPDATE user_badges SET state = ?::jsonb WHERE user_id = ? and badge = ?");
            pst.setString(1, mapper.writeValueAsString(badge.getState()));
            pst.setLong(2, badge.getUserId());
            pst.setString(3, badge.getBadgeName().name());

            pst.executeUpdate();

        } catch (SQLException | JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to update database badge.");
        }
    }

    @Override
    public void acquireDistributedLock(final String resourceId) throws SegueDatabaseException {
        // generate 32 bit CRC based on table id and resource id so that is is more likely to be unique globally.
        CRC32 crc = new CRC32();
        crc.update((TABLE_NAME + resourceId).getBytes());

        // acquire lock
        try (Connection conn = postgresSqlDb.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT pg_advisory_lock(?)");
            pst.setLong(1, crc.getValue());
            pst.executeQuery();
        } catch (SQLException e) {
            String msg = String.format(
                    "Unable to acquire lock for event (%s).", resourceId);
            throw new SegueDatabaseException(msg);
        }
    }

    @Override
    public void releaseDistributedLock(final String resourceId) throws SegueDatabaseException {

        // generate 32 bit CRC based on table id and resource id so that is is more likely to be unique globally.
        CRC32 crc = new CRC32();
        crc.update((TABLE_NAME + resourceId).getBytes());

        // acquire lock
        try (Connection conn = postgresSqlDb.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT pg_advisory_unlock(?)");
            pst.setLong(1, crc.getValue());
            pst.executeQuery();
        } catch (SQLException e) {
            String msg = String.format(
                    "Unable to release lock for event (%s).", resourceId);
            throw new SegueDatabaseException(msg);
        }
    }
}
