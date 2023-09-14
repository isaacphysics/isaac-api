/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.dao.associations;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.AssociationToken;
import uk.ac.cam.cl.dtg.isaac.dos.UserAssociation;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * MongoAssociationDataManager.
 *
 */
public class PgAssociationDataManager implements IAssociationDataManager {
  private static final Logger log = LoggerFactory.getLogger(PgAssociationDataManager.class);

  private final PostgresSqlDb database;

  /**
   * PostgresAssociationDataManager.
   *
   * @param database
   *            - preconfigured connection
   */
  @Inject
  public PgAssociationDataManager(final PostgresSqlDb database) {
    this.database = database;
  }

  @Override
  public AssociationToken saveAssociationToken(final AssociationToken token) throws SegueDatabaseException {
    Validate.notNull(token);

    String query = "INSERT INTO user_associations_tokens(token, owner_user_id, group_id) VALUES (?, ?, ?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_SAVE_TOKEN_TOKEN, token.getToken());
      pst.setLong(FIELD_SAVE_TOKEN_OWNER_USER_ID, token.getOwnerUserId());
      pst.setLong(FIELD_SAVE_TOKEN_GROUP_ID, token.getGroupId());

      if (pst.executeUpdate() == 0) {
        throw new SegueDatabaseException("Unable to save token.");
      }

      return token;
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public void deleteToken(final String token) throws SegueDatabaseException {
    if (null == token || token.isEmpty()) {
      throw new SegueDatabaseException("Unable to locate the token requested to delete.");
    }

    String query = "DELETE FROM user_associations_tokens WHERE token = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_DELETE_TOKEN_TOKEN, token);
      pst.execute();
    } catch (SQLException e1) {
      throw new SegueDatabaseException("Postgres exception", e1);
    }
  }

  @Override
  public void createAssociation(final AssociationToken token, final Long userIdGrantingAccess)
      throws SegueDatabaseException {
    Validate.notNull(token);
    Long userIdReceivingAccess = token.getOwnerUserId();

    createAssociation(userIdReceivingAccess, userIdGrantingAccess);
  }

  @Override
  public void createAssociation(final Long userIdReceivingAccess, final Long userIdGrantingAccess)
      throws SegueDatabaseException {
    Validate.notNull(userIdReceivingAccess);

    String query = "INSERT INTO user_associations(user_id_granting_permission, user_id_receiving_permission,"
        + " created) VALUES (?, ?, ?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_CREATE_ASSOCIATION_GRANTING_USER_ID, userIdGrantingAccess);
      pst.setLong(FIELD_CREATE_ASSOCIATION_RECEIVING_USER_ID, userIdReceivingAccess);
      pst.setTimestamp(FIELD_CREATE_ASSOCIATION_CREATION_DATE, new Timestamp(new Date().getTime()));

      if (pst.executeUpdate() == 0) {
        throw new SegueDatabaseException("Unable to create association.");
      }

    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public void deleteAssociation(final Long userIdWhoGrantedAccess, final Long userIdWithAccess)
      throws SegueDatabaseException {
    if (null == userIdWhoGrantedAccess || null == userIdWithAccess) {
      throw new SegueDatabaseException("Unable to locate the association requested to delete.");
    }

    String query =
        "DELETE FROM user_associations WHERE user_id_granting_permission = ? AND user_id_receiving_permission = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_DELETE_ASSOCIATION_GRANTING_USER_ID, userIdWhoGrantedAccess);
      pst.setLong(FIELD_DELETE_ASSOCIATION_RECEIVING_USER_ID, userIdWithAccess);
      pst.execute();
    } catch (SQLException e1) {
      throw new SegueDatabaseException("Postgres exception", e1);
    }
  }

  @Override
  public void deleteAssociationsByOwner(final Long ownerUserId) throws SegueDatabaseException {
    this.deleteAssociations(ownerUserId, true);
  }

  @Override
  public void deleteAssociationsByRecipient(final Long recipientUserId) throws SegueDatabaseException {
    this.deleteAssociations(recipientUserId, false);
  }

  @Override
  public boolean hasValidAssociation(final Long userIdRequestingAccess, final Long ownerUserId)
      throws SegueDatabaseException {
    String query = "SELECT COUNT(1) AS TOTAL FROM user_associations"
        + " WHERE user_id_receiving_permission = ? AND user_id_granting_permission = ?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_HAS_ASSOCIATION_RECEIVING_USER_ID, userIdRequestingAccess);
      pst.setLong(FIELD_HAS_ASSOCIATION_GRANTING_USER_ID, ownerUserId);

      try (ResultSet results = pst.executeQuery()) {
        results.next();
        return results.getLong("TOTAL") == 1;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public List<UserAssociation> getUserAssociations(final Long userId) throws SegueDatabaseException {
    Validate.notNull(userId);

    String query = "SELECT * FROM user_associations WHERE user_id_granting_permission = ?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_ASSOCIATIONS_GRANTING_USER_ID, userId);

      try (ResultSet results = pst.executeQuery()) {
        List<UserAssociation> listOfResults = Lists.newArrayList();

        while (results.next()) {
          listOfResults.add(this.convertFromSQLToAssociation(results));
        }

        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Error while trying to find user associations by id", e);
    }
  }


  @Override
  public AssociationToken lookupAssociationToken(final String tokenCode) throws SegueDatabaseException {
    String query = "SELECT * FROM user_associations_tokens WHERE token = ?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_LOOKUP_TOKEN_TOKEN, tokenCode);

      try (ResultSet results = pst.executeQuery()) {
        List<AssociationToken> listOfResults = Lists.newArrayList();

        while (results.next()) {
          listOfResults.add(this.convertFromSQLToToken(results));
        }

        if (listOfResults.size() == 0) {
          return null;
        }

        if (listOfResults.size() > 1) {
          throw new SegueDatabaseException("Ambiguous result, expected single result and found more than one"
              + listOfResults);
        }

        return listOfResults.get(0);
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Error while trying to find token", e);
    }
  }

  @Override
  public AssociationToken getAssociationTokenByGroupId(final Long groupId) throws SegueDatabaseException {
    String query = "SELECT * FROM user_associations_tokens WHERE group_id = ?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_TOKEN_BY_GROUP_ID_GROUP_ID, groupId);

      try (ResultSet results = pst.executeQuery()) {
        List<AssociationToken> listOfResults = Lists.newArrayList();

        while (results.next()) {
          listOfResults.add(this.convertFromSQLToToken(results));
        }

        if (listOfResults.size() == 0) {
          return null;
        }

        if (listOfResults.size() > 1) {
          throw new SegueDatabaseException("Ambiguous result, expected single result and found more than one"
              + listOfResults);
        }

        return listOfResults.get(0);
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Error while trying to find token by group", e);
    }
  }

  @Override
  public List<UserAssociation> getUsersThatICanSee(final Long userId) throws SegueDatabaseException {
    Validate.notNull(userId);

    String query = "SELECT * FROM user_associations WHERE user_id_receiving_permission = ?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_VISIBLE_USERS_RECEIVING_USER_ID, userId);

      try (ResultSet results = pst.executeQuery()) {
        List<UserAssociation> listOfResults = Lists.newArrayList();

        while (results.next()) {
          listOfResults.add(this.convertFromSQLToAssociation(results));
        }

        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Error while trying to find user associations by id", e);
    }
  }

  /**
   * @param results from the sql query
   * @return a user association object
   * @throws SQLException if a required property doesn't exist
   */
  private UserAssociation convertFromSQLToAssociation(final ResultSet results) throws SQLException {
    return new UserAssociation(results.getLong("user_id_granting_permission"),
        results.getLong("user_id_receiving_permission"), results.getDate("created"));
  }

  /**
   * @param results from the sql query
   * @return an association token
   * @throws SQLException if a required property doesn't exist
   */
  private AssociationToken convertFromSQLToToken(final ResultSet results) throws SQLException {
    return new AssociationToken(results.getString("token"),
        results.getLong("owner_user_id"), results.getLong("group_id"));
  }

  /**
   * Helper function to allow deletion of all associations for a given user.
   *
   * @param userIdOfInterest - the user id of interest
   * @param isOwner - if true it will delete all cases where the user is the data owner and has shared,
   *                if false it will delete all cases where the user is the recipient.
   * @throws SegueDatabaseException - if a data base error occurs.
   */
  private void deleteAssociations(final Long userIdOfInterest, final boolean isOwner)
      throws SegueDatabaseException {
    if (null == userIdOfInterest) {
      throw new SegueDatabaseException("No user Id specified for requested delete association operation.");
    }

    StringBuilder sb = new StringBuilder();
    sb.append("DELETE FROM user_associations WHERE ");

    if (isOwner) {
      sb.append("user_id_granting_permission = ?");
    } else {
      sb.append("user_id_receiving_permission = ?");
    }

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(sb.toString())
    ) {
      pst.setLong(FIELD_DELETE_ASSOCIATIONS_USER_ID, userIdOfInterest);
      pst.execute();
    } catch (SQLException e1) {
      throw new SegueDatabaseException("Postgres exception", e1);
    }
  }

  // Field Constants
  // saveAssociationToken
  private static final int FIELD_SAVE_TOKEN_TOKEN = 1;
  private static final int FIELD_SAVE_TOKEN_OWNER_USER_ID = 2;
  private static final int FIELD_SAVE_TOKEN_GROUP_ID = 3;

  // deleteToken
  private static final int FIELD_DELETE_TOKEN_TOKEN = 1;

  // createAssociation
  private static final int FIELD_CREATE_ASSOCIATION_GRANTING_USER_ID = 1;
  private static final int FIELD_CREATE_ASSOCIATION_RECEIVING_USER_ID = 2;
  private static final int FIELD_CREATE_ASSOCIATION_CREATION_DATE = 3;

  // deleteAssociation
  private static final int FIELD_DELETE_ASSOCIATION_GRANTING_USER_ID = 1;
  private static final int FIELD_DELETE_ASSOCIATION_RECEIVING_USER_ID = 2;

  // hasValidAssociation
  private static final int FIELD_HAS_ASSOCIATION_RECEIVING_USER_ID = 1;
  private static final int FIELD_HAS_ASSOCIATION_GRANTING_USER_ID = 2;

  // getUserAssociations
  private static final int FIELD_GET_ASSOCIATIONS_GRANTING_USER_ID = 1;

  // lookupAssociationToken
  private static final int FIELD_LOOKUP_TOKEN_TOKEN = 1;

  // getAssociationTokenByGroupId
  private static final int FIELD_GET_TOKEN_BY_GROUP_ID_GROUP_ID = 1;

  // getUsersThatICanSee
  private static final int FIELD_VISIBLE_USERS_RECEIVING_USER_ID = 1;

  // deleteAssociations
  private static final int FIELD_DELETE_ASSOCIATIONS_USER_ID = 1;

}
