package uk.ac.cam.cl.dtg.segue.api.managers;

import com.google.common.collect.Maps;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePersistenceManager;
import uk.ac.cam.cl.dtg.segue.dos.UserBadge;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Created by du220 on 27/04/2018.
 */
public class UserBadgeManager {

    public enum Badge {
        TOTAL_QUESTIONS_ANSWERED
    }

    private IUserBadgePersistenceManager userBadges;
    private Map<Badge, IUserBadgePolicy> badgePolicies = Maps.newHashMap();

    /**
     *
     * @param userBadges
     */
    public UserBadgeManager(IUserBadgePersistenceManager userBadges) {
        this.userBadges = userBadges;
    }

    /**
     *
     * @param conn
     * @param user
     * @param badgeName
     * @return
     * @throws SQLException
     */
    public UserBadge getOrCreateBadge(Connection conn, RegisteredUserDTO user, Badge badgeName) throws SQLException {

        UserBadge badge = userBadges.getBadge(conn, user, badgeName);

        if (null == badge.getState()) {
            badge.setState(badgePolicies.get(badgeName).initialiseState(user));
        }

        return badge;
    }

    /**
     *
     * @param conn
     * @param user
     * @param badgeName
     * @param event
     * @throws SQLException
     */
    public void updateBadge(Connection conn, RegisteredUserDTO user, Badge badgeName, Object event) throws SQLException {

        UserBadge badge = userBadges.getBadge(conn, user, badgeName);

        if (null != badge.getState()) {

            Object newState = badgePolicies.get(badgeName).updateState(badge.getState(), event);
            int oldLevel = badgePolicies.get(badgeName).getLevel(badge.getState());
            int newLevel = badgePolicies.get(badgeName).getLevel(newState);

            if (newLevel != oldLevel) {
                // do notification
            }

            badge.setState(newState);
        } else {
            badge.setState(badgePolicies.get(badgeName).initialiseState(user));
        }

        userBadges.updateBadge(conn, badge);
    }
}
