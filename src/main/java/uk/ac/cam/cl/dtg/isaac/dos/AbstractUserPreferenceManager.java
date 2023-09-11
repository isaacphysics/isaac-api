/**
 * Copyright 2017 James Sharkey
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.List;
import java.util.Map;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

/**
 * Abstract class for managing general user preferences.
 *
 * @author James Sharkey
 */
public abstract class AbstractUserPreferenceManager {

  /**
   * Get a specific preference for a specific user.
   *
   * @param preferenceType - the type of preferences interested in
   * @param preferenceName - the name of the specific preference
   * @param userId         - the ID of the user interested in
   * @return the UserPreference object
   * @throws SegueDatabaseException - if a database error occurs
   */
  public abstract UserPreference getUserPreference(String preferenceType, String preferenceName, long userId)
      throws SegueDatabaseException;

  /**
   * Get a specific preference for many users.
   *
   * @param preferenceType - the type of preferences interested in
   * @param preferenceName - the name of the specific preference
   * @param users          - a list of user objects interested in
   * @return a map of user IDs to the UserPreference objects
   * @throws SegueDatabaseException - if a database error occurs
   */
  public abstract Map<Long, UserPreference> getUsersPreference(
      String preferenceType, String preferenceName, List<RegisteredUserDTO> users) throws SegueDatabaseException;

  /**
   * Get all preferences of one type for a specific user.
   *
   * @param preferenceType - the type of preferences interested in
   * @param userId         - the ID of the user interested in
   * @return a list of the UserPreference objects
   * @throws SegueDatabaseException - if a database error occurs
   */
  public abstract List<UserPreference> getUserPreferences(String preferenceType, long userId)
      throws SegueDatabaseException;

  /**
   * Get all preferences for a specific user.
   *
   * @param userId - the ID of the user interested in
   * @return a list of the UserPreference objects
   * @throws SegueDatabaseException - if a database error occurs
   */
  public abstract List<UserPreference> getAllUserPreferences(long userId) throws SegueDatabaseException;

  /**
   * Get all preferences of one type for many users.
   *
   * @param preferenceType - the type of preferences interested in
   * @param users          - a list of user objects interested in
   * @return a map of user IDs to a list of the UserPreference objects
   * @throws SegueDatabaseException - if a database error occurs
   */
  public abstract Map<Long, List<UserPreference>> getUserPreferences(String preferenceType,
                                                                     List<RegisteredUserDTO> users)
      throws SegueDatabaseException;

  /**
   * Save a users preferences.
   *
   * @param userPreferences - a list of the UserPreference objects to save
   * @throws SegueDatabaseException - if a database error occurs
   */
  public abstract void saveUserPreferences(List<UserPreference> userPreferences)
      throws SegueDatabaseException;
}
