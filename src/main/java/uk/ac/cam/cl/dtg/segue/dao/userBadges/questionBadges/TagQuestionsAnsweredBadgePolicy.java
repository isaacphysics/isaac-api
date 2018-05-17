package uk.ac.cam.cl.dtg.segue.dao.userBadges.questionBadges;

import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

/**
 * Concrete user badge policy for badges based on questions answered by topic
 *
 * Created by du220 on 15/05/2018.
 */
public class TagQuestionsAnsweredBadgePolicy extends AbstractQuestionsAnsweredBadgePolicy {

    private final IContentManager contentManager;
    private final String contentIndex;
    private final String topic;

    /**
     * Constructor
     *
     * @param questionManager for retrieving question attempt data
     * @param gameManager for retrieving question page data
     * @param contentManager for retrieving content data on questions
     * @param contentIndex to obtain relevant content version
     * @param topic instance-specific topic filter
     */
    public TagQuestionsAnsweredBadgePolicy(QuestionManager questionManager, GameManager gameManager,
                                           IContentManager contentManager, String contentIndex,
                                           String topic) {
        super(questionManager, gameManager);
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.topic = topic;
    }

    @Override
    Boolean isRelevantQuestion(String questionPageId) {

        try {
            ContentDTO content = contentManager.getContentById(contentIndex, questionPageId);

            if (content instanceof IsaacQuestionPageDTO) {
                return content.getTags().contains(topic);
            }

        } catch (ContentManagerException e) {
            e.printStackTrace();
        }

        return false;
    }
}
