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
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * GroupManager.
 * 
 * @author sac92
 *
 */
public class GroupManager {
	private final IUserGroupDataManager groupDatabase;
	private final UserManager userManager;

	/**
	 * GroupManager.
	 * 
	 * @param groupDatabase
	 *            - the IUserGroupManager implementation
	 */
	@Inject
	public GroupManager(final IUserGroupDataManager groupDatabase, final UserManager userManager) {
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
	public UserGroup createUserGroup(final String groupName, final RegisteredUserDTO groupOwner)
		throws SegueDatabaseException {
		Validate.notBlank(groupName);
		Validate.notNull(groupOwner);

		UserGroup group = new UserGroup(null, groupName, groupOwner.getDbId(), new Date());

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
	public void deleteGroup(final UserGroup group) throws SegueDatabaseException {
		groupDatabase.deleteGroup(group);
		// TODO: clear membership information
	}

	/**
	 * getUsersInGroup.
	 * @param group to find
	 * @return list of users who are members of the group
	 * @throws SegueDatabaseException
	 */
	public List<RegisteredUserDTO> getUsersInGroup(final UserGroup group) throws SegueDatabaseException {		
		List<String> groupMemberIds = groupDatabase.getGroupMemberIds(group.getId());
		
		return userManager.findUsers(groupMemberIds);
	}

	/**
	 * getGroupsByUser.
	 * 
	 * @param ownerUserId
	 *            - the owner of the group to search for.
	 * @return List of groups or empty list.
	 */
	public List<UserGroup> getGroupsByUser(final String ownerUserId) {
		return groupDatabase.getGroupsByOwner(ownerUserId);
	}

	/**
	 * Adds a user to a group.
	 * 
	 * @param group
	 * @param userToAdd
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public void addUserToGroup(final UserGroup group, final RegisteredUserDTO userToAdd)
		throws SegueDatabaseException {
		groupDatabase.addUserToGroup(userToAdd.getDbId(), group.getId());
	}

	/**
	 * Removes a user from a group.
	 * 
	 * @param group
	 * @param userToRemove
	 * @throws SegueDatabaseException
	 *             - If an error occurred while interacting with the database.
	 */
	public void removeUserFromGroup(final UserGroup group, final RegisteredUserDTO userToRemove)
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
	public UserGroup getGroupById(final String groupId) {
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
