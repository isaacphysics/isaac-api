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

import io.swagger.annotations.Api;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.elasticsearch.common.collect.ImmutableMap;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.api.Constants.GameboardItemState;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateAssignmentException;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * AssignmentFacade
 * 
 * This class provides endpoints to support assigning work to users.
 * 
 */
@Path("/assignments")
@Api(value = "/assignments")
public class AssignmentFacade extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(AssignmentFacade.class);

    private final AssignmentManager assignmentManager;
    private final UserManager userManager;
    private final GroupManager groupManager;
    private final GameManager gameManager;

    private final UserAssociationManager associationManager;

    /**
     * Creates an instance of the AssignmentFacade controller which provides the REST endpoints for the isaac api.
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
     * @param associationManager
     *            - So that we can determine what information is allowed to be seen by other users.
     */
    @Inject
    public AssignmentFacade(final AssignmentManager assignmentManager, final UserManager userManager,
            final GroupManager groupManager, final PropertiesLoader propertiesLoader, final GameManager gameManager,
            final ILogManager logManager, final UserAssociationManager associationManager) {
        super(propertiesLoader, logManager);
        this.userManager = userManager;
        this.gameManager = gameManager;
        this.groupManager = groupManager;
        this.assignmentManager = assignmentManager;
        this.associationManager = associationManager;
    }

    /**
     * Endpoint that will return a list of boards assigned to the current user.
     * 
     * @param request
     *            - so that we can identify the current user.
     * @param assignmentStatus
     *            - so we know what assignments to return.
     * @return List of assignments (maybe empty)
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getAssignments(@Context final HttpServletRequest request,
            @QueryParam("assignmentStatus") final GameboardState assignmentStatus) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            Collection<AssignmentDTO> assignments = this.assignmentManager.getAssignments(currentlyLoggedInUser);

            Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsByUser = this.userManager
                    .getQuestionAttemptsByUser(currentlyLoggedInUser);

            // we want to populate gameboard details for the assignment DTO.
            for (AssignmentDTO assignment : assignments) {
                assignment.setGameboard(this.gameManager.getGameboard(assignment.getGameboardId(),
                        currentlyLoggedInUser, questionAttemptsByUser));
            }

            // if they have filtered the list we should only send out the things they wanted.
            if (assignmentStatus != null) {
                List<AssignmentDTO> newList = Lists.newArrayList();
                // we want to populate gameboard details for the assignment DTO.
                for (AssignmentDTO assignment : assignments) {
                    if (assignmentStatus.equals(GameboardState.COMPLETED)
                            && assignment.getGameboard().getPercentageCompleted() == 100) {
                        newList.add(assignment);
                    } else if (!assignmentStatus.equals(GameboardState.COMPLETED)
                            && assignment.getGameboard().getPercentageCompleted() != 100) {
                        newList.add(assignment);
                    }
                }
                assignments = newList;
            }

            this.getLogManager().logEvent(currentlyLoggedInUser, request, VIEW_MY_ASSIGNMENTS, Maps.newHashMap());

            return Response.ok(assignments).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                    .build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to assignments set a given user", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error while trying to get assignments.", e).toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve the content requested. Please try again later.", e).toResponse();
        }
    }

    /**
     * Allows a user to get all assignments they have set.
     * 
     * @param request
     *            - so that we can identify the current user.
     * @param groupIdOfInterest
     *            - Optional parameter to filter the list by group id.
     * @return the assignment object.
     */
    @GET
    @Path("/assign")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getAssigned(@Context final HttpServletRequest request,
            @QueryParam("group") final String groupIdOfInterest) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            if (null == groupIdOfInterest) {
                return Response.ok(this.assignmentManager.getAllAssignmentsSetByUser(currentlyLoggedInUser)).build();
            } else {
                UserGroupDTO group = this.groupManager.getGroupById(groupIdOfInterest);

                if (null == group) {
                    return new SegueErrorResponse(Status.NOT_FOUND, "The group specified cannot be located.")
                            .toResponse();
                }

                List<AssignmentDTO> allAssignmentsSetByUserToGroup = this.assignmentManager
                        .getAllAssignmentsSetByUserToGroup(currentlyLoggedInUser, group);

                // we want to populate gameboard details for the assignment DTO.
                for (AssignmentDTO assignment : allAssignmentsSetByUserToGroup) {
                    assignment.setGameboard(this.gameManager.getGameboard(assignment.getGameboardId()));
                }

                this.getLogManager().logEvent(currentlyLoggedInUser, request, VIEW_GROUPS_ASSIGNMENTS,
                        Maps.newHashMap());

                return Response.ok(allAssignmentsSetByUserToGroup)
                        .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
            }

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to assignments set to a given group", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.").toResponse();
        }
    }

    /**
     * Allows the user to view results of an assignment they have set.
     * 
     * @param assignmentId
     *            - the id of the assignment to be looked up.
     * @param request
     *            - so that we can identify the current user.
     * @return the assignment object.
     */
    @GET
    @Path("/assign/{assignment_id}/progress")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getAssignmentProgress(@Context final HttpServletRequest request,
            @PathParam("assignment_id") final String assignmentId) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            AssignmentDTO assignment = this.assignmentManager.getAssignmentById(assignmentId);
            if (null == assignment) {
                return SegueErrorResponse.getResourceNotFoundResponse("The assignment requested cannot be found");
            }

            if (!assignment.getOwnerUserId().equals(currentlyLoggedInUser.getDbId())) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You can only view the results of assignments that you own.").toResponse();
            }

            GameboardDTO gameboard = this.gameManager.getGameboard(assignment.getGameboardId());
            UserGroupDTO group = this.groupManager.getGroupById(assignment.getGroupId());
            List<RegisteredUserDTO> groupMembers = this.groupManager.getUsersInGroup(group);

            List<ImmutableMap<String, Object>> result = Lists.newArrayList();
            final String userString = "user";
            final String resultsString = "results";

            for (Entry<RegisteredUserDTO, List<GameboardItemState>> e : this.gameManager.gatherGameProgressData(
                    groupMembers, gameboard).entrySet()) {
                UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(currentlyLoggedInUser,
                        userManager.convertToUserSummaryObject(e.getKey()));

                // can the user access the data?
                if (userSummary.isAuthorisedFullAccess()) {
                    result.add(ImmutableMap.of(userString, userSummary, resultsString, e.getValue()));
                } else {
                    result.add(ImmutableMap.of(userString, userSummary, resultsString, Lists.newArrayList()));
                }
            }

            // quick fix to sort list by user last name
            Collections.sort(result, new Comparator<ImmutableMap<String, Object>>() {
                @Override
                public int compare(final ImmutableMap<String, Object> o1, final ImmutableMap<String, Object> o2) {
                    UserSummaryDTO user1 = (UserSummaryDTO) o1.get(userString);
                    UserSummaryDTO user2 = (UserSummaryDTO) o2.get(userString);

                    if (user1.getFamilyName() == null && user2.getFamilyName() != null) {
                        return -1;
                    } else if (user1.getFamilyName() != null && user2.getFamilyName() == null) {
                        return 1;
                    } else if (user1.getFamilyName() == null && user2.getFamilyName() == null) {
                        return 0;
                    }
                    
                    return user1.getFamilyName().compareTo(user2.getFamilyName());
                }
            });

            this.getLogManager().logEvent(currentlyLoggedInUser, request, VIEW_ASSIGNMENT_PROGRESS, Maps.newHashMap());

            // get game manager completion information for this assignment.
            return Response.ok(result).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to view assignment progress", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.").toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown content database error.").toResponse();
        }
    }


    /**
     * Allows the user to view results of an assignment they have set.
     * 
     * @param assignmentId
     *            - the id of the assignment to be looked up.
     * @param request
     *            - so that we can identify the current user.
     * @return the assignment object.
     */
    @GET
    @Path("/assign/{assignment_id}/progress/download")
    @Produces("text/csv")
    @GZIP
    @Consumes(MediaType.WILDCARD)
    public Response getAssignmentProgressDownloadCSV(@Context final HttpServletRequest request,
            @PathParam("assignment_id") final String assignmentId) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
            
            AssignmentDTO assignment = this.assignmentManager.getAssignmentById(assignmentId);
            if (null == assignment) {
                return SegueErrorResponse.getResourceNotFoundResponse("The assignment requested cannot be found");
            }

            if (!assignment.getOwnerUserId().equals(currentlyLoggedInUser.getDbId())) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You can only view the results of assignments that you own.").toResponse();
            }
            
            GameboardDTO gameboard = this.gameManager.getGameboard(assignment.getGameboardId());
            UserGroupDTO group = this.groupManager.getGroupById(assignment.getGroupId());
            List<RegisteredUserDTO> groupMembers = this.groupManager.getUsersInGroup(group);
            List<String> questionIds = Lists.newArrayList();
            
            // quick hack to sort list by user last name
            Collections.sort(groupMembers, new Comparator<RegisteredUserDTO>() {
                @Override
                public int compare(final RegisteredUserDTO user1, final RegisteredUserDTO user2) {
                    
                    if (user1.getFamilyName() == null && user2.getFamilyName() != null) {
                        return -1;
                    } else if (user1.getFamilyName() != null && user2.getFamilyName() == null) {
                        return 1;
                    } else if (user1.getFamilyName() == null && user2.getFamilyName() == null) {
                        return 0;
                    }

                    return user1.getFamilyName().compareTo(user2.getFamilyName());
                }
            });
            
            StringBuilder headerBuilder = new StringBuilder();
            StringBuilder resultBuilder = new StringBuilder();
            headerBuilder.append("Results Downloaded on " + new Date() + "\n");
            headerBuilder.append("Last Name,First Name,");
            for (GameboardItem questionPage : gameboard.getQuestions()) {
                int index = 0;
                
                for (QuestionDTO question : gameManager.getAllMarkableQuestionParts(questionPage.getId())) {
                    int newCharIndex = 'A' + index;
                    headerBuilder.append(questionPage.getTitle() + " - Part " + Character.toChars(newCharIndex)[0]
                            + ",");
                    questionIds.add(question.getId());
                    index++;
                }
            }
            headerBuilder.append("% Correct,");
            headerBuilder.append("\n");
            Map<RegisteredUserDTO, Map<String, Integer>> userQuestionDataMap = this.gameManager
                    .getDetailedGameProgressData(groupMembers, gameboard);
            for (RegisteredUserDTO user : groupMembers) {

                UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(currentlyLoggedInUser,
                        userManager.convertToUserSummaryObject(user));

                // can the user access the data?
                if (userSummary.isAuthorisedFullAccess()) {
                    
                    resultBuilder.append(userSummary.getFamilyName() + "," + userSummary.getGivenName() + ",");
                    int totalCorrect = 0;
                    
                    for (String questionId : questionIds) {
                        Integer resultForQuestion = userQuestionDataMap.get(user).get(questionId);
                        
                        if (null == resultForQuestion) {
                            resultBuilder.append(",");
                        } else {
                            resultBuilder.append(resultForQuestion + ",");    
                        }
                        
                        if (resultForQuestion != null && resultForQuestion == 1) {
                            totalCorrect++;
                        }
                    }
                    
                    Double percentageCorrect = (new Double(totalCorrect) / questionIds.size()) * 100F;
                    resultBuilder.append(percentageCorrect + ",");
                    
                } else {
                    resultBuilder.append(userSummary.getFamilyName() + "," + userSummary.getGivenName() + ",");
                    for (@SuppressWarnings("unused") String questionId : questionIds) {
                        resultBuilder.append("ACCESS_REVOKED,");
                    }
                }
                resultBuilder.append("\n");
            }

            this.getLogManager().logEvent(currentlyLoggedInUser, request, DOWNLOAD_ASSIGNMENT_PROGRESS_CSV,
                    Maps.newHashMap());
            
            headerBuilder.append(resultBuilder);
            
            // get game manager completion information for this assignment.
            return Response.ok(headerBuilder.toString())
                    .header("Content-Disposition", "attachment; filename=export.csv")
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to view assignment progress", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.").toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown content database error.").toResponse();
        }
    }
    
    /**
     * Allows a user to get all groups that have been assigned to a given board.
     * 
     * @param request
     *            - so that we can identify the current user.
     * @param gameboardId
     *            - the id of the game board of interest.
     * @return the assignment object.
     */
    @GET
    @Path("/assign/{gameboard_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getAssignedGroups(@Context final HttpServletRequest request,
            @PathParam("gameboard_id") final String gameboardId) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            return Response.ok(assignmentManager.findGroupsByGameboard(currentlyLoggedInUser, gameboardId))
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to assign work", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.").toResponse();
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
    @GZIP
    public Response assignGameBoard(@Context final HttpServletRequest request,
            @PathParam("gameboard_id") final String gameboardId, @PathParam("group_id") final String groupId) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
            UserGroupDTO assigneeGroup = groupManager.getGroupById(groupId);
            if (null == assigneeGroup) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The group id specified does not exist.")
                        .toResponse();
            }

            GameboardDTO gameboard = this.gameManager.getGameboard(gameboardId);
            if (null == gameboard) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The gameboard id specified does not exist.")
                        .toResponse();
            }

            AssignmentDTO newAssignment = new AssignmentDTO();
            newAssignment.setGameboardId(gameboard.getId());
            newAssignment.setOwnerUserId(currentlyLoggedInUser.getDbId());
            newAssignment.setGroupId(groupId);

            // modifies assignment passed in to include an id.
            this.assignmentManager.createAssignment(newAssignment);

            this.getLogManager().logEvent(currentlyLoggedInUser, request, SET_NEW_ASSIGNMENT, Maps.newHashMap());

            return Response.ok(newAssignment).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (DuplicateAssignmentException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to assign work", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.").toResponse();
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
    @GZIP
    public Response deleteAssignment(@Context final HttpServletRequest request,
            @PathParam("gameboard_id") final String gameboardId, @PathParam("group_id") final String groupId) {

        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
            UserGroupDTO assigneeGroup = groupManager.getGroupById(groupId);
            if (null == assigneeGroup) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The group id specified does not exist.")
                        .toResponse();
            }

            GameboardDTO gameboard = this.gameManager.getGameboard(gameboardId);
            if (null == gameboard) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The gameboard id specified does not exist.")
                        .toResponse();
            }

            AssignmentDTO assignmentToDelete = this.assignmentManager.findAssignmentByGameboardAndGroup(
                    gameboard.getId(), groupId);

            if (null == assignmentToDelete) {
                return new SegueErrorResponse(Status.NOT_FOUND, "The assignment does not exist.").toResponse();
            }
            if (!assignmentToDelete.getOwnerUserId().equals(currentlyLoggedInUser.getDbId())) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You are not the owner of this assignment. Unable to delete it.").toResponse();
            }

            this.assignmentManager.deleteAssignment(assignmentToDelete);

            this.getLogManager().logEvent(currentlyLoggedInUser, request, DELETE_ASSIGNMENT, Maps.newHashMap());

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to delete assignment", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.").toResponse();
        }
    }
}
