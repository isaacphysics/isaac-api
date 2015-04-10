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
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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

import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.DateRangeFilterInstruction;
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
	 * @param startIndex
	 *            - the initial index for the first result.
	 * @param limit
	 *            - the maximums number of results to return
	 * @param sortOrder
	 *            - flag to indicate preferred sort order.
	 * @param showActiveOnly
	 *            - true will not impose any filtering on the results. False
	 *            will show only dates in the future. Defaults to false.
	 * @return a Response containing a list of events objects or containing a
	 *         SegueErrorResponse.
	 */
	@GET
	@Path("/")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getEvents(
			@Context final HttpServletRequest request,
			@QueryParam("tags") final String tags, 
			@DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index") final Integer startIndex,
			@DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit") final Integer limit,
			@QueryParam("sort_by") final String sortOrder,
			@QueryParam("show_active_only") final Boolean showActiveOnly) {
		// TODO: filter by location
		Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
		
		Integer newLimit = null;
		Integer newStartIndex = null;
		if (limit != null) {
			newLimit = limit;
		}
		
		if (startIndex != null) {
			newStartIndex = startIndex;
		}
		
		if (tags != null) {
			fieldsToMatch.put(TAGS_FIELDNAME, Arrays.asList(tags.split(",")));
		}
		
		final Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
		if (sortOrder != null && sortOrder.equals("title")) {
			sortInstructions.put(Constants.TITLE_FIELDNAME + "." + Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX,
					SortOrder.ASC);
		} else {
			sortInstructions.put(EVENT_DATE_FIELDNAME, SortOrder.DESC);
		}
		
		fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(EVENT_TYPE));
		
		Map<String, AbstractFilterInstruction> filterInstructions = null;
		if (null == showActiveOnly || showActiveOnly) {
			filterInstructions = Maps.newHashMap();
			DateRangeFilterInstruction anyEventsFromNow = new DateRangeFilterInstruction(new Date(), null);
			filterInstructions.put(EVENT_DATE_FIELDNAME, anyEventsFromNow);
			sortInstructions.put(EVENT_DATE_FIELDNAME, SortOrder.ASC);
		} 
		
		try {
			ResultsWrapper<ContentDTO> findByFieldNames = this.versionManager.getContentManager()
					.findByFieldNames(versionManager.getLiveVersion(),
							SegueApiFacade.generateDefaultFieldToMatch(fieldsToMatch), newStartIndex,
							newLimit, sortInstructions, filterInstructions);
			
			return Response.ok(findByFieldNames).build();
		} catch (ContentManagerException e) {
			log.error("Error during event request", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error locating the content you requested.").toResponse();
		}
	}
	
	/**
	 * REST end point to retrieve an event by id..
	 * 
	 * @param request
	 *            - this allows us to check to see if a user is currently
	 *            logged-in.
	 * @param eventId
	 *            - Id of the event of interest.
	 * @return a Response containing a list of events objects or containing
	 *         a SegueErrorResponse.
	 */
	@GET
	@Path("/{event_id}")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getEvent(
			@Context final HttpServletRequest request,
			@PathParam("event_id") final String eventId) {
		
		try {
			ContentDTO c = this.versionManager.getContentManager().getContentById(
					versionManager.getLiveVersion(), eventId);

			if (null == c) {
				return SegueErrorResponse.getResourceNotFoundResponse("The event requested could not be located");
			}
			
			if (c instanceof IsaacEventPageDTO) {
				return Response.ok(c).build();
			} else {
				return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
						"The content found is of the incorrect type.").toResponse();
			}

		} catch (ContentManagerException e) {
			log.error("Error during event request", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Error locating the content you requested.").toResponse();
		}
	}		
}
