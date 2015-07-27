/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import uk.ac.cam.cl.dtg.segue.dos.LogEvent;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * Interface for logging components.
 * 
 * This is for logging user interaction events for the purpose of research and / or technology improvement not for
 * logging system errors / events.
 * 
 */
public interface ILogManager {
    /**
     * Log an event with the persistence logging framework without looking up the user from the database.
     * 
     * @param user
     *            - user to log must not be null.
     * @param httpRequest
     *            - so we can figure out request specific information e.g. ip address.
     * @param eventType
     *            - Type of event that we are interested in.
     * @param eventDetails
     *            - Additional information associated with the event - this is expected to be a json deserializable
     *            object
     */
    void logEvent(AbstractSegueUserDTO user, HttpServletRequest httpRequest, String eventType, Object eventDetails);

    /**
     * Log an event with the persistence logging framework without looking up the user from the database.
     * 
     * @param user
     *            - user to log must not be null.
     * @param eventType
     *            - Type of event that we are interested in.
     * @param eventDetails
     *            - Additional information associated with the event - this is expected to be a json deserializable
     *            object
     */
    void logInternalEvent(AbstractSegueUserDTO user, String eventType, Object eventDetails);

    /**
     * This method will endeavour to find all log events for a given user and reassign ownership to a
     * registered user.
     * 
     * It assumes that the new userId is a registered user of the system and not anonymous.
     * 
     * @param oldUserId
     *            - the id of the old anonymous user
     * @param newUserId
     *            - the user object of the newly registered user.
     */
    void transferLogEventsToRegisteredUser(final String oldUserId, final String newUserId);

    /**
     * To enable some simple analytics we provide a way to query logs by event type.
     * 
     * @param type
     *            - string representing the type of event to find.
     * @return all events of the type requested or null if none available. The map should be of type String, Object
     */
    List<LogEvent> getLogsByType(String type);

    /**
     * Allows filtering by date range.
     * 
     * Will use now as the toDate.
     * 
     * @param type
     *            - string representing the type of event to find.
     * @param fromDate
     *            - date to start search
     * @return all events of the type requested or null if none available. The map should be of type String, Object
     */
    List<LogEvent> getLogsByType(String type, Date fromDate);

    /**
     * Allows filtering by date range.
     * 
     * @param type
     *            - string representing the type of event to find.
     * @param fromDate
     *            - date to start search
     * @param toDate
     *            - date to end search.
     * @return all events of the type requested or null if none available. The map should be of type String, Object
     */
    List<LogEvent> getLogsByType(String type, Date fromDate, Date toDate);

    /**
     * Convenience method to find out how many of a particular type of event have been logged.
     * 
     * @param type
     *            - event type of interest.
     * @return the number of that type recorded.
     */
    Long getLogCountByType(String type);

    /**
     * Allows filtering by date range.
     * 
     * @param type
     *            - string representing the type of event to find.
     * @param fromDate
     *            - date to start search
     * @param toDate
     *            - date to end search.
     * @param usersOfInterest
     *            - users of interest.
     * @return all events of the type requested or null if none available. The map should be of type String, Object
     */
    List<LogEvent> getLogsByType(String type, Date fromDate, Date toDate, List<RegisteredUserDTO> usersOfInterest);

    /**
     * @return get a set of all ip addresses ever seen in the log events.
     */
    Set<String> getAllIpAddresses();

    /**
     * @param userType
     *            to filter by.
     * @return All logs belonging to a particular class of user.
     */
    List<LogEvent> getAllLogsByUserType(Class<? extends AbstractSegueUserDTO> userType);

    /**
     * @param userType
     *            to filter by.
     * @param eventType
     *            - event of interest
     * @return All logs belonging to a particular class of user and eventId
     */
    List<LogEvent> getAllLogsByUserTypeAndEvent(Class<? extends AbstractSegueUserDTO> userType, String eventType);

    /**
     * @param prototype
     *            containing the user identifier.
     * @return all logs for the user with the identifier provided.
     */
    List<LogEvent> getAllLogsByUser(AbstractSegueUserDTO prototype);

    /**
     * Get the last log event for a given user.
     * 
     * @param prototype
     *            - containing the user's unique identifier.
     * @return the last log event.
     */
    LogEvent getLastLogForUser(AbstractSegueUserDTO prototype);

    /**
     * A more efficient way of getting the last log for all users.
     * 
     * @param qualifyingLogEventType
     *            - the log event type to include in the data.
     * @return where string is the user id and the logevent is the most recent
     */
    Map<String, LogEvent> getLastLogForAllUsers(@Nullable final String qualifyingLogEventType);

    /**
     * A more efficient way of getting the last log for all users.
     * 
     * @return where string is the user id and the logevent is the most recent
     */
    Map<String, LogEvent> getLastLogForAllUsers();

    /**
     * returns a set of event types known about from the db.
     * 
     * @return Set of event types.
     */
    Set<String> getAllEventTypes();
}
