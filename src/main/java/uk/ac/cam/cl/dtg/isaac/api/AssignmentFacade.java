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

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
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
public class AssignmentFacade {
	private static final Logger log = LoggerFactory.getLogger(AssignmentFacade.class);

	private UserManager userManager;
	private PropertiesLoader propertiesLoader;
	private GameManager gameManager;

	/**
	 * Creates an instance of the isaac controller which provides the REST
	 * endpoints for the isaac api.
	 * 
	 */
	public AssignmentFacade() {

	}

	/**
	 * Creates an instance of the isaac controller which provides the REST
	 * endpoints for the isaac api.
	 * 
	 * @param userManager
	 *            - Instance of User Manager
	 * @param propertiesLoader
	 *            - Instance of properties Loader
	 * @param gameManager
	 *            - Instance of Game Manager
	 */
	@Inject
	public AssignmentFacade(final UserManager userManager, final PropertiesLoader propertiesLoader,
			final GameManager gameManager) {
		this.userManager = userManager;
		this.propertiesLoader = propertiesLoader;
		this.gameManager = gameManager;
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
	public Response getAssignments(@Context final HttpServletRequest request) {
		try {
			RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

			// TODO: Get list of currently assigned boards.

		} catch (NoUserLoggedInException e) {
			return new SegueErrorResponse(Status.UNAUTHORIZED, "You need to be logged to assign gameboards")
					.toResponse();
		}

		return Response.ok("This functionality is not yet available.").build();
	}

	/**
	 * Allows a user to assign a gameboard to another user.
	 * 
	 * @param request
	 *            - so that we can identify the current user.
	 * @param gameboardId
	 *            - board to assign
	 * @param assigneeUserId
	 *            - assignee
	 * @return the assignment object.
	 */
	@POST
	@Path("/assign/{gameboard_id}/{user_id}")
	public Response assignGameBoard(@Context final HttpServletRequest request,
			@PathParam("gameboard_id") final String gameboardId, @PathParam("user_id") final String assigneeUserId) {
		try {
			RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
			RegisteredUserDTO assignee = userManager.getUserDTOById(assigneeUserId);

			GameboardDTO gameboard = this.gameManager.getGameboard(gameboardId);

			// TODO: Assign board
			if (null == gameboard) {
				return new SegueErrorResponse(Status.BAD_REQUEST,
						"The gameboard id specified does not exist.").toResponse();
			}
		} catch (NoUserLoggedInException e) {
			return new SegueErrorResponse(Status.UNAUTHORIZED, "You need to be logged to assign gameboards")
					.toResponse();
		} catch (NoUserException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST, "The asignee user specified does not exist.")
					.toResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.")
					.toResponse();
		}

		return Response.ok("This functionality is not yet available.").build();
	}
}
