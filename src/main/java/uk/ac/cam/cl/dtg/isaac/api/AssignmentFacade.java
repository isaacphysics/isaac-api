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
package uk.ac.cam.cl.dtg.isaac.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateAssignmentException;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.UserGroupDO;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.inject.Inject;

/**
 * AssignmentFacade
 * 
 * This class provides endpoints to support assigning work to users.
 * 
 */
@Path("/assignments")
public class AssignmentFacade extends AbstractIsaacFacade {
	private static final Logger log = LoggerFactory.getLogger(AssignmentFacade.class);

	private final AssignmentManager assignmentManager;
	private final UserManager userManager;
	private final GroupManager groupManager;
	private final GameManager gameManager;

	/**
	 * Creates an instance of the AssignmentFacade controller which provides the
	 * REST endpoints for the isaac api.
	 * 
	 * @param assignmentManager
	 *            - Instance of assignment Manager
	 * @param userManager
	 *            - Instance of User Manager
	 * @param groupManager
	 *            - Instance of Group Manager
	 * @param propertiesLoader
	 *            - Instance of properties Loader
	 * @param gameManager
	 *            - Instance of Game Manager
	 * @param logManager
	 *            - Instance of log manager
	 */
	@Inject
	public AssignmentFacade(final AssignmentManager assignmentManager, final UserManager userManager,
			final GroupManager groupManager, final PropertiesLoader propertiesLoader,
			final GameManager gameManager, final ILogManager logManager) {
		super(propertiesLoader, logManager);
		this.userManager = userManager;
		this.gameManager = gameManager;
		this.groupManager = groupManager;
		this.assignmentManager = assignmentManager;
	}

	/**
	 * Endpoint that will return a list of currently assigned boards.
	 * 
	 * @param request
	 *            - so that we can identify the current user.
	 * @return List of assignments
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAssignments(@Context final HttpServletRequest request) {
		try {
			RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

			List<AssignmentDTO> assignments = this.assignmentManager.getAssignments(currentlyLoggedInUser);

			return Response.ok(assignments).build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Database error while trying to get assignments.", e).toResponse();
		}
	}

	/**
	 * Allows a user to get all assignments they have set.
	 * 
	 * @param request
	 *            - so that we can identify the current user.
	 * @return the assignment object.
	 */
	@GET
	@Path("/assign/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAssigned(@Context final HttpServletRequest request) {
		try {
			RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

			return Response.ok(this.assignmentManager.getAllAssignmentsSetByUser(currentlyLoggedInUser))
					.build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.")
					.toResponse();
		}
	}

	/**
	 * Allows a user to get all groups that have been assigned to a given board.
	 * 
	 * @param request
	 *            - so that we can identify the current user.
	 * @param gameboardId - the id of the game board of interest.
	 * @return the assignment object.
	 */
	@GET
	@Path("/assign/{gameboard_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAssignedGroups(@Context final HttpServletRequest request,
			@PathParam("gameboard_id") final String gameboardId) {
		try {
			RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
			
			return Response.ok(assignmentManager.findGroupsByGameboard(currentlyLoggedInUser, gameboardId)).build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (SegueDatabaseException e) {
			log.error("Database error while trying to assign work", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.")
					.toResponse();
		} 
	}
	
	/**
	 * Allows a user to assign a gameboard to group of users.
	 * 
	 * @param request
	 *            - so that we can identify the current user.
	 * @param gameboardId
	 *            - board to assign
	 * @param groupId
	 *            - assignee group
	 * @return the assignment object.
	 */
	@POST
	@Path("/assign/{gameboard_id}/{group_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response assignGameBoard(@Context final HttpServletRequest request,
			@PathParam("gameboard_id") final String gameboardId, @PathParam("group_id") final String groupId) {
		try {
			RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
			UserGroupDO assigneeGroup = groupManager.getGroupById(groupId);
			if (null == assigneeGroup) {
				return new SegueErrorResponse(Status.BAD_REQUEST, "The group id specified does not exist.")
						.toResponse();
			}

			GameboardDTO gameboard = this.gameManager.getGameboard(gameboardId);
			if (null == gameboard) {
				return new SegueErrorResponse(Status.BAD_REQUEST,
						"The gameboard id specified does not exist.").toResponse();
			}

			AssignmentDTO newAssignment = new AssignmentDTO();
			newAssignment.setGameboardId(gameboard.getId());
			newAssignment.setOwnerUserId(currentlyLoggedInUser.getDbId());
			newAssignment.setGroupId(groupId);

			// modifies assignment passed in to include an id.
			this.assignmentManager.createAssignment(newAssignment);

			return Response.ok(newAssignment).build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (DuplicateAssignmentException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage())
					.toResponse();
		} catch (SegueDatabaseException e) {
			log.error("Database error while trying to assign work", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.")
					.toResponse();
		}
	}

	/**
	 * Allows a user to delete an assignment.
	 * 
	 * @param request
	 *            - so that we can identify the current user.
	 * @param gameboardId
	 *            - board id belonging to the assignment
	 * @param groupId
	 *            - assignee group
	 * @return confirmation or an error.
	 */
	@DELETE
	@Path("/assign/{gameboard_id}/{group_id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteAssignment(@Context final HttpServletRequest request,
			@PathParam("gameboard_id") final String gameboardId, @PathParam("group_id") final String groupId) {

		try {
			RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
			UserGroupDO assigneeGroup = groupManager.getGroupById(groupId);
			if (null == assigneeGroup) {
				return new SegueErrorResponse(Status.BAD_REQUEST, "The group id specified does not exist.")
						.toResponse();
			}

			GameboardDTO gameboard = this.gameManager.getGameboard(gameboardId);
			if (null == gameboard) {
				return new SegueErrorResponse(Status.BAD_REQUEST,
						"The gameboard id specified does not exist.").toResponse();
			}

			AssignmentDTO assignmentToDelete = this.assignmentManager.findAssignmentByGameboardAndGroup(
					gameboard.getId(), groupId);

			if (null == assignmentToDelete) {
				return new SegueErrorResponse(Status.NOT_FOUND,
						"The assignment does not exist.").toResponse();
			} 
			if (!assignmentToDelete.getOwnerUserId().equals(currentlyLoggedInUser.getDbId())) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You are not the owner of this assignment. Unable to delete it.").toResponse();
			}
			
			this.assignmentManager.deleteAssignment(assignmentToDelete);
			
			return Response.noContent().build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.")
					.toResponse();
		}
	}
}
