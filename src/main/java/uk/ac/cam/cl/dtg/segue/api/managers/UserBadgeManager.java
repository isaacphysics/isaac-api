package uk.ac.cam.cl.dtg.segue.api.managers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherAssignmentsBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherCpdBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherGameboardsBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherGroupsBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dos.UserBadge;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.sql.Connection;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;

/**
 * Management layer for updating and retrieving badge states
 *
 * Created by du220 on 27/04/2018.
 */
public class UserBadgeManager {

    public enum Badge {
        // teacher specific badges
        TEACHER_GROUPS_CREATED,
        TEACHER_ASSIGNMENTS_SET,
        TEACHER_BOOK_PAGES_SET,
        TEACHER_GAMEBOARDS_CREATED,
        TEACHER_CPD_EVENTS_ATTENDED
    }

    private final IUserBadgePersistenceManager userBadges;
    private final Map<Badge, IUserBadgePolicy> badgePolicies = Maps.newHashMap();

    /**
     * Constructor
     *
     * @param userBadgePersistenceManager badge persistence object for liasing with database
     * @param groupManager group manager object for badge policy dependencies
     * @param bookingManager booking manager object for badge policy dependencies
     * @param assignmentManager assignment manager object for badge policy dependencies
     * @param gameManager game manager object for badge policy dependencies
     * @param contentManager content manager object for badge policy dependencies
     * @param contentIndex specifies content version
     */
    @Inject
    public UserBadgeManager(IUserBadgePersistenceManager userBadgePersistenceManager, GroupManager groupManager,
                            EventBookingManager bookingManager, AssignmentManager assignmentManager, GameManager gameManager,
                            IContentManager contentManager, @Named(CONTENT_INDEX) String contentIndex) {

        this.userBadges = userBadgePersistenceManager;

        badgePolicies.put(Badge.TEACHER_GROUPS_CREATED, new TeacherGroupsBadgePolicy(groupManager));
        badgePolicies.put(Badge.TEACHER_ASSIGNMENTS_SET, new TeacherAssignmentsBadgePolicy(assignmentManager,
                gameManager));
        badgePolicies.put(Badge.TEACHER_GAMEBOARDS_CREATED, new TeacherGameboardsBadgePolicy(gameManager));
        badgePolicies.put(Badge.TEACHER_CPD_EVENTS_ATTENDED,
                new TeacherCpdBadgePolicy(bookingManager, contentManager, contentIndex));
    }

    /**
     * Gets an up-to-date badge by either retrieving from the database or initialising first-time on the fly
     *
     * @param conn database connection
     * @param user owner of badge record
     * @param badgeName enum of badge to be updated
     * @return user badge object
     * @throws SegueDatabaseException
     */
    public UserBadge getOrCreateBadge(Connection conn, RegisteredUserDTO user, Badge badgeName)
            throws SegueDatabaseException {

        UserBadge badge = userBadges.getBadge(conn, user, badgeName);

        if (null == badge.getState()) {
            badge.setState(badgePolicies.get(badgeName).initialiseState(user));
        }

        return badge;
    }

    /**
     * Updates the badge state and delivers to the database
     *
     * @param conn database connection
     * @param user owner of badge record
     * @param badgeName enum of badge to be updated
     * @param event indicator of
     * @return user badge object
     * @throws SegueDatabaseException
     */
    public UserBadge updateBadge(Connection conn, RegisteredUserDTO user, Badge badgeName, String event)
            throws SegueDatabaseException {

        UserBadge badge = userBadges.getBadge(conn, user, badgeName);

        if (null != badge.getState()) {

            JsonNode state = badge.getState();
            int oldLevel = badgePolicies.get(badgeName).getLevel(state);

            JsonNode newState = badgePolicies.get(badgeName).updateState(user, state, event);
            int newLevel = badgePolicies.get(badgeName).getLevel(newState);

            if (newLevel != oldLevel) {
                // todo: signal to user if significant change has occured
            }

            badge.setState(newState);

        } else {
            badge.setState(badgePolicies.get(badgeName).initialiseState(user));
        }

        userBadges.updateBadge(conn, badge);

        return badge;
    }
}
