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

import java.text.DecimalFormat;
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
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
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
    private final UserAccountManager userManager;
    private final GroupManager groupManager;
    private final GameManager gameManager;

    private final UserAssociationManager associationManager;

    private final QuestionManager questionManager;

    /**
     * Creates an instance of the AssignmentFacade controller which provides the REST endpoints for the isaac api.
     * 
     * @param assignmentManager
     *            - Instance of assignment Manager
     * @param questionManager
     *            - Instance of questions manager
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
    public AssignmentFacade(final AssignmentManager assignmentManager, final QuestionManager questionManager,
            final UserAccountManager userManager, final GroupManager groupManager,
            final PropertiesLoader propertiesLoader, final GameManager gameManager, final ILogManager logManager,
            final UserAssociationManager associationManager) {
        super(propertiesLoader, logManager);
        this.questionManager = questionManager;
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

            Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsByUser = this.questionManager
                    .getQuestionAttemptsByUser(currentlyLoggedInUser);

            // we want to populate gameboard details for the assignment DTO.
            for (AssignmentDTO assignment : assignments) {
                assignment.setGameboard(this.gameManager.getGameboard(assignment.getGameboardId(),
                        currentlyLoggedInUser, questionAttemptsByUser));

                if (assignment.getOwnerUserId() != null) {
                    try {
                        RegisteredUserDTO user = userManager.getUserDTOById(assignment.getOwnerUserId());
                        if (user == null) {
                            throw new NoUserException();
                        }
                        UserSummaryDTO userSummary = userManager.convertToUserSummaryObject(user);
                        assignment.setAssignerSummary(userSummary);
                    } catch (NoUserException e) {
                        log.warn("Assignment (" + assignment.getId() + ") exists with owner user ID (" +
                                assignment.getOwnerUserId() + ") that does not exist!");
                    }
                }
            }

            // if they have filtered the list we should only send out the things they wanted.
            if (assignmentStatus != null) {
                List<AssignmentDTO> newList = Lists.newArrayList();
                // we want to populate gameboard details for the assignment DTO.
                for (AssignmentDTO assignment : assignments) {
                    if (assignment.getGameboard() == null) {
                        continue;
                    }
                    
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
            @QueryParam("group") final Long groupIdOfInterest) {
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
            @PathParam("assignment_id") final Long assignmentId) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            AssignmentDTO assignment = this.assignmentManager.getAssignmentById(assignmentId);
            if (null == assignment) {
                return SegueErrorResponse.getResourceNotFoundResponse("The assignment requested cannot be found");
            }

            if (!assignment.getOwnerUserId().equals(currentlyLoggedInUser.getId()) 
                    && !super.isUserAnAdmin(userManager, request)) {
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
     * Allows the user to view results of an assignment they have set as a detailed csv file.
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
            @PathParam("assignment_id") final Long assignmentId) {
       
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
            
            AssignmentDTO assignment = this.assignmentManager.getAssignmentById(assignmentId);
            if (null == assignment) {
                return SegueErrorResponse.getResourceNotFoundResponse("The assignment requested cannot be found");
            }

            if (!assignment.getOwnerUserId().equals(currentlyLoggedInUser.getId())
                    && !isUserAnAdmin(userManager, request)) {
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
            headerBuilder.append(String.format("Assignment (%s) Results: Downloaded on %s \nGenerated by: %s %s \n",
                    assignmentId, new Date(), currentlyLoggedInUser.getGivenName(),
                    currentlyLoggedInUser.getFamilyName()));
            
            headerBuilder.append("\n");
            headerBuilder.append(",,");

            DecimalFormat percentageFormat = new DecimalFormat("###");
            
            for (GameboardItem questionPage : gameboard.getQuestions()) {
                int index = 0;
                
                for (QuestionDTO question : gameManager.getAllMarkableQuestionPartsDFSOrder(questionPage.getId())) {
                    //int newCharIndex = 'A' + index; // decided not to try and match the front end.
                    int newCharIndex = index + 1;
                    if (question.getTitle() != null) {
                        headerBuilder.append("\"" + questionPage.getTitle() + " - " + question.getTitle() + "\",");
                    } else {
                        headerBuilder.append("\"" + questionPage.getTitle() + " - Q" + newCharIndex + "\",");
                    }

                    questionIds.add(question.getId());
                    index++;
                }
            }
            headerBuilder.append("% Correct,");
            headerBuilder.append("\n");
            headerBuilder.append(",% Correct,");
            
            Map<RegisteredUserDTO, Map<String, Integer>> userQuestionDataMap = this.gameManager
                    .getDetailedGameProgressData(groupMembers, gameboard);

            int[] columnTotals = new int[questionIds.size()];
            for (RegisteredUserDTO user : groupMembers) {

                UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(currentlyLoggedInUser,
                        userManager.convertToUserSummaryObject(user));

                // can the user access the data?
                if (userSummary.isAuthorisedFullAccess()) {
                    resultBuilder.append(userSummary.getFamilyName() + "," + userSummary.getGivenName() + ",");
                    int totalCorrect = 0;
                    int columnNumber = 0;
                    for (String questionId : questionIds) {
                        Integer resultForQuestion = userQuestionDataMap.get(user).get(questionId);
                        
                        if (null == resultForQuestion) {
                            resultBuilder.append(",");
                        } else {
                            resultBuilder.append(resultForQuestion + ",");    
                        }
                        
                        if (resultForQuestion != null && resultForQuestion == 1) {
                            totalCorrect++;
                            columnTotals[columnNumber] += 1;
                        }
                        columnNumber++;
                    }
                    
                    Double percentageCorrect = (new Double(totalCorrect) / questionIds.size()) * 100F;
                    resultBuilder.append(percentageFormat.format(percentageCorrect) + ",");
                    
                } else {
                    resultBuilder.append(userSummary.getFamilyName() + "," + userSummary.getGivenName() + ",");
                    for (@SuppressWarnings("unused") String questionId : questionIds) {
                        resultBuilder.append("ACCESS_REVOKED,");
                    }
                }
                resultBuilder.append("\n");
            }

            this.getLogManager().logEvent(currentlyLoggedInUser, request, DOWNLOAD_ASSIGNMENT_PROGRESS_CSV,
                    ImmutableMap.of("assignmentId", assignmentId));
            
            // ignore name columns

            for (int i = 0; i < questionIds.size(); i++) {
                Double percentageCorrect = (new Double(columnTotals[i]) / groupMembers.size()) * 100F;
                headerBuilder.append(percentageFormat.format(percentageCorrect) + ",");
            }
            headerBuilder.append("\n");
            
            headerBuilder.append("Last Name,First Name\n");

            headerBuilder.append(resultBuilder);
            
            // get game manager completion information for this assignment.
            return Response.ok(headerBuilder.toString())
                    .header("Content-Disposition", "attachment; filename=assignment_progress.csv")
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
     * Allows the user to download the results of the assignments they have set to a group as a detailed csv file.
     *
     * @param groupId
     *            - the id of the group to be looked up.
     * @param request
     *            - so that we can identify the current user.
     * @return the assignment object.
     */
    @GET
    @Path("/assign/group/{group_id}/progress/download")
    @Produces("text/plain")
    @GZIP
    @Consumes(MediaType.WILDCARD)
    public Response getGroupAssignmentsProgressDownloadCSV(@Context final HttpServletRequest request,
            @PathParam("group_id") final Long groupId) {

        try {
            // Fetch the currently logged in user
            RegisteredUserDTO currentlyLoggedInUser;
            currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            if (!isUserAnAdmin(userManager, request) && !isUserStaff(userManager, request)
                    && currentlyLoggedInUser.getRole() != Role.TEACHER) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You are not a teacher, a member of staff, nor an admin.").toResponse();
            }

            // Fetch the requested group
            UserGroupDTO group;
            group = this.groupManager.getGroupById(groupId);

            // Fetch the assignments owned by the currently logged in user that are assigned to the requested group
            List<AssignmentDTO> assignments;
            assignments = this.assignmentManager.getAllAssignmentsSetByUserToGroup(currentlyLoggedInUser, group);

            // Fetch the members of the requested group
            List<RegisteredUserDTO> groupMembers;
            groupMembers = this.groupManager.getUsersInGroup(group);
            // quick hack to sort list by user last name
            groupMembers.sort((final RegisteredUserDTO user1, final RegisteredUserDTO user2) -> {
                if (user1.getFamilyName() == null && user2.getFamilyName() != null) {
                    return -1;
                } else if (user1.getFamilyName() != null && user2.getFamilyName() == null) {
                    return 1;
                } else if (user1.getFamilyName() == null && user2.getFamilyName() == null) {
                    return 0;
                } else {
                    return user1.getFamilyName().compareTo(user2.getFamilyName());
                }
            });

            // String: question part id
            // Integer: question part result
            Map<RegisteredUserDTO, Map<GameboardDTO, Map<String, Integer>>> grandTable = Maps.newHashMap();
            // Retrieve each user's progress data and cram everything into a Grand Table for later consumption
            for (AssignmentDTO assignment : assignments) {
                GameboardDTO gameboard = gameManager.getGameboard(assignment.getGameboardId());

                Map<RegisteredUserDTO, Map<String, Integer>> userQuestionDataMap;
                userQuestionDataMap = this.gameManager.getDetailedGameProgressData(groupMembers, gameboard);

                for (RegisteredUserDTO student : userQuestionDataMap.keySet()) {
                    Map<GameboardDTO, Map<String, Integer>> entry = grandTable.get(student);
                    if (null == entry) {
                        entry = Maps.newHashMap();
                    }
                    entry.put(gameboard, userQuestionDataMap.get(student));
                    grandTable.put(student, entry);
                }
            }

            StringBuilder headerBuilder = new StringBuilder();
            StringBuilder resultBuilder = new StringBuilder();
            
            headerBuilder.append(String.format("Assignments for '%s' (%s)\nDownloaded on %s\nGenerated by: %s %s\n\n",
                    group.getGroupName(), group.getId(), new Date(), currentlyLoggedInUser.getGivenName(), currentlyLoggedInUser.getFamilyName()));

            headerBuilder.append("Last Name,First Name,Overall Total");
            List<String> gameboardTitles = Lists.newArrayList();
            for (AssignmentDTO assignment : assignments) {
                GameboardDTO gameboard = gameManager.getGameboard(assignment.getGameboardId());
                String gameboardTitle = gameboard.getTitle();
                if (null != gameboardTitle) {
                    gameboardTitles.add(gameboardTitle);
                } else {
                    gameboardTitles.add(gameboard.getId());
                }
            }
            for (String gameboardTitle : gameboardTitles) {
                headerBuilder.append(",Total for '" + gameboardTitle + "'");
            }
            headerBuilder.append(",");
            Map<GameboardDTO, List<String>> gameboardQuestionIds = Maps.newHashMap();
            for (AssignmentDTO assignment : assignments) {
                GameboardDTO gameboard = gameManager.getGameboard(assignment.getGameboardId());
                for (GameboardItem questionPage : gameboard.getQuestions()) {
                    int b = 1;
                    for (QuestionDTO question : gameManager.getAllMarkableQuestionPartsDFSOrder(questionPage.getId())) {
                        List<String> questionIds = gameboardQuestionIds.get(gameboard);
                        if (null == questionIds) {
                            questionIds = Lists.newArrayList();
                        }
                        questionIds.add(question.getId());
                        gameboardQuestionIds.put(gameboard, questionIds);

                        headerBuilder.append(",\"");
                        if (gameboard.getTitle() != null) {
                            headerBuilder.append(gameboard.getTitle() + " - ");
                        } else {
                            headerBuilder.append(gameboard.getId() + " - ");
                        }
                        headerBuilder.append(questionPage.getTitle() + " - ");
                        if (question.getTitle() != null) {
                            headerBuilder.append(question.getTitle());
                        } else {
                            headerBuilder.append("Q" + b);
                        }
                        b++;
                        headerBuilder.append("\"");
                    }
                }
            }
            headerBuilder.append("\n");

            for (RegisteredUserDTO groupMember : groupMembers) {
                Map<GameboardDTO, Map<String, Integer>> userAssignments = grandTable.get(groupMember);
                List<Integer> totals = Lists.newArrayList();
                List<Integer> marks = Lists.newArrayList();
                for (AssignmentDTO assignment : assignments) {
                    GameboardDTO gameboard = gameManager.getGameboard(assignment.getGameboardId());
                    Integer total = 0;
                    for (String s : gameboardQuestionIds.get(gameboard)) {
                        Integer mark = userAssignments.get(gameboard).get(s);
                        marks.add(mark);
                        if (null != mark) {
                            total += mark;
                        } else {
                            total += 0;
                        }
                    }
                    totals.add(total);
                }
                Integer overallTotal = 0;
                overallTotal = totals.stream().reduce((a, b) -> a + b).get();

                resultBuilder.append(groupMember.getFamilyName() + "," + groupMember.getGivenName()
                        + "," + overallTotal);
                for (Integer total : totals) {
                    resultBuilder.append("," + total);
                }
                resultBuilder.append(",");
                for (Integer mark : marks) {
                    if (null != mark) {
                        resultBuilder.append("," + mark);
                    } else {
                        resultBuilder.append(",");
                    }
                }
                resultBuilder.append("\n");
            }

            headerBuilder.append(resultBuilder);
            return Response.ok(headerBuilder.toString())
                    .header("Content-Disposition", "attachment; filename=group_progress.csv")
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
            @PathParam("gameboard_id") final String gameboardId, @PathParam("group_id") final Long groupId) {
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
            newAssignment.setOwnerUserId(currentlyLoggedInUser.getId());
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
            @PathParam("gameboard_id") final String gameboardId, @PathParam("group_id") final Long groupId) {

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
            if (!assignmentToDelete.getOwnerUserId().equals(currentlyLoggedInUser.getId())) {
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
