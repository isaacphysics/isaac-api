package uk.ac.cam.cl.dtg.segue.api.managers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.questionBadges.TotalQuestionsAnsweredPolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherAssignmentsBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherCpdBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherGameboardsBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherGroupsBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dos.UserBadge;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;

/**
 * Created by du220 on 27/04/2018.
 */
public class UserBadgeManager {

    public enum Badge {

        // question answer badges
        QUESTIONS_ANSWERED_TOTAL,
        QUESTIONS_ANSWERED_WAVES,
        QUESTIONS_ANSWERED_LEVEL1,

        // teacher specific badges
        TEACHER_GROUPS_CREATED,
        TEACHER_ASSIGNMENTS_SET,
        TEACHER_GAMEBOARDS_CREATED,
        TEACHER_CPD_EVENTS_ATTENDED
    }

    private final IUserBadgePersistenceManager userBadges;
    private final Map<Badge, IUserBadgePolicy> badgePolicies = Maps.newHashMap();
    private ObjectMapper mapper = new ObjectMapper();

    /**
     *
     * @param userBadges
     */
    @Inject
    public UserBadgeManager(IUserBadgePersistenceManager userBadges,
                            GroupManager groupManager,
                            EventBookingManager bookingManager,
                            AssignmentManager assignmentManager,
                            QuestionManager questionManager,
                            GameManager gameManager,
                            IContentManager contentManager,
                            @Named(CONTENT_INDEX) String contentIndex) {

        this.userBadges = userBadges;

        badgePolicies.put(Badge.QUESTIONS_ANSWERED_TOTAL,
                new TotalQuestionsAnsweredPolicy(questionManager, gameManager,
                        contentManager, contentIndex, this));

        badgePolicies.put(Badge.TEACHER_GROUPS_CREATED, new TeacherGroupsBadgePolicy(groupManager));
        badgePolicies.put(Badge.TEACHER_ASSIGNMENTS_SET, new TeacherAssignmentsBadgePolicy(assignmentManager));
        badgePolicies.put(Badge.TEACHER_GAMEBOARDS_CREATED, new TeacherGameboardsBadgePolicy(gameManager));
        badgePolicies.put(Badge.TEACHER_CPD_EVENTS_ATTENDED,
                new TeacherCpdBadgePolicy(bookingManager, contentManager, contentIndex));
    }

    /**
     *
     * @param conn
     * @param user
     * @param badgeName
     * @return
     * @throws SQLException
     */
    public UserBadge getOrCreateBadge(Connection conn, RegisteredUserDTO user, Badge badgeName) throws SQLException, IOException {

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
    public void updateBadge(Connection conn, RegisteredUserDTO user, Badge badgeName, Object event) throws SQLException, IOException {

        UserBadge badge = userBadges.getBadge(conn, user, badgeName);

        if (null != badge.getState()) {

            try {

                JsonNode state = mapper.readTree((String) badge.getState());
                int oldLevel = badgePolicies.get(badgeName).getLevel(state);

                Object newState = badgePolicies.get(badgeName).updateState(user, state, event);
                int newLevel = badgePolicies.get(badgeName).getLevel(newState);

                if (newLevel != oldLevel) {
                    // do notification
                }

                badge.setState(newState);

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            badge.setState(badgePolicies.get(badgeName).initialiseState(user));
        }

        userBadges.updateBadge(conn, badge);
    }
}
