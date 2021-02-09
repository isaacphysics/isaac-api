/*
 * Copyright 2014 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.api;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.opencsv.CSVWriter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateAssignmentException;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.services.AssignmentService;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.UserGroup;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final UserBadgeManager userBadgeManager;
    private final AssignmentService assignmentService;

    private final List<String> bookTags = ImmutableList.of("phys_book_gcse", "physics_skills_14", "chemistry_16");

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
     * @param userBadgeManager
     *            - So that badges can be awarded to do with assignments
     * @param assignmentService
     *            - for augmenting assignments with assigner information
     */
    @Inject
    public AssignmentFacade(final AssignmentManager assignmentManager, final QuestionManager questionManager,
                            final UserAccountManager userManager, final GroupManager groupManager,
                            final PropertiesLoader propertiesLoader, final GameManager gameManager, final ILogManager logManager,
                            final UserAssociationManager associationManager, final UserBadgeManager userBadgeManager,
                            final AssignmentService assignmentService) {
        super(propertiesLoader, logManager);
        this.questionManager = questionManager;
        this.userManager = userManager;
        this.gameManager = gameManager;
        this.groupManager = groupManager;
        this.assignmentManager = assignmentManager;
        this.associationManager = associationManager;
        this.userBadgeManager = userBadgeManager;
        this.assignmentService = assignmentService;
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
    @ApiOperation(value = "List all boards assigned to the current user.")
    public Response getAssignments(@Context final HttpServletRequest request,
                                   @QueryParam("assignmentStatus") final GameboardState assignmentStatus) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
            Collection<AssignmentDTO> assignments = this.assignmentManager.getAssignments(currentlyLoggedInUser);
            Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsByUser = this.questionManager
                    .getQuestionAttemptsByUser(currentlyLoggedInUser);

            // Gather all gameboards we need to augment for the assignments in a single query
            List<String> gameboardIds = assignments.stream().map(AssignmentDTO::getGameboardId).collect(Collectors.toList());
            Map<String, GameboardDTO> gameboardsMap = this.gameManager.getGameboards(gameboardIds, currentlyLoggedInUser, questionAttemptsByUser)
                    .stream().collect(Collectors.toMap(GameboardDTO::getId, Function.identity()));

            // we want to populate gameboard details for the assignment DTO.
            for (AssignmentDTO assignment : assignments) {
                assignment.setGameboard(gameboardsMap.get(assignment.getGameboardId()));

                // Augment with group name if allowed
                // TODO: Do we want staff and/or admins here?
                UserGroupDTO group = groupManager.getGroupById(assignment.getGroupId());
                if (GroupManager.isOwnerOrAdditionalManager(group, currentlyLoggedInUser.getId())) {
                    assignment.setGroupName(group.getGroupName());
                }
            }

            this.assignmentService.augmentAssignerSummaries(assignments);

            // if they have filtered the list we should only send out the things they wanted.
            if (assignmentStatus != null) {
                List<AssignmentDTO> newList = Lists.newArrayList();
                // we want to populate gameboard details for the assignment DTO.
                for (AssignmentDTO assignment : assignments) {
                    if (assignment.getGameboard() == null || assignment.getGameboard().getQuestions().size() == 0) {
                        log.warn(String.format("Skipping broken gameboard '%s' for assignment (%s)!",
                                assignment.getGameboardId(), assignment.getId()));
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

            return Response.ok(assignments)
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
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
     * Allows a user to get all assignments they have set in light weight objects.
     *
     * If the user specifies a group ID to narrow the search full objects including questions in gameboards will be returned.
     *
     * @param request
     *            - so that we can identify the current user.
     * @param groupIdOfInterest
     *            - Optional parameter - If this is specified a fully resolved assignment object will be provided
     *            otherwise just a lightweight one per assignment will be returned.
     * @return the assignment object.
     */
    @GET
    @Path("/assign")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "List all assignments set by the current user if no group param specified.")
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
                        && !isUserAnAdmin(userManager, currentlyLoggedInUser)) {
                    return new SegueErrorResponse(Status.FORBIDDEN, "You are not the owner or manager of this group").toResponse();
                }

                Collection<AssignmentDTO> allAssignmentsSetToGroup
                        = this.assignmentManager.getAssignmentsByGroup(group.getId());

                // In order to get all the information about gameboard items, we need to use the method which augments
                // gameboards with user attempt information. But we don't _want_ this information for real, so we won't
                // do the costly loading of the real attempt information from the database:
                Map<String, Map<String, List<QuestionValidationResponse>>> fakeQuestionAttemptMap = new HashMap<>();

                // we want to populate gameboard details for the assignment DTO.
                List<String> gameboardIDs = allAssignmentsSetToGroup.stream().map(AssignmentDTO::getGameboardId).collect(Collectors.toList());
                Map<String, GameboardDTO> gameboards = this.gameManager.getGameboards(gameboardIDs, currentlyLoggedInUser, fakeQuestionAttemptMap)
                        .stream().collect(Collectors.toMap(GameboardDTO::getId, Function.identity()));
                for (AssignmentDTO assignment : allAssignmentsSetToGroup) {
                    assignment.setGameboard(gameboards.get(assignment.getGameboardId()));
                }

                this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacServerLogType.VIEW_GROUPS_ASSIGNMENTS,
                        ImmutableMap.of("groupId", group.getId()));

                return Response.ok(allAssignmentsSetToGroup)
                        .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
            }

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to assignments set to a given group", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unknown database error.").toResponse();
        } catch (ContentManagerException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
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
    @ApiOperation(value = "View the progress of a specific assignment.")
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
                    && !isUserAnAdmin(userManager, currentlyLoggedInUser)) {
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

            this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacServerLogType.VIEW_ASSIGNMENT_PROGRESS,
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
     * @param formatMode
     *            - whether to format the file in a special way. Currently only "excel" is supported,
     *            to include a UTF-8 BOM to allow Unicode student names to show correctly in Microsoft Excel.
     * @param request
     *            - so that we can identify the current user.
     * @return the assignment object.
     */
    @GET
    @Path("/assign/{assignment_id}/progress/download")
    @Produces("text/csv")
    @GZIP
    @Consumes(MediaType.WILDCARD)
    @ApiOperation(value = "Download the progress of a specific assignment.")
    public Response getAssignmentProgressDownloadCSV(@Context final HttpServletRequest request,
                                                     @PathParam("assignment_id") final Long assignmentId,
                                                     @QueryParam("format") final String formatMode) {

        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
            boolean includeUserIDs = isUserAnAdminOrEventManager(userManager, currentlyLoggedInUser);

            AssignmentDTO assignment = this.assignmentManager.getAssignmentById(assignmentId);
            if (null == assignment) {
                return SegueErrorResponse.getResourceNotFoundResponse("The assignment requested cannot be found");
            }

            UserGroupDTO group = this.groupManager.getGroupById(assignment.getGroupId());

            if (!GroupManager.isOwnerOrAdditionalManager(group, currentlyLoggedInUser.getId())
                    && !isUserAnAdmin(userManager, currentlyLoggedInUser)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You can only view the results of assignments that you own.").toResponse();
            }

            GameboardDTO gameboard = this.gameManager.getGameboard(assignment.getGameboardId());

            List<RegisteredUserDTO> groupMembers = this.groupManager.getUsersInGroup(group);

            List<String> questionPageIds = Lists.newArrayList();
            for (GameboardItem questionPage : gameboard.getQuestions()) {
                questionPageIds.add(questionPage.getId());
            }
            Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>> questionAttempts;
            questionAttempts = this.questionManager.getMatchingQuestionAttempts(groupMembers, questionPageIds);

            Map<RegisteredUserDTO, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>>
                    questionAttemptsForAllUsersOfInterest = new HashMap<>();
            for (RegisteredUserDTO user : groupMembers) {
                questionAttemptsForAllUsersOfInterest.put(user, questionAttempts.get(user.getId()));
            }

            List<String> questionIds = Lists.newArrayList();
            List<String[]> rows = Lists.newArrayList();
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            StringBuilder headerBuilder = new StringBuilder();
            if (null != formatMode && formatMode.toLowerCase().equals("excel")) {
                headerBuilder.append("\uFEFF");  // UTF-8 Byte Order Marker
            }
            headerBuilder.append(String.format("Assignment (%s) Results: Downloaded on %s \nGenerated by: %s %s \n\n",
                    assignmentId, new Date(), currentlyLoggedInUser.getGivenName(),
                    currentlyLoggedInUser.getFamilyName()));

            List<String> headerRow = Lists.newArrayList(Arrays.asList("", ""));
            if (includeUserIDs) {
                headerRow.add("");
            }

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
            if (includeUserIDs) {
                totalsRow.add("");
            }
            Collections.addAll(totalsRow, ",Correct %".split(","));

            Map<RegisteredUserDTO, Map<String, Integer>> userQuestionDataMap = new HashMap<>();

            // FIXME vvv This is duplicated code vvv
            // This is properly horrible, can someone rewrite this whole thing?
            questionAttemptsForAllUsersOfInterest.forEach((user, attempts) -> {
                Map<String, List<LightweightQuestionValidationResponse>> attemptsByQuestionId = new HashMap<>();
                for (String pageId : attempts.keySet()) {
                    Map<String, List<LightweightQuestionValidationResponse>> a = attempts.get(pageId);
                    for (String questionId : a.keySet()) {
                        List l = a.get(questionId);
                        attemptsByQuestionId.put(questionId, l);
                    }
                }
                Map<String, Integer> userAttemptsSummary = attemptsByQuestionId.entrySet().stream().collect(
                        Collectors.toMap(
                                Entry::getKey,
                                e -> e.getValue().stream().map(LightweightQuestionValidationResponse::isCorrect)
                                        .reduce(false, (a, b) -> a || b)
                        )
                ).entrySet().stream().collect(Collectors.toMap(
                        Entry::getKey,
                        e -> e.getValue() ? 1 : 0
                ));
                userQuestionDataMap.put(user, userAttemptsSummary);
            });
            // FIXME ^^^ This is duplicated code ^^^
            userQuestionDataMap.forEach((user, outcome) -> {
                questionIds.forEach(questionId -> {
                    outcome.putIfAbsent(questionId, null);
                });
            });

            List<String[]> resultRows = Lists.newArrayList();
            int[] columnTotals = new int[questionIds.size()];
            for (RegisteredUserDTO user : groupMembers) {
                ArrayList<String> resultRow = Lists.newArrayList();
                UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(currentlyLoggedInUser,
                        userManager.convertToUserSummaryObject(user));

                resultRow.add(userSummary.getFamilyName());
                resultRow.add(userSummary.getGivenName());
                if (includeUserIDs) {
                    resultRow.add(userSummary.getId().toString());
                }
                // can the user access the data?
                if (userSummary.isAuthorisedFullAccess()) {
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
                    for (@SuppressWarnings("unused") String questionId : questionIds) {
                        resultRow.add(NOT_SHARING);
                    }
                }
                Collections.addAll(resultRows, resultRow.toArray(new String[0]));
            }

            this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacServerLogType.DOWNLOAD_ASSIGNMENT_PROGRESS_CSV,
                    ImmutableMap.of("assignmentId", assignmentId));

            // ignore name columns

            for (int i = 0; i < questionIds.size(); i++) {
                double percentageCorrect = ((double) columnTotals[i] / groupMembers.size()) * 100F;
                totalsRow.add(percentageFormat.format(percentageCorrect));
            }

            rows.add(totalsRow.toArray(new String[0]));
            String userInfoHeader = includeUserIDs ? "Last Name,First Name,User ID" : "Last Name,First Name";
            rows.add(userInfoHeader.split(","));
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
     * @param formatMode
     *            - whether to format the file in a special way. Currently only "excel" is supported,
     *            to include a UTF-8 BOM to allow Unicode student names to show correctly in Microsoft Excel.
     * @param request
     *            - so that we can identify the current user.
     * @return the assignment object.
     */
    @GET
    @Path("/assign/group/{group_id}/progress/download")
    @Produces("text/csv")
    @GZIP
    @Consumes(MediaType.WILDCARD)
    @ApiOperation(value = "Download the progress of a group on all assignments set.")
    public Response getGroupAssignmentsProgressDownloadCSV(@Context final HttpServletRequest request,
                                                           @PathParam("group_id") final Long groupId,
                                                           @QueryParam("format") final String formatMode) {

        try {
            // Fetch the currently logged in user
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
            boolean includeUserIDs = isUserAnAdminOrEventManager(userManager, currentlyLoggedInUser);

            // Fetch the requested group
            UserGroupDTO group;
            group = this.groupManager.getGroupById(groupId);

            // Check the user has permission to access this group:
            if (!GroupManager.isOwnerOrAdditionalManager(group, currentlyLoggedInUser.getId())
                    && !isUserAnAdmin(userManager, currentlyLoggedInUser)) {
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
            Map<RegisteredUserDTO, Map<GameboardDTO, Map<String, Integer>>> grandTable = new HashMap<>();
            // Retrieve each user's progress data and cram everything into a Grand Table for later consumption
            List<String> gameboardsIds = assignments.stream().map(AssignmentDTO::getGameboardId).collect(Collectors.toList());
            List<GameboardDTO> gameboards;
            if (gameboardsIds.isEmpty()) {
                gameboards = new ArrayList<>();
            } else {
                gameboards = gameManager.getGameboards(gameboardsIds);
            }
            Map<String, GameboardDTO> gameboardsIdMap = gameboards.stream().collect(Collectors.toMap(GameboardDTO::getId, Function.identity()));

            Map<AssignmentDTO, GameboardDTO> assignmentGameboards = new HashMap<>();
            for (AssignmentDTO assignment : assignments) {
                GameboardDTO gameboard = gameboardsIdMap.get(assignment.getGameboardId());
                // Create an assignment -> gameboard mapping to avoid repeatedly querying the DB later on. All the efficiency!
                assignmentGameboards.put(assignment, gameboard);
            }
            List<GameboardItem> gameboardItems = gameboards.stream().map(GameboardDTO::getQuestions).flatMap(Collection::stream).collect(Collectors.toList());
            List<String> questionPageIds = gameboardItems.stream().map(GameboardItem::getId).collect(Collectors.toList());
            Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>> questionAttempts;
            try {
                questionAttempts = this.questionManager.getMatchingQuestionAttempts(groupMembers, questionPageIds);
            } catch (IllegalArgumentException e) {
                questionAttempts = new HashMap<>();
            }
            Map<RegisteredUserDTO, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>> questionAttemptsForAllUsersOfInterest = new HashMap<>();
            for (RegisteredUserDTO user : groupMembers) {
                questionAttemptsForAllUsersOfInterest.put(user, questionAttempts.get(user.getId()));
            }

            for (GameboardDTO gameboard : gameboards) {
                Map<RegisteredUserDTO, Map<String, Integer>> userQuestionDataMap = new HashMap<>();

                // FIXME vvv This is duplicated code vvv
                // This is properly horrible, can someone rewrite this whole thing?
                questionAttemptsForAllUsersOfInterest.forEach((user, attempts) -> {
                    Map<String, List<LightweightQuestionValidationResponse>> attemptsByQuestionId = new HashMap<>();
                    for (String pageId : attempts.keySet()) {
                        Map<String, List<LightweightQuestionValidationResponse>> a = attempts.get(pageId);
                        for (String questionId : a.keySet()) {
                            List l = a.get(questionId);
                            attemptsByQuestionId.put(questionId, l);
                        }
                    }
                    Map<String, Integer> userAttemptsSummary = attemptsByQuestionId.entrySet().stream().collect(
                            Collectors.toMap(
                                    Entry::getKey,
                                    e -> e.getValue().stream().map(LightweightQuestionValidationResponse::isCorrect)
                                            .reduce(false, (a, b) -> a || b)
                            )
                    ).entrySet().stream().collect(Collectors.toMap(
                            Entry::getKey,
                            e -> e.getValue() ? 1 : 0
                    ));
                    userQuestionDataMap.put(user, userAttemptsSummary);
                });
                // FIXME ^^^ This is duplicated code ^^^
                for (RegisteredUserDTO student : userQuestionDataMap.keySet()) {
                    Map<GameboardDTO, Map<String, Integer>> entry = grandTable.get(student);
                    if (null == entry) {
                        entry = Maps.newHashMap();
                    }
                    entry.put(gameboard, userQuestionDataMap.get(student));
                    grandTable.put(student, entry);
                }
            }

            // Add a header row with due dates
            ArrayList<String> dueDateRow = Lists.newArrayList();
            Collections.addAll(dueDateRow, "", "Due", "");
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");

            ArrayList<String> headerRow = Lists.newArrayList();
            if (includeUserIDs) {
                Collections.addAll(headerRow, "Last Name,First Name,User ID,% Correct Overall".split(","));
            } else {
                Collections.addAll(headerRow, "Last Name,First Name,% Correct Overall".split(","));
            }
            List<String> gameboardTitles = Lists.newArrayList();
            for (AssignmentDTO assignment : assignments) {
                if (null != assignment.getDueDate()) {
                    dueDateRow.add(dateFormatter.format(assignment.getDueDate()));
                } else {
                    dueDateRow.add(""); // No due date set
                }
                GameboardDTO gameboard = assignmentGameboards.get(assignment);
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
            dueDateRow.add("");
            headerRow.add("");

            Map<GameboardDTO, List<String>> gameboardQuestionIds = Maps.newHashMap();
            for (AssignmentDTO assignment : assignments) {
                GameboardDTO gameboard = assignmentGameboards.get(assignment);
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
                        if (null != assignment.getDueDate()) {
                            dueDateRow.add(dateFormatter.format(assignment.getDueDate()));
                        } else {
                            dueDateRow.add(""); // No due date set
                        }
                        headerRow.add(s.toString());
                    }
                }
            }

            // Moving on to actual rows...
            ArrayList<String[]> rows = Lists.newArrayList();
            rows.add(dueDateRow.toArray(new String[0]));
            rows.add(headerRow.toArray(new String[0]));

            for (RegisteredUserDTO groupMember : groupMembers) {
                // FIXME Some room for improvement here, as we can retrieve all the users with a single query.
                // FIXME Not urgent, as the dominating query is the one that retrieves question attempts above.
                UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(currentlyLoggedInUser,
                        userManager.convertToUserSummaryObject(groupMember));

                ArrayList<String> row = Lists.newArrayList();
                Map<GameboardDTO, Map<String, Integer>> userAssignments = grandTable.get(groupMember);
                List<Float> assignmentPercentages = Lists.newArrayList();
                List<Integer> marks = Lists.newArrayList();
                int totalQPartsCorrect = 0;
                int totalQPartsCount = 0;
                for (AssignmentDTO assignment : assignments) {
                    GameboardDTO gameboard = assignmentGameboards.get(assignment);
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
                if (includeUserIDs) {
                    row.add(userSummary.getId().toString());
                }

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

            StringBuilder headerBuilder = new StringBuilder();
            if (null != formatMode && formatMode.toLowerCase().equals("excel")) {
                headerBuilder.append("\uFEFF");  // UTF-8 Byte Order Marker
            }
            headerBuilder.append(String.format("Assignments for '%s' (%s)\nDownloaded on %s\nGenerated by: %s %s\n\n",
                        group.getGroupName(), group.getId(), new Date(), currentlyLoggedInUser.getGivenName(),
                        currentlyLoggedInUser.getFamilyName()))
                    .append(stringWriter.toString())
                    .append("\n\nN.B.\n\"The percentages are for question parts completed, not question pages.\"\n");

            this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacServerLogType.DOWNLOAD_GROUP_PROGRESS_CSV,
                    ImmutableMap.of("groupId", groupId));

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
    @ApiOperation(value = "List all groups assigned boards from a list of boards.",
                  notes = "The list of boards should be comma separated.")
    public Response getAssignedGroupsByGameboards(@Context final HttpServletRequest request,
                                                  @QueryParam("gameboard_ids") final String gameboardIdsQueryParam) {
        try {

            if (null == gameboardIdsQueryParam || gameboardIdsQueryParam.isEmpty()) {
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
    @ApiOperation(value = "Create a new assignment.")
    public Response assignGameBoard(@Context final HttpServletRequest request,
                                    final AssignmentDTO assignmentDTOFromClient) {

        if (assignmentDTOFromClient.getGameboardId() == null || assignmentDTOFromClient.getGroupId() == null) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "A required field was missing. Must provide group and gameboard ids").toResponse();
        }

        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
            UserGroupDTO assigneeGroup = groupManager.getGroupById(assignmentDTOFromClient.getGroupId());

            if (!isUserTeacherOrAbove(userManager, currentlyLoggedInUser)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You need a teacher account to create groups and set assignments!").toResponse();
            }

            if (!isUserStaff(userManager, currentlyLoggedInUser) && (null == assignmentDTOFromClient.getNotes() || !assignmentDTOFromClient.getNotes().isEmpty())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You cannot provide assignment notes.").toResponse();
            }

            if (null == assigneeGroup) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The group id specified does not exist.")
                        .toResponse();
            }

            if (!GroupManager.isOwnerOrAdditionalManager(assigneeGroup, currentlyLoggedInUser.getId())
                    && !isUserAnAdmin(userManager, currentlyLoggedInUser)) {
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
            this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacServerLogType.SET_NEW_ASSIGNMENT, eventDetails);

            this.userBadgeManager.updateBadge(currentlyLoggedInUser,
                    UserBadgeManager.Badge.TEACHER_ASSIGNMENTS_SET, assignmentWithID.getId().toString());

            tagsLoop:
            for (String tag : bookTags) {

                for (GameboardItem item : gameboard.getQuestions()) {
                    if (item.getTags().contains(tag)) {
                        this.userBadgeManager.updateBadge(currentlyLoggedInUser,
                                UserBadgeManager.Badge.TEACHER_BOOK_PAGES_SET, assignmentWithID.getId().toString());
                        break tagsLoop;
                    }
                }
            }

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
    @ApiOperation(value = "Delete an assignment by board ID and group ID.")
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
            if (!assigneeGroup.getOwnerId().equals(currentlyLoggedInUser.getId()) &&
                    !GroupManager.isInAdditionalManagerList(assigneeGroup, currentlyLoggedInUser.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You are not the owner of the group or a manager. Unable to delete it.").toResponse();
            }

            this.assignmentManager.deleteAssignment(assignmentToDelete);

            this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacServerLogType.DELETE_ASSIGNMENT,
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