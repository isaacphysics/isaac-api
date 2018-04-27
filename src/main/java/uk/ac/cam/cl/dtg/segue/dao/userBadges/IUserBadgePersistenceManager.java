package uk.ac.cam.cl.dtg.segue.dao.userBadges;

import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.dos.UserBadge;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by du220 on 27/04/2018.
 */
public interface IUserBadgePersistenceManager {

    /**
     *
     * @param conn
     * @param user
     * @param badgeName
     * @return
     * @throws SQLException
     */
    UserBadge getBadge(Connection conn, RegisteredUserDTO user, UserBadgeManager.Badge badgeName) throws SQLException;

    /**
     *
     * @param conn
     * @param badge
     * @throws SQLException
     */
    public void updateBadge(Connection conn, UserBadge badge) throws SQLException;

}
