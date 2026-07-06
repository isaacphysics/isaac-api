package uk.ac.cam.cl.dtg.isaac.api;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateSkillsAttemptException;
import uk.ac.cam.cl.dtg.isaac.api.managers.InvalidAnvilMarkingRequestException;
import uk.ac.cam.cl.dtg.isaac.api.managers.SkillsAttemptManager;
import uk.ac.cam.cl.dtg.isaac.dos.content.SkillsApp;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilMarkingRequestDTO;
import uk.ac.cam.cl.dtg.isaac.dto.AnvilPayloadDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/** Skills Facade, supports interaction related to the Isaac Skill Practice apps. */
@Path("/skills")
@Tag(name = "SkillsFacade", description = "/skills")
public class SkillsFacade extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(SkillsFacade.class);

    private final UserAccountManager userManager;
    private final GitContentManager contentManager;
    private final SkillsAttemptManager skillsAttemptManager;

    /**
     * Constructor.
     */
    @Inject
    public SkillsFacade(final AbstractConfigLoader properties, final UserAccountManager userManager,
                        final ILogManager logManager, final GitContentManager contentManager,
                        final SkillsAttemptManager skillsAttemptManager) {
        super(properties, logManager);
        this.userManager = userManager;
        this.contentManager = contentManager;
        this.skillsAttemptManager = skillsAttemptManager;
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
    @Produces(MediaType.APPLICATION_JSON)
    public Response answerQuestion(@Context final HttpServletRequest request,
                                   @PathParam("appId") final String appId,
                                   final AnvilMarkingRequestDTO markingRequest) {
        if (getProperties().getProperty("SKILLS_HMAC_SECRET") == null) {
            return new SegueErrorResponse(Status.NOT_IMPLEMENTED, "Endpoint has not been implemented.").toResponse();
        }
        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);

            if (!(this.contentManager.getContentDOById(appId) instanceof SkillsApp)) {
                var error = new SegueErrorResponse(Status.NOT_FOUND, "No skills app found for that id.");
                log.warn("No skills app found for given id: {}.", appId);
                return error.toResponse();
            }

            if (!skillsAttemptManager.isHmacValid(markingRequest)) {
                var error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid HMAC signature.");
                log.warn(error.getErrorMessage());
                return error.toResponse();
            }

            AnvilPayloadDTO payloadDTO = skillsAttemptManager.parsePayload(
                markingRequest.getPayload(), currentUser.getId(), appId);
            skillsAttemptManager.recordAttempt(payloadDTO);

            return Response.ok(payloadDTO).build();
        } catch (final NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (final InvalidAnvilMarkingRequestException e) {
            var error = new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage());
            log.warn(error.getErrorMessage() + ", " + e.getDetailedProblem());
            return error.toResponse();
        } catch (final DuplicateSkillsAttemptException e) {
            var error = new SegueErrorResponse(Status.CONFLICT, "Duplicate attempt ID.");
            log.warn(error.getErrorMessage());
            return error.toResponse();
        } catch (final SegueDatabaseException e) {
            var error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Something went wrong.");
            log.error(error.getErrorMessage());
            return error.toResponse();
        } catch (final ContentManagerException e) {
            var error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
    }
}
