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
package uk.ac.cam.cl.dtg.segue.api.managers;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.Validate;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserGroupDataManager;
import uk.ac.cam.cl.dtg.segue.dos.UserGroupDO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * GroupManager.
 * Responsible for managing group related logic.
 * @author sac92
 */
public class GroupManager {
	private final IUserGroupDataManager groupDatabase;
	private final UserManager userManager;

	/**
	 * GroupManager.
	 * 
	 * @param groupDatabase
	 *            - the IUserGroupManager implementation
	 * @param userManager
	 *            - the user manager so that the group manager can get user details.
	 */
	@Inject
	public GroupManager(final IUserGroupDataManager groupDatabase, final UserManager userManager) {
		Validate.notNull(groupDatabase);
		Validate.notNull(userManager);
		
		this.groupDatabase = groupDatabase;
		this.userManager = userManager;
	}

	/**
	 * createAssociationGroup.
	 * 
	 * @param groupName
	 *            - name describing the group.
	 * @param groupOwner
	 *            - the user who wishes to grant permissions to another.
	 * @return AssociationGroup
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public UserGroupDO createUserGroup(final String groupName, final RegisteredUserDTO groupOwner)
		throws SegueDatabaseException {
		Validate.notBlank(groupName);
		Validate.notNull(groupOwner);

		UserGroupDO group = new UserGroupDO(null, groupName, groupOwner.getDbId(), new Date());

		return groupDatabase.createGroup(group);
	}

	/**
	 * Delete Group.
	 * 
	 * @param group
	 *            - to delete
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public void deleteGroup(final UserGroupDO group) throws SegueDatabaseException {
		groupDatabase.deleteGroup(group);
		// TODO: clear membership information
	}

	/**
	 * getUsersInGroup.
	 * @param group to find
	 * @return list of users who are members of the group
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public List<RegisteredUserDTO> getUsersInGroup(final UserGroupDO group) throws SegueDatabaseException {		
		List<String> groupMemberIds = groupDatabase.getGroupMemberIds(group.getId());
		
		return userManager.findUsers(groupMemberIds);
	}

	/**
	 * getGroupsByOwner.
	 * 
	 * @param ownerUserId
	 *            - the owner of the group to search for.
	 * @return List of groups or empty list.
	 */
	public List<UserGroupDO> getGroupsByOwner(final String ownerUserId) {
		return groupDatabase.getGroupsByOwner(ownerUserId);
	}

	/**
	 * Adds a user to a group.
	 * 
	 * @param group - the group that the user should be added to
	 * @param userToAdd - the user to add to a group
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public void addUserToGroup(final UserGroupDO group, final RegisteredUserDTO userToAdd)
		throws SegueDatabaseException {
		groupDatabase.addUserToGroup(userToAdd.getDbId(), group.getId());
	}

	/**
	 * Removes a user from a group.
	 * 
	 * @param group - that should be affected
	 * @param userToRemove - user that should be removed.
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public void removeUserFromGroup(final UserGroupDO group, final RegisteredUserDTO userToRemove)
		throws SegueDatabaseException {
		groupDatabase.removeUserFromGroup(userToRemove.getDbId(), group.getId());
	}

	/**
	 * Find by Id.
	 * 
	 * @param groupId
	 *            to search for.
	 * @return group or null.
	 */
	public UserGroupDO getGroupById(final String groupId) {
		return groupDatabase.findById(groupId);
	}

	/**
	 * Determine if a group id exists and is valid.
	 * 
	 * @param groupId
	 *            - group id
	 * @return true if it does false if not.
	 */
	public boolean isValidGroup(final String groupId) {
		return this.groupDatabase.findById(groupId) != null;
	}
}
