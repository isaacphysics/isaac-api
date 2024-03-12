/*
 * Copyright 2017 James Sharkey
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
package uk.ac.cam.cl.dtg.isaac.dos;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 *  A Postgres specific User Preference Manager.
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

        String query = "SELECT * FROM user_preferences WHERE user_id=? AND preference_type=? AND preference_name=?;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);
            pst.setString(2, preferenceType);
            pst.setString(3, preferenceName);
            pst.setMaxRows(1); // There is a primary key to ensure uniqueness!

            try (ResultSet results = pst.executeQuery()) {
                if (results.next()) {
                    return userPreferenceFromResultSet(results);
                }
                // We must not have found anything:
                return null;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public List<UserPreference> getUserPreferences(String preferenceType, long userId) throws SegueDatabaseException {
        Validate.notBlank(preferenceType);

        String query = "SELECT * FROM user_preferences WHERE user_id=? AND preference_type=?;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);
            pst.setString(2, preferenceType);

            try (ResultSet results = pst.executeQuery()) {
                List<UserPreference> userPreferences = Lists.newArrayList();

                while (results.next()) {
                    UserPreference pref = userPreferenceFromResultSet(results);
                    userPreferences.add(pref);
                }

                return userPreferences;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public List<UserPreference> getAllUserPreferences(long userId) throws SegueDatabaseException {

        String query = "SELECT * FROM user_preferences WHERE user_id=?;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, userId);

            try (ResultSet results = pst.executeQuery()) {

                List<UserPreference> userPreferences = Lists.newArrayList();

                while (results.next()) {
                    UserPreference pref = userPreferenceFromResultSet(results);
                    userPreferences.add(pref);
                }

                return userPreferences;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public void saveUserPreferences(List<UserPreference> userPreferences) throws SegueDatabaseException {
        // Upsert the value in, using Postgres 9.5 syntax 'ON CONFLICT DO UPDATE ...'
        // Only update a conflicting row if value has changed, to ensure the last_updated date remains accurate:
        String query = "INSERT INTO user_preferences(user_id, preference_type, preference_name, preference_value, last_updated) "
                + " VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)"
                + " ON CONFLICT (user_id, preference_type, preference_name) DO UPDATE"
                + " SET preference_value=excluded.preference_value, last_updated=excluded.last_updated"
                + " WHERE user_preferences.preference_value!=excluded.preference_value;";
        try (Connection conn = database.getDatabaseConnection()) {
            conn.setAutoCommit(false);
            for (UserPreference preference : userPreferences) {
                try (PreparedStatement pst = conn.prepareStatement(query)) {
                    pst.setLong(1, preference.getUserId());
                    pst.setString(2, preference.getPreferenceType());
                    pst.setString(3, preference.getPreferenceName());
                    pst.setBoolean(4, preference.getPreferenceValue());

                    pst.executeUpdate();
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception on upsert ", e);
        }
    }
}
