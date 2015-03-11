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
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.dao.IAppDatabaseManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import static com.google.common.collect.Maps.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;

/**
 * This class is responsible for managing and persisting user data.
 */
public class AssignmentPersistenceManager {
	private static final Logger log = LoggerFactory.getLogger(AssignmentPersistenceManager.class);

	private static final String DB_OWNER_ID = "ownerUserId";
	private static final String DB_GROUP_FKEY = "groupId";

	private final IAppDatabaseManager<AssignmentDO> assignmentsDataManager;	
	private final MapperFacade mapper;

	/**
	 * Creates a new user data manager object.
	 * 
	 * @param databaseForAssignments
	 *            - the database reference used for persistence.
	 * @param mapper
	 *            - An instance of an automapper that can be used for mapping to
	 *            and from AssignmentDOs and DTOs.
	 */
	@Inject
	public AssignmentPersistenceManager(final IAppDatabaseManager<AssignmentDO> databaseForAssignments,
			final MapperFacade mapper) {
		this.assignmentsDataManager = databaseForAssignments;
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
	public String saveAssignment(final AssignmentDTO assignment)
		throws SegueDatabaseException {		
		AssignmentDO assignmentToSave = mapper.map(assignment, AssignmentDO.class);

		String resultId = assignmentsDataManager.save(assignmentToSave);
		log.debug("Saving Assignment... Assignment ID: " + assignment.getId() + " Db id : " + resultId);

		return resultId;
	}


	/**
	 * Find a assignment by id.
	 * 
	 * @param assignmentId
	 *            - the id to search for.
	 * @return the assignment or null if we can't find it..
	 * @throws SegueDatabaseException  - if there is a problem accessing the database.
	 */
	public AssignmentDTO getAssignmentById(final String assignmentId) throws SegueDatabaseException {
		if (null == assignmentId || assignmentId.isEmpty()) {
			return null;
		}
		
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
	 * @param groupId
	 *            - to search for
	 * @return assignments as a list
	 * @throws SegueDatabaseException
	 *             - if there is an error when accessing the database.
	 */
	public List<AssignmentDTO> getAssignmentsByGroupId(final String groupId) throws SegueDatabaseException {
		
		// find all assignments related to this groupId.
		Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatchForAssignmentSearch = Maps.newHashMap();

		fieldsToMatchForAssignmentSearch.put(immutableEntry(Constants.BooleanOperator.AND, DB_GROUP_FKEY),
				Arrays.asList(groupId));

		return findByMappedConditions(fieldsToMatchForAssignmentSearch);
	}

	
	/**
	 * Retrieve all Assignments for a given
	 * group and set by a given user.
	 * 
	 * @param assignmentOwnerId
	 *            - to search for
	 * @param groupId
	 *            - to search for
	 * @return assignments as a list
	 * @throws SegueDatabaseException
	 *             - if there is an error when accessing the database.
	 */
	public List<AssignmentDTO> getAssignmentsByOwnerIdAndGroupId(final String assignmentOwnerId,
			final String groupId) throws SegueDatabaseException {
		
		// find all assignments related to this groupId.
		Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatchForAssignmentSearch = Maps.newHashMap();

		fieldsToMatchForAssignmentSearch.put(immutableEntry(Constants.BooleanOperator.AND, DB_GROUP_FKEY),
				Arrays.asList(groupId));
		fieldsToMatchForAssignmentSearch.put(immutableEntry(Constants.BooleanOperator.AND, DB_OWNER_ID),
				Arrays.asList(assignmentOwnerId));

		return findByMappedConditions(fieldsToMatchForAssignmentSearch);
	}
	
	/**
	 * getAssignmentsByGameboardAndOwner.
	 * 
	 * @param gameboardId - gameboard of interest
	 * @param ownerId - the user id who might have assigned the gameboard.
	 * @return list of assignments
	 * @throws SegueDatabaseException
	 *             - if there is an error when accessing the database.
	 */
	public List<AssignmentDTO> getAssignmentsByGameboardAndOwner(final String gameboardId,
			final String ownerId) throws SegueDatabaseException {
		Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatchForAssignmentSearch = Maps.newHashMap();

		fieldsToMatchForAssignmentSearch.put(immutableEntry(Constants.BooleanOperator.AND, GAMEBOARD_ID_FKEY),
				Arrays.asList(gameboardId));
		fieldsToMatchForAssignmentSearch.put(immutableEntry(Constants.BooleanOperator.AND, DB_OWNER_ID),
				Arrays.asList(ownerId));

		return findByMappedConditions(fieldsToMatchForAssignmentSearch);
	}
	
	/**
	 * getAssignmentsByGameboardAndGroup.
	 * 
	 * @param gameboardId
	 *            - gameboard of interest
	 * @param groupId
	 *            - the group id has the gameboard assigned.
	 * @return assignment if found null if not.
	 * @throws SegueDatabaseException
	 *             - if there is an error when accessing the database or if
	 *             duplicate assignments exist in the database..
	 */
	public List<AssignmentDTO> getAssignmentsByGameboardAndGroup(final String gameboardId,
			final String groupId) throws SegueDatabaseException {
		Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatchForAssignmentSearch = Maps.newHashMap();

		fieldsToMatchForAssignmentSearch.put(immutableEntry(Constants.BooleanOperator.AND, GAMEBOARD_ID_FKEY),
				Arrays.asList(gameboardId));
		fieldsToMatchForAssignmentSearch.put(immutableEntry(Constants.BooleanOperator.AND, DB_GROUP_FKEY),
				Arrays.asList(groupId));

		List<AssignmentDTO> assignments = findByMappedConditions(fieldsToMatchForAssignmentSearch);
		
		return assignments;
	}	
	
	/**
	 * getAssignmentsByOwner.
	 * 
	 * @param ownerId - the user id who might have assigned the gameboard.
	 * @return list of assignments
	 * @throws SegueDatabaseException
	 *             - if there is an error when accessing the database.
	 */
	public List<AssignmentDTO> getAssignmentsByOwner(final String ownerId) throws SegueDatabaseException {
		Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatchForAssignmentSearch = Maps.newHashMap();

		fieldsToMatchForAssignmentSearch.put(immutableEntry(Constants.BooleanOperator.AND, DB_OWNER_ID),
				Arrays.asList(ownerId));

		return findByMappedConditions(fieldsToMatchForAssignmentSearch);
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
	 * deleteAssignment.
	 * 
	 * @param id
	 *            - assignment id to delete.
	 * @throws SegueDatabaseException
	 *             - if we are unable to perform the delete operation.
	 */
	public void deleteAssignment(final String id) throws SegueDatabaseException {
		this.assignmentsDataManager.delete(id);
	}
	
	/**
	 * findByMapConditions.
	 * @param fieldsToMatchForAssignmentSearch - to specify match conditions.
	 * @return List of AssignmentDTOs or empty list
	 * @throws SegueDatabaseException
	 *             - if there is an error when accessing the database.
	 */
	private List<AssignmentDTO> findByMappedConditions(
			final Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatchForAssignmentSearch)
		throws SegueDatabaseException {
		List<AssignmentDO> assignmentGroupDO = this.assignmentsDataManager
				.find(fieldsToMatchForAssignmentSearch);

		List<AssignmentDO> resultToReturn = Lists.newArrayList();
		for (AssignmentDO objectToConvert : assignmentGroupDO) {
			resultToReturn.add(objectToConvert);
		}

		return this.convertToAssignmentDTOs(resultToReturn);
	}
}
