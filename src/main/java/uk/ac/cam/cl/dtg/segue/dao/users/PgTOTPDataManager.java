/*
 * Copyright 2020 Stephen Cummins
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
import uk.ac.cam.cl.dtg.segue.dao.AbstractPgDataManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.isaac.dos.users.TOTPSharedSecret;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

/**
 * Postgres specific implementation of a TOTP Secrets data manager.
 */
public class PgTOTPDataManager extends AbstractPgDataManager implements ITOTPDataManager {
    private final PostgresSqlDb database;

    /**
     * PgTOTPDataManager.
     * @param ds - the postgres datasource to use
     */
    @Inject
    public PgTOTPDataManager(final PostgresSqlDb ds) {
        this.database = ds;
    }

    @Override
    public TOTPSharedSecret get2FASharedSecret(final Long userId) throws SegueDatabaseException {
        if (null == userId) {
            return null;
        }

        String query = "SELECT * FROM user_totp WHERE user_id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query)
        ) {
            pst.setLong(FIELD_GET_SHARED_SECRET_USER_ID, userId);

            try (ResultSet results = pst.executeQuery()) {
                return this.findOne(results);
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public TOTPSharedSecret save2FASharedSecret(final Long userId, final TOTPSharedSecret credsToSave) throws SegueDatabaseException {
        // determine if it is a create or update
        TOTPSharedSecret lc = this.get2FASharedSecret(userId);

        if (null == lc) {
            // create a new one
            lc = this.createCredentials(credsToSave);
        } else {
            // update
            lc = this.updateCredentials(new TOTPSharedSecret(lc.getUserId(), credsToSave.getSharedSecret(), lc.getCreated(), new Date()));
        }

        return lc;
    }

    /**
     * Method to create a new TOTPShared Secret entry in the database.
     *
     * @param credsToSave - object to persist
     * @return the TOTPSharedSecret
     * @throws SegueDatabaseException - if we can't save the entry for some reason
     */
    private TOTPSharedSecret createCredentials(final TOTPSharedSecret credsToSave) throws SegueDatabaseException {
        String query = "INSERT INTO user_totp(user_id, shared_secret, created, last_updated) VALUES (?, ?, ?,?);";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
        ) {
            setValueHelper(pst, FIELD_CREATE_CREDENTIALS_USER_ID, credsToSave.getUserId());
            setValueHelper(pst, FIELD_CREATE_CREDENTIALS_SHARED_SECRET, credsToSave.getSharedSecret());
            setValueHelper(pst, FIELD_CREATE_CREDENTIALS_CREATED, credsToSave.getCreated());
            setValueHelper(pst, FIELD_CREATE_CREDENTIALS_LAST_UPDATED, credsToSave.getLastUpdated());

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save totp secret.");
            }

            return credsToSave;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * Modify the TOTPShared secret entry in the database.
     * @param credsToSave - the object to update
     * @return the TOTPSharedSecret for chaining.
     * @throws SegueDatabaseException - if we can't save the entry for some reason
     */
    private TOTPSharedSecret updateCredentials(final TOTPSharedSecret credsToSave) throws SegueDatabaseException {
        TOTPSharedSecret existingRecord = this.get2FASharedSecret(credsToSave.getUserId());
        if (null == existingRecord) {
            throw new SegueDatabaseException("The credentials you have tried to update do not exist.");
        }

        String query = "UPDATE user_totp SET shared_secret = ?, last_updated = ? WHERE user_id = ?;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query)
        ) {
            setValueHelper(pst, FIELD_UPDATE_CREDENTIALS_SHARED_SECRET, credsToSave.getSharedSecret());
            setValueHelper(pst, FIELD_UPDATE_CREDENTIALS_LAST_UPDATED, credsToSave.getLastUpdated());
            setValueHelper(pst, FIELD_UPDATE_CREDENTIALS_USER_ID, credsToSave.getUserId());

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save totp secret.");
            }

            return this.get2FASharedSecret(existingRecord.getUserId());
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * Remove all trace of MFA for the user account.
     *
     * @param userId of the user to affect.
     * @throws SegueDatabaseException - if we have a problem with the database connection.
     */
    @Override
    public void delete2FACredentials(final Long userId) throws SegueDatabaseException {

        String query = "DELETE FROM user_totp WHERE user_id = ?;";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query)
        ) {
            setValueHelper(pst, FIELD_DELETE_CREDENTIALS_USER_ID, userId);

            pst.executeUpdate();

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * findOne helper method to ensure that only one result matches the search criteria.
     *
     * @param results
     *            - from a jdbc database search
     * @return a single TOTPSharedSecret that matches the search criteria or null of no matches found.
     * @throws SQLException
     *             - if a db error occurs
     * @throws SegueDatabaseException
     *             - if more than one result is returned
     */
    private TOTPSharedSecret findOne(final ResultSet results) throws SegueDatabaseException, SQLException {
        // are there any results
        if (!results.isBeforeFirst()) {
            return null;
        }

        List<TOTPSharedSecret> listOfResults = Lists.newArrayList();
        while (results.next()) {
            listOfResults.add(buildTOTPSharedSecret(results));
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
     * @return totpSharedSecret object
     * @throws SQLException if we can't get a value required.
     */
    private TOTPSharedSecret buildTOTPSharedSecret(final ResultSet results) throws SQLException {

        return new TOTPSharedSecret(results.getLong("user_id"),
                results.getString("shared_secret"),
                results.getTimestamp("created"),
                results.getTimestamp("last_updated"));
    }

    // Field Constants
    // get2FASharedSecret
    private static final int FIELD_GET_SHARED_SECRET_USER_ID = 1;

    // createCredentials
    private static final int FIELD_CREATE_CREDENTIALS_USER_ID = 1;
    private static final int FIELD_CREATE_CREDENTIALS_SHARED_SECRET = 2;
    private static final int FIELD_CREATE_CREDENTIALS_CREATED = 3;
    private static final int FIELD_CREATE_CREDENTIALS_LAST_UPDATED = 4;

    // updateCredentials
    private static final int FIELD_UPDATE_CREDENTIALS_SHARED_SECRET = 1;
    private static final int FIELD_UPDATE_CREDENTIALS_LAST_UPDATED = 2;
    private static final int FIELD_UPDATE_CREDENTIALS_USER_ID = 3;

    // delete2FACredentials
    private static final int FIELD_DELETE_CREDENTIALS_USER_ID = 1;
}
