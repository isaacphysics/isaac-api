/**
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dao.associations;

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

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.AssociationToken;
import uk.ac.cam.cl.dtg.segue.dos.UserAssociation;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

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
        
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn
                    .prepareStatement(
                            "INSERT INTO user_associations_tokens(token, owner_user_id, group_id) VALUES (?, ?, ?);");
            
            pst.setString(1, token.getToken());
            pst.setLong(2, token.getOwnerUserId());
            pst.setLong(3, token.getGroupId());
            
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

        try (Connection conn = database.getDatabaseConnection()) {            
            PreparedStatement pst;
            pst = conn.prepareStatement("DELETE FROM user_associations_tokens WHERE token = ?");
            
            log.debug(pst.toString());
            
            pst.setString(1, token);
            pst.execute();
            
        } catch (SQLException e1) {
            throw new SegueDatabaseException("Postgres exception", e1);
        }
    }

    @Override
    public void createAssociation(final AssociationToken token, final Long userIdGrantingAccess)
            throws SegueDatabaseException {
        Validate.notNull(token);

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn
                    .prepareStatement("INSERT INTO "
                            + "user_associations(user_id_granting_permission, user_id_receiving_permission, created) "
                            + "VALUES (?, ?, ?);");
            
            pst.setLong(1, userIdGrantingAccess);
            pst.setLong(2, token.getOwnerUserId());
            pst.setTimestamp(3, new Timestamp(new Date().getTime()));
            
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to create association token.");
            }
            
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public void deleteAssociation(final Long userIdWhoGrantedAccess, final Long userIdWithAccess)
            throws SegueDatabaseException {
        if (null == userIdWhoGrantedAccess || null == userIdWithAccess) {
            throw new SegueDatabaseException("Unable to locate the group requested to delete.");
        }

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            
            pst = conn.prepareStatement("DELETE user_id_granting_permission groups"
                    + " WHERE user_id_granting_permission = ? AND user_id_receiving_permission = ?");
            
            pst.setLong(1, userIdWhoGrantedAccess);
            pst.setLong(2, userIdWithAccess);
            pst.execute();
        } catch (SQLException e1) {
            throw new SegueDatabaseException("Postgres exception", e1);
        }
    }

    @Override
    public boolean hasValidAssociation(final Long userIdRequestingAccess, final Long ownerUserId)
            throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT COUNT(1) AS TOTAL"
                    + " FROM user_associations"
                    + " WHERE user_id_receiving_permission = ? AND user_id_granting_permission = ?;");
            
            pst.setLong(1, userIdRequestingAccess);
            pst.setLong(2, ownerUserId);

            ResultSet results = pst.executeQuery();
            results.next();
            
            return results.getLong("TOTAL") == 1;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public List<UserAssociation> getUserAssociations(final Long userId) throws SegueDatabaseException {
        Validate.notNull(userId);

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_associations WHERE user_id_granting_permission = ?;");

            pst.setLong(1, userId);
            
            ResultSet results = pst.executeQuery();
            List<UserAssociation> listOfResults = Lists.newArrayList();
            
            while (results.next()) {
                listOfResults.add(this.convertFromSQLToAssociation(results));
            }

            return listOfResults;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Error while trying to find user associations by id", e);
        }
    }



    @Override
    public AssociationToken lookupAssociationToken(final String tokenCode) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_associations_tokens WHERE token = ?;");

            pst.setString(1, tokenCode);
            
            ResultSet results = pst.executeQuery();
            List<AssociationToken> listOfResults = Lists.newArrayList();
            
            while (results.next()) {
                listOfResults.add(this.convertFromSQLToToken(results));
            }

            if (listOfResults.size() > 1) {
                throw new SegueDatabaseException("Ambiguous result, expected single result and found more than one"
                        + listOfResults);
            }
            
            return listOfResults.get(0);
        } catch (SQLException e) {
            throw new SegueDatabaseException("Error while trying to find token", e);
        }
    }

    @Override
    public AssociationToken getAssociationTokenByGroupId(final Long groupId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_associations_tokens WHERE group_id = ?;");

            pst.setLong(1, groupId);
            
            ResultSet results = pst.executeQuery();
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
        } catch (SQLException e) {
            throw new SegueDatabaseException("Error while trying to find token by group", e);
        }
    }

    @Override
    public List<UserAssociation> getUsersThatICanSee(final Long userId) throws SegueDatabaseException {
        Validate.notNull(userId);

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM user_associations WHERE user_id_receiving_permission = ?;");

            pst.setLong(1, userId);
            
            ResultSet results = pst.executeQuery();
            List<UserAssociation> listOfResults = Lists.newArrayList();
            
            while (results.next()) {
                listOfResults.add(this.convertFromSQLToAssociation(results));
            }

            return listOfResults;
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
}
