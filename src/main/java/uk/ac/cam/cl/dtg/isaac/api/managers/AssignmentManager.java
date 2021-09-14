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
import com.google.inject.Inject;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.isaac.dao.IAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IAssignmentLike;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

/**
 * AssignmentManager.
 */
public class AssignmentManager implements IAssignmentLike.Details<AssignmentDTO> {
    private static final Logger log = LoggerFactory.getLogger(AssignmentManager.class);

    private final IAssignmentPersistenceManager assignmentPersistenceManager;
    private final GroupManager groupManager;
    private final EmailService emailService;
    private final GameManager gameManager;
    private final PropertiesLoader properties;

    /**
     * AssignmentManager.
     *  @param assignmentPersistenceManager
     *            - to save assignments
     * @param groupManager
     *            - to allow communication with the group manager.
     * @param emailService
     *            - service for sending specific emails.
     * @param gameManager
 *            - the game manager object
     */
    @Inject
    public AssignmentManager(final IAssignmentPersistenceManager assignmentPersistenceManager,
                             final GroupManager groupManager, final EmailService emailService,
                             final GameManager gameManager, final PropertiesLoader properties) {
        this.assignmentPersistenceManager = assignmentPersistenceManager;
        this.groupManager = groupManager;
        this.emailService = emailService;
        this.gameManager = gameManager;
        this.properties = properties;
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
        List<AssignmentDTO> assignments = this.groupManager.filterItemsBasedOnMembershipContext(
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

        GameboardDTO gameboard = gameManager.getGameboard(newAssignment.getGameboardId());

        final String gameboardURL = String.format("https://%s/assignment/%s", properties.getProperty(HOST_NAME),
            gameboard.getId());

        emailService.sendAssignmentEmailToGroup(newAssignment, gameboard, ImmutableMap.of("gameboardURL", gameboardURL) ,
            "email-template-group-assignment");

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
    public String getAssignmentLikeName(AssignmentDTO existingAssignment) throws SegueDatabaseException {
        GameboardDTO gameboard = gameManager.getGameboard(existingAssignment.getGameboardId());
        String name = existingAssignment.getGameboardId();
        if (gameboard != null && gameboard.getTitle() != null && !gameboard.getTitle().isEmpty()) {
            name = gameboard.getTitle();
        }
        return name;
    }

    @Override
    public String getAssignmentLikeUrl(AssignmentDTO existingAssignment) {
        return String.format("https://%s/assignment/%s",
                        properties.getProperty(HOST_NAME),
                        existingAssignment.getGameboardId());
    }
}
