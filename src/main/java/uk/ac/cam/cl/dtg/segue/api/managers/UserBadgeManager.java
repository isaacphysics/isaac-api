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
import uk.ac.cam.cl.dtg.segue.dao.userBadges.questionBadges.LevelQuestionsAnsweredBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.questionBadges.TotalQuestionsAnsweredBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.questionBadges.TagQuestionsAnsweredBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherAssignmentsBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherBookPagesBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherCpdBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherGameboardsBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.teacherBadges.TeacherGroupsBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dos.UserBadge;
import uk.ac.cam.cl.dtg.segue.dao.userBadges.IUserBadgePolicy;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
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
        TEACHER_CPD_EVENTS_ATTENDED,

        // question answer badges
        QUESTIONS_TOTAL,
        // subjects
        QUESTIONS_SUBJECT_PHYSICS,
        QUESTIONS_SUBJECT_CHEMISTRY,
        QUESTIONS_SUBJECT_MATHS,
        // topics - physics
        QUESTIONS_TOPIC_WAVES,
        QUESTIONS_TOPIC_MECHANICS,
        QUESTIONS_TOPIC_FIELDS,
        QUESTIONS_TOPIC_CIRCUITS,
        QUESTIONS_TOPIC_PHYSCHEM,
        // topics - maths
        QUESTIONS_TOPIC_GEOMETRY,
        QUESTIONS_TOPIC_CALCULUS,
        QUESTIONS_TOPIC_ALGEBRA,
        QUESTIONS_TOPIC_FUNCTIONS,
        // levels
        QUESTIONS_LEVEL_1,
        QUESTIONS_LEVEL_2,
        QUESTIONS_LEVEL_3,
        QUESTIONS_LEVEL_4,
        QUESTIONS_LEVEL_5,
        QUESTIONS_LEVEL_6
    }

    private final IUserBadgePersistenceManager userBadgePersistenceManager;
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
                            EventBookingManager bookingManager, AssignmentManager assignmentManager,
                            GameManager gameManager, QuestionManager questionManager, IContentManager contentManager,
                            @Named(CONTENT_INDEX) String contentIndex) {

        this.userBadgePersistenceManager = userBadgePersistenceManager;

        // teacher badges
        badgePolicies.put(Badge.TEACHER_GROUPS_CREATED, new TeacherGroupsBadgePolicy(groupManager));
        badgePolicies.put(Badge.TEACHER_ASSIGNMENTS_SET, new TeacherAssignmentsBadgePolicy(assignmentManager,
                gameManager));
        badgePolicies.put(Badge.TEACHER_BOOK_PAGES_SET, new TeacherBookPagesBadgePolicy(assignmentManager,
                gameManager));
        badgePolicies.put(Badge.TEACHER_GAMEBOARDS_CREATED, new TeacherGameboardsBadgePolicy(gameManager));
        badgePolicies.put(Badge.TEACHER_CPD_EVENTS_ATTENDED,
                new TeacherCpdBadgePolicy(bookingManager, contentManager, contentIndex));

        // question badges
        badgePolicies.put(Badge.QUESTIONS_TOTAL, new TotalQuestionsAnsweredBadgePolicy(questionManager,
                gameManager));
        badgePolicies.put(Badge.QUESTIONS_SUBJECT_PHYSICS, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "physics"));
        badgePolicies.put(Badge.QUESTIONS_SUBJECT_CHEMISTRY, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "chemistry"));
        badgePolicies.put(Badge.QUESTIONS_SUBJECT_MATHS, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "maths"));
        badgePolicies.put(Badge.QUESTIONS_TOPIC_WAVES, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "waves"));
        badgePolicies.put(Badge.QUESTIONS_TOPIC_MECHANICS, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "mechanics"));
        badgePolicies.put(Badge.QUESTIONS_TOPIC_FIELDS, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "fields"));
        badgePolicies.put(Badge.QUESTIONS_TOPIC_CIRCUITS, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "circuits"));
        badgePolicies.put(Badge.QUESTIONS_TOPIC_PHYSCHEM, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "chemphysics"));
        badgePolicies.put(Badge.QUESTIONS_TOPIC_GEOMETRY, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "geometry"));
        badgePolicies.put(Badge.QUESTIONS_TOPIC_CALCULUS, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "calculus"));
        badgePolicies.put(Badge.QUESTIONS_TOPIC_ALGEBRA, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "algebra"));
        badgePolicies.put(Badge.QUESTIONS_TOPIC_FUNCTIONS, new TagQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, "functions"));
        badgePolicies.put(Badge.QUESTIONS_LEVEL_1, new LevelQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, 1));
        badgePolicies.put(Badge.QUESTIONS_LEVEL_2, new LevelQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, 2));
        badgePolicies.put(Badge.QUESTIONS_LEVEL_3, new LevelQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, 3));
        badgePolicies.put(Badge.QUESTIONS_LEVEL_4, new LevelQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, 4));
        badgePolicies.put(Badge.QUESTIONS_LEVEL_5, new LevelQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, 5));
        badgePolicies.put(Badge.QUESTIONS_LEVEL_6, new LevelQuestionsAnsweredBadgePolicy(questionManager,
                gameManager, contentManager, contentIndex, 6));
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

        UserBadge badge = userBadgePersistenceManager.getBadge(conn, user, badgeName);

        if (null == badge.getState()) {
            badge.setState(badgePolicies.get(badgeName).initialiseState(user));
            userBadgePersistenceManager.updateBadge(conn, badge);
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

        UserBadge badge = userBadgePersistenceManager.getBadge(conn, user, badgeName);

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

        userBadgePersistenceManager.updateBadge(conn, badge);

        return badge;
    }

    /**
     * Gets a map of all the users badges and their values
     *
     * @param user the user of interest
     * @return a map of badge names to values
     */
    public Map<String, Object> getAllUserBadges(RegisteredUserDTO user) {

        Map<String, Object> badges = Maps.newHashMap();

        try {
            for (Badge badgeName : Badge.values()) {

                if (user.getRole().equals(Role.STUDENT) && badgeName.name().split("_")[0].equals("TEACHER")) {
                    continue;
                }

                UserBadge badge = getOrCreateBadge(null, user, badgeName);
                badges.put(badge.getBadgeName().name(),
                        badgePolicies.get(badge.getBadgeName()).getLevel(badge.getState()));
            }
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
        }

        return badges;
    }
}
