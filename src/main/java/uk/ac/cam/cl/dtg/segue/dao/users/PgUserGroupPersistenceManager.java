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

package uk.ac.cam.cl.dtg.segue.dao.users;

import static java.util.Objects.requireNonNull;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import jakarta.annotation.Nullable;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembership;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.isaac.dos.GroupStatus;
import uk.ac.cam.cl.dtg.isaac.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

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
   * @param database - preconfigured connection
   */
  @Inject
  public PgUserGroupPersistenceManager(final PostgresSqlDb database) {
    this.database = database;
  }

  @Override
  public UserGroup createGroup(final UserGroup group) throws SegueDatabaseException {
    String query = "INSERT INTO groups(group_name, owner_id, group_status, created, last_updated)"
        + " VALUES (?, ?, ?, ?, ?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
    ) {
      pst.setString(FIELD_CREATE_GROUP_GROUP_NAME, group.getGroupName());
      pst.setLong(FIELD_CREATE_GROUP_OWNER_ID, group.getOwnerId());

      Timestamp created;
      if (group.getCreated() != null) {
        created = new Timestamp(group.getCreated().getTime());
      } else {
        created = new Timestamp(new Date().getTime());
      }

      pst.setString(FIELD_CREATE_GROUP_GROUP_STATUS, GroupStatus.ACTIVE.name());

      pst.setTimestamp(FIELD_CREATE_GROUP_CREATED, created);
      pst.setTimestamp(FIELD_CREATE_GROUP_LAST_UPDATED, created);

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
    requireNonNull(group.getId());
    if (group.getStatus() == null) {
      group.setStatus(GroupStatus.ACTIVE);
    }

    String query = "UPDATE groups SET group_name=?, owner_id=?, created=?, archived=?,"
        + " additional_manager_privileges=?, group_status=?, last_updated=? WHERE id = ?;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_EDIT_GROUP_GROUP_NAME, group.getGroupName());
      pst.setLong(FIELD_EDIT_GROUP_OWNER_ID, group.getOwnerId());
      pst.setTimestamp(FIELD_EDIT_GROUP_CREATED, new Timestamp(group.getCreated().getTime()));
      pst.setBoolean(FIELD_EDIT_GROUP_ARCHIVED, group.isArchived());
      pst.setBoolean(FIELD_EDIT_GROUP_ADDITIONAL_MANAGER_PRIVILEGES, group.isAdditionalManagerPrivileges());
      pst.setString(FIELD_EDIT_GROUP_GROUP_STATUS, group.getStatus().name());
      pst.setTimestamp(FIELD_EDIT_GROUP_LAST_UPDATED, new Timestamp(group.getLastUpdated().getTime()));
      pst.setLong(FIELD_EDIT_GROUP_GROUP_ID, group.getId());

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
      this.setUsersGroupMembershipStatus(userId, groupId, GroupMembershipStatus.ACTIVE);
      return;
    }

    String query = "INSERT INTO group_memberships(group_id, user_id, status, created, updated) VALUES (?, ?, ?, ?, ?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
    ) {
      pst.setLong(FIELD_ADD_USER_GROUP_ID, groupId);
      pst.setLong(FIELD_ADD_USER_USER_ID, userId);
      pst.setString(FIELD_ADD_USER_MEMBERSHIP_STATUS, GroupMembershipStatus.ACTIVE.name());
      pst.setTimestamp(FIELD_ADD_USER_CREATED, new Timestamp(new Date().getTime()));
      pst.setTimestamp(FIELD_ADD_USER_UPDATED, new Timestamp(new Date().getTime()));

      int affectedRows = pst.executeUpdate();

      if (affectedRows == 0) {
        throw new SQLException("Adding a user to a group failed, no rows changed");
      }

    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public void setUsersGroupMembershipStatus(final Long userId, final Long groupId,
                                            final GroupMembershipStatus newStatus) throws SegueDatabaseException {
    String query = "UPDATE group_memberships SET status=? ,updated=? WHERE user_id = ? AND group_id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_SET_MEMBERSHIP_STATUS_MEMBERSHIP_STATUS, newStatus.name());
      pst.setTimestamp(FIELD_SET_MEMBERSHIP_STATUS_UPDATED, new Timestamp(new Date().getTime()));
      pst.setLong(FIELD_SET_MEMBERSHIP_STATUS_USER_ID, userId);
      pst.setLong(FIELD_SET_MEMBERSHIP_STATUS_GROUP_ID, groupId);


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
  public List<UserGroup> getGroupsByOwner(final Long ownerUserId, @Nullable final Boolean archivedGroupsOnly)
      throws SegueDatabaseException {
    String pstString = "SELECT * FROM groups WHERE owner_id = ?";
    if (archivedGroupsOnly != null) {
      pstString = pstString + " AND archived = ?";
    }

    pstString = pstString + " AND group_status <> '" + GroupStatus.DELETED.name() + "'";

    return getGroupsBySQLPst(pstString, ownerUserId, archivedGroupsOnly);

  }

  @Override
  public List<UserGroup> getGroupsByAdditionalManager(final Long additionalManagerId) throws SegueDatabaseException {
    return this.getGroupsByAdditionalManager(additionalManagerId, null);
  }

  @Override
  public List<UserGroup> getGroupsByAdditionalManager(final Long additionalManagerId,
                                                      @Nullable final Boolean archivedGroupsOnly)
      throws SegueDatabaseException {
    String pstString =
        "SELECT * FROM groups WHERE id IN (SELECT group_id FROM group_additional_managers WHERE user_id = ?)";

    if (archivedGroupsOnly != null) {
      pstString = pstString + " AND archived = ?";
    }

    pstString = pstString + " AND group_status <> '" + GroupStatus.DELETED.name() + "'";

    return getGroupsBySQLPst(pstString, additionalManagerId, archivedGroupsOnly);
  }

  @Override
  public Long getGroupCount() throws SegueDatabaseException {
    // We don't want to count 'deleted' groups:
    String query = "SELECT COUNT(1) AS TOTAL FROM groups WHERE group_status <> ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_GET_GROUP_COUNT_GROUP_STATUS, GroupStatus.DELETED.name());

      try (ResultSet results = pst.executeQuery()) {
        results.next();
        return results.getLong("TOTAL");
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception: Unable to count groups", e);
    }
  }

  @Override
  public UserGroup findGroupById(final Long groupId) throws SegueDatabaseException {
    return this.findGroupById(groupId, false);
  }

  @Override
  public UserGroup findGroupById(final Long groupId, final boolean includeDeletedGroups) throws SegueDatabaseException {
    try {
      if (includeDeletedGroups) {
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT * FROM groups WHERE id = ?")
        ) {
          pst.setLong(FIELD_GET_BY_ID_GROUP_ID, groupId);
          return buildFindGroupByIdResults(pst);
        }
      } else {
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT * FROM groups WHERE id = ? AND group_status <> ?")
        ) {
          pst.setLong(FIELD_GET_BY_ID_GROUP_ID, groupId);
          pst.setString(FIELD_GET_BY_ID_GROUP_STATUS, GroupStatus.DELETED.name());
          return buildFindGroupByIdResults(pst);
        }
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  private UserGroup buildFindGroupByIdResults(final PreparedStatement pst) throws SQLException, SegueDatabaseException {
    try (ResultSet results = pst.executeQuery()) {
      if (results.next()) {
        if (!results.isLast()) {
          throw new SegueDatabaseException("Expected a single object and found more than one.");
        }
        return this.buildGroup(results);
      } else {
        // Lots of places that call this function expect null if no group was found, i.e. was probably deleted.
        return null;
      }
    }
  }

  @Override
  public void deleteGroup(final Long groupId) throws SegueDatabaseException {
    this.deleteGroup(groupId, true);
  }

  @Override
  public void deleteGroup(final Long groupId, final boolean markAsDeleted) throws SegueDatabaseException {
    if (null == groupId) {
      throw new SegueDatabaseException("Unable to locate the group requested to delete.");
    }

    try {
      if (markAsDeleted) {
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement("UPDATE groups SET group_status=? WHERE id = ?;")
        ) {
          pst.setString(FIELD_MARK_GROUP_DELETED_GROUP_STATUS, GroupStatus.DELETED.name());
          pst.setLong(FIELD_MARK_GROUP_DELETED_ID, groupId);

          if (pst.executeUpdate() == 0) {
            throw new SegueDatabaseException("Unable to mark group as deleted.");
          }
        }
      } else {
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement("DELETE FROM groups WHERE id = ?")
        ) {
          pst.setLong(FIELD_DELETE_GROUP_GROUP_ID, groupId);
          pst.execute();
        }
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
    String query = "SELECT * FROM group_memberships INNER JOIN groups ON "
        + "groups.id = group_memberships.group_id WHERE group_id = ? AND status <> ? AND group_status <> ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_MEMBERSHIP_MAP_GROUP_ID, groupId);
      pst.setString(FIELD_GET_MEMBERSHIP_MAP_MEMBERSHIP_STATUS, GroupMembershipStatus.DELETED.name());
      pst.setString(FIELD_GET_MEMBERSHIP_MAP_GROUP_STATUS, GroupStatus.DELETED.name());

      try (ResultSet results = pst.executeQuery()) {
        Map<Long, GroupMembership> mapOfResults = Maps.newHashMap();
        while (results.next()) {
          mapOfResults.put(results.getLong("user_id"), this.buildMembershipRecord(results));
        }
        return mapOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  /**
   * Will include shallow deleted group membership as we should reuse their group entry in the db.
   *
   * @param groupId group to check
   * @param userId  user to check
   * @return true if they ever had a recorded membership entry in the db.
   * @throws SegueDatabaseException - if there is db error.
   */
  private boolean hasMembershipForGroup(final Long userId, final Long groupId) throws SegueDatabaseException {
    String query = "SELECT COUNT(1) AS TOTAL FROM group_memberships WHERE group_id = ? AND user_id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_HAS_MEMBERSHIP_GROUP_ID, groupId);
      pst.setLong(FIELD_HAS_MEMBERSHIP_USER_ID, userId);

      try (ResultSet results = pst.executeQuery()) {
        results.next();
        return results.getLong("TOTAL") == 1;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception: Unable to count groups", e);
    }

  }

  @Override
  public Collection<UserGroup> getGroupMembershipList(final Long userId) throws SegueDatabaseException {
    String query = "SELECT * FROM groups INNER JOIN group_memberships ON groups.id = group_memberships.group_id"
        + " WHERE user_id = ? AND status <> ? AND group_status <> ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_MEMBERSHIP_LIST_USER_ID, userId);
      pst.setString(FIELD_GET_MEMBERSHIP_LIST_MEMBERSHIP_STATUS, GroupMembershipStatus.DELETED.name());
      pst.setString(FIELD_GET_MEMBERSHIP_LIST_GROUP_STATUS, GroupStatus.DELETED.name());

      try (ResultSet results = pst.executeQuery()) {
        List<UserGroup> listOfResults = Lists.newArrayList();
        while (results.next()) {
          listOfResults.add(this.buildGroup(results));
        }
        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public Set<Long> getAdditionalManagerSetByGroupId(final Long groupId) throws SegueDatabaseException {
    String query = "SELECT * FROM group_additional_managers WHERE group_id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_MANAGER_SET_BY_GROUP_ID_GROUP_ID, groupId);

      // on this occasion we do not care if the group is deleted.

      try (ResultSet results = pst.executeQuery()) {
        Set<Long> listOfResults = Sets.newHashSet();
        while (results.next()) {
          listOfResults.add(results.getLong("user_id"));
        }
        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public void addUserAdditionalManagerList(final Long userId, final Long groupId) throws SegueDatabaseException {
    String query = "INSERT INTO group_additional_managers(group_id, user_id, created) VALUES (?, ?, ?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
    ) {
      pst.setLong(FIELD_ADD_ADDITIONAL_MANAGER_GROUP_ID, groupId);
      pst.setLong(FIELD_ADD_ADDITIONAL_MANAGER_USER_ID, userId);
      pst.setTimestamp(FIELD_ADD_ADDITIONAL_MANAGER_CREATED, new Timestamp(new Date().getTime()));

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
    String query = "DELETE FROM group_additional_managers WHERE group_id = ? AND user_id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_REMOVE_ADDITIONAL_MANAGER_GROUP_ID, groupId);
      pst.setLong(FIELD_REMOVE_ADDITIONAL_MANAGER_USER_ID, userId);

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
   * @throws SQLException - if we cannot extract a required property from the results set.
   */
  private UserGroup buildGroup(final ResultSet set) throws SQLException {
    return new UserGroup(set.getLong("id"), set.getString("group_name"), set.getLong("owner_id"),
        GroupStatus.valueOf(set.getString("group_status")), set.getDate("created"),
        set.getBoolean("archived"), set.getBoolean("additional_manager_privileges"),
        set.getDate("last_updated"));
  }

  private GroupMembership buildMembershipRecord(final ResultSet set) throws SQLException {
    return new GroupMembership(set.getLong("group_id"), set.getLong("user_id"),
        GroupMembershipStatus.valueOf(set.getString("status")), set.getDate("created"), set.getDate("updated"));
  }

  private List<UserGroup> getGroupsBySQLPst(final String pstString, final Long userId,
                                            @Nullable final Boolean archivedGroupsOnly) throws SegueDatabaseException {
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(pstString)
    ) {
      pst.setLong(FIELD_GET_GROUPS_BY_PST_USER_ID, userId);

      if (archivedGroupsOnly != null) {
        pst.setBoolean(FIELD_GET_GROUPS_BY_PST_ARCHIVED_ONLY, archivedGroupsOnly);
      }

      try (ResultSet results = pst.executeQuery()) {
        List<UserGroup> listOfResults = Lists.newArrayList();
        while (results.next()) {
          listOfResults.add(this.buildGroup(results));
        }

        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  // Field Constants
  // createGroup
  private static final int FIELD_CREATE_GROUP_GROUP_NAME = 1;
  private static final int FIELD_CREATE_GROUP_OWNER_ID = 2;
  private static final int FIELD_CREATE_GROUP_GROUP_STATUS = 3;
  private static final int FIELD_CREATE_GROUP_CREATED = 4;
  private static final int FIELD_CREATE_GROUP_LAST_UPDATED = 5;

  // editGroup
  private static final int FIELD_EDIT_GROUP_GROUP_NAME = 1;
  private static final int FIELD_EDIT_GROUP_OWNER_ID = 2;
  private static final int FIELD_EDIT_GROUP_CREATED = 3;
  private static final int FIELD_EDIT_GROUP_ARCHIVED = 4;
  private static final int FIELD_EDIT_GROUP_ADDITIONAL_MANAGER_PRIVILEGES = 5;
  private static final int FIELD_EDIT_GROUP_GROUP_STATUS = 6;
  private static final int FIELD_EDIT_GROUP_LAST_UPDATED = 7;
  private static final int FIELD_EDIT_GROUP_GROUP_ID = 8;

  // addUserToGroup
  private static final int FIELD_ADD_USER_GROUP_ID = 1;
  private static final int FIELD_ADD_USER_USER_ID = 2;
  private static final int FIELD_ADD_USER_MEMBERSHIP_STATUS = 3;
  private static final int FIELD_ADD_USER_CREATED = 4;
  private static final int FIELD_ADD_USER_UPDATED = 5;

  // setUsersGroupMembershipStatus
  private static final int FIELD_SET_MEMBERSHIP_STATUS_MEMBERSHIP_STATUS = 1;
  private static final int FIELD_SET_MEMBERSHIP_STATUS_UPDATED = 2;
  private static final int FIELD_SET_MEMBERSHIP_STATUS_USER_ID = 3;
  private static final int FIELD_SET_MEMBERSHIP_STATUS_GROUP_ID = 4;

  // getGroupCount
  private static final int FIELD_GET_GROUP_COUNT_GROUP_STATUS = 1;

  // findGroupById
  private static final int FIELD_GET_BY_ID_GROUP_ID = 1;
  private static final int FIELD_GET_BY_ID_GROUP_STATUS = 2;

  // deleteGroup - mark deleted
  private static final int FIELD_MARK_GROUP_DELETED_GROUP_STATUS = 1;
  private static final int FIELD_MARK_GROUP_DELETED_ID = 2;

  // deleteGroup - full delete
  private static final int FIELD_DELETE_GROUP_GROUP_ID = 1;

  // getGroupMembershipMap
  private static final int FIELD_GET_MEMBERSHIP_MAP_GROUP_ID = 1;
  private static final int FIELD_GET_MEMBERSHIP_MAP_MEMBERSHIP_STATUS = 2;
  private static final int FIELD_GET_MEMBERSHIP_MAP_GROUP_STATUS = 3;

  // hasMembershipForGroup
  private static final int FIELD_HAS_MEMBERSHIP_GROUP_ID = 1;
  private static final int FIELD_HAS_MEMBERSHIP_USER_ID = 2;

  // getGroupMembershipList
  private static final int FIELD_GET_MEMBERSHIP_LIST_USER_ID = 1;
  private static final int FIELD_GET_MEMBERSHIP_LIST_MEMBERSHIP_STATUS = 2;
  private static final int FIELD_GET_MEMBERSHIP_LIST_GROUP_STATUS = 3;

  // getAdditionalManagerSetByGroupId
  private static final int FIELD_GET_MANAGER_SET_BY_GROUP_ID_GROUP_ID = 1;

  // addUserAdditionalManagerList
  private static final int FIELD_ADD_ADDITIONAL_MANAGER_GROUP_ID = 1;
  private static final int FIELD_ADD_ADDITIONAL_MANAGER_USER_ID = 2;
  private static final int FIELD_ADD_ADDITIONAL_MANAGER_CREATED = 3;

  // removeUserFromAdditionalManagerList
  private static final int FIELD_REMOVE_ADDITIONAL_MANAGER_GROUP_ID = 1;
  private static final int FIELD_REMOVE_ADDITIONAL_MANAGER_USER_ID = 2;

  // getGroupsBySQLPst
  private static final int FIELD_GET_GROUPS_BY_PST_USER_ID = 1;
  private static final int FIELD_GET_GROUPS_BY_PST_ARCHIVED_ONLY = 2;
}
