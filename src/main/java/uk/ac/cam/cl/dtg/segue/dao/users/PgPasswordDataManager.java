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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import uk.ac.cam.cl.dtg.isaac.dos.users.LocalUserCredential;
import uk.ac.cam.cl.dtg.segue.dao.AbstractPgDataManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * Postgres specific implementation of a password data manager.
 */
public class PgPasswordDataManager extends AbstractPgDataManager implements IPasswordDataManager {
  private final PostgresSqlDb database;

  /**
   * PgUsers.
   *
   * @param ds - the postgres datasource to use
   */
  @Inject
  public PgPasswordDataManager(final PostgresSqlDb ds) {
    this.database = ds;
  }

  @Override
  public LocalUserCredential getLocalUserCredential(final Long userId) throws SegueDatabaseException {
    if (null == userId) {
      return null;
    }

    String query = "SELECT * FROM user_credentials WHERE user_id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_CREDENTIAL_USER_ID, userId);

      try (ResultSet results = pst.executeQuery()) {
        return this.findOne(results);
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public LocalUserCredential getLocalUserCredentialByResetToken(final String token) throws SegueDatabaseException {
    String query = "SELECT * FROM user_credentials WHERE reset_token = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_GET_CREDENTIAL_BY_RESET_TOKEN_TOKEN, token);

      try (ResultSet results = pst.executeQuery()) {
        return this.findOne(results);
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception while retrieving by reset token", e);
    }
  }

  @Override
  public LocalUserCredential createOrUpdateLocalUserCredential(final LocalUserCredential credsToSave)
      throws SegueDatabaseException {
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

  private LocalUserCredential createCredentials(final LocalUserCredential credsToSave) throws SegueDatabaseException {
    String query =
        "INSERT INTO user_credentials(user_id, password, security_scheme, secure_salt, reset_token, reset_expiry, last_updated)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
    ) {
      setValueHelper(pst, FIELD_CREATE_CREDENTIALS_USER_ID, credsToSave.getUserId());
      setValueHelper(pst, FIELD_CREATE_CREDENTIALS_PASSWORD, credsToSave.getPassword());
      setValueHelper(pst, FIELD_CREATE_CREDENTIALS_SECURITY_SCHEME, credsToSave.getSecurityScheme());
      setValueHelper(pst, FIELD_CREATE_CREDENTIALS_SECURE_SALT, credsToSave.getSecureSalt());
      setValueHelper(pst, FIELD_CREATE_CREDENTIALS_RESET_TOKEN, credsToSave.getResetToken());
      setValueHelper(pst, FIELD_CREATE_CREDENTIALS_RESET_EXPIRY, credsToSave.getResetExpiry());
      setValueHelper(pst, FIELD_CREATE_CREDENTIALS_LAST_UPDATED, new java.util.Date());

      if (pst.executeUpdate() == 0) {
        throw new SegueDatabaseException("Unable to save user.");
      }

      return credsToSave;
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  private LocalUserCredential updateCredentials(final LocalUserCredential credsToSave) throws SegueDatabaseException {
    LocalUserCredential existingRecord = this.getLocalUserCredential(credsToSave.getUserId());
    if (null == existingRecord) {
      throw new SegueDatabaseException("The credentials you have tried to update do not exist.");
    }

    String query = "UPDATE user_credentials SET password = ?, security_scheme = ?, secure_salt = ?,"
        + " reset_token = ?, reset_expiry = ?, last_updated = ? WHERE user_id = ?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      setValueHelper(pst, FIELD_UPDATE_CREDENTIALS_PASSWORD, credsToSave.getPassword());
      setValueHelper(pst, FIELD_UPDATE_CREDENTIALS_SECURITY_SCHEME, credsToSave.getSecurityScheme());
      setValueHelper(pst, FIELD_UPDATE_CREDENTIALS_SECURE_SALT, credsToSave.getSecureSalt());
      setValueHelper(pst, FIELD_UPDATE_CREDENTIALS_RESET_TOKEN, credsToSave.getResetToken());
      setValueHelper(pst, FIELD_UPDATE_CREDENTIALS_RESET_EXPIRY, credsToSave.getResetExpiry());
      setValueHelper(pst, FIELD_UPDATE_CREDENTIALS_LAST_UPDATED, new java.util.Date());
      setValueHelper(pst, FIELD_UPDATE_CREDENTIALS_USER_ID, credsToSave.getUserId());

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
   * @param results - from a jdbc database search
   * @return a single credential that matches the search criteria or null of no matches found.
   * @throws SQLException           - if a db error occurs
   * @throws SegueDatabaseException - if more than one result is returned
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
  private LocalUserCredential buildCredential(final ResultSet results) throws SQLException {
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

  // Field Constants
  // getLocalUserCredential
  private static final int FIELD_GET_CREDENTIAL_USER_ID = 1;

  // getLocalUserCredentialByResetToken
  private static final int FIELD_GET_CREDENTIAL_BY_RESET_TOKEN_TOKEN = 1;

  // createCredentials
  private static final int FIELD_CREATE_CREDENTIALS_USER_ID = 1;
  private static final int FIELD_CREATE_CREDENTIALS_PASSWORD = 2;
  private static final int FIELD_CREATE_CREDENTIALS_SECURITY_SCHEME = 3;
  private static final int FIELD_CREATE_CREDENTIALS_SECURE_SALT = 4;
  private static final int FIELD_CREATE_CREDENTIALS_RESET_TOKEN = 5;
  private static final int FIELD_CREATE_CREDENTIALS_RESET_EXPIRY = 6;
  private static final int FIELD_CREATE_CREDENTIALS_LAST_UPDATED = 7;

  // updateCredentials
  private static final int FIELD_UPDATE_CREDENTIALS_PASSWORD = 1;
  private static final int FIELD_UPDATE_CREDENTIALS_SECURITY_SCHEME = 2;
  private static final int FIELD_UPDATE_CREDENTIALS_SECURE_SALT = 3;
  private static final int FIELD_UPDATE_CREDENTIALS_RESET_TOKEN = 4;
  private static final int FIELD_UPDATE_CREDENTIALS_RESET_EXPIRY = 5;
  private static final int FIELD_UPDATE_CREDENTIALS_LAST_UPDATED = 6;
  private static final int FIELD_UPDATE_CREDENTIALS_USER_ID = 7;
}
