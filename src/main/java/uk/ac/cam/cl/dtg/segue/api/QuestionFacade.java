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

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.TestCase;
import uk.ac.cam.cl.dtg.isaac.dos.TestQuestion;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.AnonQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.IPQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.QuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatorUnavailableException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.RequestIPExtractor;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
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
import java.util.Date;
import java.util.List;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SegueServerLogType;
import static uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager.extractPageIdFromQuestionId;

/**
 * Question Facade
 * 
 * This facade is intended to support external interaction with segue supported questions.
 * 
 */
@Path("/questions")
@Api(value = "/questions")
public class QuestionFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(QuestionFacade.class);

    private final ContentMapper mapper;
    private final IContentManager contentManager;
    private final String contentIndex;
    private final UserAccountManager userManager;
    private final QuestionManager questionManager;
    private final UserBadgeManager userBadgeManager;
    private final UserAssociationManager userAssociationManager;
    private IMisuseMonitor misuseMonitor;
    private IUserStreaksManager userStreaksManager;

    /**
     * 
     * @param properties
     *            - the fully configured properties loader for the api.
     * @param mapper
     *            - The Content mapper object used for polymorphic mapping of content objects.
     * @param contentManager
     *            - The content version controller used by the api.
     * @param userManager
     *            - The manager object responsible for users.
     * @param questionManager
     *            - A question manager object responsible for managing questions and augmenting questions with user
     *            information.
     * @param logManager
     *            - An instance of the log manager used for recording usage of the CMS.

     */
    @Inject
    public QuestionFacade(final PropertiesLoader properties, final ContentMapper mapper,
                          final IContentManager contentManager, @Named(CONTENT_INDEX) final String contentIndex, final UserAccountManager userManager,
                          final QuestionManager questionManager,
                          final ILogManager logManager, final IMisuseMonitor misuseMonitor,
                          final UserBadgeManager userBadgeManager,
                          final IUserStreaksManager userStreaksManager,
                          final UserAssociationManager userAssociationManager) {
        super(properties, logManager);

        this.questionManager = questionManager;
        this.mapper = mapper;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.userManager = userManager;
        this.misuseMonitor = misuseMonitor;
        this.userStreaksManager = userStreaksManager;
        this.userBadgeManager = userBadgeManager;
        this.userAssociationManager = userAssociationManager;
    }

    /**
     * Warn users attempting to make GET requests to answers that we do not provide these. Log the attempt.
     *
     * @param request - the incoming request
     * @param questionId - the question the user is referring to
     * @return an error message informing the user where to find help.
     */
    @GET
    @Path("{question_id}/answer")
    @ApiOperation(value = "Provide users who try to cheat with a guide to the location of our help page.")
    public Response getQuestionAnswer(@Context final HttpServletRequest request, @PathParam("question_id") final String questionId) {
        String errorMessage = String.format("We do not provide answers to questions. See https://%s/solving_problems for more help!",
                                            getProperties().getProperty(HOST_NAME));
        try {
            AbstractSegueUserDTO currentUser = this.userManager.getCurrentUser(request);
            if (currentUser instanceof RegisteredUserDTO) {
                log.warn(String.format("MethodNotAllowed: User (%s) attempted to GET the answer to the question '%s'!",
                        ((RegisteredUserDTO) currentUser).getId(), questionId));
            } else {
                log.warn(String.format("MethodNotAllowed: Anonymous user attempted to GET the answer to the question '%s'!", questionId));
            }
            return new SegueErrorResponse(Status.METHOD_NOT_ALLOWED, errorMessage).toResponse();
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error while looking up user information.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
    }

    /**
     * Get questions answered by user per month for a given date range
     *
     * @param request - the incoming request
     * @param userIdOfInterest - The user id that the query is focused on
     * @param fromDate - the date to start counting (and the month that will be first in the response map)
     * @param toDate - The date to finish counting and the month that will be last in the response map
     * @param perDay - Whether to bin by day, instead of by month as default.
     * @return an object containing dates (first of each month) mapped to number (number of question attempts)
     */
    @GET
    @Path("answered_questions/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Return a count of question attempts per month.")
    public Response getQuestionsAnswered(@Context final HttpServletRequest request,
                                      @PathParam("user_id") final Long userIdOfInterest,
                                      @QueryParam("from_date") final Long fromDate,
                                      @QueryParam("to_date") final Long toDate,
                                      @QueryParam("per_day") final Boolean perDay) {
        try {

            if (null == fromDate || null == toDate) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        "You must specify the from_date and to_date you are interested in.").toResponse();
            }

            if (fromDate > toDate) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        "The from_date must be before the to_date!").toResponse();
            }

            RegisteredUserDTO currentUser = this.userManager.getCurrentRegisteredUser(request);

            RegisteredUserDTO userOfInterest = this.userManager.getUserDTOById(userIdOfInterest);
            UserSummaryDTO userOfInterestSummaryObject = userManager.convertToUserSummaryObject(userOfInterest);

            // decide if the user is allowed to view this data.
            if (!currentUser.getId().equals(userIdOfInterest)
                    && !userAssociationManager.hasPermission(currentUser, userOfInterestSummaryObject)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            // No point looking for stats from before the user registered (except for merged attempts at registration,
            // and these will only be ANONYMOUS_SESSION_DURATION_IN_MINUTES before registration anyway):
            Date fromDateObject = new Date(fromDate);
            if (fromDateObject.before(userOfInterest.getRegistrationDate())) {
                fromDateObject = userOfInterest.getRegistrationDate();
            }

            return Response.ok(this.questionManager.getUsersQuestionAttemptCountsByDate(userOfInterest, fromDateObject, new Date(toDate), perDay)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to find user with the id provided.").toResponse();
        } catch (SegueDatabaseException e) {
            log.error("Unable to look up user event history for user " + userIdOfInterest, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while looking up event information")
                    .toResponse();
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
    @Path("{question_id}/answer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Submit an answer to a question.",
                  notes = "The answer must be the correct Choice subclass for the question with the provided ID.")
    public Response answerQuestion(@Context final HttpServletRequest request,
            @PathParam("question_id") final String questionId, final String jsonAnswer) {
        if (null == jsonAnswer || jsonAnswer.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "No answer received.").toResponse();
        }

        Content contentBasedOnId;
        try {
            contentBasedOnId = this.contentManager.getContentDOById(
                    this.contentManager.getCurrentContentSHA(), questionId);
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

        if (this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(Constants.EnvironmentType.DEV.name())) {
            // FIXME: Currently this is in development only to as to not accidentally break live.
            // Once quizzes roll out this needs to run in PROD too.

            String questionPageId = extractPageIdFromQuestionId(questionId);
            Content pageContent;
            try {
                pageContent = this.contentManager.getContentDOById(contentIndex, questionPageId);
                if (pageContent instanceof IsaacQuiz) {
                    return new SegueErrorResponse(Status.FORBIDDEN, "This question is part of a quiz").toResponse();
                }
            } catch (ContentManagerException e) {
                // This doesn't make sense, so we'll log and continue.
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Question without page found", e);
                log.error(error.getErrorMessage(), e);
            }
        }

        try {
            ChoiceDTO answerFromClientDTO = questionManager.convertJsonAnswerToChoice(jsonAnswer);

            AbstractSegueUserDTO currentUser = this.userManager.getCurrentUser(request);

            Response response = this.questionManager.validateAnswer(question, answerFromClientDTO);

            // After validating the answer, work out whether this is abuse of the endpoint. If so, record the attempt in
            // the log, but don't save it for the user. Also, return an error.

            // We store response.getEntity() in either case so that we can treat them the same in later analysis.
            if (currentUser instanceof RegisteredUserDTO) {
                try {
                    // Monitor misuse on a per-question per-registered user basis, with higher limits:
                    misuseMonitor.notifyEvent(((RegisteredUserDTO) currentUser).getId().toString() + "|" + questionId,
                            QuestionAttemptMisuseHandler.class.getSimpleName());
                } catch (SegueResourceMisuseException e) {
                    this.getLogManager().logEvent(currentUser, request, SegueServerLogType.QUESTION_ATTEMPT_RATE_LIMITED, response.getEntity());
                    String message = "You have made too many attempts at this question part. Please try again later.";
                    return SegueErrorResponse.getRateThrottledResponse(message);
                }
            } else {
                try {
                    // Monitor misuse on a per-question per-anonymous user basis:
                    misuseMonitor.notifyEvent(((AnonymousUserDTO) currentUser).getSessionId() + "|" + questionId,
                            AnonQuestionAttemptMisuseHandler.class.getSimpleName());
                } catch (SegueResourceMisuseException e) {
                    this.getLogManager().logEvent(currentUser, request, SegueServerLogType.QUESTION_ATTEMPT_RATE_LIMITED, response.getEntity());
                    String message = "You have made too many attempts at this question part. Please log in or try again later.";
                    return SegueErrorResponse.getRateThrottledResponse(message);
                }
                try {
                    // And monitor on a blanket per IP Address basis for non-logged in users.
                    // If we see serious misuse, this could be moved to *before* the attempt validation and checking,
                    // to save server load. Since this occurs after the anon user notify event, that will catch most
                    // misuse and this will catch misuse ignoring cookies or with repeated new anon accounts.
                    misuseMonitor.notifyEvent(RequestIPExtractor.getClientIpAddr(request),
                            IPQuestionAttemptMisuseHandler.class.getSimpleName());
                } catch (SegueResourceMisuseException e) {
                    this.getLogManager().logEvent(currentUser, request, SegueServerLogType.QUESTION_ATTEMPT_RATE_LIMITED, response.getEntity());
                    String message = "Too many question attempts! Please log in or try again later.";
                    return SegueErrorResponse.getRateThrottledResponse(message);
                }
            }

            // If we get to this point, this is a valid question attempt. Record it.
            if (response.getEntity() instanceof QuestionValidationResponseDTO) {
                questionManager.recordQuestionAttempt(currentUser,
                        (QuestionValidationResponseDTO) response.getEntity());
            }

            this.getLogManager().logEvent(currentUser, request, SegueServerLogType.ANSWER_QUESTION, response.getEntity());

            // Update the user in case their streak has changed:
            if (currentUser instanceof RegisteredUserDTO) {
                this.userStreaksManager.notifyUserOfStreakChange((RegisteredUserDTO) currentUser);
            }

            return response;

        } catch (IllegalArgumentException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Bad request - " + e.getMessage(), e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to save question attempt. Try again later!");
            log.error("Unable to to record question attempt.", e);
            return error.toResponse();
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        }
    }

    /**
     * A generic question tester where a fake question is created form received choices and evaluated against a series
     * of example student answers
     * @param request - the incoming request
     * @param questionType - the type of question to construct from the available choices in testJson
     * @param testJson - a JSON structure to represent the possible choices and
     * @return a list of test cases matching those that were sent to the endpoint augmented with the validator's results
     */
    @POST
    @Path("/test")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Test a list of choices with some expected answer values")
    public Response testQuestion(@Context final HttpServletRequest request,
                                 @QueryParam("type") final String questionType, final String testJson) {
        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
            if (!isUserStaff(userManager, currentUser)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            TestQuestion testDefinition = mapper.getSharedContentObjectMapper().readValue(testJson, TestQuestion.class);
            List<TestCase> results = questionManager.testQuestion(questionType, testDefinition);
            return Response.ok(results).build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (ValidatorUnavailableException | IOException e) {
            return SegueErrorResponse.getServiceUnavailableResponse(e.getMessage());
        }
    }

    /**
     * Convert a possible answer into a question specification.
     *
     * @param request
     *            - the servlet request so we can find out if it is a known user.
     * @param jsonAnswer
     *            - answer body which will be parsed as a Choice and then converted to a ChoiceDTO.
     * @return Response containing a QuestionValidationResponse object or containing a SegueErrorResponse .
     */
    @POST
    @Path("generateSpecification")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Turn an answer into a question specification.")
    public Response generateSpecification(@Context final HttpServletRequest request, final String jsonAnswer) {
        if (null == jsonAnswer || jsonAnswer.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "No answer received.").toResponse();
        }

        try {
            if (!isUserStaff(userManager, request)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        try {
            ChoiceDTO answerFromClientDTO = questionManager.convertJsonAnswerToChoice(jsonAnswer);

            return questionManager.generateSpecification(answerFromClientDTO);
        } catch (IllegalArgumentException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Bad request - " + e.getMessage(), e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        }
    }
}
