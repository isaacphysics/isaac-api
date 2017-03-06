/*
 * Copyright 2017 James Sharkey
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
package uk.ac.cam.cl.dtg.segue.dos;

import java.util.List;
import java.util.Map;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 *  Abstract class for managing general user preferences.
 *
 *  @author James Sharkey
 */
public abstract class AbstractUserPreferenceManager {

    /**
     * Get a specific preference for a specific user.
     * @param preferenceType - the type of preferences interested in
     * @param preferenceName - the name of the specific preference
     * @param userId - the ID of the user interested in
     * @return the UserPreference object
     * @throws SegueDatabaseException - if a database error occurs
     */
    public abstract UserPreference getUserPreference(final String preferenceType, final String preferenceName, final long userId)
            throws SegueDatabaseException;

    /**
     * Get a specific preference for many users.
     * @param preferenceType - the type of preferences interested in
     * @param preferenceName - the name of the specific preference
     * @param users - a list of user objects interested in
     * @return a map of user IDs to the UserPreference objects
     * @throws SegueDatabaseException - if a database error occurs
     */
    public abstract Map<Long, UserPreference> getUsersPreference(final String preferenceType, final String preferenceName,
                                                                 final List<RegisteredUserDTO> users)
            throws SegueDatabaseException;

    /**
     * Get all preferences of one type for a specific user.
     * @param preferenceType - the type of preferences inrterested in
     * @param userId - the ID of the user interested in
     * @return a list of the UserPreference objects
     * @throws SegueDatabaseException - if a database error occurs
     */
    public abstract List<UserPreference> getUserPreferences(final String preferenceType, final long userId)
            throws SegueDatabaseException;

    /**
     * Get all preferences of one type for many users.
     * @param preferenceType - the type of preferences inrterested in
     * @param users - a list of user objects interested in
     * @return a map of user IDs to a list of the UserPreference objects
     * @throws SegueDatabaseException - if a database error occurs
     */
    public abstract Map<Long, List<UserPreference>> getUserPreferences(final String preferenceType, final List<RegisteredUserDTO> users)
            throws SegueDatabaseException;

    /**
     * Save a users preferences.
     * @param userPreferences - a list of the UserPreference objects to save
     * @throws SegueDatabaseException - if a database error occurs
     */
    public abstract void saveUserPreferences(final List<UserPreference> userPreferences)
            throws SegueDatabaseException;
}
