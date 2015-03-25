/**
 * Copyright 2015 Stephen Cummins
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Maps;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Games boards Facade.
 */
@Path("/events")
public class EventsFacade extends AbstractIsaacFacade {
	private static final Logger log = LoggerFactory
			.getLogger(EventsFacade.class);
	
	private final ContentVersionController versionManager;

	/**
	 * EventsFacade. 
	 * 
	 * @param properties
	 *            - global properties map
	 * @param logManager
	 *            - for managing logs.
	 * @param versionManager
	 *            - for retrieving event content.
	 */
	@Inject
	public EventsFacade(final PropertiesLoader properties, final ILogManager logManager,
			final ContentVersionController versionManager) {
		super(properties, logManager);
		this.versionManager = versionManager;
		
	}
	
	/**
	 * REST end point to provide a list of events.
	 * 
	 * @param request
	 *            - this allows us to check to see if a user is currently
	 *            loggedin.
	 * @param tags
	 *            - a comma separated list of tags to include in the search.
	 * @return a Response containing a list of events objects or containing
	 *         a SegueErrorResponse.
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getEvents(
			@Context final HttpServletRequest request,
			@QueryParam("type") final String tags) {
		// TODO: finish implementing this.
		// TODO: order by date
		// TODO: filter by tags
		// TODO: filter by location
		// TODO: pagination.
		
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(EVENT_TYPE));
		
		try {
			ResultsWrapper<ContentDTO> findByFieldNames = this.versionManager.getContentManager()
					.findByFieldNames(versionManager.getLiveVersion(),
							SegueApiFacade.generateDefaultFieldToMatch(fieldsToMatch), 0, -1);
			return Response.ok(findByFieldNames).build();
			
		} catch (ContentManagerException e) {
			log.error("Error during event request", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error locating the content you requested.").toResponse();
		}
	}	
}
