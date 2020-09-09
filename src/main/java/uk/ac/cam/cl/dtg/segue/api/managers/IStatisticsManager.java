package uk.ac.cam.cl.dtg.segue.api.managers;

import org.joda.time.LocalDate;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.util.locations.Location;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by du220 on 19/07/2017.
 */
public interface IStatisticsManager {

    /**
     * Output general stats. This returns a Map of String to Object and is intended to be sent directly to a
     * serializable facade endpoint.
     *
     * @return ImmutableMap<String, String> (stat name, stat value)
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
     * Get an overview of all school performance. This is for analytics / admin users.
     *
     * @return list of school to statistics mapping. The object in the map is another map with keys connections,
     *         numberActiveLastThirtyDays.
     *
     * @throws UnableToIndexSchoolsException
     *             - if there is a problem getting school details.
     * @throws SegueDatabaseException
     *             - if there is a database exception.
     */
    List<Map<String, Object>> getSchoolStatistics()
            throws UnableToIndexSchoolsException, SegueDatabaseException, SegueSearchException;

    /**
     * Get the number of users per school.
     *
     * @return A map of schools to integers (representing the number of registered users)
     * @throws UnableToIndexSchoolsException as per the description
     */
    Map<School, List<RegisteredUserDTO>> getUsersBySchool() throws UnableToIndexSchoolsException, SegueSearchException;

    /**
     * Find all users belonging to a given school.
     *
     * @param schoolId
     *            - that we are interested in.
     * @return list of users.
     * @throws SegueDatabaseException
     *             - if there is a general database error
     * @throws ResourceNotFoundException
     *             - if we cannot locate the school requested.
     * @throws UnableToIndexSchoolsException
     *             - if the school list has not been indexed.
     */
    List<RegisteredUserDTO> getUsersBySchoolId(final String schoolId) throws ResourceNotFoundException,
            SegueDatabaseException, UnableToIndexSchoolsException, SegueSearchException;

    /**
     * @return a list of userId's to last event timestamp
     */
    Map<String, Date> getLastSeenUserMap();

    /**
     * @param qualifyingLogEvent
     *            the string event type that will be looked for.
     * @return a map of userId's to last event timestamp
     * @throws SegueDatabaseException
     */
    Map<String, Date> getLastSeenUserMap(String qualifyingLogEvent) throws SegueDatabaseException;

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
     * getEventLogsByDate.
     *
     * @param eventTypes
     *            - of interest
     * @param fromDate
     *            - of interest
     * @param toDate
     *            - of interest
     * @param binDataByMonth
     *            - shall we group data by the first of every month?
     * @return Map of eventType --> map of dates and frequency
     * @throws SegueDatabaseException
     */
    Map<String, Map<LocalDate, Long>> getEventLogsByDate(final Collection<String> eventTypes,
                                                                final Date fromDate, final Date toDate, final boolean binDataByMonth) throws SegueDatabaseException;

    /**
     * getEventLogsByDate.
     *
     * @param eventTypes
     *            - of interest
     * @param fromDate
     *            - of interest
     * @param toDate
     *            - of interest
     * @param userList
     *            - user prototype to filter events. e.g. user(s) with a particular id or role.
     * @param binDataByMonth
     *            - shall we group data by the first of every month?
     * @return Map of eventType --> map of dates and frequency
     * @throws SegueDatabaseException
     */
    Map<String, Map<LocalDate, Long>> getEventLogsByDateAndUserList(final Collection<String> eventTypes,
                                                                           final Date fromDate, final Date toDate, final List<RegisteredUserDTO> userList,
                                                                           final boolean binDataByMonth) throws SegueDatabaseException;


    /**
     * Calculate the number of users from the list provided that meet the criteria.
     *
     * @param users
     *            - collection of users to consider.
     * @param lastSeenUserMap
     *            - The map of user event data. UserId --> last event date.
     * @param daysFromToday
     *            - the number of days from today that should be included in the calculation e.g. 7 would be the last
     *            week's data.
     * @return a collection containing the users who meet the criteria
     */
    public Collection<RegisteredUserDTO> getNumberOfUsersActiveForLastNDays(final Collection<RegisteredUserDTO> users,
                                                                            final Map<String, Date> lastSeenUserMap, final int daysFromToday);

    /**
     * getLocationInformation.
     *
     * @param fromDate
     *            - date to start search
     * @param toDate
     *            - date to end search
     * @return the list of all locations we know about..
     * @throws SegueDatabaseException
     *             if we can't read from the database.
     */
    @SuppressWarnings("unchecked")
    Collection<Location> getLocationInformation(final Date fromDate, final Date toDate) throws SegueDatabaseException;


    /**
     * Gets additional information for a user outlining their progress for teacher-based activity
     *
     * @param userOfInterest the user we want infor for
     * @return a map of teacher activities and the user's progress in each of them
     */
    Map<String, Object> getDetailedUserStatistics(final RegisteredUserDTO userOfInterest);



}
