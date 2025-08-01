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
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.opencsv.CSVWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateAssignmentException;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.services.AssignmentService;
import uk.ac.cam.cl.dtg.isaac.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentProgressDTO;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentStatusDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;
import uk.ac.cam.cl.dtg.util.NameFormatter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager.extractPageIdFromQuestionId;

/**
 * AssignmentFacade
 *
 * This class provides endpoints to support assigning work to users.
 *
 */
@Path("/assignments")
@Tag(name = "AssignmentFacade", description = "/assignments")
public class AssignmentFacade extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(AssignmentFacade.class);

    private final AssignmentManager assignmentManager;
    private final UserAccountManager userManager;
    private final GroupManager groupManager;
    private final GameManager gameManager;
    private final UserAssociationManager associationManager;
    private final QuestionManager questionManager;
    private final AssignmentService assignmentService;
    private final Clock clock;
    private final SimpleDateFormat timestampFormat;
    private final SimpleDateFormat dateFormat;

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
     * @param assignmentService
     *            - for augmenting assignments with assigner information
     * @param clock
     *            - for getting the current time
     */
    @Inject
    public AssignmentFacade(final AssignmentManager assignmentManager, final QuestionManager questionManager,
                            final UserAccountManager userManager, final GroupManager groupManager,
                            final AbstractConfigLoader propertiesLoader, final GameManager gameManager,
                            final ILogManager logManager, final UserAssociationManager associationManager,
                            final AssignmentService assignmentService, final Clock clock) {
        super(propertiesLoader, logManager);
        this.questionManager = questionManager;
        this.userManager = userManager;
        this.gameManager = gameManager;
        this.groupManager = groupManager;
        this.assignmentManager = assignmentManager;
        this.associationManager = associationManager;
        this.assignmentService = assignmentService;
        this.clock = clock;
        this.timestampFormat = new SimpleDateFormat("EEE dd MMM yyyy HH:mm:ss z");
        timestampFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    }

    /**
     * Endpoint that will return a list of boards assigned to the current user.
     *
     * @param request
     *            - so that we can identify the current user.
     * @return List of assignments (maybe empty)
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "List all boards assigned to the current user.")
    public Response getAssignments(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);
            // TODO (scheduled-assignments): push this logic into the manager!
            Collection<AssignmentDTO> assignments = this.assignmentManager.getAssignments(currentlyLoggedInUser)
                    .stream().filter(a -> a.scheduledStartDateIsBefore(Date.from(Instant.now(clock))))
                    .collect(Collectors.toList());

            // Gather all gameboards we need to augment for the assignments in a single query
            List<String> gameboardIds = assignments.stream().map(AssignmentDTO::getGameboardId).collect(Collectors.toList());
            Map<String, GameboardDTO> gameboardsMap = this.gameManager.getGameboardsWithAttempts(gameboardIds, currentlyLoggedInUser)
                    .stream().collect(Collectors.toMap(GameboardDTO::getId, Function.identity()));

            // we want to populate gameboard details for the assignment DTO.
            List<Long> groupIds = assignments.stream().map(AssignmentDTO::getGroupId).distinct().collect(Collectors.toList());
            List<UserGroupDTO> groups = groupManager.getGroupsByIds(groupIds, false);
            Map<Long, String> groupNameMap = groups.stream().collect(Collectors.toMap(UserGroupDTO::getId, NameFormatter::getFilteredGroupNameFromGroup));

            for (AssignmentDTO assignment : assignments) {
                assignment.setGameboard(gameboardsMap.get(assignment.getGameboardId()));
                assignment.setGroupName(groupNameMap.get(assignment.getGroupId()));
            }

            this.assignmentService.augmentAssignerSummaries(assignments);

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
     * Allows the user to fetch a single assignment that they own (or are a group manager of). Returned assignment
     * will contain gameboard and question information.
     *
     * @param request - so that we can identify the current user.
     * @param assignmentId - id of assignment to fetch
     * @return AssignmentDTO containing extra information about the gameboard and questions
     */
    @GET
    @Path("/assign/{assignmentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Fetch an assignment object populated with gameboard and question information.")
    public Response getSingleAssigned(@Context final HttpServletRequest request,
                                @PathParam("assignmentId") final Long assignmentId) {
        if (null == assignmentId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Please specify an assignment id to search for.").toResponse();
        }
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            AssignmentDTO assignment = this.assignmentManager.getAssignmentById(assignmentId);
            if (null == assignment) {
                return new SegueErrorResponse(Status.NOT_FOUND, String.format("Assignment with id %d not found.", assignmentId)).toResponse();
            }
            UserGroupDTO group = this.groupManager.getGroupById(assignment.getGroupId());
            if (null == group) {
                return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while locating the specified assignment.").toResponse();
            }

            if (!GroupManager.isOwnerOrAdditionalManager(group, currentlyLoggedInUser.getId())
                    && !isUserAnAdmin(userManager, currentlyLoggedInUser)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are not the owner or manager of this assignment.").toResponse();
            }

            // In order to get all the information about parts and pass marks, which is needed for Assignment Progress,
            // we need to use the method which augments a gameboard with user attempt information.
            // But we don't _want_ the attempt information itself for real, so we won't load it from the database:
            Map<String, Map<String, List<QuestionValidationResponse>>> fakeQuestionAttemptMap = new HashMap<>();

            assignment.setGameboard(this.gameManager.getGameboard(assignment.getGameboardId(), currentlyLoggedInUser, fakeQuestionAttemptMap));

            return Response.ok(assignment)
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error while locating the specified assignment.").toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while populating the specified assignment object.").toResponse();
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
    @Operation(summary = "List all assignments set or managed by the current user if no group param specified.")
    public Response getAssigned(@Context final HttpServletRequest request,
                                @QueryParam("group") final Long groupIdOfInterest) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            if (null == groupIdOfInterest) {
                List<UserGroupDTO> allGroupsOwnedAndManagedByUser = this.groupManager.getAllGroupsOwnedAndManagedByUser(currentlyLoggedInUser, false);
                List<AssignmentDTO> assignments = this.assignmentManager.getAllAssignmentsForSpecificGroups(allGroupsOwnedAndManagedByUser, true);
                Set<String> gameboardIds = assignments.stream().map(AssignmentDTO::getGameboardId).collect(Collectors.toSet());
                List<GameboardDTO> liteGameboards = this.gameManager.getLiteGameboards(gameboardIds);
                // Remove unneeded data from the gameboards - very messy but not as messy as feasible alternatives
                liteGameboards.forEach(g -> {
                    g.setContents(null);
                    g.setWildCard(null);
                    g.setWildCardPosition(null);
                    g.setGameFilter(null);
                    g.setLastVisited(null);
                    g.setPercentageAttempted(null);
                    g.setPercentageCorrect(null);
                    g.setCreationMethod(null);
                    g.setCreationDate(null);
                });
                Map<String, GameboardDTO> liteGameboardLookup = liteGameboards.stream().collect(Collectors.toMap(GameboardDTO::getId, Function.identity()));
                // Add lightweight gameboard to each assignment
                assignments.forEach(a -> a.setGameboard(liteGameboardLookup.get(a.getGameboardId())));
                // TODO perhaps augment the assignments with assigner information if the assigner isn't the current user
                return Response.ok(assignments).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
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

                // In order to get all the information about parts and pass marks, which is needed for Assignment Progress,
                // we need to use the method which augments gameboards with user attempt information.
                // But we don't _want_ the attempt information itself for real, so we won't load it from the database:
                Map<String, Map<String, List<QuestionValidationResponse>>> fakeQuestionAttemptMap = new HashMap<>();

                // we want to populate gameboard details for the assignment DTO.
                List<String> gameboardIDs = allAssignmentsSetToGroup.stream().map(AssignmentDTO::getGameboardId).collect(Collectors.toList());
                Map<String, GameboardDTO> gameboards = this.gameManager.getGameboards(gameboardIDs, fakeQuestionAttemptMap)
                        .stream().collect(Collectors.toMap(GameboardDTO::getId, Function.identity()));
                for (AssignmentDTO assignment : allAssignmentsSetToGroup) {
                    assignment.setGameboard(gameboards.get(assignment.getGameboardId()));
                    assignment.setGroupName(group.getGroupName());
                }

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
    @Operation(summary = "View the progress of a specific assignment.")
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
            List<AssignmentProgressDTO> result = new ArrayList<>(groupMembers.size());

            if (gameboard.getContents().isEmpty()) {
                return new SegueErrorResponse(Status.NOT_FOUND, "Assignment gameboard has no questions, or its questions no longer exist. Cannot fetch assignment progress.").toResponse();
            }

            for (ImmutablePair<RegisteredUserDTO, List<GameboardItem>> userGameboardItems : this.gameManager
                    .gatherGameProgressData(groupMembers, gameboard)) {
                UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(currentlyLoggedInUser,
                        userManager.convertToUserSummaryObject(userGameboardItems.getLeft()));

                // can the user access the data?
                if (userSummary.isAuthorisedFullAccess()) {
                    ArrayList<CompletionState> questionStates = Lists.newArrayList();
                    ArrayList<List<QuestionPartState>> questionPartStates = Lists.newArrayList();
                    ArrayList<Integer> correctQuestionParts = Lists.newArrayList();
                    ArrayList<Integer> incorrectQuestionParts = Lists.newArrayList();
                    for (GameboardItem questionResult : userGameboardItems.getRight()) {
                        questionStates.add(questionResult.getState());
                        questionPartStates.add(questionResult.getQuestionPartStates());
                        correctQuestionParts.add(questionResult.getQuestionPartsCorrect());
                        incorrectQuestionParts.add(questionResult.getQuestionPartsIncorrect());
                    }
                    result.add(new AssignmentProgressDTO(
                            userSummary,
                            correctQuestionParts,
                            incorrectQuestionParts,
                            questionStates,
                            questionPartStates
                    ));
                } else {
                    result.add(new AssignmentProgressDTO(
                            userSummary,
                            null,
                            null,
                            null,
                            null
                    ));
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
    @Operation(summary = "Download the progress of a specific assignment.")
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
            for (GameboardItem questionPage : gameboard.getContents()) {
                questionPageIds.add(questionPage.getId());
            }
            Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>> questionAttempts;
            questionAttempts = this.questionManager.getMatchingLightweightQuestionAttempts(groupMembers, questionPageIds);

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
                    assignmentId, timestampFormat.format(Date.from(Instant.now(clock))), currentlyLoggedInUser.getGivenName(),
                    currentlyLoggedInUser.getFamilyName()));

            List<String> headerRow = Lists.newArrayList(Arrays.asList("", ""));
            if (includeUserIDs) {
                headerRow.add("");
            }

            DecimalFormat percentageFormat = new DecimalFormat("###");

            for (GameboardItem questionPage : gameboard.getContents()) {
                int index = 0;

                for (Question question : gameManager.getAllMarkableDOQuestionPartsDFSOrder(questionPage.getId())) {
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
                        List<LightweightQuestionValidationResponse> l = a.get(questionId);
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
            userQuestionDataMap.forEach((user, outcome) ->
                    questionIds.forEach(questionId -> outcome.putIfAbsent(questionId, null)));

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
    @Operation(summary = "Download the progress of a group on all assignments set.")
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
            assignments = this.assignmentManager.getAllAssignmentsForSpecificGroups(Collections.singletonList(group), false);

            // Fetch the members of the requested group
            List<RegisteredUserDTO> groupMembers;
            groupMembers = this.groupManager.getUsersInGroup(group);

            // String: question part id
            // Integer: question part result
            Map<RegisteredUserDTO, Map<GameboardDTO, Map<String, Integer>>> grandTable = new HashMap<>();
            // Retrieve each user's progress data and cram everything into a Grand Table for later consumption
            List<String> gameboardsIds = assignments.stream().map(AssignmentDTO::getGameboardId).collect(Collectors.toList());
            List<GameboardDTO> gameboards = gameManager.getGameboards(gameboardsIds);

            Map<String, GameboardDTO> gameboardsIdMap = gameboards.stream().collect(Collectors.toMap(GameboardDTO::getId, Function.identity()));

            Map<AssignmentDTO, GameboardDTO> assignmentGameboards = new HashMap<>();
            for (AssignmentDTO assignment : assignments) {
                GameboardDTO gameboard = gameboardsIdMap.get(assignment.getGameboardId());
                // Create an assignment -> gameboard mapping to avoid repeatedly querying the DB later on. All the efficiency!
                assignmentGameboards.put(assignment, gameboard);
            }
            List<GameboardItem> gameboardItems = gameboards.stream().map(GameboardDTO::getContents).flatMap(Collection::stream).collect(Collectors.toList());
            List<String> questionPageIds = gameboardItems.stream().map(GameboardItem::getId).collect(Collectors.toList());
            Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>> questionAttempts;
            try {
                questionAttempts = this.questionManager.getMatchingLightweightQuestionAttempts(groupMembers, questionPageIds);
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
                            List<LightweightQuestionValidationResponse> l = a.get(questionId);
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

            ArrayList<String> headerRow = Lists.newArrayList();
            if (includeUserIDs) {
                Collections.addAll(headerRow, "Last Name,First Name,User ID,% Correct Overall".split(","));
            } else {
                Collections.addAll(headerRow, "Last Name,First Name,% Correct Overall".split(","));
            }
            List<String> gameboardTitles = Lists.newArrayList();
            for (AssignmentDTO assignment : assignments) {
                if (null != assignment.getDueDate()) {
                    dueDateRow.add(dateFormat.format(assignment.getDueDate()));
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
                for (GameboardItem questionPage : gameboard.getContents()) {
                    int b = 1;
                    for (Question question : gameManager.getAllMarkableDOQuestionPartsDFSOrder(questionPage.getId())) {
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
                            dueDateRow.add(dateFormat.format(assignment.getDueDate()));
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
                    List<GameboardItem> questions = gameboard.getContents();
                    Map<String, Integer> gameboardPartials = Maps.newHashMap();
                    for (GameboardItem question : questions) {
                        gameboardPartials.put(question.getId(), 0);
                    }
                    HashMap<String, Integer> questionParts = new HashMap<>(gameboardPartials);
                    for (String s : questionIds) {
                        Integer mark = userAssignments.get(gameboard).get(s);
                        String questionPageId = extractPageIdFromQuestionId(s);
                        questionParts.put(questionPageId, questionParts.get(questionPageId) + 1);
                        marks.add(mark);
                        if (null != mark) {
                            gameboardPartials.put(questionPageId, gameboardPartials.get(questionPageId) + mark);
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
                        group.getGroupName(), group.getId(), timestampFormat.format(Date.from(Instant.now(clock))),
                            currentlyLoggedInUser.getGivenName(), currentlyLoggedInUser.getFamilyName()))
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
     * Allows a user to assign a gameboard to one or more groups of users. We assume that each partial AssignmentDTO object has
     * the same gameboardId, notes and dueDate to make validation easier, but this could be changed in theory, given a more
     * flexible front-end.
     *
     * @param request
     *            - so that we can identify the current user.
     * @param assignmentDTOsFromClient a list of partially completed DTO(s) for the assignment(s).
     * @return a list of ids of successful assignments, and a list of failed (an AssignmentSettingResponseDTO)
     */
    @POST
    @Path("/assign_bulk")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Create one or more new assignment(s).")
    public Response assignGameBoards(@Context final HttpServletRequest request,
                                    final List<AssignmentDTO> assignmentDTOsFromClient) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            // Assert user is allowed to set assignments - tutors and above are allowed to do so
            boolean userIsTutorOrAbove = isUserTutorOrAbove(userManager, currentlyLoggedInUser);
            boolean userIsStaffOrEventLeader = isUserStaffOrEventLeader(userManager, currentlyLoggedInUser);
            if (!userIsTutorOrAbove) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You need a tutor or teacher account to create groups and set assignments!").toResponse();
            }

            // Assert that there is at least one assignment, and that multiple assignments are only set by staff
            if (assignmentDTOsFromClient.isEmpty()) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You need to specify at least one assignment to set.").toResponse();
            }

            List<AssignmentStatusDTO> assigmentStatuses = new ArrayList<>();
            Map<String, GameboardDTO> gameboardMap = new HashMap<>();
            Map<Long, UserGroupDTO> groupMap = groupManager.getGroupsByIds(
                    assignmentDTOsFromClient.stream().map(AssignmentDTO::getGroupId).collect(Collectors.toList()),
                    true
                ).stream().collect(Collectors.toMap(UserGroupDTO::getId, Function.identity()));

            for (AssignmentDTO assignmentDTO : assignmentDTOsFromClient) {
                if (null == assignmentDTO.getGameboardId() || null == assignmentDTO.getGroupId()) {
                    assigmentStatuses.add(new AssignmentStatusDTO(assignmentDTO.getGroupId(), "A required field was missing. Must provide gameboard id and group id."));
                    continue;
                }

                // Staff and event leaders can set assignment notes up to a max length of MAX_NOTE_CHAR_LENGTH,
                // teachers cannot set notes.
                boolean notesIsNullOrEmpty = null == assignmentDTO.getNotes() || assignmentDTO.getNotes().isEmpty();
                if (userIsStaffOrEventLeader) {
                    boolean notesIsTooLong = null != assignmentDTO.getNotes() && assignmentDTO.getNotes().length() > MAX_NOTE_CHAR_LENGTH;
                    if (notesIsTooLong) {
                        assigmentStatuses.add(new AssignmentStatusDTO(assignmentDTO.getGroupId(), "Your assignment notes exceed the maximum allowed length of "
                                + MAX_NOTE_CHAR_LENGTH + " characters."));
                        continue;
                    }
                } else if (!notesIsNullOrEmpty) {
                    assigmentStatuses.add(new AssignmentStatusDTO(assignmentDTO.getGroupId(), "You are not allowed to add assignment notes."));
                    continue;
                }

                if (null != assignmentDTO.getDueDate() && !assignmentDTO.dueDateIsAfter(Date.from(Instant.now(clock)))) {
                    assigmentStatuses.add(new AssignmentStatusDTO(assignmentDTO.getGroupId(), "The assignment cannot be due in the past."));
                    continue;
                }

                if (null != assignmentDTO.getScheduledStartDate()) {
                    Date oneYearInFuture = DateUtils.addYears(Date.from(Instant.now(clock)), 1);
                    if (assignmentDTO.getScheduledStartDate().after(oneYearInFuture)) {
                        assigmentStatuses.add(new AssignmentStatusDTO(assignmentDTO.getGroupId(), "The assignment cannot be scheduled to begin more than one year in the future."));
                        continue;
                    }
                    if (null != assignmentDTO.getDueDate() && !assignmentDTO.dueDateIsAfter(assignmentDTO.getScheduledStartDate())) {
                        assigmentStatuses.add(new AssignmentStatusDTO(assignmentDTO.getGroupId(), "The assignment cannot be scheduled to begin after it is due."));
                        continue;
                    }
                    // If the assignment will have started in the next hour (meaning it might miss the related emails
                    // being scheduled), then remove it so that the assignment is set immediately.
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(Date.from(Instant.now(clock)));
                    cal.add(Calendar.HOUR_OF_DAY, 1);
                    if (assignmentDTO.getScheduledStartDate().before(cal.getTime())) {
                        assignmentDTO.setScheduledStartDate(null);
                    }
                }

                try {
                    // Get the gameboard:
                    // The `computeIfAbsent` Map function won't work because of checked SegueDatabaseException (for getGameboard/getGroupById)
                    GameboardDTO gameboard = gameboardMap.get(assignmentDTO.getGameboardId());
                    if (null == gameboard) {
                        gameboard = this.gameManager.getGameboard(assignmentDTO.getGameboardId());
                        if (null == gameboard) {
                            assigmentStatuses.add(new AssignmentStatusDTO(assignmentDTO.getGroupId(), "The gameboard id specified does not exist."));
                            continue;
                        }
                        gameboardMap.put(gameboard.getId(), gameboard);
                    }
                    assignmentDTO.setGameboard(gameboard);

                    // Get the group:
                    UserGroupDTO assigneeGroup = groupMap.get(assignmentDTO.getGroupId());
                    if (null == assigneeGroup) {
                        assigmentStatuses.add(new AssignmentStatusDTO(assignmentDTO.getGroupId(), "The group id specified does not exist."));
                        continue;
                    }
                    if (!GroupManager.isOwnerOrAdditionalManager(assigneeGroup, currentlyLoggedInUser.getId())
                            && !isUserAnAdmin(userManager, currentlyLoggedInUser)) {
                        assigmentStatuses.add(new AssignmentStatusDTO(assignmentDTO.getGroupId(), "You can only set assignments to groups you own or manage."));
                        continue;
                    }

                    assignmentDTO.setOwnerUserId(currentlyLoggedInUser.getId());
                    assignmentDTO.setAssignerSummary(userManager.convertToUserSummaryObject(currentlyLoggedInUser));
                    assignmentDTO.setCreationDate(null);
                    assignmentDTO.setId(null);

                    // modifies assignment passed in to include an id.
                    AssignmentDTO assignmentWithID = this.assignmentManager.createAssignment(assignmentDTO);

                    LinkedHashMap<String, Object> eventDetails = new LinkedHashMap<>();
                    eventDetails.put(Constants.GAMEBOARD_ID_FKEY, assignmentWithID.getGameboardId());
                    eventDetails.put(GROUP_FK, assignmentWithID.getGroupId());
                    eventDetails.put(ASSIGNMENT_FK, assignmentWithID.getId());
                    eventDetails.put(ASSIGNMENT_DUEDATE, assignmentWithID.getDueDate());
                    eventDetails.put(ASSIGNMENT_SCHEDULED_START_DATE, assignmentWithID.getScheduledStartDate());
                    this.getLogManager().logEvent(currentlyLoggedInUser, request, IsaacServerLogType.SET_NEW_ASSIGNMENT, eventDetails);

                    // Assigning to this group was a success
                    assigmentStatuses.add(new AssignmentStatusDTO(assignmentWithID.getGroupId(), assignmentWithID.getId()));
                } catch (DuplicateAssignmentException e) {
                    assigmentStatuses.add(new AssignmentStatusDTO(assignmentDTO.getGroupId(), e.getMessage()));
                } catch (SegueDatabaseException e) {
                    log.error("Database error while trying to assign work", e);
                    assigmentStatuses.add(new AssignmentStatusDTO(assignmentDTO.getGroupId(), "Unknown database error."));
                }
            }
            return Response.ok(assigmentStatuses).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to assign work", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error assigning work to groups.").toResponse();
        }
    }

    /**
     * Allows a user to assign a gameboard to group of users.
     *
     * @param request
     *            - so that we can identify the current user.
     * @param assignmentDTOFromClient a partially completed DTO for the assignment.
     * @return an AssignmentSettingResponseDTO (see assignGameBoards)
     */
    @POST
    @Path("/assign/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Deprecated
    @Operation(summary = "Create a new assignment.")
    public Response assignGameBoard(@Context final HttpServletRequest request,
                                    final AssignmentDTO assignmentDTOFromClient) {
        return this.assignGameBoards(request, Collections.singletonList(assignmentDTOFromClient));
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
    @Operation(summary = "Delete an assignment by board ID and group ID.")
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
            if (!GroupManager.isOwnerOrAdditionalManager(assigneeGroup, currentlyLoggedInUser.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You are not the owner of the group or a manager. Unable to delete assignment.").toResponse();
            }

            // Check if user is additional manager, and if so if they are either the creator of the assignment or additional
            // manager privileges are enabled
            if (!assignmentToDelete.getOwnerUserId().equals(currentlyLoggedInUser.getId())
                    && !GroupManager.hasAdditionalManagerPrivileges(assigneeGroup, currentlyLoggedInUser.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You do not have permission to delete this assignment. Unable to delete it.").toResponse();
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
