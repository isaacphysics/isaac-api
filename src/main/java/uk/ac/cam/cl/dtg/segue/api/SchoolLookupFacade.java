package uk.ac.cam.cl.dtg.segue.api;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Segue School Lookup service.
 * 
 */
@Path("/")
public class SchoolLookupFacade {
	private static final Logger log = LoggerFactory.getLogger(SchoolLookupFacade.class);

	/**
	 * SchoolLookupService default constructor.
	 */
	public SchoolLookupFacade() {

	}

	/**
	 * Rest Endpoint that will return you a list of schools based on the query
	 * you provide.
	 * 
	 * @param searchQuery
	 *            - query to search fields against.
	 * @return A response containing a list of school objects or a
	 *         SegueErrorResponse.
	 */
	@GET
	@Produces("application/json")
	@Path("schools")
	public Response schoolSearch(@QueryParam("query") final String searchQuery) {
		if (null == searchQuery || searchQuery.isEmpty()) {
			return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a search query")
					.toResponse();
		}

		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		SchoolListReader reader = injector.getInstance(SchoolListReader.class);

		List<School> list;
		try {
			list = reader.findSchoolByNameOrPostCode(searchQuery);
		} catch (UnableToIndexSchoolsException e) {
			String message = "Unable to create / access the index of schools for the schools service.";
			log.error(message, e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message, e).toResponse();
		}

		return Response.ok(list).build();
	}
}
