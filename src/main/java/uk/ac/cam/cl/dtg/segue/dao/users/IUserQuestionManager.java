package uk.ac.cam.cl.dtg.segue.dao.users;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.QuestionAttemptUserRecord;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;

/**
 * IUserQuestionManager.
 * 
 */
public interface IUserQuestionManager {
    
    /**
     * Update a particular field on a user object.
     * 
     * @param userId
     *            - the user id to try and find.
     * @param questionPageId
     *            - the high level id of the question page. This may be used for determining whether a page of questions
     *            has been completed.
     * @param fullQuestionId
     *            - the full id of the question.
     * @param questionAttempt
     *            - the question attempt object recording the users result.
     * @throws SegueDatabaseException
     *             - if there is an error during the database operation.
     */
    void registerQuestionAttempt(final String userId, final String questionPageId, final String fullQuestionId,
            final QuestionValidationResponse questionAttempt) throws SegueDatabaseException;

    /**
     * Get a users question attempts.
     * 
     * @param userId
     *            - the id of the user to search for.
     * @return the questionAttempts map or an empty map if the user has not yet registered any attempts.
     * @throws SegueDatabaseException
     *             - If there is a database error.
     */
    QuestionAttemptUserRecord getQuestionAttempts(final String userId) throws SegueDatabaseException;
}
