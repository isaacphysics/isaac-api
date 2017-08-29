/*
 * Copyright 2017 Stephen Cummins
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

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.users.LocalUserCredential;

import java.sql.*;
import java.util.List;

/**
 * Postgres specific implementation of a password data manager.
 */
public class PgPasswordDataManager implements IPasswordDataManager {
    private final PostgresSqlDb database;

    /**
     * PgUsers.
     * @param ds - the postgres datasource to use
     */
    @Inject
    public PgPasswordDataManager(final PostgresSqlDb ds) {
        this.database = ds;
    }

    @Override
    public LocalUserCredential getLocalUserCredential(Long userId) throws SegueDatabaseException {
        if (null == userId) {
            return null;
        }

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_credentials WHERE user_id = ?");
            pst.setLong(1, userId);

            ResultSet results = pst.executeQuery();

            return this.findOne(results);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public LocalUserCredential getLocalUserCredentialByResetToken(final String token) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_credentials WHERE reset_token = ?");
            pst.setString(1, token);

            ResultSet results = pst.executeQuery();

            return this.findOne(results);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception while retrieving by reset token", e);
        }
    }

    @Override
    public LocalUserCredential createOrUpdateLocalUserCredential(LocalUserCredential credsToSave) throws SegueDatabaseException {
        // determine if it is a create or update
        LocalUserCredential lc = this.getLocalUserCredential(credsToSave.getUserId());

        if (null == lc) {
            // create a new one
            lc = this.createCredentials(credsToSave);
        } else {
            // update
            lc = this.updateCredentials(credsToSave);
        }

        return lc;
    }

    private LocalUserCredential createCredentials(LocalUserCredential credsToSave) throws SegueDatabaseException {

        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn
                    .prepareStatement(
                            "INSERT INTO user_credentials(user_id, password, security_scheme, secure_salt, reset_token, "
                                    + "reset_expiry, last_updated) "
                                    + "VALUES (?, ?, ?, ?, ?, ?, ?);",
                            Statement.RETURN_GENERATED_KEYS);


            setValueHelper(pst, 1, credsToSave.getUserId());
            setValueHelper(pst, 2, credsToSave.getPassword());
            setValueHelper(pst, 3, credsToSave.getSecurityScheme());
            setValueHelper(pst, 4, credsToSave.getSecureSalt());
            setValueHelper(pst, 5, credsToSave.getResetToken());
            setValueHelper(pst, 6, credsToSave.getResetExpiry());
            setValueHelper(pst, 7, new java.util.Date());

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save user.");
            }

            return credsToSave;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    private LocalUserCredential updateCredentials(LocalUserCredential credsToSave) throws SegueDatabaseException {
        LocalUserCredential existingRecord = this.getLocalUserCredential(credsToSave.getUserId());
        if (null == existingRecord) {
            throw new SegueDatabaseException("The credentials you have tried to update do not exist.");
        }

        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn
                    .prepareStatement(
                            "UPDATE user_credentials SET password = ?, security_scheme = ?, secure_salt = ?, reset_token = ?, "
                                    + "reset_expiry = ?, last_updated = ? "
                                    + "WHERE user_id = ?;");


            setValueHelper(pst, 1, credsToSave.getPassword());
            setValueHelper(pst, 2, credsToSave.getSecurityScheme());
            setValueHelper(pst, 3, credsToSave.getSecureSalt());
            setValueHelper(pst, 4, credsToSave.getResetToken());
            setValueHelper(pst, 5, credsToSave.getResetExpiry());
            setValueHelper(pst, 6, new java.util.Date());
            setValueHelper(pst, 7, credsToSave.getUserId());

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save user.");
            }

            return this.getLocalUserCredential(existingRecord.getUserId());
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }


    /**
     * findOne helper method to ensure that only one result matches the search criteria.
     *
     * @param results
     *            - from a jdbc database search
     * @return a single credential that matches the search criteria or null of no matches found.
     * @throws SQLException
     *             - if a db error occurs
     * @throws SegueDatabaseException
     *             - if more than one result is returned
     */
    private LocalUserCredential findOne(final ResultSet results) throws SegueDatabaseException, SQLException {
        // are there any results
        if (!results.isBeforeFirst()) {
            return null;
        }

        List<LocalUserCredential> listOfResults = Lists.newArrayList();
        while (results.next()) {
            listOfResults.add(buildCredential(results));
        }

        if (listOfResults.size() > 1) {
            throw new SegueDatabaseException("Ambiguous result, expected single result and found more than one"
                    + listOfResults);
        }

        return listOfResults.get(0);
    }

    /**
     * Construct an appropriate POJO.
     *
     * @param results - sql results
     * @return localUserCredential object
     * @throws SQLException if we can't get a value required.
     */
    private LocalUserCredential buildCredential(ResultSet results) throws SQLException{
        LocalUserCredential toReturn = new LocalUserCredential();
        toReturn.setUserId(results.getLong("user_id"));
        toReturn.setPassword(results.getString("password"));
        toReturn.setSecurityScheme(results.getString("security_scheme"));
        toReturn.setSecureSalt(results.getString("secure_salt"));
        toReturn.setResetToken(results.getString("reset_token"));
        toReturn.setResetExpiry(results.getTimestamp("reset_expiry"));
        toReturn.setCreated(results.getTimestamp("created"));
        toReturn.setLastUpdated(results.getTimestamp("last_updated"));
        return toReturn;
    }

    /**
     * Helper that picks the correct pst method based on the value provided.
     *
     * @param pst - prepared statement - already initialised
     * @param index - index of the value to be replaced in the pst
     * @param value - value
     * @throws SQLException
     */
    private void setValueHelper(final PreparedStatement pst, final int index, final Object value) throws SQLException {
        if (null == value) {
            pst.setNull(index, java.sql.Types.NULL);
            return;
        }

        if (value.getClass().isEnum()) {
            pst.setString(index, ((Enum<?>) value).name());
        }

        if (value instanceof String) {
            pst.setString(index, (String) value);
        }

        if (value instanceof Integer) {
            pst.setInt(index, (Integer) value);
        }

        if (value instanceof Long) {
            pst.setLong(index, (Long) value);
        }

        if (value instanceof java.util.Date) {
            pst.setTimestamp(index, new java.sql.Timestamp(((java.util.Date) value).getTime()));
        }
    }
}
