package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.sql.Connection;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;

/**
 * Management layer to signal when question has been answered to relevant question badges
 * Created by du220 on 08/05/2018.
 */
public class QuestionBadgeManager {

    private final UserBadgeManager userBadgeManager;
    private final IContentManager contentManager;
    private final String contentIndex;

    private final Map<String, UserBadgeManager.Badge> tagBadges =
            ImmutableMap.<String, UserBadgeManager.Badge>builder()
                    .put("waves", UserBadgeManager.Badge.QUESTIONS_TOPIC_WAVES)
                    .put("mechanics", UserBadgeManager.Badge.QUESTIONS_TOPIC_MECHANICS)
                    .put("fields", UserBadgeManager.Badge.QUESTIONS_TOPIC_FIELDS)
                    .put("circuits", UserBadgeManager.Badge.QUESTIONS_TOPIC_CIRCUITS)
                    .put("chemphysics", UserBadgeManager.Badge.QUESTIONS_TOPIC_PHYSCHEM)
                    .put("geometry", UserBadgeManager.Badge.QUESTIONS_TOPIC_GEOMETRY)
                    .put("calculus", UserBadgeManager.Badge.QUESTIONS_TOPIC_CALCULUS)
                    .put("algebra", UserBadgeManager.Badge.QUESTIONS_TOPIC_ALGEBRA)
                    .put("functions", UserBadgeManager.Badge.QUESTIONS_TOPIC_FUNCTIONS)
                    .put("physics", UserBadgeManager.Badge.QUESTIONS_SUBJECT_PHYSICS)
                    .put("chemistry", UserBadgeManager.Badge.QUESTIONS_SUBJECT_CHEMISTRY)
                    .put("maths", UserBadgeManager.Badge.QUESTIONS_SUBJECT_MATHS)
            .build();


    private final Map<Integer, UserBadgeManager.Badge> levelBadges =
            ImmutableMap.<Integer, UserBadgeManager.Badge>builder()
                    .put(1, UserBadgeManager.Badge.QUESTIONS_LEVEL_1)
                    .put(2, UserBadgeManager.Badge.QUESTIONS_LEVEL_2)
                    .put(3, UserBadgeManager.Badge.QUESTIONS_LEVEL_3)
                    .put(4, UserBadgeManager.Badge.QUESTIONS_LEVEL_4)
                    .put(5, UserBadgeManager.Badge.QUESTIONS_LEVEL_5)
                    .put(6, UserBadgeManager.Badge.QUESTIONS_LEVEL_6)
            .build();


    /**
     * Constructor
     *
     * @param userBadgeManager lower layer general badge manager
     * @param contentManager for retrieving content data on questions
     * @param contentIndex to obtain relevant content version
     */
    @Inject
    public QuestionBadgeManager(UserBadgeManager userBadgeManager, IContentManager contentManager,
                                @Named(CONTENT_INDEX) String contentIndex) {

        this.userBadgeManager = userBadgeManager;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
    }

    /**
     * Method to notify any question badges who's update should be triggered on the currently answered question
     *
     * @param conn a database connection object
     * @param user the user who has answered the question
     * @param questionId the id of the question answered
     */
    public void updateQuestionBadges(Connection conn, RegisteredUserDTO user, String questionId) {

        try {

            // update total questions counter first
            this.userBadgeManager.updateBadge(conn, user,
                    UserBadgeManager.Badge.QUESTIONS_TOTAL, questionId);

            // get additional details
            ContentDTO questionDetails = this.getQuestionDetails(questionId.split("\\|")[0]);

            if (questionDetails instanceof IsaacQuestionPageDTO) {

                // update tag badges
                for (String tag : questionDetails.getTags()) {
                    if (tagBadges.containsKey(tag)) {
                        this.userBadgeManager.updateBadge(conn, user, tagBadges.get(tag), questionId);
                    }
                }

                // update level badges
                if (levelBadges.containsKey(questionDetails.getLevel())) {
                    this.userBadgeManager.updateBadge(conn, user,
                            levelBadges.get(questionDetails.getLevel()), questionId);
                }
            }

        } catch (ContentManagerException | SegueDatabaseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method to retrieve info on a question page
     *
     * @param questionPageId the id of the question page
     * @return a contentDTO object containing content data
     * @throws ContentManagerException
     */
    private ContentDTO getQuestionDetails(String questionPageId) throws ContentManagerException {
        return this.contentManager.getContentById(this.contentIndex, questionPageId);
    }
}
