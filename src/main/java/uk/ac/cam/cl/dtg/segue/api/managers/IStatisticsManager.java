package uk.ac.cam.cl.dtg.segue.api.managers;

import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;

import java.util.Map;

/**
 * Created by du220 on 19/07/2017.
 */
public interface IStatisticsManager {

    /**
     * Output general statistics.
     * <p>
     * This returns a Map of String to Object and is intended to be sent directly to a
     * serializable facade endpoint.
     *
     * @return ImmutableMap(String, String) of (stat name, stat value)
     * @throws SegueDatabaseException - if there is a database error.
     */
    Map<String, Object> getGeneralStatistics()
            throws SegueDatabaseException;

    /**
     * LogCount.
     *
     * @param logTypeOfInterest
     *            - the log event that we care about.
     * @return the number of logs of that type (or an estimate).
     * @throws SegueDatabaseException
     *             if there is a problem with the database.
     */
    Long getLogCount(final String logTypeOfInterest) throws SegueDatabaseException;

    /**
     * getUserQuestionInformation. Produces a map that contains information about the total questions attempted,
     * (and those correct) "totalQuestionsAttempted", "totalCorrect",
     *  ,"attemptsByTag", questionAttemptsByLevelStats.
     *
     * @param userOfInterest
     *            - the user you wish to compile statistics for.
     * @return gets high level statistics about the questions a user has completed.
     * @throws SegueDatabaseException
     *             - if something went wrong with the database.
     * @throws ContentManagerException
     *             - if we are unable to look up the content.
     */
    Map<String, Object> getUserQuestionInformation(final RegisteredUserDTO userOfInterest)
            throws SegueDatabaseException, ContentManagerException;

    /**
     * Gets additional information for a user outlining their progress for teacher-based activity.
     *
     * @param userOfInterest the user we want infor for
     * @return a map of teacher activities and the user's progress in each of them
     */
    Map<String, Object> getDetailedUserStatistics(final RegisteredUserDTO userOfInterest);

}
