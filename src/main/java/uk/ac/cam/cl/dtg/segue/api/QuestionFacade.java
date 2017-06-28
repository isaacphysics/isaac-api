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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.AnonQuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.QuestionAttemptMisuseHandler;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.IOException;
import java.util.List;

import static uk.ac.cam.cl.dtg.segue.api.Constants.ANSWER_QUESTION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.QUESTION_ATTEMPT_RATE_LIMITED;

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
    private IMisuseMonitor misuseMonitor;

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
                          final ILogManager logManager, final IMisuseMonitor misuseMonitor) {
        super(properties, logManager);

        this.questionManager = questionManager;
        this.mapper = mapper;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.userManager = userManager;
        this.misuseMonitor = misuseMonitor;
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
    public Response answerQuestion(@Context final HttpServletRequest request,
            @PathParam("question_id") final String questionId, final String jsonAnswer) {
        if (null == jsonAnswer || jsonAnswer.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "No answer received.").toResponse();
        }

        AbstractSegueUserDTO currentUser = this.userManager.getCurrentUser(request);

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

        // decide if we have been given a list or an object and put it in a list
        // either way
        List<ChoiceDTO> answersFromClient = Lists.newArrayList();
        try {
            // convert single object into a list.
            Choice answerFromClient = mapper.getSharedContentObjectMapper().readValue(jsonAnswer, Choice.class);
            // convert to a DTO so that it strips out any untrusted data.
            ChoiceDTO answerFromClientDTO = mapper.getAutoMapper().map(answerFromClient, ChoiceDTO.class);

            answersFromClient.add(answerFromClientDTO);
        } catch (JsonMappingException | JsonParseException e) {
            log.info("Failed to map to any expected input...", e);
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Unable to map response to a "
                    + "Choice object so failing with an error", e);
            return error.toResponse();
        } catch (IOException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Unable to map response to a "
                    + "Choice object so failing with an error", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }

        // validate the answer.
        Response response;
        try {
            response = this.questionManager.validateAnswer(question, Lists.newArrayList(answersFromClient));

            // After validating the answer, work out whether this is abuse of the endpoint. If so, record the attempt in
            // the log, but don't save it for the user. Also, return an error.

            // We store response.getEntity() in either case so that we can treat them the same in later analysis.
            if (currentUser instanceof RegisteredUserDTO) {
                try {
                    misuseMonitor.notifyEvent(((RegisteredUserDTO) currentUser).getId().toString() + "|" + questionId,
                            QuestionAttemptMisuseHandler.class.toString());
                } catch (SegueResourceMisuseException e) {
                    this.getLogManager().logEvent(currentUser, request, QUESTION_ATTEMPT_RATE_LIMITED, response.getEntity());
                    String message = "You have made too many attempts at this question part. Please try again later.";
                    return SegueErrorResponse.getRateThrottledResponse(message);
                }
            } else {
                try {
                    misuseMonitor.notifyEvent(((AnonymousUserDTO) currentUser).getSessionId() + "|" + questionId,
                            AnonQuestionAttemptMisuseHandler.class.toString());
                } catch (SegueResourceMisuseException e) {
                    this.getLogManager().logEvent(currentUser, request, QUESTION_ATTEMPT_RATE_LIMITED, response.getEntity());
                    String message = "You have made too many attempts at this question part. Please log in or try again later.";
                    return SegueErrorResponse.getRateThrottledResponse(message);
                }
            }

            // If we get to this point, this is a valid question attempt. Record it.

            if (response.getEntity() instanceof QuestionValidationResponseDTO) {
                questionManager.recordQuestionAttempt(currentUser,
                        (QuestionValidationResponseDTO) response.getEntity());
            }

            this.getLogManager().logEvent(currentUser, request, ANSWER_QUESTION, response.getEntity());

            return response;

        } catch (IllegalArgumentException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Bad request - " + e.getMessage(), e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
    }
}
