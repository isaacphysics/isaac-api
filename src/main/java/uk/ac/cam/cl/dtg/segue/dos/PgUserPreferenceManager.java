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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static java.lang.Math.min;

/**
 *  A postgres specific User Preference Manager
 */
public class PgUserPreferenceManager extends AbstractUserPreferenceManager {

    private final PostgresSqlDb database;
    private static final Logger log = LoggerFactory.getLogger(PgUserPreferenceManager.class);

    /**
     * Create a PgUserPreferenceManager
     * @param database - a pre-configured postgres database object
     */
    @Inject
    public PgUserPreferenceManager(PostgresSqlDb database) {
        this.database = database;
    }

    private UserPreference userPreferenceFromResultSet(ResultSet results) throws SQLException {
        return new UserPreference(results.getLong("user_id"), results.getString("preference_type"),
                results.getString("preference_name"), results.getBoolean("preference_value"));
    }

    @Override
    public UserPreference getUserPreference(String preferenceType, String preferenceName, long userId)
            throws SegueDatabaseException {
        Validate.notBlank(preferenceType);
        Validate.notBlank(preferenceName);

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_preferences WHERE user_id=? AND preference_type=? AND preference_name=?;");

            pst.setLong(1, userId);
            pst.setString(2, preferenceType);
            pst.setString(3, preferenceName);
            pst.setMaxRows(1); // There is a primary key to ensure uniqueness!

            ResultSet results = pst.executeQuery();

            if (results.next()) {
                return userPreferenceFromResultSet(results);
            }

            // We must not have found anything:
            return null;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public Map<Long, UserPreference> getUsersPreference(String preferenceType, String preferenceName, List<RegisteredUserDTO> users)
            throws SegueDatabaseException {
        Validate.notBlank(preferenceType);
        Validate.notBlank(preferenceName);

        Map<Long, UserPreference> usersPreferenceMap = Maps.newHashMap();

        int pageSize = 10000;
        int fromIndex = 0;
        int toIndex = min(pageSize, users.size());

        while (fromIndex < toIndex) {

            List<RegisteredUserDTO> pagedUsers = users.subList(fromIndex, toIndex);
            try (Connection conn = database.getDatabaseConnection()) {
                PreparedStatement pst;
                StringBuilder sb = new StringBuilder();
                sb.append("SELECT * FROM user_preferences WHERE user_id IN (");

                for (int i = 0; i < pagedUsers.size(); i++) {
                    sb.append("?").append(i < pagedUsers.size() - 1 ? ", " : "");
                }
                sb.append(") AND preference_type=? AND preference_name=? ORDER BY user_id ASC;");

                pst = conn.prepareStatement(sb.toString());
                for (int i = 1; i <= pagedUsers.size(); i++) {
                    pst.setLong(i, pagedUsers.get(i - 1).getId());
                }
                pst.setString(pagedUsers.size() + 1, preferenceType);
                pst.setString(pagedUsers.size() + 2, preferenceName);

                ResultSet results = pst.executeQuery();

                while (results.next()) {
                    Long userId = results.getLong("user_id");
                    UserPreference pref = userPreferenceFromResultSet(results);
                    usersPreferenceMap.put(userId, pref);
                }

                fromIndex = toIndex;
                toIndex = min(toIndex + pageSize, users.size());

            } catch (SQLException e) {
                throw new SegueDatabaseException("Postgres exception", e);
            }
        }
        return usersPreferenceMap;
    }

    @Override
    public List<UserPreference> getUserPreferences(String preferenceType, long userId) throws SegueDatabaseException {
        Validate.notBlank(preferenceType);

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_preferences WHERE user_id=? AND preference_type=?;");

            pst.setLong(1, userId);
            pst.setString(2, preferenceType);

            ResultSet results = pst.executeQuery();

            List<UserPreference> userPreferences = Lists.newArrayList();

            while (results.next()) {
                UserPreference pref = userPreferenceFromResultSet(results);
                userPreferences.add(pref);
            }

            return userPreferences;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public List<UserPreference> getAllUserPreferences(long userId) throws SegueDatabaseException {

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_preferences WHERE user_id=?;");

            pst.setLong(1, userId);

            ResultSet results = pst.executeQuery();

            List<UserPreference> userPreferences = Lists.newArrayList();

            while (results.next()) {
                UserPreference pref = userPreferenceFromResultSet(results);
                userPreferences.add(pref);
            }

            return userPreferences;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public Map<Long, List<UserPreference>> getUserPreferences(String preferenceType, List<RegisteredUserDTO> users)
            throws SegueDatabaseException {
        Validate.notBlank(preferenceType);

        Map<Long, List<UserPreference>> usersPreferencesMap = Maps.newHashMap();

        int pageSize = 10000;
        int fromIndex = 0;
        int toIndex = min(pageSize, users.size());

        while (fromIndex < toIndex) {

            List<RegisteredUserDTO> pagedUsers = users.subList(fromIndex, toIndex);
            try (Connection conn = database.getDatabaseConnection()) {
                PreparedStatement pst;
                StringBuilder sb = new StringBuilder();
                sb.append("SELECT * FROM user_preferences WHERE user_id IN (");

                for (int i = 0; i < pagedUsers.size(); i++) {
                    sb.append("?").append(i < pagedUsers.size() - 1 ? ", " : "");
                }
                sb.append(") AND preference_type=? ORDER BY user_id ASC, preference_name ASC;");

                pst = conn.prepareStatement(sb.toString());
                for (int i = 1; i <= pagedUsers.size(); i++) {
                    pst.setLong(i, pagedUsers.get(i - 1).getId());
                }
                pst.setString(pagedUsers.size() + 1, preferenceType);

                ResultSet results = pst.executeQuery();

                while (results.next()) {
                    Long userId = results.getLong("user_id");
                    UserPreference pref = userPreferenceFromResultSet(results);
                    List<UserPreference> values;
                    if (usersPreferencesMap.containsKey(userId) && usersPreferencesMap.get(userId) != null) {
                        values = usersPreferencesMap.get(userId);
                    } else {
                        values = Lists.newArrayList();
                        usersPreferencesMap.put(userId, values);
                    }
                    values.add(pref);
                }

                fromIndex = toIndex;
                toIndex = min(toIndex + pageSize, users.size());

            } catch (SQLException e) {
                throw new SegueDatabaseException("Postgres exception", e);
            }
        }
        return usersPreferencesMap;
    }

    @Override
    public void saveUserPreferences(List<UserPreference> userPreferences) throws SegueDatabaseException {

        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            conn.setAutoCommit(false);
            for (UserPreference preference : userPreferences) {
                // Upsert the value in, using Postgres 9.5 syntax 'ON CONFLICT DO UPDATE ...'
                pst = conn.prepareStatement("INSERT INTO user_preferences (user_id, preference_type, preference_name, preference_value)" +
                        " VALUES (?, ?, ?, ?) ON CONFLICT (user_id, preference_type, preference_name) DO UPDATE" +
                        " SET preference_value=excluded.preference_value WHERE user_preferences.user_id=excluded.user_id" +
                        " AND user_preferences.preference_type=excluded.preference_type" +
                        " AND user_preferences.preference_name=excluded.preference_name;");
                pst.setLong(1, preference.getUserId());
                pst.setString(2, preference.getPreferenceType());
                pst.setString(3, preference.getPreferenceName());
                pst.setBoolean(4, preference.getPreferenceValue());

                pst.executeUpdate();
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception on upsert ", e);
        }
    }
}
