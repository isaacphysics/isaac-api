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

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.api.client.util.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;

import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
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
import uk.ac.cam.cl.dtg.isaac.api.managers.*;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.SegueContentFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
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
 * Events Facade.
 */
@Path("/events")
@Api(value = "/events")
public class EventsFacade extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(EventsFacade.class);

    private final EventBookingManager bookingManager;

    private final UserAccountManager userManager;

    private final IContentManager contentManager;
    private final String contentIndex;

    /**
     * EventsFacade.
     * 
     * @param properties
     *            - global properties map
     * @param logManager
     *            - for managing logs.
     * @param contentManager
     *            - for retrieving event content.
     * @param bookingManager
     *            - Instance of Booking Manager
     * @param userManager
     *            - Instance of User Manager
     */
    @Inject
    public EventsFacade(final PropertiesLoader properties, final ILogManager logManager,
                        final EventBookingManager bookingManager,
                        final UserAccountManager userManager, final IContentManager contentManager, @Named(Constants.CONTENT_INDEX) final String contentIndex) {
        super(properties, logManager);
        this.bookingManager = bookingManager;
        this.userManager = userManager;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
    }

    /**
     * REST end point to provide a list of events.
     * 
     * @param request
     *            - this allows us to check to see if a user is currently loggedin.
     * @param tags
     *            - a comma separated list of tags to include in the search.
     * @param startIndex
     *            - the initial index for the first result.
     * @param limit
     *            - the maximums number of results to return
     * @param sortOrder
     *            - flag to indicate preferred sort order.
     * @param showActiveOnly
     *            - true will impose filtering on the results. False will not. Defaults to false.
     * @param showInactiveOnly
     *            - true will impose filtering on the results. False will not. Defaults to false.
     * @return a Response containing a list of events objects or containing a SegueErrorResponse.
     */
    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getEvents(@Context final HttpServletRequest request, @QueryParam("tags") final String tags,
            @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index") final Integer startIndex,
            @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit") final Integer limit,
            @QueryParam("sort_by") final String sortOrder,
            @QueryParam("show_active_only") final Boolean showActiveOnly,
            @QueryParam("show_inactive_only") final Boolean showInactiveOnly,
            @QueryParam("show_booked_only") final Boolean showMyBookingsOnly) {
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
            filterInstructions.put(EVENT_ENDDATE_FIELDNAME, anyEventsFromNow);
            sortInstructions.put(EVENT_DATE_FIELDNAME, SortOrder.ASC);
        }

        if (null != showInactiveOnly && showInactiveOnly) {
            if (showActiveOnly) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        "You cannot request both show active and inactive only.").toResponse();
            }

            filterInstructions = Maps.newHashMap();
            DateRangeFilterInstruction anyEventsToNow = new DateRangeFilterInstruction(null, new Date());
            filterInstructions.put(EVENT_ENDDATE_FIELDNAME, anyEventsToNow);
            sortInstructions.put(EVENT_DATE_FIELDNAME, SortOrder.DESC);
        }

        try {
            ResultsWrapper<ContentDTO> findByFieldNames = null;

            if (showMyBookingsOnly) {
                RegisteredUserDTO currentUser = null;
                try {
                    currentUser = this.userManager.getCurrentRegisteredUser(request);
                } catch (NoUserLoggedInException e) {
                    /* Safe to ignore; will just leave currentUser null. */
                }
                if (null != currentUser) {
                    findByFieldNames = getEventsBookedByUser(request, fieldsToMatch.get(TAGS_FIELDNAME), currentUser);
                } else {
                    SegueErrorResponse.getNotLoggedInResponse();
                }
            } else {
                findByFieldNames = this.contentManager.findByFieldNames(
                    this.contentIndex, SegueContentFacade.generateDefaultFieldToMatch(fieldsToMatch),
                    newStartIndex, newLimit, sortInstructions, filterInstructions);

                // augment (maybe slow for large numbers of bookings)
                for (ContentDTO c : findByFieldNames.getResults()) {
                    this.augmentEventWithBookingInformation(request, c);
                }
            }

            return Response.ok(findByFieldNames).build();
        } catch (ContentManagerException e) {
            log.error("Error during event request", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the content you requested.")
                    .toResponse();
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error accessing your bookings.")
                .toResponse();
        }
    }

	/**
     * Get Events Booked by user.
     *
     * @param request - the http request so we can resolve booking information
     * @param tags - the tags we want to filter on
     * @param currentUser - the currently logged on user.
     * @return a list of event pages that the user has been booked
     * @throws SegueDatabaseException
     * @throws ContentManagerException
     */
    private ResultsWrapper<ContentDTO> getEventsBookedByUser(final HttpServletRequest request, final List<String> tags, final RegisteredUserDTO currentUser) throws SegueDatabaseException, ContentManagerException {
        List<ContentDTO> filteredResults = Lists.newArrayList();

        Map<String, BookingStatus> userBookingMap = this.bookingManager.getAllEventStatesForUser(currentUser.getId());

        for (String eventId : userBookingMap.keySet()) {
			if (BookingStatus.CANCELLED.equals(userBookingMap.get(eventId))) {
				continue;
			}

			final IsaacEventPageDTO eventDTOById = this.getEventDTOById(request, eventId);

			if (tags != null) {
				Set<String> tagsList = Sets.newHashSet(tags);
				tagsList.retainAll(eventDTOById.getTags()); // get intersection
				if (tagsList.size() == 0) {
					// if the intersection is empty then we can continue
					continue;
				}
			}

			filteredResults.add(eventDTOById);
		}
        return new ResultsWrapper<>(filteredResults, new Long(filteredResults.size()));
    }

    /**
     * REST end point to retrieve an event by id..
     * 
     * @param request
     *            - this allows us to check to see if a user is currently logged-in.
     * @param eventId
     *            - Id of the event of interest.
     * @return a Response containing a list of events objects or containing a SegueErrorResponse.
     */
    @GET
    @Path("/{event_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getEvent(@Context final HttpServletRequest request,
            @PathParam("event_id") final String eventId) {
        try {
            IsaacEventPageDTO page = getEventDTOById(request, eventId);

            return Response.ok(page).build();
        } catch (ContentManagerException e) {
            log.error("Error during event request", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the content you requested.")
                    .toResponse();
        } catch (SegueDatabaseException e) {
            log.error("Error during event request", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error resolving event bookings.")
                .toResponse();
        }
    }

    /**
     * getAllEventBookings.
     * 
     * @param request
     *            - so we can determine if the user is logged in
     * @param countOnly
     *            - If we only want to return a count
     * @return a list of booking objects
     */
    @GET
    @Path("/bookings")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getAllEventBookings(@Context final HttpServletRequest request,
            @QueryParam("count_only") final Boolean countOnly) {
        try {
            if (!isUserStaff(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access this endpoint.")
                        .toResponse();
            }

            if (countOnly != null && countOnly) {
                return Response.ok(ImmutableMap.of("count", bookingManager.getAllBookings().size())).build();
            }

            return Response.ok(ImmutableMap.of("results", bookingManager.getAllBookings())).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error occurred while trying to retrieve all event booking information.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        }
    }

    /**
     * Find a booking by id.
     * 
     * @param request
     *            - for authentication
     * @param bookingId
     *            - the booking of interest.
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
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access this endpoint.")
                        .toResponse();
            }

            return Response.ok(bookingManager.getBookingById(Long.parseLong(bookingId))).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NumberFormatException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "The booking id provided is invalid.").toResponse();
        } catch (ResourceNotFoundException e) {
            return new SegueErrorResponse(Status.NOT_FOUND, "The booking you requested does not exist.").toResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error occurred while trying to retrieve all event booking information.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        }
    }

    /**
     * Allow a staff user to promote a user from the waiting list.
     *
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - event booking containing updates, must contain primary id.
     * @param userId
     *            - the user to be promoted.
     * @param additionalInformation
     *            - additional information to be stored with this booking e.g. dietary requirements.
     * @return the updated booking.
     */
    @POST
    @Path("{event_id}/bookings/{user_id}/promote")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response promoteUserFromWaitingList(@Context final HttpServletRequest request,
                                                     @PathParam("event_id") final String eventId,
                                                     @PathParam("user_id") final Long userId, final Map<String, String> additionalInformation) {
        try {
            if (!isUserStaff(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be a staff user to access this endpoint.")
                    .toResponse();
            }

            RegisteredUserDTO userOfInterest = this.userManager.getUserDTOById(userId);

            IsaacEventPageDTO event = this.getEventDTOById(request, eventId);

            EventBookingDTO eventBookingDTO
                    = this.bookingManager.promoteFromWaitingListOrCancelled(event, userOfInterest);

            this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                    Constants.ADMIN_EVENT_WAITING_LIST_PROMOTION, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, event.getId(),
                                                                         USER_ID_FKEY_FIELDNAME, userId));
            return Response.ok(eventBookingDTO).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error occurred while trying to update a event booking";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                "Content Database error occurred while trying to retrieve event booking information.")
                .toResponse();
        } catch (EmailMustBeVerifiedException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "In order to book on this event the user account must have a verified email address. ")
                .toResponse();
        } catch (DuplicateBookingException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "The user has already been booked on this event. Unable to create a duplicate booking.")
                .toResponse();
        } catch (EventIsFullException e) {
            return new SegueErrorResponse(Status.CONFLICT,
                "This event is already full. Unable to book the user on to it.")
                .toResponse();
        } catch (EventBookingUpdateException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "Unable to modify the booking", e)
                .toResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "The user doesn't exist, so unable to book them onto an event", e)
                .toResponse();
        }
    }

    /**
     * gets a list of event bookings based on a given event id.
     * 
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - the event of interest.
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
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access this endpoint.")
                        .toResponse();
            }

            return Response.ok(bookingManager.getBookingByEventId(eventId)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error occurred while trying to retrieve all event booking information.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    /**
     * createBooking for a specific isaac user.
     * - Will attempt to create a waiting list booking if the event is already full.
     * - Must be a Staff user.
     * 
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - event id
     * @param userId
     *            - user id
     * @param additionalInformation
     *            - additional information to be stored with this booking e.g. dietary requirements.
     * @return the new booking
     */
    @POST
    @Path("{event_id}/bookings/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response createBookingForGivenUser(@Context final HttpServletRequest request,
            @PathParam("event_id") final String eventId, @PathParam("user_id") final Long userId, final Map<String, String> additionalInformation) {
        try {
            if (!isUserStaff(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access this endpoint.")
                        .toResponse();
            }

            RegisteredUserDTO bookedUser = userManager.getUserDTOById(userId);

            IsaacEventPageDTO event = this.getEventDTOById(request, eventId);

            if (bookingManager.isUserBooked(eventId, userId)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "User is already booked on this event.")
                        .toResponse();
            }

            EventBookingDTO booking = bookingManager.createBookingOrAddToWaitingList(event, bookedUser, additionalInformation);
            this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                    Constants.ADMIN_EVENT_BOOKING_CONFIRMED, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, event.getId(), USER_ID_FKEY_FIELDNAME, userId));

            return Response.ok(booking).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error occurred while trying to book a user onto an event.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.NOT_FOUND, "Unable to locate the user requested").toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Content Database error occurred while trying to retrieve all event booking information.")
                    .toResponse();
        } catch (DuplicateBookingException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "User already booked on this event. Unable to create a duplicate booking.")
                .toResponse();
        }
    }

    /**
     * createBooking for the current user.
     *
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - event id
     * @return the new booking if allowed to book.
     */
    @POST
    @Path("{event_id}/bookings")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response createBookingForMe(@Context final HttpServletRequest request,
                                             @PathParam("event_id") final String eventId, final Map<String, String> additionalInformation) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            IsaacEventPageDTO event = this.getEventDTOById(request, eventId);

            if (EventStatus.CLOSED.equals(event.getEventStatus())){
                return new SegueErrorResponse(Status.BAD_REQUEST, "Sorry booking for this event is closed. Please try again later.")
                    .toResponse();
            }

            if (bookingManager.isUserBooked(eventId, user.getId())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You are already booked on this event.")
                    .toResponse();
            }

            EventBookingDTO eventBookingDTO = bookingManager.requestBooking(event, user, additionalInformation);

            this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                    Constants.EVENT_BOOKING, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, event.getId()));

            return Response.ok(eventBookingDTO).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error occurred while trying to book a user onto an event.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                "Content Database error occurred while trying to retrieve all event booking information.")
                .toResponse();
        } catch (EmailMustBeVerifiedException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "In order to book on this event your user account must have a verified email address. Please verify your address to make a booking.")
                .toResponse();
        } catch (DuplicateBookingException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "You have already been booked on this event. Unable to create a duplicate booking.")
                .toResponse();
        } catch (EventIsFullException e) {
            return new SegueErrorResponse(Status.CONFLICT,
                "This event is already full. Unable to book you on to it.")
                .toResponse();
        } catch (EventDeadlineException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "The booking deadline for this event has passed. No more bookings are being accepted.")
                .toResponse();
        }
    }

    /**
     * Add current user to waiting list for the given event.
     *
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - event id
     * @return the new booking
     */
    @POST
    @Path("{event_id}/waiting_list")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response addMeToWaitingList(@Context final HttpServletRequest request,
                                             @PathParam("event_id") final String eventId, final Map<String, String> additionalInformation) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            IsaacEventPageDTO event = this.getEventDTOById(request, eventId);

            if (bookingManager.isUserBooked(eventId, user.getId())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You are already booked on this event.")
                    .toResponse();
            }

            EventBookingDTO eventBookingDTO = bookingManager.requestWaitingListBooking(event, user, additionalInformation);
            this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                    Constants.EVENT_WAITING_LIST_BOOKING, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, event.getId()));

            return Response.ok(eventBookingDTO).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error occurred while trying to book a user onto an event.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                "Content Database error occurred while trying to retrieve all event booking information.")
                .toResponse();
        } catch (EmailMustBeVerifiedException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "In order to book on this event your user account must have a verified email address. Please verify your address to make a booking.")
                .toResponse();
        } catch (DuplicateBookingException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "You have already been booked on this event. Unable to create a duplicate booking.")
                .toResponse();
        } catch (EventDeadlineException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "The booking deadline for this event has passed. No more bookings are being accepted.")
                .toResponse();
        } catch (EventIsNotFullException e) {
            return new SegueErrorResponse(Status.CONFLICT,
                "There are spaces on this event and the deadline has not passed. Please use the request booking endpoint to book you on to it.")
                .toResponse();
        }
    }

    /**
     * This function allows a user who has booked onto an event to cancel their booking.
     *
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - event id
     * @return the new booking
     */
    @DELETE
    @Path("{event_id}/bookings/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response cancelBooking(@Context final HttpServletRequest request,
                                        @PathParam("event_id") final String eventId) {
        return this.cancelBooking(request, eventId, null);
    }

    /**
     * This function allows cancellation of a booking.
     *
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - event id
     * @param userId
     *            - user id
     * @return the new booking
     */
    @DELETE
    @Path("{event_id}/bookings/{user_id}/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response cancelBooking(@Context final HttpServletRequest request,
                                        @PathParam("event_id") final String eventId, @PathParam("user_id") final Long userId) {
        try {
            IsaacEventPageDTO event = this.getEventDTOById(request, eventId);

            RegisteredUserDTO userLoggedIn = this.userManager.getCurrentRegisteredUser(request);
            RegisteredUserDTO userOwningBooking;

            if (null == userId) {
                userOwningBooking = userLoggedIn;
            } else {
                userOwningBooking = this.userManager.getUserDTOById(userId);
            }

            if (event.getDate() != null && new Date().after(event.getDate())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You cannot cancel a booking on an event that has already started.")
                    .toResponse();
            }

            // if the user id is null then it means they are changing their own booking.
            if (userId != null && !isUserStaff(userManager, request) ) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to change another user's booking.")
                    .toResponse();
            }

            if (!bookingManager.hasBookingWithStatus(eventId, userOwningBooking.getId(), BookingStatus.WAITING_LIST) && !bookingManager.hasBookingWithStatus(eventId, userOwningBooking.getId(), BookingStatus.CONFIRMED)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "User is not booked on this event.").toResponse();
            }

            bookingManager.cancelBooking(event, userOwningBooking);

            if (!userOwningBooking.equals(userLoggedIn)) {
                this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                        Constants.ADMIN_EVENT_BOOKING_CANCELLED, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, event.getId(), USER_ID_FKEY_FIELDNAME, userOwningBooking.getId()));
            } else {
                this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                        Constants.EVENT_BOOKING_CANCELLED, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, event.getId()));
            }

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error occurred while trying to delete an event booking.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (ContentManagerException e) {
            log.error("Error during event request", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the content you requested.")
                .toResponse();
        } catch (NoUserException e) {
            return SegueErrorResponse.getResourceNotFoundResponse("Unable to locate user specified.");
        }
    }

    /**
     * This function allows an administrator to attempt to resend the last confirmation email send for a given booking.
     *
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - event id
     * @param userId
     *            - user id
     * @return the new booking
     */
    @POST
    @Path("{event_id}/bookings/{user_id}/resend_confirmation")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response resendEventEmail(@Context final HttpServletRequest request,
                                        @PathParam("event_id") final String eventId, @PathParam("user_id") final Long userId) {
        try {
            if (!isUserAnAdminOrEventManager(userManager,request)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            IsaacEventPageDTO event = this.getEventDTOById(request, eventId);
            RegisteredUserDTO user = this.userManager.getUserDTOById(userId);

            this.bookingManager.resendEventEmail(event, user);

            log.info(String.format("User (%s) has just resent an event email to user id (%s)",
                    this.userManager.getCurrentRegisteredUser(request).getEmail(), user.getId()));

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error occurred while trying to resend an event email.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (ContentManagerException e) {
            log.error("Error during event request", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the content you requested.")
                    .toResponse();
        } catch (NoUserException e) {
            return SegueErrorResponse.getResourceNotFoundResponse("Unable to locate user specified.");
        }
    }

    /**
     * Delete a booking.
     *
     * This is an admin function to allow staff to delete a booking permanently.
     *
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - event id
     * @param userId
     *            - user id
     * @return the new booking
     */
    @DELETE
    @Path("{event_id}/bookings/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response deleteBooking(@Context final HttpServletRequest request,
            @PathParam("event_id") final String eventId, @PathParam("user_id") final Long userId) {
        try {
            if (!isUserAnAdmin(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an Admin user to access this endpoint.")
                        .toResponse();
            }

            if (!bookingManager.hasBookingWithStatus(eventId, userId, BookingStatus.WAITING_LIST) && !bookingManager.hasBookingWithStatus(eventId, userId, BookingStatus.CONFIRMED)
                && !bookingManager.hasBookingWithStatus(eventId, userId, BookingStatus.CANCELLED)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "User is not booked on this event.").toResponse();
            }

            IsaacEventPageDTO event = this.getEventDTOById(request, eventId);
            RegisteredUserDTO user = this.userManager.getUserDTOById(userId);

            bookingManager.deleteBooking(event, user);

            this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                    Constants.ADMIN_EVENT_BOOKING_DELETED, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, eventId, USER_ID_FKEY_FIELDNAME, userId));

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error occurred while trying to delete an event booking.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (NoUserException e) {
            return SegueErrorResponse.getResourceNotFoundResponse("Unable to locate user specified.");
        } catch (ContentManagerException e) {
            log.error("Error during event request", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the content you requested.")
                    .toResponse();
        }
    }

    /**
     * REST end point to provide a list of events.
     *
     * @param request
     *            - this allows us to check to see if a user is currently loggedin.
     * @param startIndex
     *            - the initial index for the first result.
     * @param limit
     *            - the maximums number of results to return
     * @param showActiveOnly
     *            - true will impose filtering on the results. False will not. Defaults to false.
     * @return a Response containing a list of events objects or containing a SegueErrorResponse.
     */
    @GET
    @Path("/overview")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getEventOverviews(@Context final HttpServletRequest request,
                                    @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index") final Integer startIndex,
                                    @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit") final Integer limit,
                                    @QueryParam("show_active_only") final Boolean showActiveOnly) {
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();

        Integer newLimit = null;
        Integer newStartIndex = null;
        if (limit != null) {
            newLimit = limit;
        }

        if (startIndex != null) {
            newStartIndex = startIndex;
        }

        final Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
        sortInstructions.put(EVENT_DATE_FIELDNAME, SortOrder.DESC);

        fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(EVENT_TYPE));

        Map<String, AbstractFilterInstruction> filterInstructions = null;
        if (null == showActiveOnly || showActiveOnly) {
            filterInstructions = Maps.newHashMap();
            DateRangeFilterInstruction anyEventsFromNow = new DateRangeFilterInstruction(new Date(), null);
            filterInstructions.put(EVENT_ENDDATE_FIELDNAME, anyEventsFromNow);
            sortInstructions.put(EVENT_DATE_FIELDNAME, SortOrder.ASC);
        }

        try {
            if (!isUserStaff(userManager, request)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            ResultsWrapper<ContentDTO> findByFieldNames = null;

            findByFieldNames = this.contentManager.findByFieldNames(
                this.contentIndex, SegueContentFacade.generateDefaultFieldToMatch(fieldsToMatch),
                newStartIndex, newLimit, sortInstructions, filterInstructions);

            List<Map<String, Object>> resultList = Lists.newArrayList();

            for (ContentDTO c : findByFieldNames.getResults()) {
                if (!(c instanceof  IsaacEventPageDTO)) {
                    continue;
                }

                IsaacEventPageDTO e = (IsaacEventPageDTO) c;
                ImmutableMap.Builder<String, Object> eventOverviewBuilder = new ImmutableMap.Builder<>();
                eventOverviewBuilder.put("id", e.getId());
                eventOverviewBuilder.put("title", e.getTitle());
                eventOverviewBuilder.put("subtitle", e.getSubtitle());
                eventOverviewBuilder.put("date", e.getDate());
                eventOverviewBuilder.put("bookingDeadline",
                        e.getBookingDeadline() == null ? e.getDate() : e.getBookingDeadline());
                eventOverviewBuilder.put("eventStatus", e.getEventStatus());

                if (null != e.getLocation()) {
                    eventOverviewBuilder.put("location", e.getLocation());
                }

                eventOverviewBuilder.put("numberOfConfirmedBookings",
                        this.bookingManager.countNumberOfBookingsWithStatus(e.getId(), BookingStatus.CONFIRMED));
                eventOverviewBuilder.put("numberOfWaitingListBookings",
                        this.bookingManager.countNumberOfBookingsWithStatus(e.getId(), BookingStatus.WAITING_LIST));

                if (null != e.getNumberOfPlaces()) {
                    eventOverviewBuilder.put("numberOfPlaces", e.getNumberOfPlaces());
                }

                resultList.add(eventOverviewBuilder.build());
            }

            return Response.ok(new ResultsWrapper<>(resultList, findByFieldNames.getTotalResults())).build();
        } catch (ContentManagerException e) {
            log.error("Error during event request", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the content you requested.")
                .toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Error occurred during event overview look up", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the database content you requested.")
                .toResponse();
        }
    }

    /**
     * REST end point to provide a summary of events suitable for mapping.
     *
     * @param request
     *            - this allows us to check to see if a user is currently logged in.
     * @param startIndex
     *            - the initial index for the first result.
     * @param limit
     *            - the maximums number of results to return
     * @param showActiveOnly
     *            - true will impose filtering on the results. False will not. Defaults to false.
     * @return a Response containing a list of event map summaries or containing a SegueErrorResponse.
     */
    @GET
    @Path("/map_data")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getEventMapData(@Context final HttpServletRequest request, @QueryParam("tags") final String tags,
                                            @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index") final Integer startIndex,
                                            @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit") final Integer limit,
                                            @QueryParam("show_active_only") final Boolean showActiveOnly) {
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
        sortInstructions.put(EVENT_DATE_FIELDNAME, SortOrder.DESC);

        fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(EVENT_TYPE));

        Map<String, AbstractFilterInstruction> filterInstructions = null;
        if (null == showActiveOnly || showActiveOnly) {
            filterInstructions = Maps.newHashMap();
            DateRangeFilterInstruction anyEventsFromNow = new DateRangeFilterInstruction(new Date(), null);
            filterInstructions.put(EVENT_ENDDATE_FIELDNAME, anyEventsFromNow);
            sortInstructions.put(EVENT_DATE_FIELDNAME, SortOrder.ASC);
        }

        try {
            ResultsWrapper<ContentDTO> findByFieldNames = null;

            findByFieldNames = this.contentManager.findByFieldNames(this.contentIndex,
                    SegueContentFacade.generateDefaultFieldToMatch(fieldsToMatch),
                    newStartIndex, newLimit, sortInstructions, filterInstructions);

            List<Map<String, Object>> resultList = Lists.newArrayList();

            for (ContentDTO c : findByFieldNames.getResults()) {
                if (!(c instanceof  IsaacEventPageDTO)) {
                    continue;
                }

                IsaacEventPageDTO e = (IsaacEventPageDTO) c;
                if (null == e.getLocation() || (null == e.getLocation().getLatitude() && null == e.getLocation().getLongitude())) {
                    // Ignore events without locations.
                    continue;
                }
                if (e.getLocation().getLatitude().equals(0.0) && e.getLocation().getLongitude().equals(0.0)) {
                    // Ignore events with locations that haven't been set properly.
                    log.info("Event with 0.0 lat/long:  " + e.getId());
                    continue;
                }

                ImmutableMap.Builder<String, Object> eventOverviewBuilder = new ImmutableMap.Builder<>();
                eventOverviewBuilder.put("id", e.getId());
                eventOverviewBuilder.put("title", e.getTitle());
                eventOverviewBuilder.put("date", e.getDate());
                eventOverviewBuilder.put("subtitle", e.getSubtitle());
                if (e.getEventStatus() != null) {
                    eventOverviewBuilder.put("status", e.getEventStatus());
                }
                // The schema required needs lat and long at top-level, so add address at top-level too.
                eventOverviewBuilder.put("address", e.getLocation().getAddress());
                eventOverviewBuilder.put("latitude", e.getLocation().getLatitude());
                eventOverviewBuilder.put("longitude", e.getLocation().getLongitude());

                if (null != e.getBookingDeadline()) {
                    eventOverviewBuilder.put("deadline", e.getBookingDeadline());
                }

                resultList.add(eventOverviewBuilder.build());
            }

            return Response.ok(new ResultsWrapper<>(resultList, findByFieldNames.getTotalResults())).build();
        } catch (ContentManagerException e) {
            log.error("Error during event request", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the content you requested.")
                    .toResponse();
        }
    }

    /**
     * A helper method for retrieving an event and the number of places available and if the user is booked or not.
     *
     *
     * @param request so we can determine if the user is logged in
     * @param id the id of the event of interest
     * @return the fully populated event dto with user context information.
     * @throws ContentManagerException - if there is a problem finding the event information
     * @throws SegueDatabaseException if there is a database error.
	 */
    private IsaacEventPageDTO getEventDTOById(final HttpServletRequest request, final String id) throws ContentManagerException, SegueDatabaseException {

        ContentDTO c = this.contentManager.getContentById(this.contentManager.getCurrentContentSHA(), id);

        if (null == c) {
            throw new ResourceNotFoundException(String.format("Unable to locate the event with id; %s", id));
        }

        IsaacEventPageDTO page = null;
        if (c instanceof IsaacEventPageDTO) {
            page = (IsaacEventPageDTO) c;

            try {
                RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

                Boolean userBooked = this.bookingManager.isUserBooked(id, user.getId());
                page.setUserBooked(userBooked);
                page.setUserOnWaitList(this.bookingManager.hasBookingWithStatus(id, user.getId(), BookingStatus.WAITING_LIST));
            } catch (NoUserLoggedInException e) {
                // no action as we don't require the user to be logged in.
                page.setUserBooked(null);
            }

            page.setPlacesAvailable(this.bookingManager.getPlacesAvailable(page));
        }
        return page;
    }

	/**
     * Augment a single event with booking information before we send it out.
     *
     * @param request - for user look up
     * @param possibleEvent - a ContentDTO that should hopefully be an IsaacEventPageDTO.
     * @return an augmented IsaacEventPageDTO.
     * @throws SegueDatabaseException
     */
    private IsaacEventPageDTO augmentEventWithBookingInformation(final HttpServletRequest request, final ContentDTO possibleEvent) throws SegueDatabaseException {
        if (possibleEvent instanceof IsaacEventPageDTO) {
            IsaacEventPageDTO page = (IsaacEventPageDTO) possibleEvent;

            try {
                RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

                Boolean userBooked = this.bookingManager.isUserBooked(page.getId(), user.getId());
                page.setUserBooked(userBooked);
                page.setUserOnWaitList(this.bookingManager.hasBookingWithStatus(page.getId(), user.getId(), BookingStatus.WAITING_LIST));
            } catch (NoUserLoggedInException e) {
                // no action as we don't require the user to be logged in.
                page.setUserBooked(null);
            }

            page.setPlacesAvailable(this.bookingManager.getPlacesAvailable(page));
            return page;
        } else {
            throw new ClassCastException("The object provided was not an event.");
        }
    }
}