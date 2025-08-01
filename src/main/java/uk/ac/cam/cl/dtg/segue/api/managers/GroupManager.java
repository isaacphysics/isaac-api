/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembership;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.isaac.dos.GroupStatus;
import uk.ac.cam.cl.dtg.isaac.dos.UserGroup;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardProgressSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IAssignmentLike;
import uk.ac.cam.cl.dtg.isaac.dto.UserGameboardProgressSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.GroupMembershipDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithGroupMembershipDTO;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserGroupPersistenceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * GroupManager. Responsible for managing group related logic.
 * 
 * @author sac92
 */
public class GroupManager {
    private static final Logger log = LoggerFactory.getLogger(GroupManager.class);

    private final IUserGroupPersistenceManager groupDatabase;
    private final UserAccountManager userManager;
    private final GameManager gameManager;
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
                        final GameManager gameManager, final MapperFacade dtoMapper) {
        Objects.requireNonNull(groupDatabase);
        Objects.requireNonNull(userManager);
        Objects.requireNonNull(gameManager);

        this.groupDatabase = groupDatabase;
        this.userManager = userManager;
        this.gameManager = gameManager;
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
        Objects.requireNonNull(groupOwner);

        Date now = new Date();
        UserGroup group = new UserGroup(null, groupName, groupOwner.getId(), GroupStatus.ACTIVE, now, false, false, now, false);

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
        Objects.requireNonNull(groupToEdit);
        UserGroup userGroup = dtoMapper.map(groupToEdit, UserGroup.class);
        userGroup.setLastUpdated(new Date());

        UserGroup existingGroup = groupDatabase.findGroupById(groupToEdit.getId());
        UserGroupDTO group = this.convertGroupToDTO(groupDatabase.editGroup(userGroup));

        if (existingGroup.isAdditionalManagerPrivileges() != group.isAdditionalManagerPrivileges()) {
            // Notify observers of change in additional manager privileges
            for (IGroupObserver interestedParty : this.groupsObservers) {
                interestedParty.onAdditionalManagerPrivilegesChanged(group);
            }
        }

        return group;
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
        Objects.requireNonNull(group);
        groupDatabase.deleteGroup(group.getId());
    }

    /**
     * getUsersInGroup. This sorts the users by given name, then family name (case-insensitive)
     * 
     * @param group
     *            to find
     * @return list of users who are members of the group, sorted by given name, then family name (case-insensitive)
     * @throws SegueDatabaseException
     *             - If an error occurred while interacting with the database.
     */
    public List<RegisteredUserDTO> getUsersInGroup(final UserGroupDTO group) throws SegueDatabaseException {
        Objects.requireNonNull(group);
        List<Long> groupMemberIds = Lists.newArrayList(groupDatabase.getGroupMemberIds(group.getId()));

        if (groupMemberIds.isEmpty()) {
            return Lists.newArrayList();
        }

        List<RegisteredUserDTO> users = userManager.findUsers(groupMemberIds);
        // Sort the users by name
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
     * Helper method to consistently sort users by given name then family name in a case-insensitive order.
     * @param users
     *            - list of users.
     */
    private void orderUsersByName(final List<RegisteredUserDTO> users) {
        // Remove apostrophes so that string containing them are ordered in the same way as in Excel.
        // I.e. we want that "O'Aaa" < "Obbb" < "O'Ccc"
        Comparator<String> excelStringOrder = Comparator.nullsLast((String a, String b) ->
                String.CASE_INSENSITIVE_ORDER.compare(a.replaceAll("'", ""), b.replaceAll("'", "")));

        // If names differ only by an apostrophe (i.e. "O'A" and "Oa"), break ties using name including any apostrophes:
        users.sort(Comparator
                .comparing(RegisteredUserDTO::getFamilyName, excelStringOrder)
                .thenComparing(RegisteredUserDTO::getGivenName, excelStringOrder)
                .thenComparing(RegisteredUserDTO::getFamilyName));
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
        Objects.requireNonNull(ownerUser);
        List<UserGroupDTO> combinedResults = Lists.newArrayList();
        combinedResults.addAll(convertGroupsToDTOs(groupDatabase.getGroupsByOwner(ownerUser.getId(), archivedGroupsOnly)));
        combinedResults.addAll(convertGroupsToDTOs(groupDatabase.getGroupsByAdditionalManager(ownerUser.getId(), archivedGroupsOnly)));
        return combinedResults;
    }

    /**
     * getGroupMembershipList. Gets the groups a user is a member of.
     * 
     * @param userToLookup
     *            - the user to search for group membership details for.
     * @param augmentGroups
     *            - whether to add owner and manager information to a group.
     * @return the list of groups the user belongs to.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    public List<UserGroupDTO> getGroupMembershipList(final RegisteredUserDTO userToLookup, final boolean augmentGroups)
            throws SegueDatabaseException {
        Objects.requireNonNull(userToLookup);

        return convertGroupsToDTOs(this.groupDatabase.getGroupMembershipList(userToLookup.getId()), augmentGroups);
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
        Objects.requireNonNull(group);
        Objects.requireNonNull(userToAdd);

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
        Objects.requireNonNull(group);
        Objects.requireNonNull(user);
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
        Objects.requireNonNull(group);
        Objects.requireNonNull(userToRemove);
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

    public List<UserGroupDTO> getGroupsByIds(final List<Long> groupIds, final Boolean augmentGroups) throws SegueDatabaseException {

        List<UserGroup> groups = groupDatabase.findGroupsByIds(groupIds);

        return convertGroupsToDTOs(groups, augmentGroups);

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
        Objects.requireNonNull(group);
        Objects.requireNonNull(userToAdd);

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
     * Transfer group ownership from the group owner to another user.
     *
     * @param group - group to affect
     * @param newOwner - user to promote to owner of the group
     * @param oldOwner - user (must be previous group owner) to demote to additional manager status
     * @return The group DTO
     * @throws SegueDatabaseException if there is a db error
     * @throws IllegalAccessException if oldOwner is not the current owner of the group
     */
    public UserGroupDTO promoteUserToOwner(final UserGroupDTO group, final RegisteredUserDTO newOwner, final RegisteredUserDTO oldOwner) throws SegueDatabaseException, IllegalAccessException {
        Objects.requireNonNull(group);
        Objects.requireNonNull(newOwner);
        Objects.requireNonNull(oldOwner);

        // Old owner must actually be the old (current) owner of the group
        if (!oldOwner.getId().equals(group.getOwnerId())) {
            throw new IllegalAccessException("The user with id: " + oldOwner.getId() + " is not the current owner of the group with id: " + group.getId() + "!");
        }

        if (newOwner.getId().equals(oldOwner.getId())) {
            // No ownership change
            return group;
        }

        // Change old and new owners additional manager status if appropriate
        if (!group.getAdditionalManagersUserIds().contains(oldOwner.getId())) {
            this.groupDatabase.addUserAdditionalManagerList(oldOwner.getId(), group.getId());
        }
        if (group.getAdditionalManagersUserIds().contains(newOwner.getId())) {
            this.groupDatabase.removeUserFromAdditionalManagerList(newOwner.getId(), group.getId());
        }

        // ! We are mutating this group object, but this particular mutation should be safe !
        group.setOwnerId(newOwner.getId());

        // Notify observers of ownership change
        for (IGroupObserver interestedParty : this.groupsObservers) {
            interestedParty.onAdditionalManagerPromotedToOwner(group, newOwner);
        }

        return this.editUserGroup(group);
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
        Objects.requireNonNull(group);
        Objects.requireNonNull(userToAdd);

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
        List<UserGroupDTO> groups = this.getGroupMembershipList(user, false);
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
     * Helper function to check if a user has additional permissions to modify and manage a group.
     * @param group - dto
     * @param userIdToCheck - user id to verify
     * @return whether the user is an owner or an additional manager with privileges.
     */
    public static boolean hasAdditionalManagerPrivileges(final UserGroupDTO group, final Long userIdToCheck) {
        return group.getOwnerId().equals(userIdToCheck) || (isInAdditionalManagerList(group, userIdToCheck) && group.isAdditionalManagerPrivileges());
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
     * Convert a collection of group DOs into DTOs.
     * 
     * @param groups - to convert
     * @param augmentGroups - whether owner and manager information is required for the group
     * @return groupDTOs
     * @throws SegueDatabaseException
     *      *            - if there is a database problem.
     */
    private List<UserGroupDTO> convertGroupsToDTOs(final Collection<UserGroup> groups, final boolean augmentGroups)
            throws SegueDatabaseException {
        List<UserGroupDTO> result = Lists.newArrayList();

        // go through each group and get the related user information in the correct format
        for (UserGroup group : groups) {
            UserGroupDTO dtoToReturn = dtoMapper.map(group, UserGroupDTO.class);
            result.add(dtoToReturn);
        }

        if (augmentGroups) {

            Set<Long> groupIds = groups.stream().map(UserGroup::getId).collect(Collectors.toSet());
            Map<Long, Set<Long>> groupAdditionalManagers = groupDatabase.getAdditionalManagerSetsByGroupIds(groupIds);
            Set<Long> ownerManagerIds = groups.stream().map(UserGroup::getOwnerId).collect(Collectors.toSet());
            ownerManagerIds.addAll(groupAdditionalManagers.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()));

            List<RegisteredUserDTO> userLookup = userManager.findUsers(ownerManagerIds);
            Map<Long, RegisteredUserDTO> userLookupCache = userLookup.stream().collect(Collectors.toMap(RegisteredUserDTO::getId, Function.identity()));

            for (UserGroupDTO groupDTO : result) {
                // set owner summary:
                RegisteredUserDTO ownerUser = userLookupCache.get(groupDTO.getOwnerId());
                if (null != ownerUser) {
                    groupDTO.setOwnerSummary(userManager.convertToDetailedUserSummaryObject(ownerUser, UserSummaryWithEmailAddressDTO.class));
                } else {
                    log.debug(String.format("Group (%s) has owner ID (%s) that no longer exists!", groupDTO.getId(), groupDTO.getOwnerId()));
                }

                // set additional manager summary:
                Set<Long> additionalManagers = groupAdditionalManagers.get(groupDTO.getId());
                Set<UserSummaryWithEmailAddressDTO> setOfUsers = Sets.newHashSet();
                if (additionalManagers != null) {
                    for (Long additionalManagerId : additionalManagers) {
                        RegisteredUserDTO managerUser = userLookupCache.get(additionalManagerId);
                        if (managerUser != null) {
                            setOfUsers.add(userManager.convertToDetailedUserSummaryObject(managerUser, UserSummaryWithEmailAddressDTO.class));
                        } else {
                            log.debug(String.format("Group (%s) has manager ID (%s) that no longer exists!", groupDTO.getId(), groupDTO.getOwnerId()));
                        }
                    }
                }

                groupDTO.setAdditionalManagers(setOfUsers);
            }
        }

        return result;
    }
    /**
     * Convert a collection of group DOs into DTOs.
     *
     * @param groups - to convert
     * @return groupDTOs
     * @throws SegueDatabaseException
     *      *            - if there is a database problem.
     */
    private List<UserGroupDTO> convertGroupsToDTOs(final Collection<UserGroup> groups) throws SegueDatabaseException {
        return convertGroupsToDTOs(groups, true);
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

    /**
     * Returns the progress of all the members of a group for all their assignments.
     *
     * @param groupMembers Group members for whom to return progress
     * @param assignments  Assignments for which to calculate progress
     * @return Progress per group member, per assignment
     *
     * @throws SegueDatabaseException
     * @throws ContentManagerException
     */
    public List<UserGameboardProgressSummaryDTO> getGroupProgressSummary(List<RegisteredUserDTO> groupMembers,
                                                                         Collection<AssignmentDTO> assignments)
            throws SegueDatabaseException, ContentManagerException {

        List<UserGameboardProgressSummaryDTO> groupProgressSummary = new ArrayList<>();
        Map<RegisteredUserDTO, List<GameboardProgressSummaryDTO>> userProgressMap = new HashMap<>();
        for (RegisteredUserDTO user : groupMembers) {
            userProgressMap.put(user, new ArrayList<>());
        }

        for (AssignmentDTO assignment : assignments) {
            // Not sure why I have to do this but AssignmentDTO::getGameboard returns null
            GameboardDTO gameboard = gameManager.getGameboard(assignment.getGameboardId());

            List<ImmutablePair<RegisteredUserDTO, List<GameboardItem>>> userProgressData = gameManager.gatherGameProgressData(groupMembers, gameboard);

            for (ImmutablePair<RegisteredUserDTO, List<GameboardItem>> userProgress : userProgressData) {
                RegisteredUserDTO user = userProgress.getKey();
                List<GameboardItem> progress = userProgress.getValue();

                int questionPartsCorrect = 0,
                    questionPartsIncorrect = 0,
                    questionPartsNotAttempted = 0,
                    questionPartsTotal = 0,
                    questionPagesPerfect = 0;
                float passMark = 0.0f;

                for (GameboardItem gameboardItem : progress) {
                    questionPartsCorrect += gameboardItem.getQuestionPartsCorrect();
                    questionPartsIncorrect += gameboardItem.getQuestionPartsIncorrect();
                    questionPartsNotAttempted += gameboardItem.getQuestionPartsNotAttempted();
                    questionPartsTotal += gameboardItem.getQuestionPartsTotal();
                    passMark += gameboardItem.getPassMark();
                    Constants.CompletionState state = gameboardItem.getState();
                    if (state == Constants.CompletionState.ALL_CORRECT) {
                        questionPagesPerfect += 1;
                    }
                }
                passMark = passMark / (progress.size() * 100.0f);

                GameboardProgressSummaryDTO summary = new GameboardProgressSummaryDTO();
                summary.setAssignmentId(assignment.getId());
                summary.setGameboardId(assignment.getGameboardId());
                summary.setGameboardTitle(gameboard.getTitle());
                summary.setDueDate(assignment.getDueDate());
                summary.setCreationDate(assignment.getCreationDate());
                summary.setQuestionPartsCorrect(questionPartsCorrect);
                summary.setQuestionPartsIncorrect(questionPartsIncorrect);
                summary.setQuestionPartsNotAttempted(questionPartsNotAttempted);
                summary.setQuestionPartsTotal(questionPartsTotal);
                summary.setPassMark(passMark);
                summary.setQuestionPagesPerfect(questionPagesPerfect);
                summary.setQuestionPagesTotal(progress.size());
                userProgressMap.get(user).add(summary);
            }
        }

        userProgressMap.forEach((user, progress) -> {
            UserGameboardProgressSummaryDTO summary = new UserGameboardProgressSummaryDTO();
            summary.setUser(userManager.convertToUserSummaryObject(user));
            summary.setProgress(progress);
            groupProgressSummary.add(summary);
        });

        return groupProgressSummary;
    }

    public <T extends IAssignmentLike> List<T> filterItemsBasedOnMembershipContext(final List<T> assignments, final Long userId) throws SegueDatabaseException {
        List<T> results = Lists.newArrayList();

        Map<Long, GroupMembership> groupMembershipMap = groupDatabase.getGroupMembershipMapForUser(userId);

        for (T assignment : assignments) {

            GroupMembership membershipRecord = groupMembershipMap.get(assignment.getGroupId());
            // if they are inactive and they became inactive before the assignment was sent we want to skip the assignment.

            Date assignmentStartDate = assignment.getScheduledStartDate();
            if (assignmentStartDate == null) {
                assignmentStartDate = assignment.getCreationDate();
            }
            if (GroupMembershipStatus.INACTIVE.equals(membershipRecord.getStatus())
                && membershipRecord.getUpdated().before(assignmentStartDate)) {
                continue;
            }

            results.add(assignment);
        }
        return results;
    }
}
