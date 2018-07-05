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

import com.google.common.collect.ImmutableMap;
import com.opencsv.CSVWriter;
import io.swagger.annotations.Api;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Arrays;
import java.util.ArrayList;
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

import org.apache.commons.lang3.tuple.ImmutablePair;
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

    private final String NOT_SHARING = "NOT_SHARING";

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
                            throw new NoUserException("No user found with this ID.");
                        }
                        UserSummaryDTO userSummary = userManager.convertToUserSummaryObject(user);
                        assignment.setAssignerSummary(userSummary);
                    } catch (NoUserException e) {
                        log.warn("Assignment (" + assignment.getId() + ") exists with owner user ID ("
                                + assignment.getOwnerUserId() + ") that does not exist!");
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
                List<UserGroupDTO> allGroupsOwnedAndManagedByUser = this.groupManager.getAllGroupsOwnedAndManagedByUser(currentlyLoggedInUser, false);
                return Response.ok(this.assignmentManager.getAllAssignmentsForSpecificGroups(allGroupsOwnedAndManagedByUser))
                        .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
            } else {
                UserGroupDTO group = this.groupManager.getGroupById(groupIdOfInterest);

                if (null == group) {
                    return new SegueErrorResponse(Status.NOT_FOUND, "The group specified cannot be located.")
                            .toResponse();
                }

                if (!GroupManager.isOwnerOrAdditionalManager(group, currentlyLoggedInUser.getId())
                        && !isUserAnAdmin(userManager, request)) {
                    return new SegueErrorResponse(Status.FORBIDDEN, "You are not the owner or manager of this group").toResponse();
                }

                Collection<AssignmentDTO> allAssignmentsSetToGroup
                        = this.assignmentManager.getAssignmentsByGroup(group.getId());

                // we want to populate gameboard details for the assignment DTO.
                for (AssignmentDTO assignment : allAssignmentsSetToGroup) {
                    assignment.setGameboard(this.gameManager.getGameboard(assignment.getGameboardId()));
                }

                this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacLogType.VIEW_GROUPS_ASSIGNMENTS,
                        ImmutableMap.of("groupId", group.getId()));

                return Response.ok(allAssignmentsSetToGroup)
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

            UserGroupDTO group = this.groupManager.getGroupById(assignment.getGroupId());

            if (!GroupManager.isOwnerOrAdditionalManager(group, currentlyLoggedInUser.getId())
                    && !isUserAnAdmin(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You can only view the results of assignments that you own.").toResponse();
            }

            GameboardDTO gameboard = this.gameManager.getGameboard(assignment.getGameboardId());

            List<RegisteredUserDTO> groupMembers = this.groupManager.getUsersInGroup(group);

            List<ImmutableMap<String, Object>> result = Lists.newArrayList();
            final String userString = "user";
            final String resultsString = "results";
            final String correctPartString = "correctPartResults";
            final String incorrectPartString = "incorrectPartResults";

            for (ImmutablePair<RegisteredUserDTO, List<GameboardItem>> userGameboardItems : this.gameManager
                    .gatherGameProgressData(groupMembers, gameboard)) {
                UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(currentlyLoggedInUser,
                        userManager.convertToUserSummaryObject(userGameboardItems.getLeft()));

                // can the user access the data?
                if (userSummary.isAuthorisedFullAccess()) {
                    ArrayList<GameboardItemState> states = Lists.newArrayList();
                    ArrayList<Integer> correctQuestionParts = Lists.newArrayList();
                    ArrayList<Integer> incorrectQuestionParts = Lists.newArrayList();
                    for (GameboardItem questionResult : userGameboardItems.getRight()) {
                        states.add(questionResult.getState());
                        correctQuestionParts.add(questionResult.getQuestionPartsCorrect());
                        incorrectQuestionParts.add(questionResult.getQuestionPartsIncorrect());
                    }
                    result.add(ImmutableMap.of(userString, userSummary, resultsString, states,
                            correctPartString, correctQuestionParts, incorrectPartString, incorrectQuestionParts));
                } else {
                    result.add(ImmutableMap.of(userString, userSummary, resultsString, Lists.newArrayList(),
                            correctPartString, Lists.newArrayList(), incorrectPartString, Lists.newArrayList()));
                }
            }

            this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacLogType.VIEW_ASSIGNMENT_PROGRESS,
                    ImmutableMap.of(ASSIGNMENT_FK, assignment.getId()));

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

            UserGroupDTO group = this.groupManager.getGroupById(assignment.getGroupId());

            if (!GroupManager.isOwnerOrAdditionalManager(group, currentlyLoggedInUser.getId())
                    && !isUserAnAdmin(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You can only view the results of assignments that you own.").toResponse();
            }
            
            GameboardDTO gameboard = this.gameManager.getGameboard(assignment.getGameboardId());

            List<RegisteredUserDTO> groupMembers = this.groupManager.getUsersInGroup(group);
            List<String> questionIds = Lists.newArrayList();

            List<String[]> rows = Lists.newArrayList();
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            StringBuilder headerBuilder = new StringBuilder();
            headerBuilder.append(String.format("Assignment (%s) Results: Downloaded on %s \nGenerated by: %s %s \n\n",
                    assignmentId, new Date(), currentlyLoggedInUser.getGivenName(),
                    currentlyLoggedInUser.getFamilyName()));

            List<String> headerRow = Lists.newArrayList(Arrays.asList("", ""));

            DecimalFormat percentageFormat = new DecimalFormat("###");

            for (GameboardItem questionPage : gameboard.getQuestions()) {
                int index = 0;
                
                for (QuestionDTO question : gameManager.getAllMarkableQuestionPartsDFSOrder(questionPage.getId())) {
                    //int newCharIndex = 'A' + index; // decided not to try and match the front end.
                    int newCharIndex = index + 1;
                    if (question.getTitle() != null) {
                        headerRow.add(question.getTitle() + " - " + questionPage.getTitle());
                    } else {
                        headerRow.add("Q" + newCharIndex + " - " + questionPage.getTitle());
                    }

                    questionIds.add(question.getId());
                    index++;
                }
            }
            headerRow.add("% Correct");
            rows.add(headerRow.toArray(new String[0]));

            List<String> totalsRow = Lists.newArrayList();
            Collections.addAll(totalsRow, ",Correct %".split(","));

            Map<RegisteredUserDTO, Map<String, Integer>> userQuestionDataMap = this.gameManager
                    .getDetailedGameProgressData(groupMembers, gameboard);

            List<String[]> resultRows = Lists.newArrayList();
            int[] columnTotals = new int[questionIds.size()];
            for (RegisteredUserDTO user : groupMembers) {
                ArrayList<String> resultRow = Lists.newArrayList();
                UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(currentlyLoggedInUser,
                        userManager.convertToUserSummaryObject(user));

                // can the user access the data?
                if (userSummary.isAuthorisedFullAccess()) {
                    resultRow.add(userSummary.getFamilyName());
                    resultRow.add(userSummary.getGivenName());
                    int totalCorrect = 0;
                    int columnNumber = 0;
                    for (String questionId : questionIds) {
                        Integer resultForQuestion = userQuestionDataMap.get(user).get(questionId);
                        
                        if (null == resultForQuestion) {
                            resultRow.add("");
                        } else {
                            resultRow.add(String.format("%d", resultForQuestion));
                        }
                        
                        if (resultForQuestion != null && resultForQuestion == 1) {
                            totalCorrect++;
                            columnTotals[columnNumber] += 1;
                        }
                        columnNumber++;
                    }
                    
                    double percentageCorrect = ((double) totalCorrect / questionIds.size()) * 100F;
                    resultRow.add(percentageFormat.format(percentageCorrect));
                    
                } else {
                    resultRow.add(userSummary.getFamilyName());
                    resultRow.add(userSummary.getGivenName());
                    for (@SuppressWarnings("unused") String questionId : questionIds) {
                        resultRow.add(NOT_SHARING);
                    }
                }
                Collections.addAll(resultRows, resultRow.toArray(new String[0]));
            }

            this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacLogType.DOWNLOAD_ASSIGNMENT_PROGRESS_CSV,
                    ImmutableMap.of("assignmentId", assignmentId));
            
            // ignore name columns

            for (int i = 0; i < questionIds.size(); i++) {
                double percentageCorrect = ((double) columnTotals[i] / groupMembers.size()) * 100F;
                totalsRow.add(percentageFormat.format(percentageCorrect));
            }

            rows.add(totalsRow.toArray(new String[0]));
            rows.add("Last Name,First Name".split(","));
            rows.addAll(resultRows);
            csvWriter.writeAll(rows);
            csvWriter.close();

            headerBuilder.append(stringWriter.toString());
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
        } catch (IOException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while building the CSV file.").toResponse();
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

            // Fetch the requested group
            UserGroupDTO group;
            group = this.groupManager.getGroupById(groupId);

            // Check the user has permission to access this group:
            if (!GroupManager.isOwnerOrAdditionalManager(group, currentlyLoggedInUser.getId())
                && !isUserAnAdmin(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You can only view the results of assignments that you own.").toResponse();
            }

            // Fetch all assignments set to the requested group:
            List<AssignmentDTO> assignments;
            assignments = this.assignmentManager.getAllAssignmentsForSpecificGroups(Collections.singletonList(group));

            // Fetch the members of the requested group
            List<RegisteredUserDTO> groupMembers;
            groupMembers = this.groupManager.getUsersInGroup(group);

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

            ArrayList<String> headerRow = Lists.newArrayList();
            Collections.addAll(headerRow, "Last Name,First Name,% Correct Overall".split(","));
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
                headerRow.add("% Correct for '" + gameboardTitle + "'");
            }
            headerRow.add("");
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

                        StringBuilder s = new StringBuilder();
                        if (question.getTitle() != null) {
                            s.append(question.getTitle());
                        } else {
                            s.append("Q").append(b);
                        }
                        s.append(" - ").append(questionPage.getTitle()).append(" - ");
                        if (gameboard.getTitle() != null) {
                            s.append(gameboard.getTitle());
                        } else {
                            s.append(gameboard.getId());
                        }
                        b++;
                        headerRow.add(s.toString());
                    }
                }
            }

            // Moving on to actual rows...
            ArrayList<String[]> rows = Lists.newArrayList();
            rows.add(headerRow.toArray(new String[0]));

            for (RegisteredUserDTO groupMember : groupMembers) {

                UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(currentlyLoggedInUser,
                        userManager.convertToUserSummaryObject(groupMember));

                ArrayList<String> row = Lists.newArrayList();
                Map<GameboardDTO, Map<String, Integer>> userAssignments = grandTable.get(groupMember);
                List<Float> assignmentPercentages = Lists.newArrayList();
                List<Integer> marks = Lists.newArrayList();
                int totalQPartsCorrect = 0;
                int totalQPartsCount = 0;
                for (AssignmentDTO assignment : assignments) {
                    GameboardDTO gameboard = gameManager.getGameboard(assignment.getGameboardId());
                    int assignmentQPartsCorrect = 0;
                    int assignmentQPartsCount = 0;
                    List<String> questionIds = gameboardQuestionIds.get(gameboard);
                    List<GameboardItem> questions = gameboard.getQuestions();
                    Map<String, Integer> gameboardPartials = Maps.newHashMap();
                    for (GameboardItem question : questions) {
                        gameboardPartials.put(question.getId(), 0);
                    }
                    HashMap<String, Integer> questionParts = new HashMap<>(gameboardPartials);
                    for (String s : questionIds) {
                        Integer mark = userAssignments.get(gameboard).get(s);
                        String[] tokens = s.split("\\|");
                        questionParts.put(tokens[0], questionParts.get(tokens[0]) + 1);
                        marks.add(mark);
                        if (null != mark) {
                            gameboardPartials.put(tokens[0], gameboardPartials.get(tokens[0]) + mark);
                        }
                    }
                    for (Entry<String, Integer> entry : gameboardPartials.entrySet()) {
                        assignmentQPartsCorrect += entry.getValue();
                        assignmentQPartsCount += questionParts.get(entry.getKey());
                    }
                    totalQPartsCorrect += assignmentQPartsCorrect;
                    totalQPartsCount += assignmentQPartsCount;
                    assignmentPercentages.add((100f * assignmentQPartsCorrect) / assignmentQPartsCount);
                }
                float overallTotal = (100f * totalQPartsCorrect) / totalQPartsCount;

                // The next three lines could be a little better if I were not this sleepy...
                row.add(userSummary.getFamilyName());
                row.add(userSummary.getGivenName());

                if (userSummary.isAuthorisedFullAccess()) {
                    row.add(String.format("%.0f", overallTotal));
                    for (Float assignmentPercentage : assignmentPercentages) {
                        row.add(String.format("%.0f", assignmentPercentage));
                    }
                    row.add("");
                    for (Integer mark : marks) {
                        if (null != mark) {
                            row.add(String.format("%d", mark));
                        } else {
                            row.add("");
                        }
                    }

                } else {
                    row.add(NOT_SHARING);
                    for (@SuppressWarnings("unused") Float assignmentPercentage : assignmentPercentages) {
                        row.add(NOT_SHARING);
                    }
                    row.add("");
                    for (@SuppressWarnings("unused") Integer mark : marks) {
                        row.add(NOT_SHARING);
                    }
                }
                rows.add(row.toArray(new String[0]));
            }

            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            csvWriter.writeAll(rows);
            csvWriter.close();

            String headerBuilder = String.format("Assignments for '%s' (%s)\nDownloaded on %s\nGenerated by: %s %s\n\n",
                    group.getGroupName(), group.getId(), new Date(), currentlyLoggedInUser.getGivenName(),
                    currentlyLoggedInUser.getFamilyName()) + stringWriter.toString()
                    + "\n\nN.B.\n\"The percentages are for question parts completed, not question pages.\"\n";

            this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacLogType.DOWNLOAD_GROUP_PROGRESS_CSV,
                    ImmutableMap.of("groupId", groupId));

            return Response.ok(headerBuilder)
                    .header("Content-Disposition", "attachment; filename=group_progress.csv")
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to view assignment progress", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.").toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown content database error.").toResponse();
        } catch (IOException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while building the CSV file.").toResponse();
        }
    }

    /**
     * Allows a user to get all groups that have been assigned to a given list of boards.
     *
     * @param request
     *            - so that we can identify the current user.
     * @param gameboardIdsQueryParam
     *            - The comma seperated list of gameboard ids.
     * @return the assignment object.
     */
    @GET
    @Path("/assign/groups")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getAssignedGroupsByGameboards(@Context final HttpServletRequest request,
            @QueryParam("gameboard_ids") final String gameboardIdsQueryParam) {
        try {

            if (null == gameboardIdsQueryParam || gameboardIdsQueryParam.isEmpty() ) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a comma separated list of gameboard_ids in the query param")
                        .toResponse();
            }

            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
            Map<String, Object> gameboardGroups = Maps.newHashMap();


            for (String gameboardId : gameboardIdsQueryParam.split(",")) {
                gameboardGroups.put(gameboardId, assignmentManager.findGroupsByGameboard(currentlyLoggedInUser, gameboardId));
            }

            return Response.ok(gameboardGroups)
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
     * @param assignmentDTOFromClient a partially completed DTO for the assignment.
     * @return the assignment object.
     */
    @POST
    @Path("/assign/")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response assignGameBoard(@Context final HttpServletRequest request,
            final AssignmentDTO assignmentDTOFromClient) {

        if (assignmentDTOFromClient.getGameboardId() == null || assignmentDTOFromClient.getGroupId() == null) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "A required field was missing. Must provide group and gameboard ids").toResponse();
        }

        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
            UserGroupDTO assigneeGroup = groupManager.getGroupById(assignmentDTOFromClient.getGroupId());
            if (null == assigneeGroup) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The group id specified does not exist.")
                        .toResponse();
            }

            if (!GroupManager.isOwnerOrAdditionalManager(assigneeGroup, currentlyLoggedInUser.getId())
                    && !isUserAnAdmin(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You can only set assignments to groups you own or manage.").toResponse();
            }

            GameboardDTO gameboard = this.gameManager.getGameboard(assignmentDTOFromClient.getGameboardId());
            if (null == gameboard) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The gameboard id specified does not exist.")
                        .toResponse();
            }

            assignmentDTOFromClient.setOwnerUserId(currentlyLoggedInUser.getId());
            assignmentDTOFromClient.setCreationDate(null);
            assignmentDTOFromClient.setId(null);

            // modifies assignment passed in to include an id.
            AssignmentDTO assignmentWithID = this.assignmentManager.createAssignment(assignmentDTOFromClient);

            LinkedHashMap<String, Object> eventDetails = new LinkedHashMap<>();
            eventDetails.put(Constants.GAMEBOARD_ID_FKEY, assignmentWithID.getGameboardId());
            eventDetails.put(GROUP_FK, assignmentWithID.getGroupId());
            eventDetails.put(ASSIGNMENT_FK, assignmentWithID.getId());
            eventDetails.put(ASSIGNMENT_DUEDATE_FK, assignmentWithID.getDueDate());
            this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacLogType.SET_NEW_ASSIGNMENT, eventDetails);

            return Response.ok(assignmentDTOFromClient).build();
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

            this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacLogType.DELETE_ASSIGNMENT,
                    ImmutableMap.of(ASSIGNMENT_FK, assignmentToDelete.getId()));

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to delete assignment", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.").toResponse();
        }
    }
}
