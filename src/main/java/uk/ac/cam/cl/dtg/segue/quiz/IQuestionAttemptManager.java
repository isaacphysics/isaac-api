package uk.ac.cam.cl.dtg.segue.quiz;

import uk.ac.cam.cl.dtg.segue.api.Constants.*;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * IQuestionAttemptManager. Objects implementing this interface are responsible for recording question attempts
 * information
 * 
 */
public interface IQuestionAttemptManager {
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
    void registerQuestionAttempt(final Long userId, final String questionPageId, final String fullQuestionId,
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
    Map<String, Map<String, List<QuestionValidationResponse>>> getQuestionAttempts(final Long userId)
            throws SegueDatabaseException;

    /**
     * Get a users most recent question attempts.
     *
     * @param userId
     *            - the id of the user to search for.
     * @param limit
     *            - the maximum number of question attempts to return
     * @return the questionAttempts map or an empty map if the user has not yet registered any attempts.
     * @throws SegueDatabaseException
     *             - If there is a database error.
     */
    Map<String, Map<String, List<QuestionValidationResponse>>> getMostRecentQuestionAttempts(final Long userId, final Integer limit)
            throws SegueDatabaseException;

    /**
     * A method that makes a single database request for a group of users and questions to get all of their attempt
     * information back.
     * 
     * @param userIds
     *            - list of user ids to look up results for.
     * @param questionPage
     *            - list of question page ids (prefixes to question ids) that we should look up.
     * @return a Map of userId --> Map of question_page --> Map of Question_id --> List of lightweight users attempts
     *          (without the actual question attempt values).
     * @throws SegueDatabaseException
     *             - if a database error occurrs
     */
    Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>>
            getQuestionAttemptsByUsersAndQuestionPrefix(List<Long> userIds, List<String> questionPage)
            throws SegueDatabaseException;
    
    /**
     * @param userId
     *            - some anonymous identifier
     * @param questionPageId
     *            - question page id
     * @param fullQuestionId
     *            - full question id
     * @param questionAttempt
     *            - attempt details
     * @throws SegueDatabaseException
     *             - if there are db problems
     */
    void registerAnonymousQuestionAttempt(String userId, String questionPageId, String fullQuestionId,
            QuestionValidationResponse questionAttempt) throws SegueDatabaseException;

    /**
     * @param anonymousId
     *            - some anonymous identifier
     * @return List of questionpage --> question id --> list of QuestionResponses.
     */
    Map<String, Map<String, List<QuestionValidationResponse>>> getAnonymousQuestionAttempts(String anonymousId) throws SegueDatabaseException;

    /**
     * @param anonymousId
     *            - some anonymous identifier
     * @param limit
     *            - the maximum number of question attempts to return
     * @return List of questionpage --> question id --> list of QuestionResponses.
     */
    Map<String, Map<String, List<QuestionValidationResponse>>> getMostRecentAnonymousQuestionAttempts(String anonymousId, final Integer limit)
            throws SegueDatabaseException;


    /**
     * Convenience method to merge anonymous user question attempts with registered user records.
     * 
     * @param anonymousUserId
     *            - the id of an anonymous user.
     * @param registeredUserId
     *            - the id of the registered user
     * @throws SegueDatabaseException
     *             - if something goes wrong.
     */
    void mergeAnonymousQuestionInformationWithRegisteredUserRecord(String anonymousUserId, Long registeredUserId)
            throws SegueDatabaseException;

    /**
     * Count the users by role which have answered questions over the previous time interval
     * @param timeInterval time interval over which to count
     * @return map of counts for each role
     * @throws SegueDatabaseException
     *             - if there is a problem with the database.
     */
    Map<Role, Long> getAnsweredQuestionRolesOverPrevious(TimeInterval timeInterval)
            throws SegueDatabaseException;

    /**
     * getQuestionAttemptCountForUserByDateRange.
     *
     * An optimised method for getting question attempt counts data by month for a given date range.
     *
     * This relies on the database doing the binning for us.
     *
     * @param fromDate
     *            - the earliest date the log event can have occurred
     * @param toDate
     *            - the latest date the log event can have occurred
     * @param userId
     *            - the list of users ids we are interested in.
     * @return a collection of log events that match the above criteria or an empty collection.
     * @throws SegueDatabaseException
     *             - if we cannot retrieve the data from the database.
     */
    Map<Date, Long> getQuestionAttemptCountForUserByDateRange(Date fromDate, Date toDate,
                                                              Long userId, Boolean perDay) throws SegueDatabaseException;
}
