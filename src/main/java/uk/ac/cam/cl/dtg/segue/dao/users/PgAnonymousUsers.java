/*
 * Copyright 2019 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dao.users;

import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.users.AnonymousUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * @author Stephen Cummins
 *
 */
public class PgAnonymousUsers implements IAnonymousUserDataManager {
    private final PostgresSqlDb database;

    /**
     * PgAnonymousUsers.
     * @param ds - the postgres datasource to use
     */
    @Inject
    public PgAnonymousUsers(final PostgresSqlDb ds) {
        this.database = ds;
    }

    @Override
    public AnonymousUser storeAnonymousUser(final AnonymousUser user) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {

            pst = conn.prepareStatement("INSERT INTO temporary_user_store (id, temporary_app_data, created, last_updated)" +
                    " VALUES (?,?::text::jsonb,?,?);");
            pst.setString(1, user.getSessionId());
            pst.setString(2, "{\"questionAttempts\":{}}");
            pst.setTimestamp(3, new java.sql.Timestamp(user.getDateCreated().getTime()));
            pst.setTimestamp(4, new java.sql.Timestamp(user.getDateCreated().getTime()));

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save anonymous user.");
            }
            return user;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception on creating anonymous user ", e);
        }
    }

    @Override
    public void deleteAnonymousUser(final AnonymousUser userToDelete) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("DELETE FROM temporary_user_store WHERE id = ?");
            pst.setString(1, userToDelete.getSessionId());

            int executeUpdate = pst.executeUpdate();
            if (executeUpdate == 0) {
                throw new ResourceNotFoundException("Could not delete the requested anonymous user.");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception while trying to delete anonymous user", e);
        }
    }

    @Override
    public AnonymousUser getById(final String id) throws SegueDatabaseException {
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement("SELECT * FROM temporary_user_store WHERE id = ?");
            pst.setString(1, id);

            ResultSet result = pst.executeQuery();
            // are there any results
            if (!result.isBeforeFirst()) {
                return null;
            }
            result.next();

            AnonymousUser userToReturn = new AnonymousUser(result.getString("id"),
                    result.getTimestamp("created"), result.getTimestamp("last_updated"));
            updateLastUpdatedDate(userToReturn);

            return userToReturn;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception while trying to get anonymous user", e);
        }
    }

    @Override
    public Long getCountOfAnonymousUsers() throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT COUNT(*) AS TOTAL FROM temporary_user_store;");

            ResultSet results = pst.executeQuery();
            results.next();
            return results.getLong("TOTAL");
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception: Unable to count log events by type", e);
        }
    }

    /**
     * Mutates input object update date and persists update to db.
     * @param user - to update
     * @return user;
     */
    private AnonymousUser updateLastUpdatedDate(final AnonymousUser user) throws SegueDatabaseException {
        Validate.notNull(user);

        try (Connection conn = database.getDatabaseConnection()) {
            Date newUpdatedDate = new Date();
            PreparedStatement pst;
            pst = conn.prepareStatement("UPDATE temporary_user_store SET last_updated = ? WHERE id = ?");
            pst.setTimestamp(1, new java.sql.Timestamp(newUpdatedDate.getTime()));
            pst.setString(2, user.getSessionId());
            pst.execute();

            user.setLastUpdated(newUpdatedDate);

            return user;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
}