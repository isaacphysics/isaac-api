/*
 * Copyright 2017 Dan Underwood
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

import com.google.api.client.util.Lists;
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
 *  A postgres specific User Achievement Manager
 */
public class PgUserAchievementManager extends AbstractUserAchievementManager {

    private final PostgresSqlDb database;
    private static final Logger log = LoggerFactory.getLogger(PgUserAchievementManager.class);

    /**
     * Create a PgUserAchievementManager
     * @param database - a pre-configured postgres database object
     */
    @Inject
    public PgUserAchievementManager(PostgresSqlDb database) {
        this.database = database;
    }

    private UserAchievement userAchievementFromResultSet(ResultSet results) throws SQLException {
        return new UserAchievement(results.getString("achievement_id"), results.getInt("threshold"));
    }

    @Override
    public List<UserAchievement> getUserAchievements(long userId) throws SegueDatabaseException {

        try (Connection conn = this.database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_achievements WHERE user_id=?;");

            pst.setLong(1, userId);

            ResultSet results = pst.executeQuery();

            List<UserAchievement> userAchievements = Lists.newArrayList();

            while (results.next()) {
                UserAchievement achievement = userAchievementFromResultSet(results);
                userAchievements.add(achievement);
            }

            return userAchievements;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
}
