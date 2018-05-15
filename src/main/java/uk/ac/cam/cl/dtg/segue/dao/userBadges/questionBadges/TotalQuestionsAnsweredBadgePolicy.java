package uk.ac.cam.cl.dtg.segue.dao.userBadges.questionBadges;

import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;

/**
 * Concrete user badge policy for badges based on questions answered in total
 *
 * Created by du220 on 15/05/2018.
 */
public class TotalQuestionsAnsweredBadgePolicy extends AbstractQuestionsAnsweredBadgePolicy {

    /**
     * Constructor
     *
     * @param questionManager for retrieving question attempt data
     * @param gameManager for retrieving question page data
     */
    public TotalQuestionsAnsweredBadgePolicy(QuestionManager questionManager, GameManager gameManager) {
        super(questionManager, gameManager);
    }

    @Override
    Boolean isRelevantQuestion(String questionPageId) {
        // we are counting all questions, so all of them are relevant
        return true;
    }
}
