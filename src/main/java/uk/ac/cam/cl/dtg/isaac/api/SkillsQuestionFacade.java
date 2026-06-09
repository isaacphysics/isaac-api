package uk.ac.cam.cl.dtg.isaac.api;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/skills-questions")
@Tag(name = "SkillsQuestionFacade", description = "/skills-questions")
public class SkillsQuestionFacade extends AbstractIsaacFacade {

    private final UserAccountManager userManager;

    @Inject
    public SkillsQuestionFacade(final AbstractConfigLoader properties, final UserAccountManager userManager,
                                final ILogManager logManager) {
        super(properties, logManager);
        this.userManager = userManager;
    }

    @POST
    @Path("/{appId}/answer")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response answerQuestion(@Context final HttpServletRequest request,
                                   @PathParam("appId") final String appId,
                                   final String body) {
        try {
            userManager.getCurrentRegisteredUser(request);
            return Response.ok().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }
}
