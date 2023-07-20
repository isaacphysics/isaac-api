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
package uk.ac.cam.cl.dtg.segue.api;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.TokenOwnerLookupMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.InvalidUserAssociationTokenException;
import uk.ac.cam.cl.dtg.segue.dao.associations.UserGroupNotFoundException;
import uk.ac.cam.cl.dtg.isaac.dos.AssociationToken;
import uk.ac.cam.cl.dtg.isaac.dos.UserAssociation;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * AuthorizationFacade.
 * 
 * @author Stephen Cummins
 */
@Path("/authorisations")
@Tag(name = "/authorisations")
public class AuthorisationFacade extends AbstractSegueFacade {
    private final UserAccountManager userManager;
    private final UserAssociationManager associationManager;
    private final GroupManager groupManager;

    private static final Logger log = LoggerFactory.getLogger(AuthorisationFacade.class);
    private IMisuseMonitor misuseMonitor;

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
     * @param misuseMonitor
     *            - so that we can prevent overuse of protected resources.
     */
    @Inject
    public AuthorisationFacade(final AbstractConfigLoader properties, final UserAccountManager userManager,
                               final ILogManager logManager, final UserAssociationManager associationManager, final GroupManager groupManager,
                               final IMisuseMonitor misuseMonitor) {
        super(properties, logManager);
        this.userManager = userManager;
        this.associationManager = associationManager;
        this.groupManager = groupManager;
        this.misuseMonitor = misuseMonitor;
    }

    /**
     * Get all users who can see the current user's data.
     *
     * @param request
     *            - so we can identify the current user.
     * @throws NoUserException
     *            - when the target user cannot be found.
     * @return List of user associations.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List all users granted access to the current user's data.")
    public Response getUsersWithAccess(@Context final HttpServletRequest request) throws NoUserException {
        try {
            RegisteredUserDTO requestingUser = userManager.getCurrentRegisteredUser(request);
            return getUsersWithAccessSpecificUser(request, requestingUser.getId());
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Get all users who can see the specified user's data.
     * 
     * @param request
     *            - so we can identify the current user.
     * @param userId
     *            - the user id we want the associations of.
     * @throws NoUserException
     *            - when the target user cannot be found.
     * @return List of user associations.
     */
    @GET
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List all users granted access to the specified user's data.")
    public Response getUsersWithAccessSpecificUser(@Context final HttpServletRequest request, @PathParam("userId") Long userId) throws NoUserException {
        try {
            RegisteredUserDTO requestingUser = userManager.getCurrentRegisteredUser(request);

            RegisteredUserDTO user;
            if (userId.equals(requestingUser.getId())) {
                user = requestingUser;
            } else if (isUserStaff(userManager, requestingUser)) {
                user = userManager.getUserDTOById(userId);
            } else {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access the associations of another user.")
                        .toResponse();
            }

            List<Long> userIdsWithAccess = Lists.newArrayList();
            for (UserAssociation a : associationManager.getAssociations(user)) {
                userIdsWithAccess.add(a.getUserIdReceivingPermission());
            }

            return Response.ok(userManager.convertToDetailedUserSummaryObjectList(userManager.findUsers(userIdsWithAccess), UserSummaryWithEmailAddressDTO.class))
                    .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        }
    }

    /**
     * Revoke a user association.
     *
     * This endpoint enables a users to revoke access to their data for a specific user they had previously approved.
     *
     * @param request
     *            - so we can find out the current user
     * @param userIdToRevoke
     *            - so we can delete any associations that the user might have previously granted.
     * @return response with no content or a segueErrorResponse
     */
    @DELETE
    @Path("/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Revoke a specific user's access to the current user's data.")
    public Response revokeOwnerAssociation(@Context final HttpServletRequest request,
            @PathParam("userId") final Long userIdToRevoke) {
        if (null == userIdToRevoke) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "revokeUserId value must be specified.").toResponse();
        }

        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
            RegisteredUserDTO userToRevoke = userManager.getUserDTOById(userIdToRevoke);
            associationManager.revokeAssociation(user, userToRevoke);

            this.getLogManager().logEvent(user, request, SegueServerLogType.REVOKE_USER_ASSOCIATION,
                    ImmutableMap.of(USER_ID_LIST_FKEY_FIELDNAME, Collections.singletonList(userIdToRevoke)));

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

    /**
     * Revoke all user associations
     *
     * This endpoint enables a users to revoke access to their data for all users they had previously approved.
     *
     * @param request
     *            - so we can find out the current user
     * @return response with no content or a segueErrorResponse
     */
    @DELETE
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Revoke all users granted access to the current user's data.")
    public Response revokeAllOwnerAssociations(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            List<Long> userIdsWithAccess = associationManager.getAssociations(user).stream()
                    .map(UserAssociation::getUserIdReceivingPermission).collect(Collectors.toList());

            associationManager.revokeAllAssociationsByOwnerUser(user);

            this.getLogManager().logEvent(user, request, SegueServerLogType.REVOKE_USER_ASSOCIATION,
                    ImmutableMap.of(USER_ID_LIST_FKEY_FIELDNAME, userIdsWithAccess));

            return Response.status(Status.NO_CONTENT).build();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to access association token. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Release a user association.
     *
     * This endpoint is used when a user who had previously been granted access to another's data no longer needs it
     * and wants to end the sharing relationship from their side.
     *
     * @param request
     *            - so we can find out the current user
     * @param associationOwner
     *            - the original data owner who authorised the data sharing link.
     * @return response with no content or a segueErrorResponse
     */
    @DELETE
    @Path("release/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Release the current user's access to another user's data.")
    public Response releaseAssociation(@Context final HttpServletRequest request,
                                       @PathParam("userId") final Long associationOwner) {
        if (null == associationOwner) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "revokeUserId value must be specified.").toResponse();
        }

        try {
            RegisteredUserDTO ownerUser = userManager.getUserDTOById(associationOwner);
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            associationManager.revokeAssociation(ownerUser, user);

            this.getLogManager().logEvent(user, request, SegueServerLogType.RELEASE_USER_ASSOCIATION,
                    ImmutableMap.of(USER_ID_LIST_FKEY_FIELDNAME, Collections.singletonList(associationOwner)));

            return Response.status(Status.NO_CONTENT).build();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to access association token. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to locate user to revoke").toResponse();
        }
    }

    /**
     * Release all user associations.
     *
     * This endpoint is used when a user who had previously been granted access to other users's data no longer needs
     * the access and wants to end all the sharing relationships from their side.
     *
     * @param request
     *            - so we can find out the current user
     * @return response with no content or a segueErrorResponse
     */
    @DELETE
    @Path("release")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Release the current user's access to all other users's data.")
    public Response releaseAllAssociations(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            List<Long> userIdsWithAccess = associationManager.getAssociationsForOthers(user).stream()
                    .map(UserAssociation::getUserIdGrantingPermission).collect(Collectors.toList());

            associationManager.revokeAllAssociationsByRecipientUser(user);

            this.getLogManager().logEvent(user, request, SegueServerLogType.RELEASE_USER_ASSOCIATION,
                    ImmutableMap.of(USER_ID_LIST_FKEY_FIELDNAME, userIdsWithAccess));

            return Response.status(Status.NO_CONTENT).build();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to access association token. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Get all users whose data the current user can see.
     *
     * @param request
     *            - so we can identify the current user.
     * @throws NoUserException
     *            - when the target user cannot be found.
     * @return List of user associations.
     */
    @GET
    @Path("/other_users")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List all users the current user has been granted access by.")
    public Response getCurrentAccessRights(@Context final HttpServletRequest request) throws NoUserException {
        try {
            RegisteredUserDTO requestingUser = userManager.getCurrentRegisteredUser(request);
            return getCurrentAccessRightsForSpecificUser(request, requestingUser.getId());
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Get all users whose data the specified user can see.
     * 
     * @param request
     *            - so we can identify the current user.
     * @param userId
     *            - the id of the user we want the access rights of.
     * @throws NoUserException
     *            - when the target user cannot be found.
     * @return List of user associations.
     */
    @GET
    @Path("/other_users/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List all users the specified user has been granted access by.")
    public Response getCurrentAccessRightsForSpecificUser(@Context final HttpServletRequest request, @PathParam("userId") Long userId) throws NoUserException{
        try {
            RegisteredUserDTO requestingUser = userManager.getCurrentRegisteredUser(request);

            RegisteredUserDTO user;
            if (userId.equals(requestingUser.getId())) {
                user = requestingUser;
            } else if (isUserStaff(userManager, requestingUser)) {
                user = userManager.getUserDTOById(userId);
            } else {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access the associations of another user.")
                        .toResponse();
            }

            List<Long> userIdsGrantingAccess = Lists.newArrayList();
            for (UserAssociation a : associationManager.getAssociationsForOthers(user)) {
                userIdsGrantingAccess.add(a.getUserIdGrantingPermission());
            }

            return Response
                    .ok(userManager.convertToUserSummaryObjectList(userManager.findUsers(userIdsGrantingAccess)))
                    .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        }
    }

    /**
     * Function to allow users to create or get an existing AssociationToken.
     * 
     * This token can be used by another user to grant view permissions to their user data.
     * 
     * @param request
     *            - so we can find out who the current user is
     * @param groupId
     *            - GroupId must exist.
     * @return a Response containing an association token or a SegueErrorResponse.
     */
    @GET
    @Path("/token/{groupId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get the group join token for the specified group.")
    public Response getAssociationToken(@Context final HttpServletRequest request,
            @PathParam("groupId") final Long groupId) {
        if (null == groupId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group id must be specified.").toResponse();
        }

        try {
            // verify the user requesting a token is allowed to do so for this group
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
            UserGroupDTO group = this.groupManager.getGroupById(groupId);

            if (!GroupManager.isOwnerOrAdditionalManager(group, user.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You do not have permission to create or request a group token for this group. " +
                        "Only owners or additional managers can.").toResponse();
            }

            AssociationToken token = associationManager.generateAssociationToken(user, groupId);

            return Response.ok(token).cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                    .build();
        } catch (ResourceNotFoundException e) {
            return new SegueErrorResponse(Status.NOT_FOUND, "Unable to locate the group requested.").toResponse();
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
     * Function to allow user to find out the account owner of a particular token. This allows them to know if they
     * really want to grant access or not.
     * 
     * @param request
     *            - so we can find out who the current user is
     * @param token
     *            - so we look up the owner.
     * @return a Response containing a user summary object or a SegueErrorResponse.
     */
    @GET
    @Path("/token/{token}/owner")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "List the users a group join token will grant access to.")
    public Response getTokenOwnerUserSummary(@Context final HttpServletRequest request,
            @PathParam("token") final String token) {
        if (null == token || token.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Token value must be specified.").toResponse();
        }

        RegisteredUserDTO currentRegisteredUser = null;
        try {
            // ensure the user is logged in
            currentRegisteredUser = userManager.getCurrentRegisteredUser(request);
            AssociationToken associationToken = this.associationManager.lookupTokenDetails(currentRegisteredUser, token);

            UserGroupDTO group = this.groupManager.getGroupById(associationToken.getGroupId());

            misuseMonitor.notifyEvent(currentRegisteredUser.getId().toString(),
                    TokenOwnerLookupMisuseHandler.class.getSimpleName());

            List<UserSummaryWithEmailAddressDTO> usersLinkedToToken = Lists.newArrayList();
            // add owner
            usersLinkedToToken.add(group.getOwnerSummary());
            // add additional managers
            usersLinkedToToken.addAll(group.getAdditionalManagers());

            return Response.ok(usersLinkedToToken)
                    .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (InvalidUserAssociationTokenException e) {
            log.info(String.format("User (%s) attempted to use token (%s) but it is invalid or no longer exists.",
                    currentRegisteredUser.getId(), token));

            return new SegueErrorResponse(Status.BAD_REQUEST, "The token provided is invalid or no longer exists.")
                    .toResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to get association token. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (SegueResourceMisuseException e) {
            String message = "You have exceeded the number of requests allowed for this endpoint. "
                    + "Please try again later.";
            return SegueErrorResponse.getRateThrottledResponse(message);  
        }
    }

    /**
     * Function to allow users to use an AssociationToken to create a new association.
     * 
     * @param request
     *            - so we can find out who the current user is
     * @param token
     *            - so we can create a group for associated users to fall into.
     * @return a Response containing an association token or a SegueErrorResponse.
     */
    @POST
    @Path("/use_token/{token}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Use a group join token to authorise users and join a group.",
                  description = "This should be used after listing the group owners and managers and asking the user's permission to share data.")
    public Response useToken(@Context final HttpServletRequest request, @PathParam("token") final String token) {
        if (null == token || token.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Token value must be specified.").toResponse();
        }

        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            AssociationToken associationToken = associationManager.createAssociationWithToken(token, user);

            UserGroupDTO group = groupManager.getGroupById(associationToken.getGroupId());
            List<Long> usersApproved = new ArrayList<>();
            usersApproved.add(group.getOwnerId());
            usersApproved.addAll(group.getAdditionalManagersUserIds());

            this.getLogManager().logEvent(user, request, SegueServerLogType.CREATE_USER_ASSOCIATION,
                    ImmutableMap.of(ASSOCIATION_TOKEN_FIELDNAME, associationToken.getToken(),
                                    GROUP_FK, associationToken.getGroupId(),
                                    USER_ID_LIST_FKEY_FIELDNAME, usersApproved));

            return Response.ok(new ImmutableMap.Builder<String, String>().put("result", "success").build()).build();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to get association token. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (InvalidUserAssociationTokenException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "The token provided is invalid or no longer exists.")
                    .toResponse();
        }
    }
}
