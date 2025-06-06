/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.api;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuiz;
import uk.ac.cam.cl.dtg.isaac.dos.TestCase;
import uk.ac.cam.cl.dtg.isaac.dos.TestQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.UserPreference;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatorUnavailableException;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.AnonQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.IPQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.LLMFreeTextQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.QuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseLockTimoutException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;
import uk.ac.cam.cl.dtg.util.RequestIPExtractor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
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
import java.util.Date;
import java.util.List;
import java.util.Map;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager.extractPageIdFromQuestionId;

/**
 * Question Facade
 * 
 * This facade is intended to support external interaction with segue supported questions.
 * 
 */
@Path("/questions")
@Tag(name = "QuestionFacade", description = "/questions")
public class QuestionFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(QuestionFacade.class);

    public static final class NoUserConsentGrantedException extends Exception {
        NoUserConsentGrantedException(final String message) {
            super(message);
        }
    }

    private final ContentMapper mapper;
    private final GitContentManager contentManager;
    private final UserAccountManager userManager;
    private final AbstractUserPreferenceManager userPreferenceManager;

    private final QuestionManager questionManager;
    private final UserAssociationManager userAssociationManager;
    private final IMisuseMonitor misuseMonitor;
    private final IUserStreaksManager userStreaksManager;

    /**
     * This method checks whether a user can answer LLM marked questions and, if not, throws an exception indicating why.
     * As well as this, it returns a type narrowed version of the current user, as it is one of the conditions.
     * @param user - the user to check.
     * @return the passed in user, narrowed to a RegisteredUserDTO.
     * @throws ValidatorUnavailableException - if the LLM marker feature is not enabled by configuration.
     * @throws NoUserLoggedInException - if the user is not logged in.
     * @throws NoUserConsentGrantedException - if the user has not consented to sending their attempts to the LLM provider.
     * @throws SegueResourceMisuseException - if the user has exceeded the number of attempts they can make over a period of time.
     * @throws SegueDatabaseException - if there is an unexpected problem with the database.
     */
    public RegisteredUserDTO assertUserCanAnswerLLMQuestions(final AbstractSegueUserDTO user) throws
            SegueDatabaseException, NoUserLoggedInException, NoUserConsentGrantedException,
            ValidatorUnavailableException, SegueResourceMisuseException
    {
        if (!"on".equals(this.getProperties().getProperty(LLM_MARKER_FEATURE))) {
            throw new ValidatorUnavailableException(
                    "LLM marked questions are currently unavailable. Please try again later!");
        } else if (user instanceof AnonymousUserDTO) {
            throw new NoUserLoggedInException();
        } else if (user instanceof RegisteredUserDTO) {
            RegisteredUserDTO registeredUser = ((RegisteredUserDTO) user);

            // Check for consent
            UserPreference llmProviderConsent = userPreferenceManager.getUserPreference(
                    IsaacUserPreferences.CONSENT.toString(), LLM_PROVIDER_NAME, registeredUser.getId());
            if (llmProviderConsent == null || !llmProviderConsent.getPreferenceValue()) {
                throw new NoUserConsentGrantedException(
                        String.format("You must consent to sending your attempts to %s.", LLM_PROVIDER_NAME));
            }

            // Check misuse handler
            String userId = registeredUser.getId().toString();
            String llmFreeTextMisuseEventName = LLMFreeTextQuestionAttemptMisuseHandler.class.getSimpleName();
            if (misuseMonitor.getRemainingUses(userId, llmFreeTextMisuseEventName) <= 0) {
                log.warn("User " + userId + " has reached the LLM question attempt limit.");
                throw new SegueResourceMisuseException(
                        "You have exceeded the number of attempts you can make on LLM marked free-text questions. " +
                        "Please try again later.");
            }
        }

        assert user instanceof RegisteredUserDTO; // We've already checked for AnonymousUserDTO above.
        return (RegisteredUserDTO) user;
    }

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
     * @param userPreferenceManager
     *            - The manager object responsible for user preferences.
     * @param questionManager
     *            - A question manager object responsible for managing questions and augmenting questions with user
     *            information.
     * @param logManager
     *            - An instance of the log manager used for recording usage of the CMS.

     */
    @Inject
    public QuestionFacade(final AbstractConfigLoader properties, final ContentMapper mapper,
                          final GitContentManager contentManager, final UserAccountManager userManager,
                          final AbstractUserPreferenceManager userPreferenceManager, final QuestionManager questionManager,
                          final ILogManager logManager, final IMisuseMonitor misuseMonitor,
                          final IUserStreaksManager userStreaksManager,
                          final UserAssociationManager userAssociationManager) {
        super(properties, logManager);

        this.questionManager = questionManager;
        this.mapper = mapper;
        this.contentManager = contentManager;
        this.userManager = userManager;
        this.userPreferenceManager = userPreferenceManager;
        this.misuseMonitor = misuseMonitor;
        this.userStreaksManager = userStreaksManager;
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
    @Operation(summary = "Provide users who try to cheat with a guide to the location of our help page.")
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
    @Operation(summary = "Return a count of question attempts per month.")
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

            // decide if the user is allowed to view this data. If user isn't viewing their own data, user viewing
            // must have a valid connection with the user of interest and be at least a teacher.
            if (!currentUser.getId().equals(userIdOfInterest)
                    && !userAssociationManager.hasTeacherPermission(currentUser, userOfInterestSummaryObject)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            // No point looking for stats from before the user registered (except for merged attempts at registration,
            // and these will only be ANONYMOUS_SESSION_DURATION_IN_MINUTES before registration anyway):
            Date fromDateObject = new Date(fromDate);
            if (fromDateObject.before(userOfInterest.getRegistrationDate())) {
                fromDateObject = userOfInterest.getRegistrationDate();
            }

            return Response.ok(this.questionManager.getUsersQuestionAttemptCountsByDate(userOfInterest, fromDateObject, new Date(toDate), perDay))
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
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
     * Check if a user can answer a particular question type.
     * Initially only used for LLM questions, but could be used for future question types which require expensive
     * operations to verify or mark.
     * HTTP error codes are used to indicate the reason a user cannot answer a question.
     * @param request
     *            - the servlet request to can find out if it is a known user.
     * @param questionType
     *            - the type of question to check if the user can attempt.
     * @return Response containing a map with a single key "remainingAttempts" and the number of attempts remaining for
     *         the user to attempt the question type.
     */
    @GET
    @Path("{question_type}/can_attempt")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Check if a user can attempt a question of a certain type.")
    public Response canAttemptQuestionType(@Context final HttpServletRequest request,
                                           @PathParam("question_type") final String questionType) {
        Map<String, Object> response = Maps.newHashMap();
        SegueErrorResponse error = null;

        try {
            AbstractSegueUserDTO currentUser = this.userManager.getCurrentUser(request);

            // Anonymous users are rate limited across all questions rather than per question as is the case for registered users.
            boolean isAnonymousUser = currentUser instanceof AnonymousUserDTO;
            if (isAnonymousUser && misuseMonitor.hasMisused(RequestIPExtractor.getClientIpAddr(request),
                    IPQuestionAttemptMisuseHandler.class.getSimpleName())) {
                throw new SegueResourceMisuseException(IPQuestionAttemptMisuseHandler.DEFAULT_FEEDBACK_MESSAGE);
            }

            // Prevent access to LLM marked questions without signing in, consenting to terms and checking misuse.
            if (LLM_FREE_TEXT_QUESTION_TYPE.equals(questionType)) {
                RegisteredUserDTO registeredUser = assertUserCanAnswerLLMQuestions(currentUser);
                int remainingAttempts = misuseMonitor.getRemainingUses(
                        registeredUser.getId().toString(),
                        LLMFreeTextQuestionAttemptMisuseHandler.class.getSimpleName());
                response.put("remainingAttempts", remainingAttempts);
            }

            // If we get to this point, the user can attempt the question.
            return Response.ok(response).build();

        } catch (ValidatorUnavailableException e) {
            error = new SegueErrorResponse(Status.SERVICE_UNAVAILABLE, e.getMessage());
            error.setBypassGenericSiteErrorPage(true);
        } catch (NoUserLoggedInException e) {
            error = new SegueErrorResponse(Status.UNAUTHORIZED, e.getMessage());
            error.setBypassGenericSiteErrorPage(true);
        } catch (NoUserConsentGrantedException e) {
            error = new SegueErrorResponse(Status.FORBIDDEN, e.getMessage());
            error.setBypassGenericSiteErrorPage(true);
        } catch (SegueResourceMisuseException e) {
            error = new SegueErrorResponse(Status.TOO_MANY_REQUESTS, e.getMessage());
            error.setBypassGenericSiteErrorPage(true);
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst checking if user can attempt a question type.";
            log.error(message, e);
            error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message);
        }
        return error.toResponse();
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
    @Operation(summary = "Submit an answer to a question.",
                  description = "The answer must be the correct Choice subclass for the question with the provided ID.")
    public Response answerQuestion(@Context final HttpServletRequest request,
            @PathParam("question_id") final String questionId, final String jsonAnswer) {
        if (null == jsonAnswer || jsonAnswer.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "No answer received.").toResponse();
        }

        Content contentBasedOnId;
        try {
            contentBasedOnId = this.contentManager.getContentDOById(
                    questionId);
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

        // Prevent attempting a question through this endpoint if this question is part of a quiz.
        String questionPageId = extractPageIdFromQuestionId(questionId);
        Content pageContent;
        try {
            pageContent = this.contentManager.getContentDOById(questionPageId);
            if (pageContent instanceof IsaacQuiz) {
                return new SegueErrorResponse(Status.FORBIDDEN, "This question is part of a quiz").toResponse();
            }
        } catch (ContentManagerException e) {
            // This doesn't make sense, so we'll log and continue.
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Question without page found", e);
            log.error(error.getErrorMessage(), e);
        }

        try {
            ChoiceDTO answerFromClientDTO = questionManager.convertJsonAnswerToChoice(jsonAnswer);

            AbstractSegueUserDTO currentUser = this.userManager.getCurrentUser(request);

            // Prevent access to LLM marked questions without signing in, consenting to terms and checking misuse.
            if (LLM_FREE_TEXT_QUESTION_TYPE.equals(question.getType())) {
                RegisteredUserDTO registeredUser = assertUserCanAnswerLLMQuestions(currentUser);
                misuseMonitor.notifyEvent(
                        registeredUser.getId().toString(),
                        LLMFreeTextQuestionAttemptMisuseHandler.class.getSimpleName());
            }

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
                    return SegueErrorResponse.getRateThrottledResponse(IPQuestionAttemptMisuseHandler.DEFAULT_FEEDBACK_MESSAGE);
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
        } catch (SegueDatabaseLockTimoutException e) {
            // This error isn't great, but it's not bad enough for the full-page error:
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to save question attempt. Try again later!");
            error.setBypassGenericSiteErrorPage(true);
            log.warn("Lock timeout attempting to save anonymous user question attempt!");
            return error.toResponse();
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to save question attempt. Try again later!");
            log.error("Unable to to record question attempt.", e);
            return error.toResponse();
        } catch (ErrorResponseWrapper responseWrapper) {
            return responseWrapper.toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserConsentGrantedException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, e.getMessage()).toResponse();
        } catch (ValidatorUnavailableException e) {
            return SegueErrorResponse.getServiceUnavailableResponse(e.getMessage());
        } catch (SegueResourceMisuseException e) {
            return SegueErrorResponse.getRateThrottledResponse(e.getMessage());
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
    @Operation(summary = "Test a list of choices with some expected answer values.",
               description = "At present only isaacFreeTextQuestions are supported.")
    public Response testQuestion(@Context final HttpServletRequest request,
                                 @QueryParam("type") final String questionType, final String testJson) {
        try {
            // TODO: review whether this endpoint is still required?
            if (null == questionType || !questionType.equals("isaacFreeTextQuestion")) {
                return SegueErrorResponse.getBadRequestResponse("Only isaacFreeTextQuestions are supported.");
            }

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
    @Operation(summary = "Turn an answer into a question specification.")
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
