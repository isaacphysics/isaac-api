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
package uk.ac.cam.cl.dtg.segue.dao.users;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.api.client.util.Sets;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import javax.annotation.Nullable;
import javax.validation.constraints.Null;

/**
 * PgAssociationDataManager.
 * 
 */
public class PgUserGroupPersistenceManager implements IUserGroupPersistenceManager {
    private static final Logger log = LoggerFactory.getLogger(PgUserGroupPersistenceManager.class);

    private final PostgresSqlDb database;

    /**
     * PostgresAssociationDataManager.
     * 
     * @param database
     *            - preconfigured connection
     */
    @Inject
    public PgUserGroupPersistenceManager(final PostgresSqlDb database) {
        this.database = database;
    }

    @Override
    public UserGroup createGroup(final UserGroup group) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn
                    .prepareStatement(
                            "INSERT INTO groups(group_name, owner_id, created)"
                            + " VALUES (?, ?, ?);",
                            Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, group.getGroupName());
            pst.setLong(2, group.getOwnerId());
            
            if (group.getCreated() != null) {
                pst.setTimestamp(3, new Timestamp(group.getCreated().getTime()));    
            } else {
                pst.setTimestamp(3, new Timestamp(new Date().getTime()));
            }
            
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save group.");
            }
            
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Long id = generatedKeys.getLong(1);
                    group.setId(id);

                } else {
                    throw new SQLException("Creating group failed, no ID obtained.");
                }
            }
            
            return group;
            
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public UserGroup editGroup(final UserGroup group) throws SegueDatabaseException {
        Validate.notNull(group.getId());
        
        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn
                    .prepareStatement("UPDATE groups SET group_name=?, owner_id=?, created=?, archived=? WHERE id = ?;");
            pst.setString(1, group.getGroupName());
            pst.setLong(2, group.getOwnerId());
            pst.setTimestamp(3, new Timestamp(group.getCreated().getTime()));
            pst.setBoolean(4, group.isArchived());
            pst.setLong(5, group.getId());
            
            log.debug(pst.toString());
            
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save group.");
            }

            return this.findById(group.getId());
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public void addUserToGroup(final Long userId, final Long groupId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn
                    .prepareStatement(
                            "INSERT INTO group_memberships(group_id, user_id, created) VALUES (?, ?, ?);",
                            Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, groupId);
            pst.setLong(2, userId);
            pst.setTimestamp(3, new Timestamp(new Date().getTime()));
            
            int affectedRows = pst.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Adding a user to a group failed, no rows changed");
            }
            
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }        
    }

    @Override
    public void removeUserFromGroup(final Long userId, final Long groupId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("DELETE FROM group_memberships WHERE group_id = ? AND user_id = ?");
            pst.setLong(1, groupId);
            pst.setLong(2, userId);
            
            pst.execute();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }


    @Override
    public List<UserGroup> getGroupsByOwner(final Long ownerUserId) throws SegueDatabaseException {
        return this.getGroupsByOwner(ownerUserId, null);
    }

    @Override
    public List<UserGroup> getGroupsByOwner(final Long ownerUserId, @Nullable final Boolean archivedGroupsOnly) throws SegueDatabaseException {
        String pstString = "SELECT * FROM groups WHERE owner_id = ?";
        if (archivedGroupsOnly != null) {
            pstString = pstString +  " AND archived = ?";
        }

        return getGroupsBySQLPst(pstString, ownerUserId, archivedGroupsOnly);

    }

    @Override
    public List<UserGroup> getGroupsByAdditionalManager(final Long additionalManagerId) throws SegueDatabaseException {
        return this.getGroupsByAdditionalManager(additionalManagerId, null);
    }

    @Override
    public List<UserGroup> getGroupsByAdditionalManager(final Long additionalManagerId, @Nullable final Boolean archivedGroupsOnly) throws SegueDatabaseException {
        String pstString = "SELECT * FROM groups WHERE id IN (SELECT group_id FROM group_additional_managers WHERE user_id = ?)";
        if (archivedGroupsOnly != null) {
            pstString = pstString +  " AND archived = ?";
        }

        return getGroupsBySQLPst(pstString, additionalManagerId, archivedGroupsOnly);
    }

    @Override
    public Long getGroupCount() throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT COUNT(1) AS TOTAL FROM groups");

            ResultSet results = pst.executeQuery();
            results.next();
            return results.getLong("TOTAL");
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception: Unable to count groups", e);
        }
    }

    @Override
    public UserGroup findById(final Long groupId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM groups WHERE id = ?");
            pst.setLong(1, groupId);

            ResultSet results = pst.executeQuery();

            if (results.next()) {
                if (!results.isLast()) {
                    throw new SegueDatabaseException("Expected a single object and found more than one.");
                }
                return this.buildGroup(results);
            } else {
                // Lots of places that call this function expect null if no group was found, i.e. was probably deleted.
                return null;
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public void deleteGroup(final Long groupId) throws SegueDatabaseException {
        if (null == groupId) {
            throw new SegueDatabaseException("Unable to locate the group requested to delete.");
        }

        try (Connection conn = database.getDatabaseConnection()) {
            try {
                PreparedStatement pst;
                pst = conn.prepareStatement("DELETE FROM groups WHERE id = ?");
                pst.setLong(1, groupId);
                pst.execute();
                                
            } catch (SQLException e) {
             
                throw e;
            }
        } catch (SQLException e1) {
            throw new SegueDatabaseException("Postgres exception", e1);
        } 
    }

    @Override
    public List<Long> getGroupMemberIds(final Long groupId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM group_memberships WHERE group_id = ?");
            pst.setLong(1, groupId);

            ResultSet results = pst.executeQuery();
            
            List<Long> listOfResults = Lists.newArrayList();
            while (results.next()) {
                listOfResults.add(results.getLong("user_id"));
            }
            
            return listOfResults;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public Collection<UserGroup> getGroupMembershipList(final Long userId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn
                    .prepareStatement("SELECT * FROM groups INNER JOIN group_memberships"
                            + " ON groups.id = group_memberships.group_id"
                            + " WHERE group_memberships.user_id = ?");
            pst.setLong(1, userId);

            ResultSet results = pst.executeQuery();
            
            List<UserGroup> listOfResults = Lists.newArrayList();
            while (results.next()) {
                listOfResults.add(this.buildGroup(results));
            }
            
            return listOfResults;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public Set<Long> getAdditionalManagerSetByGroupId(final Long groupId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn
                    .prepareStatement("SELECT * FROM group_additional_managers"
                            + " WHERE group_id = ?");
            pst.setLong(1, groupId);

            ResultSet results = pst.executeQuery();

            Set<Long> listOfResults = Sets.newHashSet();
            while (results.next()) {
                listOfResults.add(results.getLong("user_id"));
            }

            return listOfResults;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public void addUserAdditionalManagerList(final Long userId, final Long groupId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn
                    .prepareStatement(
                            "INSERT INTO group_additional_managers(group_id, user_id, created) VALUES (?, ?, ?);",
                            Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, groupId);
            pst.setLong(2, userId);
            pst.setTimestamp(3, new Timestamp(new Date().getTime()));

            int affectedRows = pst.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Adding a user to the additional manager list failed, no rows changed");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public void removeUserFromAdditionalManagerList(final Long userId, final Long groupId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("DELETE FROM group_additional_managers WHERE group_id = ? AND user_id = ?");
            pst.setLong(1, groupId);
            pst.setLong(2, userId);

            pst.execute();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * buildGroup. Convenience method to build a group.
     * 
     * @param set
     * @return a Group object
     * @throws SQLException
     *             - if we cannot extract a required property from the results set.
     */
    private UserGroup buildGroup(final ResultSet set) throws SQLException {
        return new UserGroup(set.getLong("id"), set.getString("group_name"), set.getLong("owner_id"),
                set.getDate("created"), set.getBoolean("archived"));
    }

    private List<UserGroup> getGroupsBySQLPst(final String pstString, final Long userId, @Nullable final Boolean archivedGroupsOnly) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement(pstString);
            pst.setLong(1, userId);

            if (archivedGroupsOnly != null) {
                pst.setBoolean(2, archivedGroupsOnly);
            }

            ResultSet results = pst.executeQuery();
            List<UserGroup> listOfResults = Lists.newArrayList();
            while (results.next()) {
                listOfResults.add(this.buildGroup(results));
            }

            return listOfResults;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }
}
