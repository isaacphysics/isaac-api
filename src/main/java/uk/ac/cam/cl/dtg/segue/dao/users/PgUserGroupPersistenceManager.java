/*
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
import java.util.Map;
import java.util.Set;

import com.google.api.client.util.Sets;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembership;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.segue.dos.GroupStatus;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import javax.annotation.Nullable;

/**
 * PgUserGroupPersistenceManager.
 * Postgres implementation of the IUserGroupPersistenceManager
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
                            "INSERT INTO groups(group_name, owner_id, group_status, created, last_updated)"
                            + " VALUES (?, ?, ?, ?, ?);",
                            Statement.RETURN_GENERATED_KEYS);
            pst.setString(1, group.getGroupName());
            pst.setLong(2, group.getOwnerId());

            Timestamp created;
            if (group.getCreated() != null) {
                created = new Timestamp(group.getCreated().getTime());
            } else {
                created = new Timestamp(new Date().getTime());
            }

            pst.setString(3, GroupStatus.ACTIVE.name());

            pst.setTimestamp(4, created);
            pst.setTimestamp(5, created);

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
        if (group.getStatus() == null) {
            group.setStatus(GroupStatus.ACTIVE);
        }

        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement(
                    "UPDATE groups SET group_name=?, owner_id=?, created=?, archived=?, group_status=?, last_updated=? WHERE id = ?;");

            pst.setString(1, group.getGroupName());
            pst.setLong(2, group.getOwnerId());
            pst.setTimestamp(3, new Timestamp(group.getCreated().getTime()));
            pst.setBoolean(4, group.isArchived());
            pst.setString(5, group.getStatus().name());
            pst.setTimestamp(6, new Timestamp(group.getLastUpdated().getTime()));
            pst.setLong(7, group.getId());
            
            log.debug(pst.toString());
            
            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save group.");
            }

            return this.findGroupById(group.getId());
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public void addUserToGroup(final Long userId, final Long groupId) throws SegueDatabaseException {
        // first check if they already have a membership record
        if (this.hasMembershipForGroup(userId, groupId)) {
            this.setUsersGroupMembershipStatus(userId,groupId, GroupMembershipStatus.ACTIVE);
            return;
        }

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst = conn
                    .prepareStatement(
                            "INSERT INTO group_memberships(group_id, user_id, status, created, updated) VALUES (?, ?, ?, ?, ?);",
                            Statement.RETURN_GENERATED_KEYS);
            pst.setLong(1, groupId);
            pst.setLong(2, userId);
            pst.setString(3, GroupMembershipStatus.ACTIVE.name());
            pst.setTimestamp(4, new Timestamp(new Date().getTime()));
            pst.setTimestamp(5, new Timestamp(new Date().getTime()));

            int affectedRows = pst.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Adding a user to a group failed, no rows changed");
            }
            
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }        
    }

    @Override
    public void setUsersGroupMembershipStatus(final Long userId, final Long groupId, final GroupMembershipStatus newStatus) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn
                    .prepareStatement("UPDATE group_memberships SET status=? ,updated=? WHERE user_id = ? AND group_id = ?");
            pst.setString(1, newStatus.name());
            pst.setTimestamp(2, new Timestamp(new Date().getTime()));
            pst.setLong(3, userId);
            pst.setLong(4, groupId);


            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to update membership status.");
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public void removeUserFromGroup(final Long userId, final Long groupId) throws SegueDatabaseException {
        this.setUsersGroupMembershipStatus(userId, groupId, GroupMembershipStatus.DELETED);
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

        pstString = pstString + " AND group_status <> '" + GroupStatus.DELETED.name() + "'";

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

        pstString = pstString + " AND group_status <> '" + GroupStatus.DELETED.name() + "'";

        return getGroupsBySQLPst(pstString, additionalManagerId, archivedGroupsOnly);
    }

    @Override
    public Long getGroupCount() throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            // we don't want to count 'deleted' groups.
            pst = conn.prepareStatement("SELECT COUNT(1) AS TOTAL FROM groups " +
                    "WHERE status <> ?");

            pst.setString(1, GroupStatus.DELETED.name());

            ResultSet results = pst.executeQuery();
            results.next();
            return results.getLong("TOTAL");
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception: Unable to count groups", e);
        }
    }

    @Override
    public UserGroup findGroupById(final Long groupId) throws SegueDatabaseException {
        return this.findGroupById(groupId, false);
    }

    @Override
    public UserGroup findGroupById(final Long groupId, boolean includeDeletedGroups) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            if(includeDeletedGroups) {
                pst = conn.prepareStatement("SELECT * FROM groups WHERE id = ?");
            } else {
                pst = conn.prepareStatement("SELECT * FROM groups WHERE id = ? AND group_status <> ?");
                pst.setString(2, GroupStatus.DELETED.name());
            }

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
        this.deleteGroup(groupId, true);
    }

    @Override
    public void deleteGroup(final Long groupId, boolean markAsDeleted) throws SegueDatabaseException {
        if (null == groupId) {
            throw new SegueDatabaseException("Unable to locate the group requested to delete.");
        }

        try (Connection conn = database.getDatabaseConnection()) {
                PreparedStatement pst;
                if (markAsDeleted) {
                        pst = conn
                                .prepareStatement("UPDATE groups SET group_status=? WHERE id = ?;");
                        pst.setString(1, GroupStatus.DELETED.name());
                        pst.setLong(2, groupId);

                        if (pst.executeUpdate() == 0) {
                            throw new SegueDatabaseException("Unable to mark group as deleted.");
                        }
                } else {
                    pst = conn.prepareStatement("DELETE FROM groups WHERE id = ?");
                    pst.setLong(1, groupId);
                    pst.execute();
                }

        } catch (SQLException e1) {
            throw new SegueDatabaseException("Postgres exception", e1);
        }
    }

    @Override
    public Collection<Long> getGroupMemberIds(final Long groupId) throws SegueDatabaseException {
        return this.getGroupMembershipMap(groupId).keySet();
    }

    @Override
    public Map<Long, GroupMembership> getGroupMembershipMap(final Long groupId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;

            pst = conn.prepareStatement("SELECT * FROM group_memberships INNER JOIN groups ON " +
                    "groups.id = group_memberships.group_id WHERE group_id = ? AND status <> ? AND group_status <> ?");

            pst.setLong(1, groupId);
            pst.setString(2, GroupMembershipStatus.DELETED.name());
            pst.setString(3, GroupStatus.DELETED.name());

            ResultSet results = pst.executeQuery();

            Map<Long, GroupMembership> mapOfResults = Maps.newHashMap();
            while (results.next()) {
                mapOfResults.put(results.getLong("user_id"), this.buildMembershipRecord(results));
            }

            return mapOfResults;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    /**
     * Will include shallow deleted group membership as we should reuse their group entry in the db
     * @param groupId group to check
     * @param userId user to check
     * @return true if they ever had a recorded membership entry in the db.
     * @throws SegueDatabaseException - if there is db error.
     */
    private boolean hasMembershipForGroup(final Long groupId, final Long userId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;

            pst = conn.prepareStatement("SELECT COUNT(1) AS TOTAL FROM group_memberships WHERE group_id = ? AND user_id = ?");
            pst.setLong(1, groupId);
            pst.setLong(2, userId);
            ResultSet results = pst.executeQuery();
            results.next();
            return results.getLong("TOTAL") == 1;
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception: Unable to count groups", e);
        }

    }

    @Override
    public Collection<UserGroup> getGroupMembershipList(final Long userId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn
                    .prepareStatement("SELECT * FROM groups INNER JOIN group_memberships"
                            + " ON groups.id = group_memberships.group_id"
                            + " WHERE user_id = ? AND status <> ? AND group_status <> ?");
            pst.setLong(1, userId);
            pst.setString(2, GroupMembershipStatus.DELETED.name());
            pst.setString(3, GroupStatus.DELETED.name());
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

            // on this occasion we do not care if the group is deleted.

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
     * @param set - the result set
     * @return a Group object
     * @throws SQLException
     *             - if we cannot extract a required property from the results set.
     */
    private UserGroup buildGroup(final ResultSet set) throws SQLException {
        return new UserGroup(set.getLong("id"), set.getString("group_name"), set.getLong("owner_id"),
                GroupStatus.valueOf(set.getString("group_status")), set.getDate("created"),
                set.getBoolean("archived"), set.getDate("last_updated"));
    }

    private GroupMembership buildMembershipRecord(final ResultSet set) throws SQLException {
        return new GroupMembership(set.getLong("group_id"), set.getLong("user_id"),
                GroupMembershipStatus.valueOf(set.getString("status")), set.getDate("created"), set.getDate("updated"));
    }

    private List<UserGroup> getGroupsBySQLPst(final String pstString, final Long userId, @Nullable final Boolean archivedGroupsOnly) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement(pstString);
            pst.setLong(1, userId);

            if (archivedGroupsOnly != null) {
                pst.setBoolean(3, archivedGroupsOnly);
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