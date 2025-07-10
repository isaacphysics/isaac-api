package uk.ac.cam.cl.dtg.isaac.api;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/hello")
public class HelloFacade extends AbstractIsaacFacade {
    @Inject
    public HelloFacade(final AbstractConfigLoader properties, final ILogManager logManager) {
        super(properties, logManager);
    }

    @GET
    @Path("/")
    public final Response hello() {
        return Response.ok("Hello World!").build();
    }
}
