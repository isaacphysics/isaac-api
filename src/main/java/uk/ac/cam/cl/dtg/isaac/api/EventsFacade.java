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
import javax.ws.rs.POST;
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

import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
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

	private EventBookingPersistenceManager bookingManager;

	private UserManager userManager;
	
	/**
	 * EventsFacade. 
	 * 
	 * @param properties
	 *            - global properties map
	 * @param logManager
	 *            - for managing logs.
	 * @param versionManager
	 *            - for retrieving event content.
	 * @param bookingManager
	 *            - Instance of Booking Manager
	 * @param userManager
	 *            - Instance of User Manager
	 */
	@Inject
	public EventsFacade(final PropertiesLoader properties, final ILogManager logManager,
			final ContentVersionController versionManager,
			final EventBookingPersistenceManager bookingManager,
			final UserManager userManager) {
		super(properties, logManager);
		this.versionManager = versionManager;
		this.bookingManager = bookingManager;
		this.userManager = userManager;
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
	 *            - true will impose filtering on the results. False
	 *            will not. Defaults to false.
	 * @param showInactiveOnly
	 *            - true will impose filtering on the results. False
	 *            will not. Defaults to false.
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
			@QueryParam("show_active_only") final Boolean showActiveOnly,
			@QueryParam("show_inactive_only") final Boolean showInactiveOnly) {
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
		
		if (showInactiveOnly != null && showInactiveOnly) {
			if (showActiveOnly) {
				return new SegueErrorResponse(Status.BAD_REQUEST,
						"You cannot request both show active and in active only.").toResponse();
			}
			
			filterInstructions = Maps.newHashMap();
			DateRangeFilterInstruction anyEventsToNow = new DateRangeFilterInstruction(null, new Date());
			filterInstructions.put(EVENT_DATE_FIELDNAME, anyEventsToNow);
			sortInstructions.put(EVENT_DATE_FIELDNAME, SortOrder.DESC);
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
	
	/**
	 * getAllEventBookings.
	 * @param request - for authentication
	 * @return a list of booking objects
	 */
	@GET
	@Path("/bookings")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getAllEventBookings(@Context final HttpServletRequest request) {
		try {			
			if (!isUserStaff(userManager, request)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an admin user to access this endpoint.").toResponse();
			}
			
			return Response.ok(bookingManager.getAllBookings()).build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Database error ocurred while trying to retrieve all event booking information.")
					.toResponse();
		}
	}
	
	/**
	 * Find a booking by id.
	 * @param request - for authentication
	 * @param bookingId - the booking of interest.
	 * @return The booking information.
	 */
	@GET
	@Path("/bookings/{booking_id}")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getEventBookingById(@Context final HttpServletRequest request,
			@PathParam("booking_id") final String bookingId) {
		try {
			if (!isUserStaff(userManager, request)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an admin user to access this endpoint.").toResponse();
			}
			
			return Response.ok(bookingManager.getBookingById(Long.parseLong(bookingId))).build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (NumberFormatException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"The booking id provided is invalid.")
					.toResponse();
		}  catch (ResourceNotFoundException e) {
			return new SegueErrorResponse(Status.NOT_FOUND,
					"The booking you requested does not exist.")
					.toResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Database error ocurred while trying to retrieve all event booking information.")
					.toResponse();
		}
	}

	/**
	 * gets a list of event bookings based on a given event id.
	 * @param request - for authentication
	 * @param eventId - the event of interest.
	 * @return list of bookings.
	 */
	@GET
	@Path("{event_id}/bookings")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response getEventBookingByEventId(@Context final HttpServletRequest request,
			@PathParam("event_id") final String eventId) {
		try {
			if (!isUserStaff(userManager, request)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an admin user to access this endpoint.").toResponse();
			}

			return Response.ok(bookingManager.getBookingByEventId(eventId)).build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Database error ocurred while trying to retrieve all event booking information.")
					.toResponse();
		}
	}
	

	/**
	 * createBooking.
	 * 
	 * @param request - for authentication
	 * @param eventId - event id
	 * @param userId - user id
	 * @return the new booking
	 */
	@POST
	@Path("{event_id}/bookings/{user_id}")
	@Produces(MediaType.APPLICATION_JSON)
	@GZIP
	public final Response createBooking(@Context final HttpServletRequest request,
			@PathParam("event_id") final String eventId, @PathParam("user_id") final String userId) {
		try {
			if (!isUserStaff(userManager, request)) {
				return new SegueErrorResponse(Status.FORBIDDEN,
						"You must be an admin user to access this endpoint.").toResponse();
			}

			RegisteredUserDTO bookedUser = userManager.getUserDTOById(userId);
			
			ContentDTO event = this.versionManager.getContentManager().getContentById(
					versionManager.getLiveVersion(), eventId);
			
			// TODO: make it so anyone can book on to a future event.
			
			if (null == event) {
				return new SegueErrorResponse(Status.NOT_FOUND,
						"Unable to locate the event requested")
						.toResponse();
			}
			
			if (bookingManager.isUserBooked(eventId, userId)) {
				return new SegueErrorResponse(Status.BAD_REQUEST, "User is already booked on this event.")
						.toResponse();
			}
			
			return Response.ok(bookingManager.createBooking(eventId, bookedUser.getDbId())).build();
		} catch (NoUserLoggedInException e) {
			return SegueErrorResponse.getNotLoggedInResponse();
		} catch (SegueDatabaseException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Database error ocurred while trying to retrieve all event booking information.")
					.toResponse();
		} catch (NoUserException e) {
			return new SegueErrorResponse(Status.NOT_FOUND,
					"Unable to locate the user requested")
					.toResponse();
		} catch (ContentManagerException e) {
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Content Database error ocurred while trying to retrieve all event booking information.")
					.toResponse();
		}
	}	
}
