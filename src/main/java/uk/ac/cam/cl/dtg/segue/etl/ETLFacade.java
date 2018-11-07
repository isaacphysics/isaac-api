package uk.ac.cam.cl.dtg.segue.etl;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.segue.api.AbstractSegueFacade;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * Created by Ian on 17/10/2016.
 */
@Path("/etl")
@Api(value = "/etl")
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

    @POST
    @Path("/new_version_alert/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response newVersionAlert(@PathParam("version") final String newVersion) {
        etlManager.notifyNewVersion(newVersion);
        log.info("Finished processing ETL request");
        return Response.ok().build();
    }

    @GET
    @Path("/ping")
    @Produces(MediaType.APPLICATION_JSON)
    public Response statusCheck() {
        return Response.ok().entity("{\"code\" : 200}").build();
    }

}
