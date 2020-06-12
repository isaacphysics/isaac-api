/*
 * Copyright 2015 Stephen Cummins
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
package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.IAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.IGroupObserver;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.GroupMembershipDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.util.NameFormatter;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.util.NameFormatter.getFilteredGroupNameFromGroup;
import static uk.ac.cam.cl.dtg.util.NameFormatter.getTeacherNameFromUser;

/**
 * AssignmentManager.
 */
public class AssignmentManager implements IGroupObserver {
    private static final Logger log = LoggerFactory.getLogger(AssignmentManager.class);

    private final IAssignmentPersistenceManager assignmentPersistenceManager;
    private final GroupManager groupManager;
    private final EmailManager emailManager;
    private final UserAccountManager userManager;
	private final GameManager gameManager;
    private final UserAssociationManager userAssociationManager;
    private final PropertiesLoader properties;

    /**
     * AssignmentManager.
     * 
     * @param assignmentPersistenceManager
     *            - to save assignments
     * @param groupManager
     *            - to allow communication with the group manager.
     * @param emailManager
     *            - email manager
     * @param userManager
     *            - the user manager object
     * @param gameManager
     *            - the game manager object
     * @param userAssociationManager
     *            - the userAssociationManager manager object
     */
    @Inject
    public AssignmentManager(final IAssignmentPersistenceManager assignmentPersistenceManager,
            final GroupManager groupManager, final EmailManager emailManager, final UserAccountManager userManager,
            final GameManager gameManager, final UserAssociationManager userAssociationManager, final PropertiesLoader properties) {
        this.assignmentPersistenceManager = assignmentPersistenceManager;
        this.groupManager = groupManager;
        this.emailManager = emailManager;
        this.userManager = userManager; 
		this.gameManager = gameManager;
        this.userAssociationManager = userAssociationManager;
        this.properties = properties;
        groupManager.registerInterestInGroups(this);
    }

    /**
     * Get Assignments set for a given user.
     * 
     * @param user
     *            - to get the assignments for.
     * @return List of assignments for the given user.
     * @throws SegueDatabaseException
     *             - if we cannot complete a required database operation.
     */
    public Collection<AssignmentDTO> getAssignments(final RegisteredUserDTO user) throws SegueDatabaseException {
        List<UserGroupDTO> groups = groupManager.getGroupMembershipList(user, false);

        if (groups.size() == 0) {
            log.debug(String.format("User (%s) does not have any groups", user.getId()));
            return Lists.newArrayList();
        }

        List<Long> groupIds = groups.stream().map(UserGroupDTO::getId).collect(Collectors.toList());
        List<AssignmentDTO> assignments = this.filterAssignmentsBasedOnGroupMembershipContext(
                this.assignmentPersistenceManager.getAssignmentsByGroupList(groupIds), user.getId());

        return assignments;
    }

    /**
     * Get all assignments for a given group id
     * @param groupId - to which the assignments have been assigned
     * @return all assignments
     * @throws SegueDatabaseException
     */
    public Collection<AssignmentDTO> getAssignmentsByGroup(final Long groupId) throws SegueDatabaseException {
        return this.assignmentPersistenceManager.getAssignmentsByGroupId(groupId);
    }

    /**
     * getAssignmentById.
     * 
     * @param assignmentId
     *            to find
     * @return the assignment.
     * @throws SegueDatabaseException
     *             - if we cannot complete a required database operation.
     */
    public AssignmentDTO getAssignmentById(final Long assignmentId) throws SegueDatabaseException {
        return this.assignmentPersistenceManager.getAssignmentById(assignmentId);
    }
    

    /**
     * create Assignment.
     * 
     * @param newAssignment
     *            - to create - will be modified to include new id.
     * @return the assignment object now with the id field populated.
     * @throws SegueDatabaseException
     *             - if we cannot complete a required database operation.
     */
    public AssignmentDTO createAssignment(final AssignmentDTO newAssignment) throws SegueDatabaseException {
        Validate.isTrue(newAssignment.getId() == null, "The id field must be empty.");
        Validate.notNull(newAssignment.getGameboardId());
        Validate.notNull(newAssignment.getGroupId());

        if (assignmentPersistenceManager.getAssignmentsByGameboardAndGroup(newAssignment.getGameboardId(),
                newAssignment.getGroupId()).size() != 0) {
            log.error(String.format("Duplicated Assignment Exception - cannot assign the same work %s to a group %s",
                    newAssignment.getGameboardId(), newAssignment.getGroupId()));
            throw new DuplicateAssignmentException("You cannot assign the same work to a group more than once.");
        }
        
        newAssignment.setCreationDate(new Date());
        newAssignment.setId(this.assignmentPersistenceManager.saveAssignment(newAssignment));

        UserGroupDTO userGroupDTO = groupManager.getGroupById(newAssignment.getGroupId());
        List<RegisteredUserDTO> usersToEmail = Lists.newArrayList();
        Map<Long, GroupMembershipDTO> userMembershipMapforGroup = this.groupManager.getUserMembershipMapForGroup(userGroupDTO.getId());
        GameboardDTO gameboard = gameManager.getGameboard(newAssignment.getGameboardId());
        
        // filter users so those who are inactive in the group aren't emailed
        for (RegisteredUserDTO user : groupManager.getUsersInGroup(userGroupDTO)) {
            if (GroupMembershipStatus.ACTIVE.equals(userMembershipMapforGroup.get(user.getId()).getStatus())) {
                usersToEmail.add(user);
            }
        }

		// inform all members of the group that there is now an assignment for them.
        try {
            final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yy");
            final String gameboardURL = String.format("https://%s/assignment/%s", properties.getProperty(HOST_NAME),
                    gameboard.getId());

            String dueDate = "";
            if (newAssignment.getDueDate() != null) {
                dueDate = String.format(" (due on %s)", DATE_FORMAT.format(newAssignment.getDueDate()));
            }

            String gameboardName = gameboard.getId();
            if (gameboard.getTitle() != null) {
                gameboardName = gameboard.getTitle();
            }

            RegisteredUserDTO assignmentOwnerDTO = this.userManager.getUserDTOById(newAssignment.getOwnerUserId());

            String groupName = getFilteredGroupNameFromGroup(userGroupDTO);
            String assignmentOwner = getTeacherNameFromUser(assignmentOwnerDTO);

            for (RegisteredUserDTO userDTO : usersToEmail) {
                emailManager.sendTemplatedEmailToUser(userDTO,
                        emailManager.getEmailTemplateDTO("email-template-group-assignment"),
                        ImmutableMap.of(
                                "gameboardURL", gameboardURL,
                                "gameboardName", gameboardName,
                                "assignmentDueDate", dueDate,
                                "groupName", groupName,
                                "assignmentOwner", assignmentOwner
                        ), EmailType.ASSIGNMENTS);
            }

        } catch (ContentManagerException e) {
            log.error("Could not send group assignment emails due to content issue", e);
        } catch (NoUserException e) {
            log.error("Could not send group assignment emails because owner did not exist.", e);
        }

        return newAssignment;
    }

    /**
     * Assignments set by user.
     *
     * @param user
     *            - who set the assignments
     * @return the assignments.
     * @throws SegueDatabaseException
     *             - if we cannot complete a required database operation.
     */
    public List<AssignmentDTO> getAllAssignmentsSetByUser(final RegisteredUserDTO user) throws SegueDatabaseException {
        Validate.notNull(user);
        return this.assignmentPersistenceManager.getAssignmentsByOwner(user.getId());
    }

    /**
     * Get all assignments for a list of groups.
     *
     * @param groups to include in the search
     * @return a list of assignments set to the group ids provided.
     * @throws SegueDatabaseException
     *             - if we cannot complete a required database operation.
     */
    public List<AssignmentDTO> getAllAssignmentsForSpecificGroups(final Collection<UserGroupDTO> groups) throws SegueDatabaseException {
        Validate.notNull(groups);
        // TODO - Is there a better way of doing this empty list check? Database method explodes if given it.
        if (groups.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> groupIds = groups.stream().map(UserGroupDTO::getId).collect(Collectors.toList());
        return this.assignmentPersistenceManager.getAssignmentsByGroupList(groupIds);
    }

    /**
     * deleteAssignment.
     * 
     * @param assignment
     *            - to delete (must have an id).
     * @throws SegueDatabaseException
     *             - if we cannot complete a required database operation.
     */
    public void deleteAssignment(final AssignmentDTO assignment) throws SegueDatabaseException {
        Validate.notNull(assignment);
        Validate.notNull(assignment.getId());
        this.assignmentPersistenceManager.deleteAssignment(assignment.getId());
    }

    /**
     * findAssignmentByGameboardAndGroup.
     * 
     * @param gameboardId
     *            to match
     * @param groupId
     *            group id to match
     * @return assignment or null if none matches the parameters provided.
     * @throws SegueDatabaseException
     *             - if we cannot complete a required database operation.
     */
    public AssignmentDTO findAssignmentByGameboardAndGroup(final String gameboardId, final Long groupId)
            throws SegueDatabaseException {
        List<AssignmentDTO> assignments = this.assignmentPersistenceManager.getAssignmentsByGameboardAndGroup(
                gameboardId, groupId);

        if (assignments.size() == 0) {
            return null;
        } else if (assignments.size() == 1) {
            return assignments.get(0);
        }

        throw new SegueDatabaseException(String.format(
                "Duplicate Assignment (group: %s) (gameboard: %s) Exception: %s", groupId, gameboardId, assignments));
    }

    /**
     * findGroupsByGameboard.
     * 
     * @param user
     *            - owner of assignments (teacher)
     * @param gameboardId
     *            - the gameboard id to query
     * @return Empty List if none or a List or groups.
     * @throws SegueDatabaseException
     *             - If something goes wrong with database access.
     */
    public List<UserGroupDTO> findGroupsByGameboard(final RegisteredUserDTO user, final String gameboardId)
            throws SegueDatabaseException {
        Validate.notNull(user);
        Validate.notBlank(gameboardId);

        List<UserGroupDTO> allGroupsForUser = this.groupManager.getAllGroupsOwnedAndManagedByUser(user, false);
        List<AssignmentDTO> allAssignmentsForMyGroups = this.getAllAssignmentsForSpecificGroups(allGroupsForUser);

        List<UserGroupDTO> groups = Lists.newArrayList();

        for (AssignmentDTO assignment : allAssignmentsForMyGroups) {
            if (assignment.getGameboardId().equals(gameboardId)) {
                groups.add(groupManager.getGroupById(assignment.getGroupId()));
            }
        }

        return groups;
    }

    @Override
    public void onGroupMembershipRemoved(final UserGroupDTO group, final RegisteredUserDTO user) {
		// do nothing
    }

    @Override
    public void onMemberAddedToGroup(final UserGroupDTO group, final RegisteredUserDTO user) {
		Validate.notNull(group);
		Validate.notNull(user);

        // Try to email user to let them know
        try {
            List<AssignmentDTO> existingAssignments = this.getAllAssignmentsForSpecificGroups(Collections.singletonList(group));

            emailManager.sendTemplatedEmailToUser(user,
                    emailManager.getEmailTemplateDTO("email-template-group-welcome"),
                    this.prepareGroupWelcomeEmailTokenMap(user, group, existingAssignments),
                    EmailType.SYSTEM);

        } catch (ContentManagerException e) {
            log.info("Could not send group welcome email ", e);
        } catch (SegueDatabaseException e) {
            log.error("Unable to send group welcome e-mail due to a database error. Failing silently.", e);
        }
    }

    @Override
    public void onAdditionalManagerAddedToGroup(final UserGroupDTO group, final RegisteredUserDTO additionalManagerUser) {
        Validate.notNull(group);
        Validate.notNull(additionalManagerUser);

        // Try to email user to let them know:
        try {
            RegisteredUserDTO groupOwner = this.userManager.getUserDTOById(group.getOwnerId());

            String groupOwnerName = getTeacherNameFromUser(groupOwner);
            String groupOwnerEmail = "Unknown";
            if (groupOwner != null && groupOwner.getEmail() != null && !groupOwner.getEmail().isEmpty()) {
                groupOwnerEmail = groupOwner.getEmail();
            }
            String groupName = "Unknown";
            if (group.getGroupName() != null && !group.getGroupName().isEmpty()) {
                groupName = group.getGroupName();
            }

            Map<String, Object> emailProperties = new ImmutableMap.Builder<String, Object>()
                    .put("ownerName", groupOwnerName)
                    .put("ownerEmail", groupOwnerEmail)
                    .put("groupName", groupName)
                    .build();
            emailManager.sendTemplatedEmailToUser(additionalManagerUser,
                    emailManager.getEmailTemplateDTO("email-template-group-additional-manager-welcome"),
                    emailProperties,
                    EmailType.SYSTEM);

        } catch (ContentManagerException e) {
            log.info("Could not send group additional manager email ", e);
        } catch (NoUserException e) {
            log.info(String.format("Could not find owner user object of group %s", group.getId()), e);
        } catch (SegueDatabaseException e) {
            log.error("Unable to send group additional manager e-mail due to a database error. Failing silently.", e);
        }
    }

    /**
     * Helper to build up the list of tokens for addToGroup email.
     *
     * @param userDTO - identity of user
     * @param userGroup - group being added to.
     * @param existingAssignments - Any existing assignments that have been set.
     * @return a map of string to string, with some values that may want to be shown in the email.
     * @throws SegueDatabaseException if we can't get the gameboard details.
     */
    private Map<String, Object> prepareGroupWelcomeEmailTokenMap(final RegisteredUserDTO userDTO, final UserGroupDTO userGroup,
                                                                 final List<AssignmentDTO> existingAssignments)
            throws SegueDatabaseException {
        Validate.notNull(userDTO);

        UserSummaryWithEmailAddressDTO groupOwner = userGroup.getOwnerSummary();
        String groupOwnerName = getTeacherNameFromUser(groupOwner);

        String teacherInfo;
        if (!userGroup.getAdditionalManagers().isEmpty()) {
            teacherInfo = String.format("your teachers %s and %s",
                    userGroup.getAdditionalManagers().stream().map(NameFormatter::getTeacherNameFromUser).collect(Collectors.joining(", ")),
                    groupOwnerName);
        } else {
            teacherInfo = String.format("your teacher %s", groupOwnerName);
        }

        String groupName = getFilteredGroupNameFromGroup(userGroup);

        if (existingAssignments != null) {
            existingAssignments.sort(Comparator.comparing(AssignmentDTO::getCreationDate));
        }

        final DateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
        StringBuilder htmlSB = new StringBuilder();
        StringBuilder plainTextSB = new StringBuilder();

        if (existingAssignments != null && existingAssignments.size() > 0) {
            htmlSB.append("Your teacher has assigned the following assignments:<br>");
            plainTextSB.append("Your teacher has assigned the following assignments:\n");

            for (int i = 0; i < existingAssignments.size(); i++) {
                GameboardDTO gameboard = gameManager.getGameboard(existingAssignments.get(i).getGameboardId());
                String gameboardName = existingAssignments.get(i).getGameboardId();
                if (gameboard != null && gameboard.getTitle() != null && !gameboard.getTitle().isEmpty()) {
                    gameboardName = gameboard.getTitle();
                }

                String gameboardUrl = String.format("https://%s/assignment/%s",
                        properties.getProperty(HOST_NAME),
                        existingAssignments.get(i).getGameboardId());

                String dueDate = "";
                if (existingAssignments.get(i).getDueDate() != null) {
                    dueDate = String.format(", due on %s", DATE_FORMAT.format(existingAssignments.get(i).getDueDate()));
                }

                htmlSB.append(String.format("%d. <a href='%s'>%s</a> (set on %s%s)<br>", i + 1, gameboardUrl,
                        gameboardName, DATE_FORMAT.format(existingAssignments.get(i).getCreationDate()), dueDate));

                plainTextSB.append(String.format("%d. %s (set on %s%s)\n", i + 1, gameboardName,
                        DATE_FORMAT.format(existingAssignments.get(i).getCreationDate()), dueDate));
            }
        } else if (existingAssignments != null) {
            htmlSB.append("No assignments have been set yet.<br>");
            plainTextSB.append("No assignments have been set yet.\n");
        }

        return new ImmutableMap.Builder<String, Object>()
                .put("teacherName", groupOwnerName)
                .put("teacherInfo", teacherInfo)
                .put("groupName", groupName)
                .put("assignmentsInfo", plainTextSB.toString())
                .put("assignmentsInfo_HTML", htmlSB.toString())
                .build();
    }

    private List<AssignmentDTO> filterAssignmentsBasedOnGroupMembershipContext(List<AssignmentDTO> assignments, Long userId) throws SegueDatabaseException {
        Map<Long, Map<Long, GroupMembershipDTO>> groupIdToUserMembershipInfoMap = Maps.newHashMap();
        List<AssignmentDTO> results = Lists.newArrayList();

        for (AssignmentDTO assignment : assignments) {
            if (!groupIdToUserMembershipInfoMap.containsKey(assignment.getGroupId())) {
                groupIdToUserMembershipInfoMap.put(assignment.getGroupId(), this.groupManager.getUserMembershipMapForGroup(assignment.getGroupId()));
            }

            GroupMembershipDTO membershipRecord = groupIdToUserMembershipInfoMap.get(assignment.getGroupId()).get(userId);
            // if they are inactive and they became inactive before the assignment was sent we want to skip the assignment.
            if (GroupMembershipStatus.INACTIVE.equals(membershipRecord.getStatus())
                    && membershipRecord.getUpdated().before(assignment.getCreationDate()) ) {
                continue;
            }

            results.add(assignment);
        }
        return results;
    }
}
