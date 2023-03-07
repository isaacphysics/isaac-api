package uk.ac.cam.cl.dtg.segue.etl;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.AbstractSegueFacade;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 *
 * Created by Ian on 17/10/2016.
 */
@Path("/etl")
@Tag(name = "/etl")
public class ETLFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(ETLFacade.class);

    private final ETLManager etlManager;

    /**
     * Constructor that provides a properties loader.
     *
     * @param properties the propertiesLoader.
     */
    @Inject
    public ETLFacade(PropertiesLoader properties, ETLManager manager) {
        super(properties, null);
        this.etlManager = manager;
    }

    @POST
    @Path("/set_version_alias/{alias}/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a content version alias.",
                  description = "This is primarily used to set the 'live' content version.")
    public Response setLiveVersion(@PathParam("alias") final String alias, @PathParam("version") final String version) {

        try {
            etlManager.setNamedVersion(alias, version);
            log.info("Finished processing ETL request");
            return Response.ok().build();
        } catch (Exception e) {
            log.error("Failed to set alias version:" + e.getMessage());
            log.info("Finished processing ETL request");
            return Response.serverError().entity(e.getMessage()).build();
        }

    }

    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Check the status of the ETL server.")
    public Response statusCheck() {
        return Response.ok().entity("{\"code\" : 200}").build();
    }

}
