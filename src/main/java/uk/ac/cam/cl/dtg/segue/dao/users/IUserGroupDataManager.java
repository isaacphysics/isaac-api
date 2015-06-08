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

import java.util.Collection;
import java.util.List;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;

/**
 * Interface for data manager classes that deal with user association data.
 *
 */
public interface IUserGroupDataManager {

    /**
     * Get groups by owner.
     * 
     * @param ownerUserId
     *            the owner Id to find all groups for.
     * @return List of groups belonging to owner user.
     */
    List<UserGroup> getGroupsByOwner(String ownerUserId);

    /**
     * Find User group by Id.
     * 
     * @param groupId
     *            - the id of the group to find.
     * @return group
     */
    UserGroup findById(String groupId);

    /**
     * @param groupId
     *            group to lookup
     * @return member user ids.
     */
    List<String> getGroupMemberIds(String groupId);

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
    void addUserToGroup(String userId, String groupId) throws SegueDatabaseException;

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
    void removeUserFromGroup(String userId, String groupId) throws SegueDatabaseException;

    /**
     * Delete group and all membership information.
     * 
     * @param groupId
     *            to delete.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    void deleteGroup(String groupId) throws SegueDatabaseException;

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
     * getGroupMembershipList.
     * 
     * @param userId
     *            - to lookup
     * @return the list of groups the user belongs to.
     * @throws SegueDatabaseException
     *             - if a database error occurs.
     */
    Collection<UserGroup> getGroupMembershipList(String userId) throws SegueDatabaseException;

}
