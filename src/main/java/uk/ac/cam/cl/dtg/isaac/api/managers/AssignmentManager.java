/**
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

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.dao.AssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * AssignmentManager.
 */
public class AssignmentManager {
	private static final Logger log = LoggerFactory.getLogger(AssignmentManager.class);

	private final AssignmentPersistenceManager assignmentPersistenceManager;
	private final GroupManager groupManager;

	/**
	 * AssignmentManager.
	 * 
	 * @param assignmentPersistenceManager
	 *            - to save assignments
	 * @param groupManager
	 *            - to allow communication with the group manager.
	 */
	@Inject
	public AssignmentManager(final AssignmentPersistenceManager assignmentPersistenceManager,
			final GroupManager groupManager) {
		this.assignmentPersistenceManager = assignmentPersistenceManager;
		this.groupManager = groupManager;
	}

	/**
	 * Get Assignments.
	 * 
	 * @param user
	 *            - to get the assignments for.
	 * @return List of assignments for the given user.
	 * @throws SegueDatabaseException
	 *             - if we cannot complete a required database operation.
	 */
	public List<AssignmentDTO> getAssignments(final RegisteredUserDTO user) throws SegueDatabaseException {
		List<UserGroup> groups = groupManager.getGroupsByOwner(user.getDbId());

		if (groups.size() == 0) {
			log.debug(String.format("User (%s) does not have any groups", user.getDbId()));
			return Lists.newArrayList();
		}

		List<AssignmentDTO> assignments = Lists.newArrayList();
		for (UserGroup group : groups) {
			assignments.addAll(this.assignmentPersistenceManager.getAssignmentsByGroupId(group));
		}

		return assignments;
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

		if (assignmentPersistenceManager.getAssignmentsByGameboardAndGroup(
				newAssignment.getGameboardId(), newAssignment.getGroupId()).size() != 0) {
			throw new DuplicateAssignmentException(String.format(
					"You cannot assign the same work (%s) to a group (%s) more than once.",
					newAssignment.getGameboardId(), newAssignment.getGroupId()));
		}
		
		newAssignment.setCreationDate(new Date());
		newAssignment.setId(this.assignmentPersistenceManager.saveAssignment(newAssignment));
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
	public List<AssignmentDTO> getAllAssignmentsSetByUser(final RegisteredUserDTO user)
		throws SegueDatabaseException {
		return this.assignmentPersistenceManager.getAssignmentsByOwner(user.getDbId());
	}

	/**
	 * deleteAssignment.
	 * 
	 * @param assignment
	 *            - to delete.
	 * @throws SegueDatabaseException
	 *             - if we cannot complete a required database operation.
	 */
	public void deleteAssignment(final AssignmentDTO assignment) throws SegueDatabaseException {
		Validate.notNull(assignment);
		Validate.notBlank(assignment.getId());
		this.assignmentPersistenceManager.deleteAssignment(assignment.getId());
	}

	/**
	 * findAssignmentByGameboardAndGroup.
	 * 
	 * @param gameboardId
	 *            to match
	 * @param groupId
	 *            group id to match
	 * @return assignment
	 * @throws SegueDatabaseException
	 *             - if we cannot complete a required database operation.
	 */
	public AssignmentDTO findAssignmentByGameboardAndGroup(final String gameboardId, final String groupId)
		throws SegueDatabaseException {
		List<AssignmentDTO> assignments = this.assignmentPersistenceManager
				.getAssignmentsByGameboardAndGroup(gameboardId, groupId);

		if (assignments.size() == 0) {
			return null;
		} else if (assignments.size() == 1) {
			return assignments.get(0);
		}

		throw new SegueDatabaseException(String.format(
				"Duplicate Assignment (group: %s) (gameboard: %s) Exception: %s", groupId, gameboardId,
				assignments));
	}
}
