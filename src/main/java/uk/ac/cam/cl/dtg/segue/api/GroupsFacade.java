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

import static uk.ac.cam.cl.dtg.segue.api.Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.InvalidUserAssociationTokenException;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * GroupsFacade. This is responsible for exposing group management functionality.
 * 
 * @author Stephen Cummins
 */
@Path("/groups")
public class GroupsFacade extends AbstractSegueFacade {
    private final UserManager userManager;

    private static final Logger log = LoggerFactory.getLogger(GroupsFacade.class);
    private final GroupManager groupManager;
    private final UserAssociationManager associationManager;

    /**
     * Create an instance of the authentication Facade.
     * 
     * @param properties
     *            - properties loader for the application
     * @param userManager
     *            - user manager for the application
     * @param logManager
     *            - so we can log interesting events.
     * @param groupManager
     *            - so that we can manage groups.
     * @param associationsManager
     *            - so we can decide what information is allowed to be exposed.
     */
    @Inject
    public GroupsFacade(final PropertiesLoader properties, final UserManager userManager, final ILogManager logManager,
            final GroupManager groupManager, final UserAssociationManager associationsManager) {
        super(properties, logManager);
        this.userManager = userManager;
        this.groupManager = groupManager;
        this.associationManager = associationsManager;
    }

    /**
     * Get all groups owned by the current user.
     * 
     * @param request
     *            - so we can identify the current user.
     * @param cacheRequest
     *            - so that we can control caching of this endpoint
     * @return List of groups for the current user.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupsForCurrentUser(@Context final HttpServletRequest request,
            @Context final Request cacheRequest) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
            List<UserGroupDTO> groups = groupManager.getGroupsByOwner(user);

            // Calculate the ETag based user id and groups they own
            EntityTag etag = new EntityTag(user.getDbId().hashCode() + groups.toString().hashCode() + "");
            Response cachedResponse = generateCachedResponse(cacheRequest, etag,
                    Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            return Response.ok(groups).tag(etag)
                    .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Get all groups owned by the user id provided.
     * 
     * ADMIN Only end point.
     * 
     * @param request
     *            - so we can identify the current user.
     * @param userId
     *            - so we can lookup the group ownership information.
     * @return List of user associations.
     */
    @GET
    @Path("/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupsForGivenUser(@Context final HttpServletRequest request,
            @PathParam("user_id") final String userId) {
        try {
            if (null == userId || userId.isEmpty()) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        "You must provide a valid user id to access this endpoint.").toResponse();
            }

            if (!isUserStaff(userManager, request)) {
                SegueErrorResponse.getIncorrectRoleResponse();
            }

            RegisteredUserDTO userOfInterest = userManager.getUserDTOById(userId);

            List<UserGroupDTO> groups = groupManager.getGroupsByOwner(userOfInterest);
            return Response.ok(groups).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to create user group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.NOT_FOUND, "Unable to locate the user with the id provided: " + userId)
                    .toResponse();
        }
    }

    /**
     * Function to allow users to create a group. This function requires the group DTO to be provided.
     * 
     * @param request
     *            - so we can find out who the current user is
     * @param groupDTO
     *            - Containing required information to create a group
     * @return a Response containing the group
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createGroup(@Context final HttpServletRequest request, final UserGroup groupDTO) {
        if (null == groupDTO.getGroupName() || groupDTO.getGroupName().isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group name must be specified.").toResponse();
        }

        if (groupDTO.getId() != null) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "You should use the edit endpoint and specify the group id in the URL").toResponse();
        }

        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
            UserGroupDTO group = groupManager.createUserGroup(groupDTO.getGroupName(), user);

            this.getLogManager().logEvent(user, request, Constants.CREATE_USER_GROUP,
                    ImmutableMap.of(Constants.GROUP_FK, group.getId()));

            return Response.ok(group).build();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to create user group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Function to allow users to edit a group. This function requires the group DTO to be provided.
     * 
     * @param request
     *            - so we can find out who the current user is
     * @param groupDTO
     *            - Containing required information to create a group
     * @param groupId
     *            - Id of the group to edit.
     * @return a Response containing the group
     */
    @POST
    @Path("/{group_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response editGroup(@Context final HttpServletRequest request, final UserGroup groupDTO,
            @PathParam("group_id") final String groupId) {
        if (null == groupDTO.getGroupName() || groupDTO.getGroupName().isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group name must be specified.").toResponse();
        }

        if (null == groupId || groupId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group Id must be specified.").toResponse();
        }

        if (!groupId.equals(groupDTO.getId())) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group Id in request url must match data object.")
                    .toResponse();
        }

        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            // get existing group
            UserGroupDTO existingGroup = groupManager.getGroupById(groupId);

            if (null == existingGroup) {
                return new SegueErrorResponse(Status.NOT_FOUND, "Group specified does not exist.").toResponse();
            }

            if (!existingGroup.getOwnerId().equals(user.getDbId())) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "The group you have attempted to edit does not belong to you.").toResponse();
            }

            UserGroupDTO group = groupManager.editUserGroup(groupDTO);

            return Response.ok(group).build();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to create user group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * List all group membership.
     * 
     * @param request
     *            - for authentication
     * @param cacheRequest
     *            - so that we can control caching of this endpoint
     * @param groupId
     *            - to look up info for a group.
     * @return List of registered users.
     */
    @GET
    @Path("{group_id}/membership/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUsersInGroup(@Context final HttpServletRequest request, @Context final Request cacheRequest,
            @PathParam("group_id") final String groupId) {
        if (null == groupId || groupId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group name must be specified.").toResponse();
        }

        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            UserGroupDTO group = groupManager.getGroupById(groupId);

            if (!group.getOwnerId().equals(user.getDbId()) && !isUserAnAdmin(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are not the owner of this group").toResponse();
            }

            List<UserSummaryDTO> summarisedMemberInfo = userManager.convertToUserSummaryObjectList(groupManager
                    .getUsersInGroup(group));

            // Calculate the ETag based user id and groups they own
            EntityTag etag = new EntityTag(group.getId().hashCode() + summarisedMemberInfo.toString().hashCode()
                    + summarisedMemberInfo.size() + "");
            Response cachedResponse = generateCachedResponse(cacheRequest, etag,
                    Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            Collections.sort(summarisedMemberInfo, new Comparator<UserSummaryDTO>() {
                @Override
                public int compare(final UserSummaryDTO arg0, final UserSummaryDTO arg1) {
                    return arg0.getFamilyName().compareTo(arg1.getFamilyName());
                }
            });

            associationManager.enforceAuthorisationPrivacy(user, summarisedMemberInfo);

            return Response.ok(summarisedMemberInfo).tag(etag)
                    .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK)).build();
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Add User to Group.
     * 
     * @param request
     *            - for authentication
     * @param groupId
     *            - group of interest.
     * @param userId
     *            - user to add.
     * @return 200 or error response
     */
    @POST
    @Path("{group_id}/membership/{user_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUserToGroup(@Context final HttpServletRequest request,
            @PathParam("group_id") final String groupId, @PathParam("user_id") final String userId) {
        if (null == groupId || groupId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group name must be specified.").toResponse();
        }

        try {
            // ensure there is a user currently logged in.
            userManager.getCurrentRegisteredUser(request);

            UserGroupDTO groupBasedOnId = groupManager.getGroupById(groupId);

            RegisteredUserDTO userToAdd = userManager.getUserDTOById(userId);

            groupManager.addUserToGroup(groupBasedOnId, userToAdd);

            return Response.ok().build();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to add user to a group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "User specified does not exist.").toResponse();
        }
    }

    /**
     * Remove User from a Group.
     * 
     * @param request
     *            - for authentication
     * @param cacheRequest
     *            - so that we can control caching of this endpoint
     * @param groupId
     *            - group of interest.
     * @param userId
     *            - user to remove.
     * @return 200 containing new list of users in the group or error response
     */
    @DELETE
    @Path("{group_id}/membership/{user_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUserFromGroup(@Context final HttpServletRequest request, @Context final Request cacheRequest,
            @PathParam("group_id") final String groupId, @PathParam("user_id") final String userId) {
        if (null == groupId || groupId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group name must be specified.").toResponse();
        }

        try {
            // ensure there is a user currently logged in.
            RegisteredUserDTO currentRegisteredUser = userManager.getCurrentRegisteredUser(request);

            UserGroupDTO groupBasedOnId = groupManager.getGroupById(groupId);

            if (!currentRegisteredUser.getDbId().equals(groupBasedOnId.getOwnerId())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are not the owner of this group").toResponse();
            }

            RegisteredUserDTO userToRemove = userManager.getUserDTOById(userId);

            groupManager.removeUserFromGroup(groupBasedOnId, userToRemove);

            return this.getUsersInGroup(request, cacheRequest, groupId);
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to add user to a group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "User specified does not exist.").toResponse();
        }
    }

    /**
     * Delete group.
     * 
     * @param request
     *            - for authentication
     * @param groupId
     *            - group to delete.
     * @return NO Content response or error response
     */
    @DELETE
    @Path("/{group_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteGroup(@Context final HttpServletRequest request, 
            @PathParam("group_id") final String groupId) {
        if (null == groupId || groupId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group name must be specified.").toResponse();
        }

        try {
            // ensure there is a user currently logged in.
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);

            UserGroupDTO groupBasedOnId = groupManager.getGroupById(groupId);

            if (!currentUser.getDbId().equals(groupBasedOnId.getOwnerId())) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You are not the owner of this group, and therefore do not have permission to delete it.")
                        .toResponse();
            }

            groupManager.deleteGroup(groupBasedOnId);
            // clean up old association tokens.
            associationManager.deleteAssociationTokenByGroupId(groupBasedOnId.getId());
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to add user to a group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (InvalidUserAssociationTokenException e) {
            log.error(String.format("Attempted to clean up token database for group id: %s, "
                    + "but it failed as the token did not exist.", groupId), e);
        }

        return Response.noContent().build();
    }
}
