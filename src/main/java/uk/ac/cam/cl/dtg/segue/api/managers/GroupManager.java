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
package uk.ac.cam.cl.dtg.segue.api.managers;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembership;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.segue.dos.GroupStatus;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembership;
import uk.ac.cam.cl.dtg.segue.dto.users.GroupMembershipDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryWithGroupMembershipDTO;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

        groupsObservers = new LinkedList<>();
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

        Date now = new Date();
        UserGroup group = new UserGroup(null, groupName, groupOwner.getId(), GroupStatus.ACTIVE, now, false, now);

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
    public UserGroupDTO editUserGroup(final UserGroupDTO groupToEdit) throws SegueDatabaseException {
        Validate.notNull(groupToEdit);
        UserGroup userGroup = dtoMapper.map(groupToEdit, UserGroup.class);
        userGroup.setLastUpdated(new Date());
        return this.convertGroupToDTO(groupDatabase.editGroup(userGroup));
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

        List<RegisteredUserDTO> users = userManager.findUsers(groupMemberIds);
        this.orderUsersByName(users);
        return users;
    }

    /**
     * Get a map representing the current membership of a given group.
     * @param groupId - group of interest
     * @return map of user id to membership record.
     * @throws SegueDatabaseException
     *              - If an error occurred while interacting with the database.
     */
    public Map<Long, GroupMembershipDTO> getUserMembershipMapForGroup(Long groupId) throws SegueDatabaseException {
        Map<Long, GroupMembershipDTO> result = Maps.newHashMap();
        for(Map.Entry<Long, GroupMembership> entry : this.groupDatabase.getGroupMembershipMap(groupId).entrySet()) {
            result.put(entry.getKey(), dtoMapper.map(entry.getValue(), GroupMembershipDTO.class));
        }
        return result;
    }

    /**
     * Get an individual users groupMembershipStatus
     * @param userId - userId
     * @param groupId - groupId
     * @return the membership status
     */
    public GroupMembershipStatus getGroupMembershipStatus(Long userId, Long groupId) throws SegueDatabaseException {
        return this.getUserMembershipMapForGroup(groupId).get(userId).getStatus();
    }

    /**
     * Helper method to consistently sort users by family name then given name in a case-insensitive order.
     * @param users
     *            - list of users.
     */
    private void orderUsersByName(final List<RegisteredUserDTO> users) {
        users.sort((userA, userB) -> ComparisonChain.start().
                compare(userA.getFamilyName(), userB.getFamilyName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).
                compare(userA.getGivenName(), userB.getGivenName(), Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)).
                result());
    }

    /**
     * get all groups by owner.
     *
     * @param ownerUser
     *            - the owner of the groups to search for.
     * @return List of groups or empty list.
     * @throws SegueDatabaseException if there is a db error
     */
    public List<UserGroupDTO> getGroupsByOwner(final RegisteredUserDTO ownerUser) throws SegueDatabaseException {
        Validate.notNull(ownerUser);
        return convertGroupToDTOs(groupDatabase.getGroupsByOwner(ownerUser.getId()));
    }

    /**
     * getAllGroupsOwnedAndManagedByUser.
     *
     * This method will get all groups that a user could have an interest in.
     * I.e. if the user is the owner or additional manager of the group the group should be included in the list.
     *
     * @param ownerUser
     *            - the owner of the groups to search for.
     * @param archivedGroupsOnly
     *            if true then only archived groups will be returned,
     *            if false then only unarchived groups will be returned.
     * @return List of groups or empty list.
     * @throws SegueDatabaseException - if there is a db error
     */
    public List<UserGroupDTO> getAllGroupsOwnedAndManagedByUser(final RegisteredUserDTO ownerUser, boolean archivedGroupsOnly) throws SegueDatabaseException {
        Validate.notNull(ownerUser);
        List<UserGroupDTO> combinedResults = Lists.newArrayList();
        combinedResults.addAll(convertGroupToDTOs(groupDatabase.getGroupsByOwner(ownerUser.getId(), archivedGroupsOnly)));
        combinedResults.addAll(convertGroupToDTOs(groupDatabase.getGroupsByAdditionalManager(ownerUser.getId(), archivedGroupsOnly)));
        return combinedResults;
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
     * @throws SegueDatabaseException if there is a db error
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
            // although we should force the user membership status to be active for the group.
            this.setMembershipStatus(group, userToAdd, GroupMembershipStatus.ACTIVE);
            log.info(String.format("User (%s) is already a member of the group with id %s. Skipping.",
                    userToAdd.getId(), group.getId()));
        }
    }

    /**
     * Change users group membership status
     *
     * @param group
     *            - that should be affected
     * @param user
     *            - user that should be affected.
     * @param newStatus
     *            - the new membership status
     * @throws SegueDatabaseException
     *             - If an error occurred while interacting with the database.
     */
    public void setMembershipStatus(final UserGroupDTO group, final RegisteredUserDTO user, GroupMembershipStatus newStatus)
            throws SegueDatabaseException {
        Validate.notNull(group);
        Validate.notNull(user);
        // we don't want people to delete user membership via this route as observers are not notified.
        Validate.isTrue(!GroupMembershipStatus.DELETED.equals(newStatus), "Deletion of a group membership should not use this route.");
        groupDatabase.setUsersGroupMembershipStatus(user.getId(), group.getId(), newStatus);
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
        UserGroup group = groupDatabase.findGroupById(groupId);

        if (null == group) {
            throw new ResourceNotFoundException("The group id specified (" + groupId.toString() + ") does not exist.");
        }

        return convertGroupToDTO(group);
    }

    /**
     * Add a user to the list of additional managers who are allowed to manage the group.
     *
     * @param group - group to grant permission for
     * @param userToAdd - user to grant permission to
     * @return The group DTO
     * @throws SegueDatabaseException if there is a db error
     */
    public UserGroupDTO addUserToManagerList(final UserGroupDTO group, final RegisteredUserDTO userToAdd) throws SegueDatabaseException {
        Validate.notNull(group);
        Validate.notNull(userToAdd);

        if (group.getAdditionalManagersUserIds().contains(userToAdd.getId())) {
            // don't add them if they are already in there
            return group;
        }
        this.groupDatabase.addUserAdditionalManagerList(userToAdd.getId(), group.getId());

        // Notify observers of change
        for (IGroupObserver interestedParty : this.groupsObservers) {
            interestedParty.onAdditionalManagerAddedToGroup(group, userToAdd);
        }

        return this.getGroupById(group.getId());
    }

    /**
     * Remove a user from the list of additional managers who are allowed to manage the group.
     *
     * @param group - group to affect
     * @param userToAdd - user to remove from the management list
     * @return The group DTO
     * @throws SegueDatabaseException if there is a db error
     */
    public UserGroupDTO removeUserFromManagerList(final UserGroupDTO group, final RegisteredUserDTO userToAdd) throws SegueDatabaseException {
        Validate.notNull(group);
        Validate.notNull(userToAdd);

        if (!group.getAdditionalManagersUserIds().contains(userToAdd.getId())) {
            // don't remove them if they are not in there
            return group;
        }
        this.groupDatabase.removeUserFromAdditionalManagerList(userToAdd.getId(), group.getId());

        return this.getGroupById(group.getId());
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
            return this.groupDatabase.findGroupById(groupId) != null;
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
     * @throws SegueDatabaseException if there is a db error
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
     * Helper function to check if a user id is in the additional managers list of the group dto.
     * @param group - dto
     * @param userIdToCheck - user id to verify
     * @return true if they are in the list false if not.
     */
    public static boolean isInAdditionalManagerList(final UserGroupDTO group, final Long userIdToCheck) {
        return group.getAdditionalManagersUserIds().contains(userIdToCheck);
    }

    /**
     * Helper function to check if a user has general permission to access a group.
     * @param group - dto
     * @param userIdToCheck - user id to verify
     * @return whether the user is an owner or an additional manager.
     */
    public static boolean isOwnerOrAdditionalManager(final UserGroupDTO group, final Long userIdToCheck) {
        return group.getOwnerId().equals(userIdToCheck) || isInAdditionalManagerList(group, userIdToCheck);
    }

    /**
     * @param group
     *            to convert
     * @return groupDTO
     * @throws SegueDatabaseException
     *            - if there is a database problem.
     */
    private UserGroupDTO convertGroupToDTO(final UserGroup group) throws SegueDatabaseException {
        UserGroupDTO dtoToReturn = dtoMapper.map(group, UserGroupDTO.class);

        try {
            dtoToReturn.setOwnerSummary(userManager.convertToDetailedUserSummaryObject(userManager.getUserDTOById(group.getOwnerId()), UserSummaryWithEmailAddressDTO.class));
        } catch (NoUserException e) {
            // This should never happen!
            log.error(String.format("Group (%s) has owner ID (%s) that no longer exists!", group.getId(), group.getOwnerId()));
        }

        Set<UserSummaryWithEmailAddressDTO> setOfUsers = Sets.newHashSet();
        Set<Long> additionalManagers = this.groupDatabase.getAdditionalManagerSetByGroupId(group.getId());

        if (additionalManagers != null) {
            setOfUsers.addAll(userManager.convertToDetailedUserSummaryObjectList(userManager.findUsers(additionalManagers), UserSummaryWithEmailAddressDTO.class));
        }

        dtoToReturn.setAdditionalManagers(setOfUsers);

        return dtoToReturn;
    }

    /**
     * @param groups
     *            to convert
     * @return groupDTOs
     */
    private List<UserGroupDTO> convertGroupToDTOs(final Iterable<UserGroup> groups) throws SegueDatabaseException {
        List<UserGroupDTO> result = Lists.newArrayList();
        for (UserGroup group : groups) {
            result.add(convertGroupToDTO(group));
        }
        return result;
    }

    /**
     * Mutates the list to include group membership information
     *
     * @param group group to look up membership info
     * @param summarisedMemberInfo - the list containing summarised user objects - this will be replaced with summarised user objects that include membership information
     * @throws SegueDatabaseException - if there is an error.
     */
    public void convertToUserSummaryGroupMembership(UserGroupDTO group, List<UserSummaryDTO> summarisedMemberInfo) throws SegueDatabaseException {
        List<UserSummaryWithGroupMembershipDTO> result = Lists.newArrayList();
        Map<Long, GroupMembershipDTO> userMembershipMapforMap = this.getUserMembershipMapForGroup(group.getId());

        for(UserSummaryDTO dto : summarisedMemberInfo) {
            UserSummaryWithGroupMembershipDTO newDTO = dtoMapper.map(dto, UserSummaryWithGroupMembershipDTO.class);
            GroupMembershipDTO groupMembershipDTO = userMembershipMapforMap.get(newDTO.getId());
            newDTO.setGroupMembershipInformation(dtoMapper.map(groupMembershipDTO, GroupMembershipDTO.class));
            result.add(newDTO);
        }

        summarisedMemberInfo.clear();
        summarisedMemberInfo.addAll(result);
    }
}
