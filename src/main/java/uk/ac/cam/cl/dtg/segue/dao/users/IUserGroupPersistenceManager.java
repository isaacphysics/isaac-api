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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembership;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;

import javax.annotation.Nullable;

/**
 * Interface for data manager classes that deal with group data.
 *
 */
public interface IUserGroupPersistenceManager {

    /**
     * Get all groups by owner.
     *
     * @param ownerUserId
     *            the owner Id to find all groups for.
     * @return List of groups belonging to owner user.
     * @throws SegueDatabaseException
     *             - if we cannot contact the database.
     */
    List<UserGroup> getGroupsByOwner(Long ownerUserId) throws SegueDatabaseException;

    /**
     * Get groups by owner.
     * 
     * @param ownerUserId
     *            the owner Id to find all groups for.
     * @param archivedGroupsOnly
     *            if true then only archived groups will be returned,
     *            if false then only unarchived groups will be returned.
     *            if null then we will return all groups.
     * @return List of groups belonging to owner user.
     * @throws SegueDatabaseException
     *             - if we cannot contact the database.
     */
    List<UserGroup> getGroupsByOwner(Long ownerUserId, Boolean archivedGroupsOnly) throws SegueDatabaseException;

    /**
     * Find User group by Id including deleted groups.
     *
     * Note this should only be used when trying to reconstruct assignment state, never when exposing group information directly to users.
     *
     * @param groupId
     *            - the id of the group to find.
     * @param includeDeletedGroups
     *            - Enable retrieval of groups marked as deleted
     * @return group
     * @throws SegueDatabaseException
     *             - if we cannot contact the database.
     */
    UserGroup findGroupById(Long groupId, boolean includeDeletedGroups) throws SegueDatabaseException;

    /**
     * Find User group by Id.
     * 
     * @param groupId
     *            - the id of the group to find.
     * @return group
     * @throws SegueDatabaseException
     *             - if we cannot contact the database.
     */
    UserGroup findGroupById(Long groupId) throws SegueDatabaseException;

    /**
     * Create a group that users can be assigned to.
     * 
     * This is only to support organisation of accounts that can access data about other users.
     * 
     * @param group
     *            - to save
     * @return group saved (with database id included)
     * @throws SegueDatabaseException
     *             if there is a problem with the database operation.
     */
    UserGroup createGroup(UserGroup group) throws SegueDatabaseException;

    /**
     * Adds a user to a group.
     * 
     * @param userId
     *            the user id to add to the group
     * @param groupId
     *            the group id to add to.
     * @throws SegueDatabaseException
     *             - if there is a problem adding the group membership
     */
    void addUserToGroup(Long userId, Long groupId) throws SegueDatabaseException;

    /**
     * Update group membership status for a given user
     * @param userId - the id of the user
     * @param groupId - the group they are a member of
     * @param newStatus - e.g. active, inactive or deleted.
     *
     * @throws SegueDatabaseException - if an error occurs.
     */
    void setUsersGroupMembershipStatus(Long userId, Long groupId, GroupMembershipStatus newStatus) throws SegueDatabaseException;

    /**
     * Remove a user from a group.
     * 
     * @param userId
     *            the user id to remove from the group
     * @param groupId
     *            the group id of interest.
     * @throws SegueDatabaseException
     *             - if there is a problem removing the group membership
     */
    void removeUserFromGroup(Long userId, Long groupId) throws SegueDatabaseException;

    /**
     * Mark the group status as deleted.
     *
     * @param groupId
     *            to delete.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    void deleteGroup(Long groupId) throws SegueDatabaseException;

    /**
     * Delete group and all membership information.
     * 
     * @param groupId
     *            to delete.
     * @param markAsDeleted
     *            if true, then the group will have the status set to deleted rather than actually being removed,
     *            false will actually delete the group from the database permanently
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    void deleteGroup(Long groupId, boolean markAsDeleted) throws SegueDatabaseException;

    /**
     * Edit the group information for an existing group.
     * 
     * @param group
     *            to edit
     * @return edited group.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    UserGroup editGroup(UserGroup group) throws SegueDatabaseException;

    /**
     * Collects a list of user ids who are a member of a given group.
     * @param groupId - group of interest
     * @return list of user ids.
     * @throws SegueDatabaseException - if there is a database error
     */
    Collection<Long> getGroupMemberIds(Long groupId) throws SegueDatabaseException;

    /**
     * Create a map of user id to membership status so that group membership information can be used to change behaviour.
     * @param groupId of interest
     * @return
     * @throws SegueDatabaseException
     */
    Map<Long, GroupMembership> getGroupMembershipMap(Long groupId) throws SegueDatabaseException;

    /**
     * getGroupMembershipList.
     * The list of groups the user belongs to.
     * @param userId
     *            - to lookup
     * @return the list of groups that the user belongs to.
     * @throws SegueDatabaseException
     *             - if a database error occurs.
     */
    Collection<UserGroup> getGroupMembershipList(Long userId) throws SegueDatabaseException;

    /**
     * Useful for getting the number of groups in the database.
     * @return the total number of groups
     * @throws SegueDatabaseException
     *             - if we cannot contact the database.
     */
    Long getGroupCount() throws SegueDatabaseException;

    /**
     * Get the list of Id's representing the users who currently are listed as additional managers for a group.
     *
     * @param groupId - the group id of interest
     * @return list of user ids.
     * @throws SegueDatabaseException
     */
    Set<Long> getAdditionalManagerSetByGroupId(final Long groupId) throws SegueDatabaseException;

    /**
     * Get groups by additional manager id.
     *
     * @param additionalManagerId
     *            the additional Manager Id to find all groups for.
     * @throws SegueDatabaseException
     *             - if we cannot contact the database.
     */
    List<UserGroup> getGroupsByAdditionalManager(final Long additionalManagerId) throws SegueDatabaseException;

    /**
     * Get groups by additional manager id
     *
     * @param additionalManagerId
     *            the owner Id to find all groups for.
     * @param archivedGroupsOnly
     *            if true then only archived groups will be returned,
     *            if false then only unarchived groups will be returned.
     *            if null then we will return all groups.
     * @return List of groups belonging to owner user.
     * @throws SegueDatabaseException
     *             - if we cannot contact the database.
     */
    List<UserGroup> getGroupsByAdditionalManager(final Long additionalManagerId, @Nullable final Boolean archivedGroupsOnly) throws SegueDatabaseException;

    /**
     * Add a user to the additional manager list for a group.
     *
     * @param userId - user Id to add
     * @param groupId - group id to be affected
     * @throws SegueDatabaseException - if we cannot contact the database.
     */
    void addUserAdditionalManagerList(final Long userId, final Long groupId) throws SegueDatabaseException;

    /**
     * Remove a user to the additional manager list for a group.
     *
     * @param userId - user Id to add
     * @param groupId - group id to be affected
     * @throws SegueDatabaseException - if we cannot contact the database.
     */
    void removeUserFromAdditionalManagerList(final Long userId, final Long groupId) throws SegueDatabaseException;
}
