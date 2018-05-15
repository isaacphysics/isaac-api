package uk.ac.cam.cl.dtg.segue.dao.userBadges.questionBadges;

import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

/**
 * Concrete user badge policy for badges based on questions answered by level
 *
 * Created by du220 on 15/05/2018.
 */
public class LevelQuestionsAnsweredBadgePolicy extends AbstractQuestionsAnsweredBadgePolicy {

    private final IContentManager contentManager;
    private final String contentIndex;
    private final Integer level;

    /**
     * Constructor
     *
     * @param questionManager for retrieving question attempt data
     * @param gameManager for retrieving question page data
     * @param contentManager for retrieving content data on questions
     * @param contentIndex to obtain relevant content version
     * @param level instance-specific level filter
     */
    public LevelQuestionsAnsweredBadgePolicy(QuestionManager questionManager, GameManager gameManager,
                                             IContentManager contentManager, String contentIndex,
                                             Integer level) {
        super(questionManager, gameManager);
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.level = level;
    }

    @Override
    Boolean isRelevantQuestion(String questionPageId) {
        try {
            ContentDTO content = contentManager.getContentById(contentIndex, questionPageId);

            if (content instanceof IsaacQuestionPageDTO) {
                return content.getLevel().equals(level);
            }

        } catch (ContentManagerException e) {
            e.printStackTrace();
        }

        return false;
    }
}
