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
package uk.ac.cam.cl.dtg.segue.api;

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

import org.elasticsearch.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.InvalidUserAssociationTokenException;
import uk.ac.cam.cl.dtg.segue.dao.associations.UserGroupNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.associations.UserAssociationException;
import uk.ac.cam.cl.dtg.segue.dos.AssociationToken;
import uk.ac.cam.cl.dtg.segue.dos.UserAssociation;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * AuthorizationFacade.
 * 
 * @author Stephen Cummins
 */
@Path("/authorisations")
public class AuthorisationFacade extends AbstractSegueFacade {
	private final UserManager userManager;
	private final UserAssociationManager associationManager;

	private static final Logger log = LoggerFactory.getLogger(AuthorisationFacade.class);

	/**
	 * Create an instance of the authentication Facade.
	 * 
	 * @param properties
	 *            - properties loader for the application
	 * @param userManager
	 *            - user manager for the application
	 * @param logManager
	 *            - so we can log interesting events.
	 * @param associationManager
	 *            - so that we can create associations.
	 */
	@Inject
	public AuthorisationFacade(final PropertiesLoader properties, final UserManager userManager,
			final ILogManager logManager, final UserAssociationManager associationManager) {
		super(properties, logManager);
		this.userManager = userManager;
		this.associationManager = associationManager;
	}

	/**
	 * Get all users who can see my data.
	 * 
	 * @param request
	 *            - so we can identify the current user.
	 * @return List of user associations.
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsersWithAccess(@Context final HttpServletRequest request) {
		try {
			RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

			List<String> userIdsWithAccess = Lists.newArrayList();
			for (UserAssociation a : associationManager.getAssociations(user)) {
				userIdsWithAccess.add(a.getUserIdReceivingPermission());
			}

			return Response.ok(
					userManager.convertToUserSummaryObjectList(userManager.findUsers(userIdsWithAccess)))
					.build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
		}
	}
	
	/**
	 * Get all users whose data I can see.
	 * 
	 * @param request
	 *            - so we can identify the current user.
	 * @return List of user associations.
	 */
	@GET
	@Path("/other_users")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCurrentAccessRights(@Context final HttpServletRequest request) {
		try {
			RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

			List<String> userIdsGrantingAccess = Lists.newArrayList();
			for (UserAssociation a : associationManager.getAssociationsForOthers(user)) {
				userIdsGrantingAccess.add(a.getUserIdGrantingPermission());
			}

			return Response.ok(
					userManager.convertToUserSummaryObjectList(userManager.findUsers(userIdsGrantingAccess)))
					.build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
		}
	}

	/**
	 * Function to allow users to create an AssociationToken.
	 * 
	 * This token can be used by another user to grant view permissions to their
	 * user data.
	 * 
	 * @param request
	 *            - so we can find out who the current user is
	 * @param groupId
	 *            - GroupId must exist.
	 * @return a Response containing an association token or a
	 *         SegueErrorResponse.
	 */
	@GET
	@Path("/associations/token/{groupId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAssociationToken(@Context final HttpServletRequest request,
			@PathParam("groupId") final String groupId) {
		if (null == groupId || groupId.isEmpty()) {
			return new SegueErrorResponse(Status.BAD_REQUEST, "Group id must be specified.").toResponse();
		}
		
		try {
			RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);			
			AssociationToken token = associationManager.getAssociationToken(user, groupId);
			
			return Response.ok(token).build();
		} catch (SegueDatabaseException e) {
			log.error("Database error while trying to get association token. ", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (UserGroupNotFoundException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST, "Error connecting to group", e).toResponse();
		}
	}

	/**
	 * Function to allow users to use an AssociationToken to create a new
	 * association.
	 * 
	 * @param request
	 *            - so we can find out who the current user is
	 * @param token
	 *            - so we can create a group for associated users to fall into.
	 * @return a Response containing an association token or a
	 *         SegueErrorResponse.
	 */
	@POST
	@Path("/associations/use_token/{token}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response useAssociationToken(@Context final HttpServletRequest request,
			@PathParam("token") final String token) {
		if (null == token || token.isEmpty()) {
			return new SegueErrorResponse(Status.BAD_REQUEST, "Token value must be specified.").toResponse();
		}

		try {
			RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

			associationManager.createAssociationWithToken(token, user);

			return Response.ok(new ImmutableMap.Builder<String, String>().put("result", "success").build()).build();
		} catch (SegueDatabaseException e) {
			log.error("Database error while trying to get association token. ", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (UserAssociationException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to create association", e).toResponse();
		} catch (InvalidUserAssociationTokenException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"The token provided is Invalid or no longer exists.").toResponse();
		}
	}

	/**
	 * Revoke a user association.
	 * 
	 * @param request
	 *            - so we can find out the current user
	 * @param userIdToRevoke
	 *            - so we can delete any associations that the user might have.
	 * @return response with no content or a segueErrorResponse
	 */
	@DELETE
	@Path("/associations/{userId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response revokeAssociation(@Context final HttpServletRequest request,
			@PathParam("userId") final String userIdToRevoke) {
		if (null == userIdToRevoke || userIdToRevoke.isEmpty()) {
			return new SegueErrorResponse(Status.BAD_REQUEST, "revokeUserId value must be specified.")
					.toResponse();
		}

		try {
			RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
			RegisteredUserDTO userToRevoke = userManager.getUserDTOById(userIdToRevoke);
			associationManager.revokeAssociation(user, userToRevoke);

			return Response.status(Status.NO_CONTENT).build();
		} catch (SegueDatabaseException e) {
			log.error("Database error while trying to get association token. ", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (NoUserException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to locate user to revoke").toResponse();
		}
	}
}
