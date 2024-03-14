/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao;

import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;

import jakarta.servlet.http.HttpServletRequest;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

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
     * @throws SegueDatabaseException 
     */
    void logEvent(AbstractSegueUserDTO user, HttpServletRequest httpRequest, LogType eventType, Object eventDetails);

    /**
     * Log an arbitrary event from the frontend.
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
     * @throws SegueDatabaseException
     */
    void logExternalEvent(AbstractSegueUserDTO user, HttpServletRequest httpRequest, String eventType, Object eventDetails);

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
     * @throws SegueDatabaseException 
     */
    void logInternalEvent(AbstractSegueUserDTO user, LogType eventType, Object eventDetails);

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
     * Convenience method to find out how many of a particular type of event have been logged.
     * 
     * @param type
     *            - event type of interest.
     * @return the number of that type recorded.
     * @throws SegueDatabaseException 
     */
    Long getLogCountByType(String type) throws SegueDatabaseException;
}
