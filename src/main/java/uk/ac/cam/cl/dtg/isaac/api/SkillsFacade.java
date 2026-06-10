package uk.ac.cam.cl.dtg.isaac.api;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.InvalidMarkingResponseException;
import uk.ac.cam.cl.dtg.isaac.api.managers.SkillsManager;
import uk.ac.cam.cl.dtg.isaac.dos.content.AnvilApp;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Skills Facade, supports interaction related to the Isaac Skill Practice apps.
 */
@Path("/skills")
@Tag(name = "SkillsFacade", description = "/skills")
public class SkillsFacade extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(SkillsFacade.class);

    private final UserAccountManager userManager;
    private final GitContentManager contentManager;
    private final SkillsManager skillsManager;

    /**
     * Constructor.
     */
    @Inject
    public SkillsFacade(final AbstractConfigLoader properties, final UserAccountManager userManager,
                        final ILogManager logManager, final GitContentManager contentManager,
                        final SkillsManager skillsManager) {
        super(properties, logManager);
        this.userManager = userManager;
        this.contentManager = contentManager;
        this.skillsManager = skillsManager;
    }

    /**
     * Endpoint that records when a user has answered a question.
     *
     * @param request
     *            - the servlet request so we can find out if it is a known user.
     * @param appId
     *            - the app that the attempt belongs to
     */
    @POST
    @Path("/{appId}/answer")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response answerQuestion(@Context final HttpServletRequest request,
                                   @PathParam("appId") final String appId,
                                   final String body) {
        try {
            userManager.getCurrentRegisteredUser(request);
            skillsManager.parseResponse(body);
            if (!(this.contentManager.getContentDOById(appId) instanceof AnvilApp)) {
                var error = new SegueErrorResponse(Status.NOT_FOUND, "No app found for given id: " + appId);
                log.warn(error.getErrorMessage());
                return error.toResponse();
            }
            return Response.ok().build();
        } catch (final NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (final InvalidMarkingResponseException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (final ContentManagerException e) {
            var error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
    }
}
