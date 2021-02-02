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

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.QuizManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_HOUR;

/**
 * Games boards Facade.
 */
@Path("/quiz")
@Api(value = "/quiz")
public class QuizFacade extends AbstractIsaacFacade {
    private final IContentManager contentManager;
    private final QuizManager quizManager;
    private final UserAccountManager userManager;

    private static final Logger log = LoggerFactory.getLogger(QuizFacade.class);

    /**
     * QuizFacade. For management of quizzes
     *  @param properties
     *            - global properties map
     * @param logManager
     *            - for managing logs.
     * @param contentManager
     *            - for the content etag
     * @param quizManager
     *            - for quiz interaction
     * @param userManager
     *            - for the currently logged in user
     */
    @Inject
    public QuizFacade(final PropertiesLoader properties, final ILogManager logManager,
                      final IContentManager contentManager, final QuizManager quizManager,
                      final UserAccountManager userManager) {
        super(properties, logManager);
        this.contentManager = contentManager;

        this.quizManager = quizManager;
        this.userManager = userManager;
    }

    /**
     * Get quizzes visible to this user.
     *
     * Anonymous users can't see quizzes.
     * Students can see quizzes with the visibleToStudents flag set.
     * Teachers and higher can see all quizzes.
     *
     * @return a Response containing a list of ContentSummaryDTO for the visible quizzes.
     */
    @GET
    @Path("available")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get quizzes visible to this user.")
    public final Response getAvailableQuizzes(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            boolean isStudent = !isUserTeacherOrAbove(userManager, user);

            EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + "");

            ResultsWrapper<ContentSummaryDTO> summary = this.quizManager.getAvailableQuizzes(isStudent, null, null);

            return Response.ok(summary).tag(etag)
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, isStudent))
                .build();
        } catch (ContentManagerException e) {
            String message = "ContentManagerException whilst getting available quizzes";
            log.error(message, e);
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Preview a quiz. Only available to teachers and above.
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
    @Path("/{quizId}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Preview an individual quiz.")
    public final Response previewQuiz(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
                                      @PathParam("quizId") final String quizId) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(httpServletRequest);

            if (!(isUserTeacherOrAbove(userManager, user))) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            if (null == quizId || quizId.isEmpty()) {
                return new SegueErrorResponse(Response.Status.BAD_REQUEST, "You must provide a valid quiz id.").toResponse();
            }

            EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + quizId.hashCode() + "");

            Response cachedResponse = generateCachedResponse(request, etag);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            IsaacQuizDTO quiz = this.quizManager.findQuiz(quizId);

            return Response.ok(quiz)
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, false)).tag(etag).build();
        } catch (ContentManagerException e) {
            String message = "ContentManagerException whilst previewing a quiz";
            log.error(message, e);
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }
}
