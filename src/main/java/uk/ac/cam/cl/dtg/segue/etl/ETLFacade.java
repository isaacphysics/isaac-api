package uk.ac.cam.cl.dtg.segue.etl;

import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uk.ac.cam.cl.dtg.segue.api.AbstractSegueFacade;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Created by Ian on 17/10/2016.
 */
@Path("/etl")
@Tag(name = "/etl")
public class ETLFacade extends AbstractSegueFacade {

  /**
   * Constructor that provides a properties loader.
   *
   * @param properties the propertiesLoader.
   */
  @Inject
  public ETLFacade(final PropertiesLoader properties) {
    super(properties, null);
  }

  @GET
  @Path("/ping")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Check the status of the ETL server.")
  public Response statusCheck() {
    return Response.ok().entity("{\"code\" : 200}").build();
  }

}
