package uk.ac.cam.cl.dtg.isaac.api.managers;

import org.apache.commons.lang3.Validate;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ProgressManager {
    private final GameManager gameManager;
    private final QuestionManager questionManager;
    private final GameboardPersistenceManager gameboardPersistenceManager;

    /**
     * Progress Manager
     * @param gameManager To augment the game items with question attempts
     * @param questionManager To get the question attempts
     * @param gameboardPersistenceManager To convert the question into a gameboard item
     */
    public ProgressManager(final GameManager gameManager, final QuestionManager questionManager,
                           final GameboardPersistenceManager gameboardPersistenceManager) {
        this.gameManager = gameManager;
        this.questionManager = questionManager;
        this.gameboardPersistenceManager = gameboardPersistenceManager;
    }

    /**
     * This method will return the most recent of the question attempts for a given user as a map.
     *
     * @param registeredUser
     *            - the user to get attempts of.
     * @param limit
     *            - the maximum number of question attempts to return
     * @return list of GameboardItems.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    public List<GameboardItem> getMostRecentQuestionAttemptsByUser(
            final RegisteredUserDTO registeredUser, final Integer limit) throws SegueDatabaseException, ContentManagerException {
        Validate.notNull(registeredUser);

        List<String> questionPageIds = questionManager.getMostRecentQuestionAttemptsByUser(registeredUser, limit);
        Map<String, ContentDTO> questionMap = questionManager.getQuestionMap(questionPageIds);
        Map<String, Map<String, List<QuestionValidationResponse>>> usersQuestionAttempts = questionManager.getQuestionAttemptsByUser(registeredUser);
        List<GameboardItem> questions = new ArrayList<>();

        for (String questionPageId : questionPageIds) {
            ContentDTO content = questionMap.get(questionPageId);

            if (content instanceof IsaacQuestionPageDTO) {
                IsaacQuestionPageDTO qp = (IsaacQuestionPageDTO) content;
                if (qp.getSupersededBy() != null && !qp.getSupersededBy().equals("")) {
                    // This question has been superseded. Don't include it.
                    continue;
                }
                GameboardItem questionInfo = this.gameboardPersistenceManager.convertToGameboardItem(content);
                gameManager.augmentGameItemWithAttemptInformation(questionInfo, usersQuestionAttempts);
                questions.add(questionInfo);
            }
        }
        return questions;
    }

    /**
     * This method will return the questions which a user has attempted but never correctly answered.
     *
     * @param registeredUser
     *            - the user to get attempts of.
     * @param bookOnly
     *            - Flag to only select questions with the book tag.
     * @return List of GameboardItems.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    public List<GameboardItem> getEasiestUnsolvedQuestions(
            final RegisteredUserDTO registeredUser, final Integer limit, final Boolean bookOnly) throws SegueDatabaseException, ContentManagerException {
        Validate.notNull(registeredUser);
        List<String> questionPageIds = questionManager.getUnsolvedQuestions(registeredUser);
        Map<String, ContentDTO> questionMap = questionManager.getQuestionMap(questionPageIds);
        Map<String, Map<String, List<QuestionValidationResponse>>> usersQuestionAttempts = questionManager.getQuestionAttemptsByUser(registeredUser);

        List<GameboardItem> questions = new ArrayList<>();
        for (String questionPageId : questionPageIds) {
            ContentDTO content = questionMap.get(questionPageId);

            if (content instanceof IsaacQuestionPageDTO) {
                if (bookOnly && !content.getTags().contains("book")) {
                    // Only interested in books, this is not a book. Skip it.
                    continue;
                }

                IsaacQuestionPageDTO qp = (IsaacQuestionPageDTO) content;
                if (qp.getSupersededBy() != null && !qp.getSupersededBy().equals("")) {
                    // This question has been superseded. Don't include it.
                    continue;
                }
                GameboardItem questionInfo = this.gameboardPersistenceManager.convertToGameboardItem(content);
                gameManager.augmentGameItemWithAttemptInformation(questionInfo, usersQuestionAttempts);
                questions.add(questionInfo);
            }
        }
        questions.sort(Comparator.nullsLast(Comparator.comparing(GameboardItem::getLevel,  Comparator.nullsLast(Comparator.naturalOrder()))));
        return questions.subList(0, Math.min(limit, questions.size()));
    }
}
