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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.GroupManagerLookupMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.List;
import java.util.Map;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * GroupsFacade. This is responsible for exposing group management functionality.
 * 
 * @author Stephen Cummins
 */
@Path("/groups")
@Api(value = "/groups")
public class GroupsFacade extends AbstractSegueFacade {
    private final UserAccountManager userManager;

    private static final Logger log = LoggerFactory.getLogger(GroupsFacade.class);
    private final GroupManager groupManager;
    private final UserAssociationManager associationManager;
    private final UserBadgeManager userBadgeManager;
    private IMisuseMonitor misuseMonitor;

    /**
     * Create an instance of the authentication Facade.
     *
     * @param properties          - properties loader for the application
     * @param userManager         - user manager for the application
     * @param logManager          - so we can log interesting events.
     * @param groupManager        - so that we can manage groups.
     * @param associationsManager - so we can decide what information is allowed to be exposed.
     */
    @Inject
    public GroupsFacade(final PropertiesLoader properties, final UserAccountManager userManager,
                        final ILogManager logManager, final GroupManager groupManager,
                        final UserAssociationManager associationsManager,
                        final UserBadgeManager userBadgeManager,
                        final IMisuseMonitor misuseMonitor) {
        super(properties, logManager);
        this.userManager = userManager;
        this.groupManager = groupManager;
        this.associationManager = associationsManager;
        this.userBadgeManager = userBadgeManager;
        this.misuseMonitor = misuseMonitor;
    }

    /**
     * Get all groups owned by or available to the current user.
     *
     * @param request            - so we can identify the current user.
     * @param cacheRequest       - so that we can control caching of this endpoint
     * @param archivedGroupsOnly - include archived groups in response - default is false - i.e. show only unarchived
     * @return List of groups for the current user.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all groups owned or managed by the current user.")
    public Response getGroupsForCurrentUser(@Context final HttpServletRequest request,
                                            @Context final Request cacheRequest, @QueryParam("archived_groups_only") final boolean archivedGroupsOnly) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
            List<UserGroupDTO> groups = groupManager.getAllGroupsOwnedAndManagedByUser(user, archivedGroupsOnly);

            // Calculate the ETag based user id and groups they own
            EntityTag etag = new EntityTag(user.getId().hashCode() + groups.toString().hashCode() + "");
            Response cachedResponse = generateCachedResponse(cacheRequest, etag,
                    Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            return Response.ok(groups).tag(etag)
                    .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to get user group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        }
    }

    /**
     * Get all groups where the user is a member.
     *
     * @param request            - so we can identify the current user.
     * @param cacheRequest       - so that we can control caching of this endpoint
     * @param archivedGroupsOnly - include archived groups in response - default is false - i.e. show only unarchived
     * @throws NoUserException   - when the user cannot be found.
     * @return List of groups for the current user.
     */
    @GET
    @Path("/membership")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupMembership(@Context final HttpServletRequest request,
                                       @Context final Request cacheRequest,
                                       @QueryParam("archived_groups_only") final boolean archivedGroupsOnly) throws NoUserException {
        try {
            RegisteredUserDTO requestingUser = userManager.getCurrentRegisteredUser(request);
            return getGroupMembershipSpecificUser(request, cacheRequest, requestingUser.getId(), archivedGroupsOnly);
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Get all groups where the user is a member.
     *
     * @param request            - so we can identify the current user.
     * @param cacheRequest       - so that we can control caching of this endpoint
     * @param userId             - the user we want the groups of.
     * @param archivedGroupsOnly - include archived groups in response - default is false - i.e. show only unarchived
     * @throws NoUserException   - when the user cannot be found.
     * @return List of groups for the current user.
     */
    @GET
    @Path("/membership/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupMembershipSpecificUser(@Context final HttpServletRequest request,
                                                   @Context final Request cacheRequest,
                                                   @PathParam("userId") Long userId,
                                                   @QueryParam("archived_groups_only") final boolean archivedGroupsOnly) throws NoUserException {
        try {
            RegisteredUserDTO requestingUser = userManager.getCurrentRegisteredUser(request);

            RegisteredUserDTO user;
            if (userId.equals(requestingUser.getId())) {
                user = requestingUser;
            } else if (isUserStaff(userManager, requestingUser)) {
                user = userManager.getUserDTOById(userId);
            } else {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access the groups of another user.")
                        .toResponse();
            }

            List<UserGroupDTO> groups = groupManager.getGroupMembershipList(user);

            List<Map<String, Object>> results = Lists.newArrayList();
            for(UserGroupDTO group : groups) {
                ImmutableMap<String, Object> map = ImmutableMap.of("group", group, "membershipStatus", this.groupManager.getGroupMembershipStatus(user.getId(), group.getId()).name());

                // check if the group doesn't have a last updated date if not it means that we shouldn't show students the group name as teachers may not have realised the names are public..
                if (group.getLastUpdated() == null) {
                    group.setGroupName(null);
                }

                results.add(map);
            }

            return Response.ok(results).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to get user group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        }
    }

    /**
     * Change user's group membership status
     *
     * @param request      - for authentication
     * @param cacheRequest - so that we can control caching of this endpoint
     * @param groupId      - group of interest.
     * @param newStatus - containing the new status for the user's membership
     * @return 200 containing new list of users in the group or error response
     */
    @POST
    @Path("/membership/{group_id}/{new_status}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response changeGroupMembershipStatus(@Context final HttpServletRequest request, @Context final Request cacheRequest,
                                                @PathParam("group_id") final Long groupId, @PathParam("new_status")  final String newStatus) {
        if (null == groupId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group id must be specified.").toResponse();
        }

        try {
            // ensure there is a user currently logged in.
            RegisteredUserDTO currentRegisteredUser = userManager.getCurrentRegisteredUser(request);
            UserGroupDTO groupBasedOnId = groupManager.getGroupById(groupId);

            GroupMembershipStatus newStatusEnum = GroupMembershipStatus.valueOf(newStatus);
            if (newStatusEnum.equals(GroupMembershipStatus.DELETED)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You are not allowed to completely delete yourself from a group").toResponse();
            }

            this.groupManager.setMembershipStatus(groupBasedOnId, currentRegisteredUser, newStatusEnum);

            this.getLogManager().logEvent(currentRegisteredUser, request, SegueLogType.CHANGE_GROUP_MEMBERSHIP_STATUS,
                    ImmutableMap.of(Constants.GROUP_FK, groupBasedOnId.getId(),
                            USER_ID_FKEY_FIELDNAME, currentRegisteredUser.getId(),
                            "newStatus", newStatusEnum.name()));

            return Response.ok().build();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to add user to a group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (IllegalArgumentException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "The new status provided is not valid.").toResponse();
        }
    }

    /**
     * Get all groups owned by the user id provided.
     * <p>
     * ADMIN Only end point.
     *
     * @param request - so we can identify the current user.
     * @param userId  - so we can lookup the group ownership information.
     * @return List of user associations.
     */
    @GET
    @Path("/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all groups owned or managed by another user.")
    public Response getGroupsForGivenUser(@Context final HttpServletRequest request,
                                          @PathParam("user_id") final Long userId) {
        try {
            if (null == userId) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        "You must provide a valid user id to access this endpoint.").toResponse();
            }

            if (!isUserAnAdmin(userManager, request)) {
                SegueErrorResponse.getIncorrectRoleResponse();
            }

            RegisteredUserDTO userOfInterest = userManager.getUserDTOById(userId);

            List<UserGroupDTO> groups = groupManager.getGroupsByOwner(userOfInterest);
            return Response.ok(groups).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
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
     * @param request  - so we can find out who the current user is
     * @param groupDTO - Containing required information to create a group
     * @return a Response containing the group
     */
    @POST
    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create a new group.",
                  notes = "The group information must be a GroupDTO object, although only 'groupName' is used.")
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

            this.getLogManager().logEvent(user, request, SegueLogType.CREATE_USER_GROUP,
                    ImmutableMap.of(Constants.GROUP_FK, group.getId()));

            this.userBadgeManager.updateBadge(user, UserBadgeManager.Badge.TEACHER_GROUPS_CREATED,
                    group.getId().toString());

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
     * @param request  - so we can find out who the current user is
     * @param groupDTO - Containing required information to create a group
     * @param groupId  - Id of the group to edit.
     * @return a Response containing the group
     */
    @POST
    @Path("/{group_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update an existing group.",
                  notes = "The 'group_id' parameter must match the group ID in the request body.")
    public Response editGroup(@Context final HttpServletRequest request, final UserGroupDTO groupDTO,
                              @PathParam("group_id") final Long groupId) {
        if (null == groupDTO.getGroupName() || groupDTO.getGroupName().isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group name must be specified.").toResponse();
        }

        if (null == groupId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group ID must be specified.").toResponse();
        }

        if (!groupId.equals(groupDTO.getId())) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group ID in request url must match data object.")
                    .toResponse();
        }

        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            // get existing group
            UserGroupDTO existingGroup = groupManager.getGroupById(groupId);

            if (null == existingGroup) {
                return new SegueErrorResponse(Status.NOT_FOUND, "Group specified does not exist.").toResponse();
            }

            if (!existingGroup.getOwnerId().equals(user.getId())) {
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
     * @param request      - for authentication
     * @param cacheRequest - so that we can control caching of this endpoint
     * @param groupId      - to look up info for a group.
     * @return List of registered users.
     */
    @GET
    @Path("{group_id}/membership/")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all members of a group.")
    public Response getUsersInGroup(@Context final HttpServletRequest request, @Context final Request cacheRequest,
                                    @PathParam("group_id") final Long groupId) {
        if (null == groupId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group ID must be specified.").toResponse();
        }

        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            UserGroupDTO group = groupManager.getGroupById(groupId);

            if (!GroupManager.isOwnerOrAdditionalManager(group, user.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "Only owners, admins or managers can view membership of groups").toResponse();
            }

            List<UserSummaryDTO> summarisedMemberInfo = userManager.convertToUserSummaryObjectList(groupManager
                    .getUsersInGroup(group));

            associationManager.enforceAuthorisationPrivacy(user, summarisedMemberInfo);

            groupManager.convertToUserSummaryGroupMembership(group, summarisedMemberInfo);

            // Calculate the ETag based on the users, their revoked status and their membership status
            // For caching purposes (to ensure changes to group status are noticed, this must be the last thing we do:
            EntityTag etag = new EntityTag(group.getId().hashCode() + summarisedMemberInfo.toString().hashCode()
                    + summarisedMemberInfo.size() + "");
            Response cachedResponse = generateCachedResponse(cacheRequest, etag, Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            return Response.ok(summarisedMemberInfo).tag(etag)
                    .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Add User to Group.
     *
     * @param request - for authentication
     * @param groupId - group of interest.
     * @param userId  - user to add.
     * @return 200 or error response
     */
    @POST
    @Path("{group_id}/membership/{user_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add a user to a group without approval.",
                  notes = "This endpoint does not grant group owners access to the user's data.")
    public Response addUserToGroup(@Context final HttpServletRequest request,
                                   @PathParam("group_id") final Long groupId, @PathParam("user_id") final Long userId) {
        if (null == groupId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group ID must be specified.").toResponse();
        }

        try {
            if (!isUserAnAdmin(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "Only admins can directly add a user to a group without a token").toResponse();
            }

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
     * @param request      - for authentication
     * @param cacheRequest - so that we can control caching of this endpoint
     * @param groupId      - group of interest.
     * @param userId       - user to remove.
     * @return 200 containing new list of users in the group or error response
     */
    @DELETE
    @Path("{group_id}/membership/{user_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Remove a user from a group.")
    public Response removeUserFromGroup(@Context final HttpServletRequest request, @Context final Request cacheRequest,
                                        @PathParam("group_id") final Long groupId, @PathParam("user_id") final Long userId) {
        if (null == groupId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group ID must be specified.").toResponse();
        }

        try {
            // ensure there is a user currently logged in.
            RegisteredUserDTO currentRegisteredUser = userManager.getCurrentRegisteredUser(request);

            UserGroupDTO groupBasedOnId = groupManager.getGroupById(groupId);

            if (!GroupManager.isOwnerOrAdditionalManager(groupBasedOnId, currentRegisteredUser.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are not the owner or manager of this group").toResponse();
            }

            RegisteredUserDTO userToRemove = userManager.getUserDTOById(userId);

            groupManager.removeUserFromGroup(groupBasedOnId, userToRemove);

            this.getLogManager().logEvent(currentRegisteredUser, request, SegueLogType.REMOVE_USER_FROM_GROUP,
                    ImmutableMap.of(Constants.GROUP_FK, groupBasedOnId.getId(),
                            USER_ID_FKEY_FIELDNAME, userToRemove.getId()));

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
     * @param request - for authentication
     * @param groupId - group to delete.
     * @return NO Content response or error response
     */
    @DELETE
    @Path("/{group_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a group.")
    public Response deleteGroup(@Context final HttpServletRequest request,
                                @PathParam("group_id") final Long groupId) {
        if (null == groupId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Group ID must be specified.").toResponse();
        }

        try {
            // ensure there is a user currently logged in.
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);

            UserGroupDTO groupBasedOnId = groupManager.getGroupById(groupId);

            if (!currentUser.getId().equals(groupBasedOnId.getOwnerId())) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be the owner of the group in order to delete it.")
                        .toResponse();
            }

            groupManager.deleteGroup(groupBasedOnId);

            this.getLogManager().logEvent(currentUser, request, SegueLogType.DELETE_USER_GROUP,
                    ImmutableMap.of(Constants.GROUP_FK, groupBasedOnId.getId()));

        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to add user to a group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        return Response.noContent().build();
    }

    /**
     * Add an additional manager to a group.
     *
     * @param request - for authentication
     * @param groupId - group to delete the manager from
     * @param responseMap - a map containing the email of the user to add
     * @return No Content response or error response
     */
    @POST
    @Path("{group_id}/manager")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add an additional manager to a group.",
                  notes = "The email of the user to add must be provided as 'email' in the request body and they must have a teacher account.")
    public Response addAdditionalManagerToGroup(@Context final HttpServletRequest request,
                                                @PathParam("group_id") final Long groupId,
                                                final Map<String, String> responseMap) {
        if (null == groupId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "The group must be specified.").toResponse();
        }

        if (null == responseMap || !responseMap.containsKey("email") || responseMap.get("email").isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "The email of the user to add must be specified.").toResponse();
        }

        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            // Check for abuse of this endpoint:
            misuseMonitor.notifyEvent(user.getId().toString(), GroupManagerLookupMisuseHandler.class.getSimpleName());

            RegisteredUserDTO userToAdd = this.userManager.getUserDTOByEmail(responseMap.get("email"));
            UserGroupDTO group = groupManager.getGroupById(groupId);

            if (null == group || !(group.getOwnerId().equals(user.getId()) || isUserAnAdmin(userManager, user))) {
                return new SegueErrorResponse(Status.FORBIDDEN, "Only group owners can modify additional group managers!").toResponse();
            }

            if (null == userToAdd || Role.STUDENT.equals(userToAdd.getRole())) {
                // deliberately be vague about whether the account exists or they don't have a teacher account to avoid account scanning.
                return new SegueErrorResponse(Status.BAD_REQUEST, "There was a problem adding the user specified. Please make sure their email address is correct and they have a teacher account.").toResponse();
            }

            if (group.getOwnerId().equals(userToAdd.getId())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The group owner cannot be added as an additional manager!").toResponse();
            }

            if (GroupManager.isInAdditionalManagerList(group, userToAdd.getId())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "This user is already an additional manager").toResponse();
            }

            this.getLogManager().logEvent(user, request, SegueLogType.ADD_ADDITIONAL_GROUP_MANAGER,
                    ImmutableMap.of(GROUP_FK, group.getId(), USER_ID_FKEY_FIELDNAME, userToAdd.getId()));

            return Response.ok(this.groupManager.addUserToManagerList(group, userToAdd)).build();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to add manager to a group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "There was a problem adding the user specified. Please make sure their email address is correct and they have a teacher account.").toResponse();
        } catch (SegueResourceMisuseException e) {
            String message = "You have exceeded the number of requests allowed for this endpoint. "
                    + "Please try again later.";
            return SegueErrorResponse.getRateThrottledResponse(message);
        }

    }

    /**
     * Delete an additional manager from a group.
     *
     * @param request - for authentication
     * @param groupId - group to delete the manager from
     * @param userId - the additional manager to delete
     * @return No Content response or error response
     */
    @DELETE
    @Path("{group_id}/manager/{user_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Remove an additional manager from a group.")
    public Response removeAdditionalManagerFromGroup(@Context final HttpServletRequest request,
                                                @PathParam("group_id") final Long groupId,
                                                   @PathParam("user_id") final Long userId) {
        if (null == groupId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "The group ID must be specified.").toResponse();
        }

        if (null == userId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "The ID of the user to remove must be specified.").toResponse();
        }

        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            UserGroupDTO group = groupManager.getGroupById(groupId);

            if (!group.getOwnerId().equals(user.getId()) && !isUserAnAdmin(userManager, user)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "Only group owners can modify additional group managers!").toResponse();
            }

            RegisteredUserDTO userToRemove = this.userManager.getUserDTOById(userId);

            this.getLogManager().logEvent(user, request, SegueLogType.DELETE_ADDITIONAL_GROUP_MANAGER,
                    ImmutableMap.of(GROUP_FK, group.getId(), USER_ID_FKEY_FIELDNAME, userId));

            return Response.ok(this.groupManager.removeUserFromManagerList(group, userToRemove)).build();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to remove manager from a group. ", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "User specified does not exist.").toResponse();
        }
    }
}
