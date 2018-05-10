package uk.ac.cam.cl.dtg.segue.dao.userBadges;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.UserBadge;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by du220 on 13/04/2018.
 */
public class PgUserBadgePersistenceManager implements IUserBadgePersistenceManager {

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
    public UserBadge getBadge(Connection conn, RegisteredUserDTO user, UserBadgeManager.Badge badgeName)
            throws SegueDatabaseException {

        try {
            if (null == conn) {
                conn = postgresSqlDb.getDatabaseConnection();
            }

            PreparedStatement pst;
            pst = conn.prepareStatement("INSERT INTO user_badges (user_id, badge)" +
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
            throw new SegueDatabaseException("Unable to get badge definition from database.");
        }
    }

    @Override
    public void updateBadge(Connection conn, UserBadge badge) throws SegueDatabaseException {

        try {
            if (null == conn) {
                conn = postgresSqlDb.getDatabaseConnection();
            }

            PreparedStatement pst;
            pst = conn.prepareStatement("UPDATE user_badges SET state = ?::jsonb WHERE user_id = ? and badge = ?");
            pst.setString(1, mapper.writeValueAsString(badge.getState()));
            pst.setLong(2, badge.getUserId());
            pst.setString(3, badge.getBadgeName().name());

            pst.executeUpdate();

        } catch (SQLException | JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to update database badge.");
        }
    }
}
