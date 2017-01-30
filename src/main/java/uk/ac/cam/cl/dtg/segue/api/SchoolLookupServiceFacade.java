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

import io.swagger.annotations.Api;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;

import com.google.inject.Inject;

/**
 * Segue School Lookup service.
 * 
 */
@Path("/schools")
@Api(value = "/schools")
public class SchoolLookupServiceFacade {
    private static final Logger log = LoggerFactory.getLogger(SchoolLookupServiceFacade.class);

    private SchoolListReader schoolListReader;

    /**
     * Injectable constructor.
     * 
     * @param schoolListReader
     *            - Instance of schools list Reader to initialise.
     */
    @Inject
    public SchoolLookupServiceFacade(final SchoolListReader schoolListReader) {
        this.schoolListReader = schoolListReader;
    }

    /**
     * Rest Endpoint that will return you a list of schools based on the query you provide.
     * 
     * @param request
     *            - for caching purposes.
     * @param schoolURN
     *            - find by urn.
     * @param searchQuery
     *            - query to search fields against.
     * @return A response containing a list of school objects or a SegueErrorResponse.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response schoolSearch(@Context final Request request, @QueryParam("query") final String searchQuery,
            @QueryParam("urn") final String schoolURN) {

        if ((null == searchQuery || searchQuery.isEmpty()) && (null == schoolURN || schoolURN.isEmpty())) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a search query or school URN")
                    .toResponse();
        }
        
        EntityTag etag = new EntityTag(schoolListReader.getDataLastModifiedDate().hashCode() + "");
        ResponseBuilder rb = request.evaluatePreconditions(etag);

        CacheControl cc = new CacheControl();
        cc.setMaxAge(Constants.NUMBER_SECONDS_IN_ONE_WEEK);
        cc.getCacheExtension().put("public", "");
        
        // If ETag matches the rb will be non-null;
        if (rb != null) {
            // Use the rb to return the response without any further processing
            log.debug("This school info is unchanged. Serving empty request with etag.");

            return rb.cacheControl(cc).tag(etag).build();
        }
        
        List<School> list;
        try {

            if (schoolURN != null && !schoolURN.isEmpty()) {
                list = Arrays.asList(schoolListReader.findSchoolById(schoolURN));
            } else {
                list = schoolListReader.findSchoolByNameOrPostCode(searchQuery);    
            }
            
        } catch (UnableToIndexSchoolsException | IOException e) {
            String message = "Unable to create / access the index of schools for the schools service.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message, e).toResponse();
        } catch (NumberFormatException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "The school urn provided is invalid.").toResponse();
        }

        return Response.ok(list).tag(etag).cacheControl(cc).build();
    }
}
