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
package uk.ac.cam.cl.dtg.isaac.dao;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ma.glasnost.orika.MapperFacade;

import org.apache.commons.lang3.Validate;
import org.elasticsearch.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.dos.AssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dos.AssignmentGroupDO;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.dao.IAppDatabaseManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import static com.google.common.collect.Maps.*;

/**
 * This class is responsible for managing and persisting user data.
 */
public class AssignmentPersistenceManager {
	private static final Logger log = LoggerFactory.getLogger(AssignmentPersistenceManager.class);

	private static final String DB_ID_FIELD = "_id";
	private static final String DB_ASSIGNMENT_FKEY = "assignmentId";
	private static final String DB_GROUP_FKEY = "groupId";

	private final IAppDatabaseManager<AssignmentDO> assignmentsDataManager;
	private final IAppDatabaseManager<AssignmentGroupDO> databaseForAssignmentGroups;
	
	private final MapperFacade mapper;

	/**
	 * Creates a new user data manager object.
	 * 
	 * @param databaseForAssignments
	 *            - the database reference used for persistence.
	 * @param databaseForAssignmentGroups
	 *            - the database reference used for persistence of user to
	 *            assignment to group relationships.
	 * @param mapper
	 *            - An instance of an automapper that can be used for mapping to
	 *            and from AssignmentDOs and DTOs.
	 */
	@Inject
	public AssignmentPersistenceManager(final IAppDatabaseManager<AssignmentDO> databaseForAssignments,
			final IAppDatabaseManager<AssignmentGroupDO> databaseForAssignmentGroups,
			final MapperFacade mapper) {
		this.assignmentsDataManager = databaseForAssignments;
		this.databaseForAssignmentGroups = databaseForAssignmentGroups;
		this.mapper = mapper;
	}

	/**
	 * Save an Assignment.
	 * 
	 * @param assignment
	 *            - assignment to save
	 * @return internal database id for the saved assignment.
	 * @throws SegueDatabaseException
	 *             - if there is a problem saving the assignment in the database.
	 */
	public final String saveAssignment(final AssignmentDTO assignment)
		throws SegueDatabaseException {
		AssignmentDO assignmentToSave = mapper.map(assignment, AssignmentDO.class);

		String resultId = assignmentsDataManager.save(assignmentToSave);
		log.debug("Saving Assignment... Assignment ID: " + assignment.getId() + " Db id : " + resultId);

		return resultId;
	}

	/**
	 * Add a group to an assignment.
	 * 
	 * @param groupId
	 *            - groupId to link
	 * @param assignmentId
	 *            - assignment to link
	 * @throws SegueDatabaseException
	 *             - if there is a problem persisting the link in the database.
	 */
	public void linkGroupToAssignment(final String groupId, final String assignmentId)
		throws SegueDatabaseException {
		Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatch = Maps.newHashMap();

		Map.Entry<BooleanOperator, String> userIdFieldParam = immutableEntry(BooleanOperator.AND, DB_GROUP_FKEY);
		Map.Entry<BooleanOperator, String> assignmentIdFieldParam = immutableEntry(BooleanOperator.AND,
				DB_ASSIGNMENT_FKEY);
		fieldsToMatch.put(userIdFieldParam, Arrays.asList(groupId));
		fieldsToMatch.put(assignmentIdFieldParam, Arrays.asList(assignmentId));

		List<AssignmentGroupDO> groupAssignmentDOs = this.databaseForAssignmentGroups.find(fieldsToMatch);

		if (groupAssignmentDOs.size() == 0) {
			// if this user is not already connected make a connection.
			AssignmentGroupDO assignmentUserDO = new AssignmentGroupDO(null, assignmentId, groupId,
					new Date());

			this.databaseForAssignmentGroups.save(assignmentUserDO);
		} else if (groupAssignmentDOs.size() == 1) {
			// if the group is already connected to the assignment then 
			// something is wrong.
			log.error(String.format("Group (%s) is already allocated to this assignment (%s).", groupId, assignmentId));
		} else {
			log.error("Expected one result and found multiple user assignment associations.");
		}
	}
	
	/**
	 * Allows a link between users and a assignment to be destroyed.
	 * 
	 * @param groupId
	 *            - users id.
	 * @param assignmentId
	 *            - assignment's id
	 * @throws SegueDatabaseException
	 *             - if there is an error during the delete operation.
	 */
	public void removeGroupLinkFromAssignment(final String groupId, final String assignmentId)
		throws SegueDatabaseException {
		
		// verify that the link exists and retrieve the link id.
		Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatch = Maps.newHashMap();

		Map.Entry<BooleanOperator, String> userIdFieldParam = immutableEntry(BooleanOperator.AND, DB_GROUP_FKEY);
		Map.Entry<BooleanOperator, String> assignmentIdFieldParam = immutableEntry(BooleanOperator.AND,
				DB_ASSIGNMENT_FKEY);
		fieldsToMatch.put(userIdFieldParam, Arrays.asList(groupId));
		fieldsToMatch.put(assignmentIdFieldParam, Arrays.asList(assignmentId));

		List<AssignmentGroupDO> groupAssignmentDOs = this.databaseForAssignmentGroups.find(fieldsToMatch);
		
		if (groupAssignmentDOs.size() == 1) {
			// delete it
			this.databaseForAssignmentGroups.delete(groupAssignmentDOs.get(0).getId());
		} else if (groupAssignmentDOs.size() == 0) {
			// unable to find it.
			log.info("Attempted to remove group to assignment link but there was none to remove.");
		} else {
			// too many groupAssignments found.
			throw new SegueDatabaseException(
					"Unable to delete the assignment as there is more than one "
					+ "linking that matches the search terms. Found: "
							+ groupAssignmentDOs.size());
		}
	}

	/**
	 * Find a assignment by id.
	 * 
	 * @param assignmentId
	 *            - the id to search for.
	 * @return the assignment or null if we can't find it..
	 * @throws SegueDatabaseException  - if there is a problem accessing the database.
	 */
	public final AssignmentDTO getAssignmentById(final String assignmentId) throws SegueDatabaseException {

		AssignmentDO assignmentFromDb = assignmentsDataManager.getById(assignmentId);

		if (null == assignmentFromDb) {
			return null;
		}

		AssignmentDTO assignmentDTO = this.convertToAssignmentDTO(assignmentFromDb);

		return assignmentDTO;
	}

	/**
	 * Retrieve all Assignments for a given
	 * group.
	 * 
	 * @param group
	 *            - to search for
	 * @return assignments as a list
	 * @throws SegueDatabaseException
	 *             - if there is an error when accessing the database.
	 */
	public final List<AssignmentDTO> getAssignmentsByGroupId(final UserGroup group) throws SegueDatabaseException {
		// find all assignments related to this group.
		Map<String, AssignmentGroupDO> assignmentLinksToGroup = this.findAssignmentsByAssociatedGroupId(group
				.getId());

		List<String> assignmentIdsLinkedToGroup = Lists.newArrayList();
		assignmentIdsLinkedToGroup.addAll(assignmentLinksToGroup.keySet());

		if (null == assignmentIdsLinkedToGroup || assignmentIdsLinkedToGroup.isEmpty()) {
			return Lists.newArrayList();
		}

		Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatch = Maps.newHashMap();

		fieldsToMatch
				.put(immutableEntry(Constants.BooleanOperator.OR, DB_ID_FIELD), assignmentIdsLinkedToGroup);

		List<AssignmentDTO> assignmentDTOs = this.convertToAssignmentDTOs(this.assignmentsDataManager
				.find(fieldsToMatch));

		return assignmentDTOs;
	}

	/**
	 * Convert form a list of assignment DOs to a list of assignment DTOs.
	 * 
	 * @param assignmentDOs
	 *            to convert
	 * @return assignment DTO
	 */
	private List<AssignmentDTO> convertToAssignmentDTOs(final List<AssignmentDO> assignmentDOs) {
		Validate.notNull(assignmentDOs);

		List<AssignmentDTO> assignmentDTOs = Lists.newArrayList();

		for (AssignmentDO assignmentDO : assignmentDOs) {
			assignmentDTOs.add(this.convertToAssignmentDTO(assignmentDO));
		}

		return assignmentDTOs;
	}

	/**
	 * Convert form a Assignment DO to a Assignment DTO.
	 * 
	 * This method relies on the api to fully resolve questions.
	 * 
	 * @param assignmentDO
	 *            - to convert
	 * @return Assignment DTO
	 */
	private AssignmentDTO convertToAssignmentDTO(final AssignmentDO assignmentDO) {
		return mapper.map(assignmentDO, AssignmentDTO.class);
	}
	
	/**
	 * Find all assignments that are connected to a given group.
	 * 
	 * @param groupId
	 *            to search against.
	 * @return A Map of ids to AssignmentGroup.
	 * @throws SegueDatabaseException - if there is a problem accessing the database.
	 */
	private Map<String, AssignmentGroupDO> findAssignmentsByAssociatedGroupId(final String groupId)
		throws SegueDatabaseException {
		// find all assignments related to this groupId.
		Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatchForAssignmentSearch = Maps.newHashMap();

		fieldsToMatchForAssignmentSearch.put(immutableEntry(Constants.BooleanOperator.AND, DB_GROUP_FKEY),
				Arrays.asList(groupId));

		List<AssignmentGroupDO> assignmentGroupDO = this.databaseForAssignmentGroups
				.find(fieldsToMatchForAssignmentSearch);

		Map<String, AssignmentGroupDO> resultToReturn = Maps.newHashMap();
		for (AssignmentGroupDO objectToConvert : assignmentGroupDO) {
			resultToReturn.put(objectToConvert.getAssignmentId(), objectToConvert);
		}

		return resultToReturn;
	}	
}
