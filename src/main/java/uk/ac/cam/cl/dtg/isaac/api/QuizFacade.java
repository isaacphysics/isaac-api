/*
 * Copyright 2021 Raspberry Pi Foundation
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

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.DoNotCall;
import com.google.inject.Inject;
import com.opencsv.CSVWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentCancelledException;
import uk.ac.cam.cl.dtg.isaac.api.managers.AttemptCompletedException;
import uk.ac.cam.cl.dtg.isaac.api.managers.DueBeforeNowException;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateAssignmentException;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAssignmentManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizAttemptManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizQuestionManager;
import uk.ac.cam.cl.dtg.isaac.api.services.AssignmentService;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizSectionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptFeedbackDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizFeedbackDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizUserFeedbackDTO;
import uk.ac.cam.cl.dtg.segue.api.ErrorResponseWrapper;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.Response.Status;
import static jakarta.ws.rs.core.Response.ok;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager.extractPageIdFromQuestionId;

/**
 * Quiz Facade
 */
@Path("/quiz")
@Tag(name = "/quiz")
public class QuizFacade extends AbstractIsaacFacade {
    private final GitContentManager contentManager;
    private final QuizManager quizManager;
    private final UserAccountManager userManager;
    private final UserAssociationManager associationManager;
    private final GroupManager groupManager;
    private final QuizAssignmentManager quizAssignmentManager;
    private final AssignmentService assignmentService;
    private final QuizAttemptManager quizAttemptManager;
    private final QuizQuestionManager quizQuestionManager;

    private static final Logger log = LoggerFactory.getLogger(QuizFacade.class);

    /**
     * QuizFacade. For management of quizzes
     * @param properties
     *            - global properties map
     * @param logManager
     *            - for managing logs.
     * @param contentManager
     *            - for the content etag
     * @param quizManager
     *            - for quiz interaction.
     * @param userManager
     *            - for user information.
     * @param associationManager
     *            - So that we can determine what information is allowed to be seen by other users.
     * @param groupManager
     *            - for group information.
     * @param quizAssignmentManager
     *            - for managing the assignment of quizzes.
     * @param assignmentService
     *            - for general assignment-related services.
     * @param quizAttemptManager
     *            - for managing attempts at quizzes.
     * @param quizQuestionManager
     *            - for parsing, validating, and persisting quiz question answers.
     */
    @Inject
    public QuizFacade(final PropertiesLoader properties, final ILogManager logManager,
                      final GitContentManager contentManager, final QuizManager quizManager,
                      final UserAccountManager userManager, final UserAssociationManager associationManager,
                      final GroupManager groupManager, final QuizAssignmentManager quizAssignmentManager,
                      final AssignmentService assignmentService, final QuizAttemptManager quizAttemptManager,
                      final QuizQuestionManager quizQuestionManager) {
        super(properties, logManager);

        this.contentManager = contentManager;
        this.quizManager = quizManager;
        this.userManager = userManager;
        this.associationManager = associationManager;
        this.groupManager = groupManager;
        this.quizAssignmentManager = quizAssignmentManager;
        this.assignmentService = assignmentService;
        this.quizAttemptManager = quizAttemptManager;
        this.quizQuestionManager = quizQuestionManager;
    }

    /**
     * Get quizzes visible to this user, starting from index 0.
     *
     * Anonymous users can't see quizzes.
     *
     * @return a Response containing a list of ContentSummaryDTO for the visible quizzes.
     */
    @GET
    @Path("/available")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get tests visible to this user, from index 0.")
    public final Response getAvailableQuizzes(@Context final Request request, @Context final HttpServletRequest httpServletRequest) {
        return getAvailableQuizzes(request, httpServletRequest, 0);
    }

    /**
     * Get quizzes visible to this user, starting from the specified index.
     *
     * Anonymous users can't see quizzes.
     *
     * @param request the Request needed for ETag checking.
     * @param httpServletRequest the Request needed for Cookies for the current user.
     * @param startIndex for pagination.
     * @return a Response containing a list of ContentSummaryDTO for the visible quizzes.
     */
    @GET
    @Path("/available/{startIndex}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get tests visible to this user, from the specified index.")
    public final Response getAvailableQuizzes(@Context final Request request,
                                              @Context final HttpServletRequest httpServletRequest,
                                              @PathParam("startIndex") final Integer startIndex) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            // Tutors cannot see quizzes that are invisible to students
            boolean showOnlyStudentVisibleQuizzes = !isUserTeacherOrAbove(userManager, user);
            String userRoleString = user.getRole().name();

            // Cache the list of quizzes based on current content version, user's role, and startIndex:
            int etagValue = this.contentManager.getCurrentContentSHA().hashCode() + user.getRole().hashCode();
            if (null != startIndex) {
                etagValue += startIndex;
            }
            EntityTag etag = new EntityTag(etagValue + "");
            Response cachedResponse = generateCachedResponse(request, etag, NEVER_CACHE_WITHOUT_ETAG_CHECK);

            if (cachedResponse != null) {
                return cachedResponse;
            }

            // FIXME: ** HARD-CODED DANGER AHEAD **
            // The limit parameter in the following call is hard-coded and should be returned to a more reasonable
            // number once we have a front-end pagination/load-more system in place.
            ResultsWrapper<ContentSummaryDTO> summary = this.quizManager.getAvailableQuizzes(showOnlyStudentVisibleQuizzes, userRoleString, startIndex, 9000);

            return ok(summary).tag(etag)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                .build();
        } catch (ContentManagerException e) {
            String message = "ContentManagerException whilst getting available tests";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }


    /**
     * Get quizzes assigned to this user.
     *
     * Shows a content summary, so we can track when a user actually attempts a quiz.
     *
     * @return a Response containing a list of QuizAssignmentDTO for the assigned quizzes.
     */
    @GET
    @Path("/assignments")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get tests assigned to this user.")
    public final Response getAssignedQuizzes(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            List<QuizAssignmentDTO> assignments = this.quizAssignmentManager.getAssignedQuizzes(user);

            this.assignmentService.augmentAssignerSummaries(assignments);

            quizManager.augmentWithQuizSummary(assignments);

            this.quizAttemptManager.augmentAssignmentsFor(user, assignments);

            return Response.ok(assignments)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst getting available tests";
            log.error(message, e);
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Get quizzes freely attempted by this user.
     *
     * Shows a content summary, so we can track when a user actually attempts a quiz.
     *
     * @return a Response containing a list of QuizAttemptDTO for the freely attempted quizzes.
     */
    @GET
    @Path("/free_attempts")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get tests freely attempted by this user.")
    public final Response getFreeAttempts(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            List<QuizAttemptDTO> attempts = this.quizAttemptManager.getFreeAttemptsFor(user);

            quizManager.augmentWithQuizSummary(attempts);

            return Response.ok(attempts)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst getting available tests";
            log.error(message, e);
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Preview a quiz. Only available to tutors and above.
     *
     * @param request
     *            - so we can deal with caching.
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizId
     *            as a string
     * @return a Response containing a list of ContentSummaryDTO for the visible quizzes.
     */
    @GET
    @Path("/{quizId}/preview")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Preview an individual test.")
    public final Response previewQuiz(@Context final Request request,
                                      @Context final HttpServletRequest httpServletRequest,
                                      @PathParam("quizId") final String quizId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            // Tutors are able to preview student tests (but not set them)
            if (!(isUserTutorOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            if (null == quizId || quizId.isEmpty()) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid test id.").toResponse();
            }

            EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + quizId.hashCode() + "");

            Response cachedResponse = generateCachedResponse(request, etag);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            IsaacQuizDTO quiz = this.quizManager.findQuiz(quizId);

            // Check this user is actually allowed to preview this quiz. A tutor counts as both a student and teacher
            // in this check
            if (null != quiz.getHiddenFromRoles() && (quiz.getHiddenFromRoles().contains(user.getRole().name())
                    || (quiz.getHiddenFromRoles().contains(Role.STUDENT.name()) && user.getRole() == Role.TUTOR)
                    || (quiz.getHiddenFromRoles().contains(Role.TEACHER.name()) && user.getRole() == Role.TUTOR))
            ) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            return ok(quiz)
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, false)).tag(etag).build();
        } catch (ContentManagerException e) {
            log.error("Content error whilst previewing a test", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This test has become unavailable.");
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Start a quiz attempt for a particular quiz assignment.
     *
     * For a student, indicates the quiz is being attempted and creates a QuizAttemptDO in the DB
     * if they are a member of the group for the assignment.
     * If an attempt has already begun, return the already begun attempt.
     *
     * @see #startFreeQuizAttempt An endpoint to allow a quiz that is visibleToStudents to be taken by students.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAssignmentId
     *            - the ID of the quiz assignment for this user.
     * @return a QuizAttemptDTO
     */
    @POST
    @Path("/assignment/{quizAssignmentId}/attempt")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Start a test attempt.")
    public final Response startQuizAttempt(@Context final Request request,
                                           @Context final HttpServletRequest httpServletRequest,
                                           @PathParam("quizAssignmentId") final Long quizAssignmentId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (null == quizAssignmentId) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid test assignment id.").toResponse();
            }

            // Get the quiz assignment
            QuizAssignmentDTO quizAssignment = this.quizAssignmentManager.getById(quizAssignmentId);

            // Check the user is an active member of the relevant group
            UserGroupDTO group = quizAssignmentManager.getGroupForAssignment(quizAssignment);
            if (!groupManager.isUserInGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are not a member of a group to which this test is assigned.").toResponse();
            }

            // Check the due date hasn't passed
            if (quizAssignment.getDueDate() != null && !quizAssignment.dueDateIsAfter(new Date())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "The due date for this test has passed.").toResponse();
            }

            // Create a quiz attempt
            QuizAttemptDTO quizAttempt = this.quizAttemptManager.fetchOrCreate(quizAssignment, user);

            quizAttempt = augmentAttempt(quizAttempt, quizAssignment, false);

            return Response.ok(quizAttempt)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                .build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst starting a test attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (AttemptCompletedException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "You have already completed your attempt for this test.").toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.GONE, "This test assignment has been cancelled.").toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst starting a test attempt", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This test has become unavailable.");
        }
    }

    /**
     * Start a quiz attempt for a free quiz (one that is visibleToStudents).
     *
     * This checks that quiz has not already been assigned. (When a quiz has been set to a student,
     * they are locked out of all previous feedback for that quiz and prevented from starting the
     * quiz freely in order to prevent some ways of cheating.)
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizId
     *            - the ID of the quiz the user wishes to attempt.
     * @return a QuizAttemptDTO
     */
    @POST
    @Path("/{quizId}/attempt")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Start a free test attempt.")
    public final Response startFreeQuizAttempt(@Context final Request request,
                                               @Context final HttpServletRequest httpServletRequest,
                                               @PathParam("quizId") final String quizId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (null == quizId || quizId.isEmpty()) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid test id.").toResponse();
            }

            // Get the quiz
            IsaacQuizDTO quiz = quizManager.findQuiz(quizId);

            // TODO: Remove this deprecated check:
            if (!quiz.getVisibleToStudents()) {
                return new SegueErrorResponse(Status.FORBIDDEN, "Free attempts are not available for test quiz.").toResponse();
            }
            // Check it is visible to this user's role:
            if (null != quiz.getHiddenFromRoles() && quiz.getHiddenFromRoles().contains(user.getRole().name())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "Free attempts are not available for test quiz.").toResponse();
            }

            // Check if there is an active assignment of this quiz
            List<QuizAssignmentDTO> activeQuizAssignments = this.quizAssignmentManager.getActiveQuizAssignments(quiz, user);

            if (!activeQuizAssignments.isEmpty()) {
                return new SegueErrorResponse(Status.FORBIDDEN, "This test has been assigned to you by a teacher. "
                        + "You can not attempt this test freely. If you have already done the test for your teacher, "
                        + "and want to do it again, ask your teacher to allow you another attempt.").toResponse();
            }

            // Create a quiz attempt
            QuizAttemptDTO quizAttempt = this.quizAttemptManager.fetchOrCreateFreeQuiz(quiz, user);

            quizAttempt = augmentAttempt(quizAttempt, null, false);

            return Response.ok(quizAttempt)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                .build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst starting a free test attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst starting a free test attempt", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This test has become unavailable.");
        }
    }

    /**
     * Get the QuizDTO for a quiz attempt.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAttemptId
     *            - the ID of the quiz the user wishes to attempt.
     * @return The QuizDTO augmented with the user answers so far.
     */
    @GET
    @Path("/attempt/{quizAttemptId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get the QuizDTO for a test attempt.")
    public final Response getQuizAttempt(@Context final HttpServletRequest httpServletRequest,
                                         @PathParam("quizAttemptId") final Long quizAttemptId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            QuizAttemptDTO quizAttempt = getIncompleteQuizAttemptForUser(quizAttemptId, user);

            QuizAssignmentDTO quizAssignment = checkQuizAssignmentNotCancelledOrOverdue(quizAttempt);

            quizAttempt = augmentAttempt(quizAttempt, quizAssignment, false);

            return Response.ok(quizAttempt)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                .build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst getting test attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "This test assignment has been cancelled.").toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst getting test attempt", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This test has become unavailable.");
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        }
    }

    /**
     * Get the feedback for a quiz attempt.
     *
     * The attempt must be completed.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAttemptId
     *            - the ID of the quiz the user wishes to see feedback for.
     * @return The feedback if this user is allowed to see it.
     */
    @GET
    @Path("/attempt/{quizAttemptId}/feedback")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get the feedback for a test attempt.")
    public final Response getQuizAttemptFeedback(@Context final HttpServletRequest httpServletRequest,
                                                 @PathParam("quizAttemptId") final Long quizAttemptId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            QuizAttemptDTO quizAttempt = getCompleteQuizAttemptForUser(quizAttemptId, user);

            QuizAssignmentDTO quizAssignment = getQuizAssignment(quizAttempt);

            IsaacQuizDTO quiz = quizManager.findQuiz(quizAttempt.getQuizId());

            QuizFeedbackMode feedbackMode;

            if (quizAssignment != null) {
                feedbackMode = quizAssignment.getQuizFeedbackMode();
            } else {
                feedbackMode = quiz.getDefaultFeedbackMode();
                if (feedbackMode == null) {
                    feedbackMode = QuizFeedbackMode.DETAILED_FEEDBACK;
                }
            }

            quizAttempt = quizQuestionManager.augmentFeedbackFor(quizAttempt, quiz, feedbackMode);

            augmentAttempt(quizAttempt, quizAssignment);

            return Response.ok(quizAttempt)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                .build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst getting test attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "This test assignment has been cancelled.").toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst getting test attempt", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This test has become unavailable.");
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        }
    }

    /**
     * Mark a QuizAttempt as complete.
     *
     * Only the user taking the quiz can mark it complete.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAttemptId
     *            - the ID of the quiz the user wishes to attempt.
     * @return The updated quiz attempt or an error.
     */
    @POST
    @Path("/attempt/{quizAttemptId}/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Mark a QuizAttempt as complete.")
    public final Response completeQuizAttempt(@Context final HttpServletRequest httpServletRequest,
                                              @PathParam("quizAttemptId") final Long quizAttemptId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            QuizAttemptDTO quizAttempt = getQuizAttempt(quizAttemptId);

            if (!quizAttempt.getUserId().equals(user.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You cannot complete someone else's test.").toResponse();
            }

            if ((quizAttempt.getCompletedDate() != null)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "That test is already complete.").toResponse();
            }

            quizAttempt = quizAttemptManager.updateAttemptCompletionStatus(quizAttempt, true);

            return Response.ok(quizAttempt).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst marking test attempt complete";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        }
    }

    /**
     * Mark a QuizAttempt as incomplete.
     *
     * Only owner and group managers for the quiz assignment can mark it incomplete.
     * If it is a self-taken quiz, you cannot mark it incomplete, as that would make it easier to tweak individual
     * questions to find the right answers (also there is no assignment so you can't construct the URL anyway).
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAssignmentId
     *            - the ID of the assignment you want to mark an attempt incomplete from.
     * @param userId
     *            - the ID of the user whose attempt you want to mark incomplete.
     * @return The updated attempt or an error.
     */
    @POST
    @Path("/assignment/{quizAssignmentId}/{userId}/incomplete")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Mark a QuizAttempt as incomplete.")
    public final Response markIncompleteQuizAttempt(@Context final HttpServletRequest httpServletRequest,
                                                    @PathParam("quizAssignmentId") final Long quizAssignmentId,
                                                    @PathParam("userId") final Long userId) {
        if (null == quizAssignmentId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Missing quizAssignmentId.").toResponse();
        }
        if (null == userId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Missing userId.").toResponse();
        }
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);


            UserGroupDTO group = quizAssignmentManager.getGroupForAssignment(assignment);

            if (assignment == null || !canManageGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You can only mark assignments incomplete for groups you own or manage.").toResponse();
            }

            if (assignment.getDueDate() != null && !assignment.dueDateIsAfter(new Date())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You cannot mark a test attempt as incomplete after the due date.").toResponse();
            }

            RegisteredUserDTO student = userManager.getUserDTOById(userId);
            if (!groupManager.isUserInGroup(student, group)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "That user is not in this group.").toResponse();
            }

            QuizAttemptDTO quizAttempt = quizAttemptManager.getByQuizAssignmentAndUser(assignment, student);

            if (quizAttempt == null || quizAttempt.getCompletedDate() == null) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "That test is already incomplete.").toResponse();
            }

            quizAttemptManager.updateAttemptCompletionStatus(quizAttempt, false);

            QuizUserFeedbackDTO feedback;
            UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(user,
                userManager.convertToUserSummaryObject(student));

            feedback = new QuizUserFeedbackDTO(userSummary,
                userSummary.isAuthorisedFullAccess() ? new QuizFeedbackDTO() : null);

            return Response.ok(feedback).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst marking test attempt incomplete";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "This test assignment has been cancelled.").toResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "No such user.").toResponse();
        }
    }

    /*
     * Log that a student viewed a particular section of a quiz.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAttemptId
     *            - the ID of the quiz the user wishes to attempt.
     * @param sectionNumber
     *            - the number of the section being viewed.
     * @return Confirmation or an error.
     */
    @POST
    @Path("/attempt/{quizAttemptId}/log")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get the QuizDTO for a test attempt.")
    public Response logQuizSectionView(@Context final HttpServletRequest httpServletRequest,
                                       @PathParam("quizAttemptId") final Long quizAttemptId,
                                       @FormParam("sectionNumber") Integer sectionNumber) {
        try {
            if (sectionNumber == null) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "Missing sectionNumber.").toResponse();
            }
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            QuizAttemptDTO quizAttempt = getIncompleteQuizAttemptForUser(quizAttemptId, user);

            QuizAssignmentDTO assignment = checkQuizAssignmentNotCancelledOrOverdue(quizAttempt);

            Map<String, Object> eventDetails = ImmutableMap.of(
                QUIZ_ID_FKEY, quizAttempt.getQuizId(),
                QUIZ_ATTEMPT_FK, quizAttempt.getId(),
                QUIZ_ASSIGNMENT_FK, assignment != null ? assignment.getId().toString() : "FREE_ATTEMPT",
                ASSIGNMENT_DUEDATE, assignment == null || assignment.getDueDate() == null ? "NO_DUE_DATE" : assignment.getDueDate(),
                QUIZ_SECTION, sectionNumber
            );

            getLogManager().logEvent(user, httpServletRequest, Constants.IsaacServerLogType.VIEW_QUIZ_SECTION, eventDetails);

            return Response.noContent().build();
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "This test assignment has been cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst logging test section view";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Record that a user has answered a question.
     *
     * @param request
     *            - the servlet request so we can find out if it is a known user.
     * @param questionId
     *            that you are attempting to answer.
     * @param jsonAnswer
     *            - answer body which will be parsed as a Choice and then converted to a ChoiceDTO.
     * @return Response containing a QuestionValidationResponse object or containing a SegueErrorResponse .
     */
    @POST
    @Path("/attempt/{quizAttemptId}/answer/{question_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Submit an answer to a question.",
        description = "The answer must be the correct Choice subclass for the question with the provided ID.")
    public Response answerQuestion(@Context final HttpServletRequest request,
                                   @PathParam("quizAttemptId") final Long quizAttemptId,
                                   @PathParam("question_id") final String questionId,
                                   final String jsonAnswer) {
        if (null == jsonAnswer || jsonAnswer.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "No answer received.").toResponse();
        }
        if (null == questionId || questionId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Missing questionId.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            QuizAttemptDTO quizAttempt = getIncompleteQuizAttemptForUser(quizAttemptId, user);

            checkQuizAssignmentNotCancelledOrOverdue(quizAttempt);

            Content contentBasedOnId;
            try {
                contentBasedOnId = this.contentManager.getContentDOById(questionId);
            } catch (ContentManagerException e1) {
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
                log.error(error.getErrorMessage(), e1);
                return error.toResponse();
            }

            Question question;
            if (contentBasedOnId instanceof Question) {
                question = (Question) contentBasedOnId;
            } else {
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND,
                    "No question object found for given id: " + questionId);
                log.warn(error.getErrorMessage());
                return error.toResponse();
            }

            String quizId = extractPageIdFromQuestionId(questionId);

            // Check the quiz this question is from is valid for this attempt.
            if (!quizId.equals(quizAttempt.getQuizId())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "This question is part of another test.").toResponse();
            }

            ChoiceDTO answerFromClientDTO = quizQuestionManager.convertJsonAnswerToChoice(jsonAnswer);

            QuestionValidationResponseDTO answer = quizQuestionManager.validateAnswer(question, answerFromClientDTO);

            quizQuestionManager.recordQuestionAttempt(quizAttempt, answer);

            this.getLogManager().logEvent(user, request, SegueServerLogType.ANSWER_QUIZ_QUESTION, answer);

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst submitting test answer";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, "This test assignment has been cancelled.").toResponse();
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        }
    }

    /**
     * Abandon a started free quiz attempt.
     *
     * This can only be done on free quiz attempts, as we don't want any startDate shenanigans on assigned quizzes.
     *
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param quizAttemptId
     *            - the ID of the quiz the user wishes to attempt.
     * @return Confirmation or an error.
     */
    @DELETE
    @Path("/attempt/{quizAttemptId}")
    @Operation(summary = "Abandon a started free test attempt.")
    public final Response abandonQuizAttempt(@Context final HttpServletRequest httpServletRequest,
                                             @PathParam("quizAttemptId") final Long quizAttemptId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (null == quizAttemptId) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid test attempt id.").toResponse();
            }

            QuizAttemptDTO quizAttempt = this.quizAttemptManager.getById(quizAttemptId);

            if (!quizAttempt.getUserId().equals(user.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You cannot cancel a test attempt for someone else.").toResponse();
            }

            if (quizAttempt.getQuizAssignmentId() != null) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You can only cancel attempts on tests you chose to take.").toResponse();
            }

            if (quizAttempt.getCompletedDate() != null) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You cannot cancel completed test attempts.").toResponse();
            }

            this.quizAttemptManager.deleteAttempt(quizAttempt);

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst deleting a free test attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    /**
     * Allows a teacher to set a quiz to a group.
     *
     * Sends emails to the members of the group informing them of the quiz.
     * Takes a quiz id, a group id, an optional end datetime for completion, and the feedbackMode.
     *
     * @param request so that we can extract user information.
     * @param clientQuizAssignment the partially constructed quiz assignment
     *
     * @return the quiz assignment that has been created, or an error.
     */
    @POST
    @Path("/assignment")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Set a test to a group, with an optional due date.")
    public final Response createQuizAssignment(@Context final HttpServletRequest request,
                                               final QuizAssignmentDTO clientQuizAssignment) {

        if (   clientQuizAssignment.getQuizId() == null
            || clientQuizAssignment.getGroupId() == null
            || clientQuizAssignment.getQuizFeedbackMode() == null) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "A required field was missing. Must provide group and test ids and a test feedback mode.").toResponse();
        }

        try {
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(request);

            if (!(isUserTeacherOrAbove(userManager, currentlyLoggedInUser))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            if (!canManageAssignment(clientQuizAssignment, currentlyLoggedInUser))
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You can only set assignments to groups you own or manage.").toResponse();

            IsaacQuizDTO quiz = this.quizManager.findQuiz(clientQuizAssignment.getQuizId());
            if (null == quiz) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "The test id specified does not exist.")
                    .toResponse();
            }

            clientQuizAssignment.setOwnerUserId(currentlyLoggedInUser.getId());
            clientQuizAssignment.setCreationDate(null);
            clientQuizAssignment.setId(null);

            // modifies assignment passed in to include an id.
            QuizAssignmentDTO assignmentWithID = this.quizAssignmentManager.createAssignment(clientQuizAssignment);

            Map<String, Object> eventDetails = ImmutableMap.of(
                QUIZ_ID_FKEY, assignmentWithID.getQuizId(),
                GROUP_FK, assignmentWithID.getGroupId(),
                QUIZ_ASSIGNMENT_FK, assignmentWithID.getId(),
                ASSIGNMENT_DUEDATE, assignmentWithID.getDueDate() == null ? "NO_DUE_DATE" : assignmentWithID.getDueDate()
            );

            this.getLogManager().logEvent(currentlyLoggedInUser, request, Constants.IsaacServerLogType.SET_NEW_QUIZ_ASSIGNMENT, eventDetails);

            List<QuizAssignmentDTO> assignments = Collections.singletonList(assignmentWithID);
            quizManager.augmentWithQuizSummary(assignments);

            return Response.ok(assignmentWithID).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (DuplicateAssignmentException|DueBeforeNowException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst setting quiz";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst setting test", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This test has become unavailable.");
        }
    }

    /**
     * Get quizzes assigned by this user.
     *
     * Optionally filtered by a particular group.
     *
     * @param groupIdOfInterest An optional ID of a group to look up.
     * @return a Response containing a list of QuizAssignmentDTO for the quizzes they have assigned.
     */
    @GET
    @Path("/assigned")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get tests assigned by this user.")
    public Response getQuizAssignments(@Context HttpServletRequest request,
                                       @QueryParam("groupId") Long groupIdOfInterest) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            List<UserGroupDTO> groups;
            if (groupIdOfInterest != null) {
                UserGroupDTO group = this.groupManager.getGroupById(groupIdOfInterest);

                if (null == group) {
                    return new SegueErrorResponse(Status.NOT_FOUND, "The group specified cannot be located.")
                        .toResponse();
                }

                if (!canManageGroup(user, group)) {
                    return new SegueErrorResponse(Status.FORBIDDEN, "You are not the owner or manager of that group").toResponse();
                }

                groups = Collections.singletonList(group);
            } else {
                groups = this.groupManager.getAllGroupsOwnedAndManagedByUser(user, false);
            }
            List<QuizAssignmentDTO> assignments = this.quizAssignmentManager.getAssignmentsForGroups(groups);

            quizManager.augmentWithQuizSummary(assignments);

            return Response.ok(assignments)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst getting assigned tests";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    /**
     * Allows a teacher to see results of an assignment
     *
     * @param httpServletRequest so that we can extract user information.
     * @param quizAssignmentId the id of the assignment to view.
     *
     * @return Details about the assignment.
     */
    @GET
    @Path("/assignment/{quizAssignmentId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "View a test assignment.")
    public final Response getQuizAssignment(@Context final HttpServletRequest httpServletRequest,
                                            @PathParam("quizAssignmentId") Long quizAssignmentId) {

        if (null == quizAssignmentId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid test assignment id.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);

            UserGroupDTO group = quizAssignmentManager.getGroupForAssignment(assignment);

            if (!canManageGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You can only view assignments to groups you own or manage.").toResponse();
            }

            this.assignmentService.augmentAssignerSummaries(Collections.singletonList(assignment));

            IsaacQuizDTO quiz = quizManager.findQuiz(assignment.getQuizId());

            List<RegisteredUserDTO> groupMembers = this.groupManager.getUsersInGroup(group);

            Map<RegisteredUserDTO, QuizFeedbackDTO> feedbackMap = quizQuestionManager.getAssignmentTeacherFeedback(quiz, assignment, groupMembers);

            List<QuizUserFeedbackDTO> userFeedback = new ArrayList<>();

            for (RegisteredUserDTO groupMember : groupMembers) {
                QuizFeedbackDTO feedback  = feedbackMap.get(groupMember);
                UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(user,
                    userManager.convertToUserSummaryObject(groupMember));

                userFeedback.add(new QuizUserFeedbackDTO(userSummary,
                    userSummary.isAuthorisedFullAccess() ? feedback : null));
            }

            assignment.setUserFeedback(userFeedback);
            assignment.setQuiz(quiz);

            return Response.ok(assignment)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "This assignment has been cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst viewing test assignment";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst viewing test assignment", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This test has become unavailable.");
        }
    }

    /**
     * Allows a teacher to download the results of an assignment as a CSV
     *
     * @param httpServletRequest so that we can extract user information.
     * @param quizAssignmentId the id of the assignment to view.
     * @param formatMode set to "excel" for some MS Excel magic
     *
     * @return Details about the assignment.
     */
    @GET
    @Path("/assignment/{quizAssignmentId}/download")
    @Produces("text/csv")
    @GZIP
    @Operation(summary = "Download a test assignment as a CSV.")
    public final Response getQuizAssignmentCSV(@Context final HttpServletRequest httpServletRequest,
                                               @PathParam("quizAssignmentId") Long quizAssignmentId,
                                               @QueryParam("format") final String formatMode) {

        if (null == quizAssignmentId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid test assignment id.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);
            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);
            UserGroupDTO group = quizAssignmentManager.getGroupForAssignment(assignment);
            if (!canManageGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You can only view assignments to groups you own or manage.").toResponse();
            }

            IsaacQuizDTO quiz = quizManager.findQuiz(assignment.getQuizId());
            List<RegisteredUserDTO> groupMembers = this.groupManager.getUsersInGroup(group);

            List<String[]> rows = Lists.newArrayList();
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            StringBuilder headerBuilder = new StringBuilder();
            if (null != formatMode && formatMode.toLowerCase().equals("excel")) {
                headerBuilder.append("\uFEFF");  // UTF-8 Byte Order Marker
            }
            headerBuilder.append(String.format("Test (%s) Results: Downloaded on %s \nGenerated by: %s %s \n\n",
                    quiz.getTitle(), new Date(), user.getGivenName(), user.getFamilyName()));
            headerBuilder.append(",,\"Completed\",\"Correct\",\"Incorrect\",\"Not Attempted\",");
            List<String> questionTitles = new ArrayList<>();
            List<String> questionIds = new ArrayList<>();
            for (ContentBaseDTO section : quiz.getChildren().stream().filter(c -> c instanceof IsaacQuizSectionDTO).collect(Collectors.toList())) {
                if (section instanceof IsaacQuizSectionDTO) {
                    IsaacQuizSectionDTO quizSection = (IsaacQuizSectionDTO) section;
                    List<ContentBaseDTO> quizQuestions = quizSection.getChildren().stream().filter(c -> c instanceof IsaacQuestionBaseDTO).collect(Collectors.toList());
                    for (int i = 0; i < quizQuestions.size(); ++i) {
                        questionTitles.add(String.format("\"%s - %s - Q%d\"", quiz.getTitle(), quizSection.getTitle(), i+1));
                        questionIds.add(quizQuestions.get(i).getId());
                    }
                }
            }
            headerBuilder.append(String.join(",", questionTitles));
            headerBuilder.append("\n");

            List<QuizUserFeedbackDTO> userFeedback = getUserFeedback(user, assignment, quiz, groupMembers);
            for (QuizUserFeedbackDTO f : userFeedback) {
                QuizFeedbackDTO feedback = f.getFeedback();
                List<String> row = new ArrayList<>(Arrays.asList(f.getUser().getFamilyName(), f.getUser().getGivenName()));
                if (null == feedback) {
                    row.add("This user has revoked their data sharing authorization.");
                } else {
                    row.add(feedback.isComplete() ? "yes" : "no");
                    QuizFeedbackDTO.Mark summaryMark = new QuizFeedbackDTO.Mark();
                    List<String> detailedMark = new ArrayList<>();
                    for (String questionId : questionIds) {
                        if (feedback.getQuestionMarks() != null) {
                            QuizFeedbackDTO.Mark questionMark = feedback.getQuestionMarks().get(questionId);
                            if (questionMark.notAttempted == 1) {
                                detailedMark.add("Not Attempted");
                                summaryMark.notAttempted += 1;
                            } else if (questionMark.incorrect == 1) {
                                detailedMark.add("Incorrect");
                                summaryMark.incorrect += 1;
                            } else if (questionMark.correct == 1) {
                                detailedMark.add("Correct");
                                summaryMark.correct += 1;
                            } else {
                                row.add("ERROR"); // This should not happen at this level
                            }
                        } else {
                            // The front-end shows this as "Not completed" so I'm leaving this empty
                            // because we already have a "Completed" column
                            row.add("");
                        }
                    }
                    row.add(String.format("%d", summaryMark.correct));
                    row.add(String.format("%d", summaryMark.incorrect));
                    row.add(String.format("%d", summaryMark.notAttempted));
                    row.addAll(detailedMark);
                }
                rows.add(row.toArray(new String[0]));
            }

            csvWriter.writeAll(rows);
            csvWriter.close();

            headerBuilder.append(stringWriter.toString());
            // get game manager completion information for this assignment.
            return Response.ok(headerBuilder.toString())
                    .header("Content-Disposition", "attachment; filename=test_results.csv")
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "This assignment has been cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst viewing test assignment";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst viewing test assignment", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This test has become unavailable.");
        } catch (IOException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while building the CSV file.").toResponse();
        }
    }

    @GET
    @Path("/group/{groupId}/download")
    @Produces("text/csv")
    @GZIP
    @Operation(summary = "Download a CSV with all the quiz results of a given group")
    public final Response getQuizResultsForGroup(@Context final HttpServletRequest httpServletRequest,
                                                 @PathParam("groupId") Long groupId,
                                                 @QueryParam("format") final String formatMode) {
        if (null == groupId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid group id.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);
            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            UserGroupDTO group = this.groupManager.getGroupById(groupId);

            List<RegisteredUserDTO> groupMembers = this.groupManager.getUsersInGroup(group);

            if (!canManageGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You can only retrieve results for groups you own or manage.").toResponse();
            }

            List<QuizAssignmentDTO> quizAssignments = this.quizAssignmentManager.getAssignmentsForGroups(Collections.singletonList(group));

            List<List<String>> rows = new ArrayList<>();
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            StringBuilder headerBuilder = new StringBuilder();
            if (null != formatMode && formatMode.toLowerCase().equals("excel")) {
                headerBuilder.append("\uFEFF");  // UTF-8 Byte Order Marker
            }
            headerBuilder.append(String.format("Quiz results for group (%s): Downloaded on %s \nGenerated by: %s %s \n\n",
                    group.getGroupName(), new Date(), user.getGivenName(), user.getFamilyName()));
            headerBuilder.append(",,");

            List<String> quizTitles = new ArrayList<>();
            List<String> questionTitles = new ArrayList<>();

            for (QuizAssignmentDTO assignment : quizAssignments) {
                String quizId = assignment.getQuizId();
                IsaacQuizDTO quiz = this.quizManager.findQuiz(quizId);
                quizTitles.add(String.format("\"%s\"", quiz.getTitle()));
                questionTitles.addAll(getQuizQuestionTitles(quiz));
            }

            for (RegisteredUserDTO groupMember : groupMembers) {
                UserSummaryDTO groupMemberSummary = this.userManager.convertToUserSummaryObject(groupMember);
                List<String> row = new ArrayList<>(Arrays.asList(groupMember.getGivenName(), groupMember.getFamilyName()));
                List<String> quizTotals = new ArrayList<>();
                List<String> questionResults = new ArrayList<>();

                for (QuizAssignmentDTO quizAssignment : quizAssignments) {
                    String quizId = quizAssignment.getQuizId();
                    IsaacQuizDTO quiz = this.quizManager.findQuiz(quizId);
                    List<String> questionIds = getQuizQuestionIds(quiz);
                    Optional<QuizUserFeedbackDTO> userFeedback = getUserFeedback(user, quizAssignment, quiz, groupMembers).stream()
                            .filter(f -> f.getUser().getId().equals(groupMember.getId())).findFirst();
                    if (!userFeedback.isPresent()) {
                        // This looks like it should work but I can't test it as I don't know how to retrieve a
                        // non-present user feedback. If everything is set up correctly with retrieving user
                        // feedback, this branch should never happen -- so maybe we might as well forcibly unwrap
                        // and deal with the exception because something has gone very wrong somewhere anyway.
                        quizTotals.add(""); // We should probably write something here to make debugging easier...
                        long questionCount = quiz.getChildren().stream()
                                .filter(c -> c instanceof IsaacQuizSectionDTO)
                                .map(s -> ((IsaacQuizSectionDTO) s).getChildren().stream().filter(t -> t instanceof IsaacQuestionBaseDTO).count())
                                .reduce(0L, Long::sum);
                        for (long i = 0L; i < questionCount; ++i) {
                            questionResults.add("-");
                        }
                    } else {
                        QuizFeedbackDTO feedback = userFeedback.get().getFeedback();
                        if (feedback != null) {
                            QuizFeedbackDTO.Mark overallMark = feedback.getOverallMark();
                            if (overallMark != null) {
                                // Add an apostrophe to the beginning of the score, so that the fraction isn't
                                // interpreted as a date in excel
                                quizTotals.add(String.format("'%d/%d", overallMark.correct, overallMark.correct + overallMark.incorrect + overallMark.notAttempted));
                            } else {
                                quizTotals.add("");
                            }
                        } else {
                            // The user has revoked access to their data
                            quizTotals.add("REVOKED");
                        }
                        if (feedback != null) {
                            Map<String, QuizFeedbackDTO.Mark> questionMarksByQuestionId = userFeedback.get().getFeedback().getQuestionMarks();
                            if (questionMarksByQuestionId == null) {
                                long questionCount = quiz.getChildren().stream()
                                        .filter(c -> c instanceof IsaacQuizSectionDTO)
                                        .map(s -> ((IsaacQuizSectionDTO) s).getChildren().stream().filter(t -> t instanceof IsaacQuestionBaseDTO).count())
                                        .reduce(0L, Long::sum);
                                for (long i = 0L; i < questionCount; ++i) {
                                    questionResults.add(""); // We should probably write something here to make debugging easier...
                                }
                            } else {
                                for (String id : questionIds) {
                                    QuizFeedbackDTO.Mark m = questionMarksByQuestionId.get(id);
                                    if (m == null) {
                                        questionResults.add(""); // We should probably write something here to make debugging easier...
                                        continue;
                                    }
                                    if (m.notAttempted == 1) {
                                        questionResults.add("Not Attempted");
                                    } else if (m.incorrect == 1) {
                                        questionResults.add("Incorrect");
                                    } else if (m.correct == 1) {
                                        questionResults.add("Correct");
                                    } else {
                                        questionResults.add(""); // We should probably write something here to make debugging easier...
                                    }
                                }
                            }
                        } else {
                            for (String id : questionIds) {
                                questionResults.add("REVOKED");
                            }
                        }
                    }
                }
                row.addAll(quizTotals);
                row.addAll(questionResults);

                rows.add(row);
            }

            headerBuilder.append(String.join(",", quizTitles)).append(",");
            headerBuilder.append(String.join(",", questionTitles));
            headerBuilder.append("\n");

            for (List<String> row : rows) {
                csvWriter.writeNext(row.toArray(new String[0]));
            }
            csvWriter.close();

            headerBuilder.append(stringWriter.toString());
            return Response.ok(headerBuilder.toString())
                    .header("Content-Disposition", "attachment; filename=group_test_results.csv")
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (ResourceNotFoundException e) {
            return SegueErrorResponse.getResourceNotFoundResponse("Something went wrong with this request.");
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst viewing quiz assignment";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst retrieving a quiz", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This quiz has become unavailable.");
        } catch (IOException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while building the CSV file.").toResponse();
        }
    }

    private List<String> getQuizQuestionTitles(IsaacQuizDTO quiz) {
        List<String> questionTitles = new ArrayList<>();
        for (ContentBaseDTO section : quiz.getChildren().stream().filter(c -> c instanceof IsaacQuizSectionDTO).collect(Collectors.toList())) {
            if (section instanceof IsaacQuizSectionDTO) {
                IsaacQuizSectionDTO quizSection = (IsaacQuizSectionDTO) section;
                List<ContentBaseDTO> quizQuestions = quizSection.getChildren().stream().filter(c -> c instanceof IsaacQuestionBaseDTO).collect(Collectors.toList());
                for (int i = 0; i < quizQuestions.size(); ++i) {
                    questionTitles.add(String.format("\"%s - %s - Q%d\"", quiz.getTitle(), quizSection.getTitle(), i+1));
                }
            }
        }
        return questionTitles;
    }

    private List<String> getQuizQuestionIds(IsaacQuizDTO quiz) {
        List<String> questionIds = new ArrayList<>();
        for (ContentBaseDTO section : quiz.getChildren().stream().filter(c -> c instanceof IsaacQuizSectionDTO).collect(Collectors.toList())) {
            if (section instanceof IsaacQuizSectionDTO) {
                IsaacQuizSectionDTO quizSection = (IsaacQuizSectionDTO) section;
                List<String> ids = quizSection.getChildren().stream()
                        .filter(c -> c instanceof IsaacQuestionBaseDTO)
                        .map(c -> c.getId())
                        .collect(Collectors.toList());
                questionIds.addAll(ids);
            }
        }
        return questionIds;
    }

    /**
     * Allows a teacher to see a specific user's attempt to an assignment.
     *
     * @param httpServletRequest so that we can extract user information.
     * @param quizAssignmentId the id of the assignment.
     * @param userId the id of the user to get the answers for.
     *
     * @return Details about the user's attempt.
     */
    @GET
    @Path("/assignment/{quizAssignmentId}/attempt/{userId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "View a test assignment attempt.")
    public final Response getQuizAssignmentAttempt(@Context final HttpServletRequest httpServletRequest,
                                                   @PathParam("quizAssignmentId") Long quizAssignmentId,
                                                   @PathParam("userId") Long userId) {

        if (null == quizAssignmentId || null == userId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid test assignment and user id id.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);

            UserGroupDTO group = quizAssignmentManager.getGroupForAssignment(assignment);

            if (!canManageGroup(user, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You can only view assignments to groups you own or manage.").toResponse();
            }

            RegisteredUserDTO student = this.userManager.getUserDTOById(userId);

            if (!groupManager.isUserInGroup(student, group)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "That student is not in the group that was assigned this test.").toResponse();
            }

            if (!associationManager.hasPermission(user, student)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "You do not have access to that student's data.").toResponse();
            }

            QuizAttemptDTO quizAttempt = quizAttemptManager.getByQuizAssignmentAndUser(assignment, student);

            if (quizAttempt == null || quizAttempt.getCompletedDate() == null) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                    "That student has not completed this test assignment.").toResponse();
            }

            IsaacQuizDTO quiz = quizManager.findQuiz(quizAttempt.getQuizId());

            quizAttempt = quizQuestionManager.augmentFeedbackFor(quizAttempt, quiz, QuizFeedbackMode.DETAILED_FEEDBACK);

            QuizAttemptFeedbackDTO quizAttemptFeedback = new QuizAttemptFeedbackDTO(userManager.convertToUserSummaryObject(student), quizAttempt);

            return Response.ok(quizAttemptFeedback)
                .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                .build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "This assignment has been cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst viewing test assignment attempt";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            log.error("Content error whilst viewing quiz assignment attempt", e);
            return SegueErrorResponse.getResourceNotFoundResponse("This test has become unavailable.");
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "That user does not exist.").toResponse();
        }
    }

    /**
     * Allows a teacher to update a quiz assignment.
     *
     * Only changes to the feedbackMode and dueDate fields are accepted. A bad request error will be thrown if any other fields are set.
     *
     * @param httpServletRequest so that we can extract user information.
     * @param quizAssignmentId the id of the assignment to update.
     * @param clientQuizAssignment a partial object with new values for the quiz assignment (only feedbackMode and dueDate may be set.)
     *
     * @return Confirmation or error.
     */
    @POST
    @Path("/assignment/{quizAssignmentId}")
    @Operation(summary = "Update a test assignment (only feedbackMode and dueDate may be updated).")
    public final Response updateQuizAssignment(@Context final HttpServletRequest httpServletRequest,
                                               @PathParam("quizAssignmentId") Long quizAssignmentId,
                                               final QuizAssignmentDTO clientQuizAssignment) {

        if (null == quizAssignmentId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid test assignment id.").toResponse();
        }

        if (   clientQuizAssignment.getId() != null
            || clientQuizAssignment.getQuizId() != null
            || clientQuizAssignment.getGroupId() != null
            || clientQuizAssignment.getOwnerUserId() != null
            || clientQuizAssignment.getCreationDate() != null)
        {
            log.warn("Attempt to change fields for test assignment id {} that aren't feedbackMode or dueDate: {}", quizAssignmentId, clientQuizAssignment);
            return new SegueErrorResponse(Status.BAD_REQUEST, "Those fields are not editable.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);

            if (!canManageAssignment(assignment, user)) return new SegueErrorResponse(Status.FORBIDDEN,
                "You can only updates assignments to groups you own or manage.").toResponse();

            if (assignment.getDueDate() != null && clientQuizAssignment.getDueDate() != null && clientQuizAssignment.getDueDate().compareTo(assignment.getDueDate()) <= 0) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You can only extend due dates into the future.").toResponse();
            }

            quizAssignmentManager.updateAssignment(assignment, clientQuizAssignment);

            if (assignment.getDueDate() != null && clientQuizAssignment.getDueDate() != null &&
                assignment.getDueDate().compareTo(clientQuizAssignment.getDueDate()) != 0) {
                Map<String, Object> eventDetails = ImmutableMap.of(
                        QUIZ_ID_FKEY, assignment.getQuizId(),
                        GROUP_FK, assignment.getGroupId(),
                        QUIZ_ASSIGNMENT_FK, assignment.getId(),
                        QUIZ_OLD_DUEDATE, assignment.getDueDate(),
                        ASSIGNMENT_DUEDATE, clientQuizAssignment.getDueDate()
                );
                this.getLogManager().logEvent(user, httpServletRequest, IsaacServerLogType.UPDATE_QUIZ_DEADLINE, eventDetails);
            } else if (assignment.getDueDate() == null && clientQuizAssignment.getDueDate() != null) {
                Map<String, Object> eventDetails = ImmutableMap.of(
                        QUIZ_ID_FKEY, assignment.getQuizId(),
                        GROUP_FK, assignment.getGroupId(),
                        QUIZ_ASSIGNMENT_FK, assignment.getId(),
                        QUIZ_OLD_DUEDATE, "NO_DUE_DATE",
                        ASSIGNMENT_DUEDATE, clientQuizAssignment.getDueDate()
                );
                this.getLogManager().logEvent(user, httpServletRequest, IsaacServerLogType.UPDATE_QUIZ_DEADLINE, eventDetails);
            }

            if (assignment.getQuizFeedbackMode() != null && clientQuizAssignment.getQuizFeedbackMode() != null &&
                assignment.getQuizFeedbackMode().compareTo(clientQuizAssignment.getQuizFeedbackMode()) != 0) {
                Map<String, Object> eventDetails = ImmutableMap.of(
                        QUIZ_ID_FKEY, assignment.getQuizId(),
                        GROUP_FK, assignment.getGroupId(),
                        QUIZ_ASSIGNMENT_FK, assignment.getId(),
                        QUIZ_OLD_FEEDBACK_MODE, assignment.getQuizFeedbackMode(),
                        QUIZ_FEEDBACK_MODE, clientQuizAssignment.getQuizFeedbackMode()
                );
                this.getLogManager().logEvent(user, httpServletRequest, IsaacServerLogType.UPDATE_QUIZ_FEEDBACK_MODE, eventDetails);
            }

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "This assignment is already cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst updating test assignment";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    /**
     * Allows a teacher to cancel a quiz they have set.
     *
     * @param httpServletRequest so that we can extract user information.
     * @param quizAssignmentId the id of the assignment to cancel.
     *
     * @return Confirmation or error.
     */
    @DELETE
    @Path("/assignment/{quizAssignmentId}")
    @Operation(summary = "Cancel a test assignment.")
    public final Response cancelQuizAssignment(@Context final HttpServletRequest httpServletRequest,
                                               @PathParam("quizAssignmentId") Long quizAssignmentId) {

        if (null == quizAssignmentId) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid test assignment id.").toResponse();
        }

        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            QuizAssignmentDTO assignment = quizAssignmentManager.getById(quizAssignmentId);

            if (!canManageAssignment(assignment, user)) return new SegueErrorResponse(Status.FORBIDDEN,
                "You can only cancel assignments to groups you own or manage.").toResponse();

            quizAssignmentManager.cancelAssignment(assignment);

            Map<String, Object> eventDetails = ImmutableMap.of(
                    QUIZ_ID_FKEY, assignment.getQuizId(),
                    QUIZ_ASSIGNMENT_FK, assignment.getId().toString(),
                    ASSIGNMENT_DUEDATE, assignment.getDueDate() == null ? "NO_DUE_DATE" : assignment.getDueDate()
            );
            getLogManager().logEvent(user, httpServletRequest, Constants.IsaacServerLogType.DELETE_QUIZ_ASSIGNMENT, eventDetails);

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (AssignmentCancelledException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "This assignment is already cancelled.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error whilst cancelling test assignment";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    @Nullable
    private QuizAssignmentDTO checkQuizAssignmentNotCancelledOrOverdue(final QuizAttemptDTO quizAttempt) throws SegueDatabaseException, AssignmentCancelledException, ErrorResponseWrapper {
        // Relying on the side-effects of getting the assignment.
        QuizAssignmentDTO quizAssignment = getQuizAssignment(quizAttempt);

        if (quizAssignment != null && quizAssignment.getDueDate() != null && !quizAssignment.dueDateIsAfter(new Date())) {
            throw new ErrorResponseWrapper(new SegueErrorResponse(Status.FORBIDDEN, "The due date for this test has passed."));
        }

        return quizAssignment;
    }

    @Nullable
    private QuizAssignmentDTO getQuizAssignment(QuizAttemptDTO quizAttempt) throws SegueDatabaseException, AssignmentCancelledException {
        if (quizAttempt.getQuizAssignmentId() != null) {
            return quizAssignmentManager.getById(quizAttempt.getQuizAssignmentId());
        }
        return null;
    }

    @SuppressWarnings( "deprecation" )
    private QuizAttemptDTO getIncompleteQuizAttemptForUser(Long quizAttemptId, RegisteredUserDTO user) throws SegueDatabaseException, ErrorResponseWrapper {
        QuizAttemptDTO quizAttempt = getQuizAttemptForUser(quizAttemptId, user);

        if (quizAttempt.getCompletedDate() != null) {
            throw new ErrorResponseWrapper(new SegueErrorResponse(Status.FORBIDDEN, "You have completed this test."));
        }
        return quizAttempt;
    }

    @SuppressWarnings( "deprecation" )
    private QuizAttemptDTO getCompleteQuizAttemptForUser(Long quizAttemptId, RegisteredUserDTO user) throws SegueDatabaseException, ErrorResponseWrapper {
        QuizAttemptDTO quizAttempt = getQuizAttemptForUser(quizAttemptId, user);

        if (quizAttempt.getCompletedDate() == null) {
            throw new ErrorResponseWrapper(new SegueErrorResponse(Status.FORBIDDEN, "You have not completed this test."));
        }
        return quizAttempt;
    }

    @DoNotCall
    @Deprecated
    private QuizAttemptDTO getQuizAttemptForUser(Long quizAttemptId, RegisteredUserDTO user) throws ErrorResponseWrapper, SegueDatabaseException {
        QuizAttemptDTO quizAttempt = this.getQuizAttempt(quizAttemptId);

        if (!quizAttempt.getUserId().equals(user.getId())) {
            throw new ErrorResponseWrapper(new SegueErrorResponse(Status.FORBIDDEN, "This is not your test attempt."));
        }
        return quizAttempt;
    }

    private QuizAttemptDTO getQuizAttempt(Long quizAttemptId) throws ErrorResponseWrapper, SegueDatabaseException {
        if (null == quizAttemptId) {
            throw new ErrorResponseWrapper(new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid test attempt id."));
        }

        return this.quizAttemptManager.getById(quizAttemptId);
    }

    private boolean canManageAssignment(QuizAssignmentDTO clientQuizAssignment, RegisteredUserDTO currentlyLoggedInUser) throws SegueDatabaseException, NoUserLoggedInException {
        UserGroupDTO assigneeGroup = quizAssignmentManager.getGroupForAssignment(clientQuizAssignment);

        return canManageGroup(currentlyLoggedInUser, assigneeGroup);
    }

    private boolean canManageGroup(RegisteredUserDTO currentlyLoggedInUser, UserGroupDTO assigneeGroup) throws NoUserLoggedInException {
        return GroupManager.isOwnerOrAdditionalManager(assigneeGroup, currentlyLoggedInUser.getId())
            || isUserAnAdmin(userManager, currentlyLoggedInUser);
    }

    private QuizAttemptDTO augmentAttempt(QuizAttemptDTO quizAttempt, @Nullable QuizAssignmentDTO quizAssignment, boolean includeCorrect) throws ContentManagerException, SegueDatabaseException {
        IsaacQuizDTO quiz = quizManager.findQuiz(quizAttempt.getQuizId());
        quiz = quizQuestionManager.augmentQuestionsForUser(quiz, quizAttempt, includeCorrect);
        quizAttempt.setQuiz(quiz);

        return augmentAttempt(quizAttempt, quizAssignment);
    }

    private List<QuizUserFeedbackDTO> getUserFeedback(RegisteredUserDTO user, QuizAssignmentDTO assignment, IsaacQuizDTO quiz, List<RegisteredUserDTO> groupMembers) throws ContentManagerException, SegueDatabaseException {
        Map<RegisteredUserDTO, QuizFeedbackDTO> feedbackMap = quizQuestionManager.getAssignmentTeacherFeedback(quiz, assignment, groupMembers);
        List<QuizUserFeedbackDTO> userFeedback = new ArrayList<>();
        for (RegisteredUserDTO groupMember : groupMembers) {
            QuizFeedbackDTO feedback  = feedbackMap.get(groupMember);
            UserSummaryDTO userSummary = associationManager.enforceAuthorisationPrivacy(user,
                    userManager.convertToUserSummaryObject(groupMember));

            userFeedback.add(new QuizUserFeedbackDTO(userSummary,
                    userSummary.isAuthorisedFullAccess() ? feedback : null));
        }
        return userFeedback;
    }


    private QuizAttemptDTO augmentAttempt(QuizAttemptDTO quizAttempt, @Nullable QuizAssignmentDTO quizAssignment) throws SegueDatabaseException {
        if (quizAssignment != null) {
            this.assignmentService.augmentAssignerSummaries(Collections.singletonList(quizAssignment));
            quizAttempt.setQuizAssignment(quizAssignment);
        }

        return quizAttempt;
    }
}
