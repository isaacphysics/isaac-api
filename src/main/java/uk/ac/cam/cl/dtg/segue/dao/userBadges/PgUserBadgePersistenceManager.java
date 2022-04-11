package uk.ac.cam.cl.dtg.segue.dao.userBadges;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.isaac.dos.ITransaction;
import uk.ac.cam.cl.dtg.isaac.dos.PgTransaction;
import uk.ac.cam.cl.dtg.isaac.dos.UserBadge;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by du220 on 13/04/2018.
 */
public class PgUserBadgePersistenceManager implements IUserBadgePersistenceManager {

    private final ObjectMapper mapper = new ObjectMapper();


    /**
     * Postgres specific database management for user badges.
     */
    public PgUserBadgePersistenceManager() {
    }

    @Override
    public UserBadge getBadge(final RegisteredUserDTO user, final UserBadgeManager.Badge badgeName,
                              final ITransaction transaction) throws SegueDatabaseException {

        if (!(transaction instanceof PgTransaction)) {
            throw new SegueDatabaseException("Unable to get badge definition from database.");
        }

        String query = "INSERT INTO user_badges (user_id, badge) VALUES (?, ?)"
                + " ON CONFLICT (user_id, badge) DO UPDATE SET user_id = excluded.user_id"
                + " WHERE user_badges.user_id = ? AND user_badges.badge = ? RETURNING *";

        Connection conn = ((PgTransaction) transaction).getConnection();
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setLong(1, user.getId());
            pst.setString(2, badgeName.name());
            pst.setLong(3, user.getId());
            pst.setString(4, badgeName.name());

            try (ResultSet results = pst.executeQuery()) {
                results.next();
                return new UserBadge(user.getId(), badgeName, (results.getString("state") != null)
                        ? mapper.readTree(results.getString("state")) : null);
            }
        } catch (SQLException | IOException e) {
            throw new SegueDatabaseException("Unable to get badge definition from database: " + e);
        }
    }

    @Override
    public void updateBadge(final UserBadge badge, final ITransaction transaction) throws SegueDatabaseException {

        if (!(transaction instanceof PgTransaction)) {
            throw new SegueDatabaseException("Unable to update database badge.");
        }

        String query = "UPDATE user_badges SET state = ?::jsonb WHERE user_id = ? and badge = ?";

        Connection conn = ((PgTransaction) transaction).getConnection();
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, mapper.writeValueAsString(badge.getState()));
            pst.setLong(2, badge.getUserId());
            pst.setString(3, badge.getBadgeName().name());

            pst.executeUpdate();

        } catch (SQLException | JsonProcessingException e) {
            throw new SegueDatabaseException("Unable to update database badge.");
        }
    }
}
