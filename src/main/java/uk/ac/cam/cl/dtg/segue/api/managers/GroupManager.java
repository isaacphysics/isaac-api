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
import java.util.LinkedList;
import java.util.List;

import ma.glasnost.orika.MapperFacade;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * GroupManager. Responsible for managing group related logic.
 * 
 * @author sac92
 */
public class GroupManager {
    private static final Logger log = LoggerFactory.getLogger(GroupManager.class);

    private final IUserGroupPersistenceManager groupDatabase;
    private final UserAccountManager userManager;
    private final MapperFacade dtoMapper;
    private List<IGroupObserver> groupsObservers;

    /**
     * GroupManager.
     * 
     * @param groupDatabase
     *            - the IUserGroupManager implementation
     * @param userManager
     *            - the user manager so that the group manager can get user details.
     * @param dtoMapper
     *            - Preconfigured dto mapper
     */
    @Inject
    public GroupManager(final IUserGroupPersistenceManager groupDatabase, final UserAccountManager userManager,
            final MapperFacade dtoMapper) {
        Validate.notNull(groupDatabase);
        Validate.notNull(userManager);

        this.groupDatabase = groupDatabase;
        this.userManager = userManager;
        this.dtoMapper = dtoMapper;

        groupsObservers = new LinkedList<IGroupObserver>();
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
    public UserGroupDTO createUserGroup(final String groupName, final RegisteredUserDTO groupOwner)
            throws SegueDatabaseException {
        Validate.notBlank(groupName);
        Validate.notNull(groupOwner);

        UserGroup group = new UserGroup(null, groupName, groupOwner.getId(), new Date(), false);

        return this.convertGroupToDTO(groupDatabase.createGroup(group));
    }

    /**
     * createAssociationGroup.
     * 
     * @param groupToEdit
     *            - group to edit.
     * @return modified group.
     * @throws SegueDatabaseException
     *             - If an error occurred while interacting with the database.
     */
    public UserGroupDTO editUserGroup(final UserGroup groupToEdit) throws SegueDatabaseException {
        Validate.notNull(groupToEdit);

        return this.convertGroupToDTO(groupDatabase.editGroup(groupToEdit));
    }

    /**
     * Delete Group and all related data.
     * 
     * @param group
     *            - to delete
     * @throws SegueDatabaseException
     *             - If an error occurred while interacting with the database.
     */
    public void deleteGroup(final UserGroupDTO group) throws SegueDatabaseException {
        Validate.notNull(group);
        groupDatabase.deleteGroup(group.getId());
    }

    /**
     * getUsersInGroup.
     * 
     * @param group
     *            to find
     * @return list of users who are members of the group
     * @throws SegueDatabaseException
     *             - If an error occurred while interacting with the database.
     */
    public List<RegisteredUserDTO> getUsersInGroup(final UserGroupDTO group) throws SegueDatabaseException {
        Validate.notNull(group);
        List<Long> groupMemberIds = groupDatabase.getGroupMemberIds(group.getId());

        if (groupMemberIds.isEmpty()) {
            return Lists.newArrayList();
        }

        return userManager.findUsers(groupMemberIds);
    }

    /**
     * get all groups by owner.
     *
     * @param ownerUser
     *            - the owner of the groups to search for.
     * @return List of groups or empty list.
     * @throws SegueDatabaseException
     */
    public List<UserGroupDTO> getGroupsByOwner(final RegisteredUserDTO ownerUser) throws SegueDatabaseException {
        Validate.notNull(ownerUser);
        return convertGroupToDTOs(groupDatabase.getGroupsByOwner(ownerUser.getId()));
    }

    /**
     * getGroupsByOwner.
     * 
     * @param ownerUser
     *            - the owner of the groups to search for.
     * @param archivedGroupsOnly
     *            if true then only archived groups will be returned,
     *            if false then only unarchived groups will be returned.
     * @return List of groups or empty list.
     * @throws SegueDatabaseException 
     */
    public List<UserGroupDTO> getGroupsByOwner(final RegisteredUserDTO ownerUser, boolean archivedGroupsOnly) throws SegueDatabaseException {
        Validate.notNull(ownerUser);
        return convertGroupToDTOs(groupDatabase.getGroupsByOwner(ownerUser.getId(), archivedGroupsOnly));
    }

    /**
     * getGroupMembershipList. Gets the groups a user is a member of.
     * 
     * @param userToLookup
     *            - the user to search for group membership details for.
     * @return the list of groups the user belongs to.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    public List<UserGroupDTO> getGroupMembershipList(final RegisteredUserDTO userToLookup)
            throws SegueDatabaseException {
        Validate.notNull(userToLookup);

        return convertGroupToDTOs(this.groupDatabase.getGroupMembershipList(userToLookup.getId()));
    }

    /**
     * Adds a user to a group.
     * 
     * @param group
     *            - the group that the user should be added to
     * @param userToAdd
     *            - the user to add to a group
     * @throws SegueDatabaseException
     *             - If an error occurred while interacting with the database.
     */
    public void addUserToGroup(final UserGroupDTO group, final RegisteredUserDTO userToAdd)
            throws SegueDatabaseException {
        Validate.notNull(group);
        Validate.notNull(userToAdd);

        // don't do it if they are already in there
        if (!this.isUserInGroup(userToAdd, group)) {
            groupDatabase.addUserToGroup(userToAdd.getId(), group.getId());

            // Notify observers of change
            for (IGroupObserver interestedParty : this.groupsObservers) {
                interestedParty.onMemberAddedToGroup(group, userToAdd);
            }

        } else {
            // otherwise it is a noop.
            log.info(String.format("User (%s) is already a member of the group with id %s. Skipping.",
                    userToAdd.getId(), group.getId()));
        }
    }

    /**
     * Removes a user from a group.
     * 
     * @param group
     *            - that should be affected
     * @param userToRemove
     *            - user that should be removed.
     * @throws SegueDatabaseException
     *             - If an error occurred while interacting with the database.
     */
    public void removeUserFromGroup(final UserGroupDTO group, final RegisteredUserDTO userToRemove)
            throws SegueDatabaseException {
        Validate.notNull(group);
        Validate.notNull(userToRemove);
        groupDatabase.removeUserFromGroup(userToRemove.getId(), group.getId());

        for (IGroupObserver interestedParty : this.groupsObservers) {
            interestedParty.onGroupMembershipRemoved(group, userToRemove);
        }
    }

    /**
     * Find by Id.
     * 
     * @param groupId
     *            to search for.
     * @return group or null.
     * @throws ResourceNotFoundException
     *             - if we cannot find the resource specified.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    public UserGroupDTO getGroupById(final Long groupId) throws ResourceNotFoundException, SegueDatabaseException {
        UserGroup group = groupDatabase.findById(groupId);

        if (null == group) {
            throw new ResourceNotFoundException("The group id specified (" + groupId.toString() + ") does not exist.");
        }

        return convertGroupToDTO(group);
    }

    /**
     * Determine if a group id exists and is valid.
     * 
     * @param groupId
     *            - group id
     * @return true if it does false if not.
     */
    public boolean isValidGroup(final Long groupId) {
        try {
            return this.groupDatabase.findById(groupId) != null;
        } catch (SegueDatabaseException e) {
            log.error("Database error while validating group: failing validation silently");
            return false;
        }
    }

    /**
     * isUserInGroup?
     * 
     * @param user
     *            - to look for
     * @param group
     *            - group to check.
     * @return true if yes false if no.
     * @throws SegueDatabaseException
     *             - if there is a database problem.
     */
    public boolean isUserInGroup(final RegisteredUserDTO user, final UserGroupDTO group) throws SegueDatabaseException {
        List<UserGroupDTO> groups = this.getGroupMembershipList(user);
        return groups.contains(group);
    }
    
    /**
     * @return the total number of groups stored in the database.
     * @throws SegueDatabaseException 
     */
    public Long getGroupCount() throws SegueDatabaseException {
        return groupDatabase.getGroupCount();
    }

    /**
     * @param interestedParty - object interested in knowing when groups change
     */
    public void registerInterestInGroups(final IGroupObserver interestedParty) {
        groupsObservers.add(interestedParty);
    }


    /**
     * @param group
     *            to convert
     * @return groupDTO
     */
    private UserGroupDTO convertGroupToDTO(final UserGroup group) {
        return dtoMapper.map(group, UserGroupDTO.class);
    }

    /**
     * @param groups
     *            to convert
     * @return groupDTOs
     */
    private List<UserGroupDTO> convertGroupToDTOs(final Iterable<UserGroup> groups) {
        List<UserGroupDTO> result = Lists.newArrayList();
        for (UserGroup group : groups) {
            result.add(convertGroupToDTO(group));
        }
        return result;
    }

}
