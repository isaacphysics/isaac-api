/*
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
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.opencsv.CSVWriter;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateBookingException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingUpdateException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventDeadlineException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventGroupReservationLimitException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventIsFullException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventIsNotFullException;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.SegueContentFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.DateRangeFilterInstruction;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

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
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final GroupManager groupManager;

    private final IContentManager contentManager;
    private final String contentIndex;
    private final UserBadgeManager userBadgeManager;
    private final UserAssociationManager userAssociationManager;
    private final UserAccountManager userAccountManager;
    private final SchoolListReader schoolListReader;

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
                        final UserAccountManager userManager, final IContentManager contentManager,
                        @Named(Constants.CONTENT_INDEX) final String contentIndex,
                        final UserBadgeManager userBadgeManager,
                        final UserAssociationManager userAssociationManager,
                        final GroupManager groupManager,
                        final UserAccountManager userAccountManager,
                        final SchoolListReader schoolListReader) {
        super(properties, logManager);
        this.bookingManager = bookingManager;
        this.userManager = userManager;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.userBadgeManager = userBadgeManager;
        this.userAssociationManager = userAssociationManager;
        this.groupManager = groupManager;
        this.userAccountManager = userAccountManager;
        this.schoolListReader = schoolListReader;
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
    @ApiOperation(value = "List events matching the provided criteria.")
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
            sortInstructions.put(DATE_FIELDNAME, SortOrder.DESC);
        }

        fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(EVENT_TYPE));

        Map<String, AbstractFilterInstruction> filterInstructions = null;
        if (null == showActiveOnly || showActiveOnly) {
            filterInstructions = Maps.newHashMap();
            DateRangeFilterInstruction anyEventsFromNow = new DateRangeFilterInstruction(new Date(), null);
            filterInstructions.put(ENDDATE_FIELDNAME, anyEventsFromNow);
            sortInstructions.put(DATE_FIELDNAME, SortOrder.ASC);
        }

        if (null != showInactiveOnly && showInactiveOnly) {
            if (showActiveOnly) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        "You cannot request both show active and inactive only.").toResponse();
            }

            filterInstructions = Maps.newHashMap();
            DateRangeFilterInstruction anyEventsToNow = new DateRangeFilterInstruction(null, new Date());
            filterInstructions.put(ENDDATE_FIELDNAME, anyEventsToNow);
            sortInstructions.put(DATE_FIELDNAME, SortOrder.DESC);
        }

        try {
            ResultsWrapper<ContentDTO> findByFieldNames = null;

            if (null != showMyBookingsOnly && showMyBookingsOnly) {
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
    private ResultsWrapper<ContentDTO> getEventsBookedByUser(final HttpServletRequest request, final List<String> tags,
                                                             final RegisteredUserDTO currentUser)
            throws SegueDatabaseException, ContentManagerException {
        List<ContentDTO> filteredResults = Lists.newArrayList();

        Map<String, BookingStatus> userBookingMap = this.bookingManager.getAllEventStatesForUser(currentUser.getId());

        for (String eventId : userBookingMap.keySet()) {
			if (BookingStatus.CANCELLED.equals(userBookingMap.get(eventId))) {
				continue;
			}

			final IsaacEventPageDTO eventDTOById = this.getAugmentedEventDTOById(request, eventId);

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
        return new ResultsWrapper<>(filteredResults, (long) filteredResults.size());
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
    @ApiOperation(value = "Get details about a specific event.")
    public final Response getEvent(@Context final HttpServletRequest request,
            @PathParam("event_id") final String eventId) {
        try {
            IsaacEventPageDTO page = getAugmentedEventDTOById(request, eventId);

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
     * Count all event bookings.
     * 
     * @param request
     *            - so we can determine if the user is logged in
     * @return a list of booking objects
     */
    @GET
    @Path("/bookings/count")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Count all event bookings.")
    public final Response getCountForAllEventBookings(@Context final HttpServletRequest request) {
        try {
            if (!isUserAnAdminOrEventManager(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access this endpoint.")
                        .toResponse();
            }

            return Response.ok(ImmutableMap.of("count", bookingManager.getCountOfEventBookings())).build();
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
    @ApiOperation(value = "Get details about an event booking.")
    public final Response getEventBookingById(@Context final HttpServletRequest request,
            @PathParam("booking_id") final String bookingId) {
        try {
            if (!isUserAnAdminOrEventManager(userManager, request)) {
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
    @ApiOperation(value = "Move a user from an event waiting list to a confirmed booking.")
    public final Response promoteUserFromWaitingList(@Context final HttpServletRequest request,
                                                     @PathParam("event_id") final String eventId,
                                                     @PathParam("user_id") final Long userId,
                                                     final Map<String, String> additionalInformation) {
        try {
            RegisteredUserDTO currentUser = this.userManager.getCurrentRegisteredUser(request);
            RegisteredUserDTO userOfInterest = this.userManager.getUserDTOById(userId);
            IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);

            if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            EventBookingDTO eventBookingDTO
                    = this.bookingManager.promoteFromWaitingListOrCancelled(event, userOfInterest);

            this.getLogManager().logEvent(currentUser, request,
                    SegueLogType.ADMIN_EVENT_WAITING_LIST_PROMOTION, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, event.getId(),
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
    @ApiOperation(value = "List event bookings for a specific event.")
    public final Response getEventBookingByEventId(@Context final HttpServletRequest request,
            @PathParam("event_id") final String eventId) {
        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);

            List<EventBookingDTO> eventBookings = bookingManager.getBookingByEventId(eventId);

            // Event leaders are only allowed to see the bookings of connected users
            if (Role.EVENT_LEADER.equals(currentUser.getRole())) {
                eventBookings = userAssociationManager.filterUnassociatedRecords(
                        currentUser, eventBookings, booking -> booking.getUserBooked().getId());
            }

            return Response.ok(eventBookings).build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error occurred while trying to retrieve all event booking information.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }

    /** gets a list of event bookings based on a given group id.
     *
     */
    @GET
    @Path("{event_id}/bookings/for_group/{group_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "List event bookings for a specific event and group.")
    public final Response getEventBookingForGivenGroup(@Context final HttpServletRequest request,
                                                       @PathParam("event_id") final String eventId,
                                                       @PathParam("group_id") final String groupId) {
        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
            UserGroupDTO group = groupManager.getGroupById(Long.parseLong(groupId));

            if (!(isUserAnAdmin(userManager, currentUser) || GroupManager.isOwnerOrAdditionalManager(group, currentUser.getId()))) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are not the owner or manager of this group.").toResponse();
            }

            IsaacEventPageDTO eventPageDTO = getRawEventDTOById(eventId);
            if (null == eventPageDTO) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "No event found with this ID.").toResponse();
            }
            if (!EventBookingManager.eventAllowsGroupBookings(eventPageDTO)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "This event does not accept group bookings.").toResponse();
            }

            List<Long> groupMemberIds = groupManager.getUsersInGroup(group)
                    .stream().map(RegisteredUserDTO::getId)
                    .collect(Collectors.toList());

            // Filter eventBookings based on whether the booked user is a member of the given group
            List<EventBookingDTO> eventBookings = bookingManager.getBookingByEventId(eventId)
                    .stream().filter(booking -> groupMemberIds.contains(booking.getUserBooked().getId()))
                    .collect(Collectors.toList());

            // Event leaders are only allowed to see the bookings of connected users
            eventBookings = userAssociationManager.filterUnassociatedRecords(currentUser, eventBookings,
                booking -> booking.getUserBooked().getId());

            return Response.ok(eventBookings).build();
        } catch (SegueDatabaseException e) {
            String errorMsg = String.format(
                    "Database error occurred while trying retrieve bookings for group (%s) on event (%s).",
                    groupId, eventId);
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Content Database error occurred while trying to retrieve event information.")
                    .toResponse();
        }
    }

    /**
     * Allows authorised users to view a csv of event attendees
     *
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - the event of interest.
     * @return list of bookings csv.
     */
    @GET
    @Path("{event_id}/bookings/download")
    @Produces("text/csv")
    @GZIP
    @ApiOperation(value = "Download event attendance csv.")
    public Response getEventBookingCSV(@Context final HttpServletRequest request,
                                                   @PathParam("event_id") final String eventId) {
        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
            IsaacEventPageDTO event = this.getRawEventDTOById(eventId);

            if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            List<EventBookingDTO> eventBookings = bookingManager.getBookingByEventId(eventId);

            // Event leaders are only allowed to see the bookings of connected users
            if (Role.EVENT_LEADER.equals(currentUser.getRole())) {
                eventBookings = userAssociationManager.filterUnassociatedRecords(
                        currentUser, eventBookings, booking -> booking.getUserBooked().getId());
            }

            List<String[]> rows = Lists.newArrayList();
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            StringBuilder headerBuilder = new StringBuilder();
            headerBuilder.append(String.format("Event (%s) Attendance: Downloaded on %s \nGenerated by: %s %s \n\n",
                    eventId, new Date(), currentUser.getGivenName(),
                    currentUser.getFamilyName()));

            List<String> headerRow = Lists.newArrayList(Arrays.asList("", ""));

            rows.add(headerRow.toArray(new String[0]));

            List<String> totalsRow = Lists.newArrayList();

            List<String[]> resultRows = Lists.newArrayList();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            for (EventBookingDTO booking : eventBookings) {
                ArrayList<String> resultRow = Lists.newArrayList();
                UserSummaryDTO resultUser = booking.getUserBooked();
                RegisteredUserDTO resultRegisteredUser = this.userAccountManager.getUserDTOById(resultUser.getId());
                String schoolId = resultRegisteredUser.getSchoolId();
                Map<String, String> resultAdditionalInformation = booking.getAdditionalInformation();
                BookingStatus resultBookingStatus = booking.getBookingStatus();
                resultRow.add(resultUser.getGivenName() + " " + resultUser.getFamilyName());
                resultRow.add(resultRegisteredUser.getRole().toString());
                if (schoolId != null) {
                    School school = schoolListReader.findSchoolById(schoolId);
                    if (null != school) {
                        resultRow.add(school.getName());
                    } else {
                        resultRow.add(schoolId);
                    }
                } else {
                    resultRow.add(resultRegisteredUser.getSchoolOther());
                }
                resultRow.add(resultBookingStatus.toString());
                resultRow.add(dateFormat.format(booking.getBookingDate()));
                resultRow.add(dateFormat.format(booking.getUpdated()));
                resultRow.add(resultAdditionalInformation.get("yearGroup"));
                resultRow.add(resultAdditionalInformation.get("jobTitle"));
                resultRow.add(resultAdditionalInformation.get("medicalRequirements"));
                resultRow.add(resultAdditionalInformation.get("accessibilityRequirements"));
                resultRow.add(resultAdditionalInformation.get("emergencyName"));
                resultRow.add(resultAdditionalInformation.get("emergencyNumber"));
                Collections.addAll(resultRows, resultRow.toArray(new String[0]));
            }


            rows.add(totalsRow.toArray(new String[0]));
            rows.add(("Name,Role,School,Booking status,Booking date,Last updated date,Year group,Job title," +  // lgtm [java/missing-space-in-concatenation]
                    "Medical/dietary requirements,Accessibility requirements,Emergency name,Emergency number").split(","));
            rows.addAll(resultRows);
            csvWriter.writeAll(rows);
            csvWriter.close();

            headerBuilder.append(stringWriter.toString());
            return Response.ok(headerBuilder.toString())
                    .header("Content-Disposition", String.format("attachment; filename=event_attendees_%s.csv", eventId))
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();

        } catch (IOException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while building the CSV file.").toResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "No user found with this ID!").toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database error occurred while trying to retrieve all event booking information.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }   catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Content Database error occurred while trying to retrieve event booking information.")
                    .toResponse();
        } catch (UnableToIndexSchoolsException e) {
        return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error while looking up schools", e)
                .toResponse();
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
    @ApiOperation(value = "Create an event booking for a user.")
    public final Response createBookingForGivenUser(@Context final HttpServletRequest request,
                                                    @PathParam("event_id") final String eventId,
                                                    @PathParam("user_id") final Long userId,
                                                    final Map<String, String> additionalInformation) {
        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
            RegisteredUserDTO bookedUser = userManager.getUserDTOById(userId);
            IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);

            if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            if (bookingManager.isUserBooked(eventId, userId)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "User is already booked on this event.")
                        .toResponse();
            }

            EventBookingDTO booking = bookingManager.createBookingOrAddToWaitingList(event, bookedUser, additionalInformation);
            this.getLogManager().logEvent(currentUser, request,
                    SegueLogType.ADMIN_EVENT_BOOKING_CREATED,
                    ImmutableMap.of(
                        EVENT_ID_FKEY_FIELDNAME, event.getId(),
                        USER_ID_FKEY_FIELDNAME, userId,
                        BOOKING_STATUS_FIELDNAME, booking.getBookingStatus().toString(),
                        ADMIN_BOOKING_REASON_FIELDNAME, additionalInformation.get("authorisation") == null ? "NOT_PROVIDED" : additionalInformation.get("authorisation")
                    ));

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
     * Add event reservations for the given users.
     *
     * @param request
     *            - so we can determine who is making the request
     * @param eventId
     *            - event id
     * @param userIds
     *            - the users to reserve spaces for
     * @return the list of bookings/reservations
     */
    @POST
    @Path("{event_id}/reservations")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add event reservations for the given users.")
    public final Response createReservationsForGivenUsers(@Context final HttpServletRequest request,
                                                          @PathParam("event_id") final String eventId,
                                                          final List<Long> userIds) {
        RegisteredUserDTO currentUser;
        IsaacEventPageDTO event;
        try {
            event = this.getRawEventDTOById(eventId);
        } catch (SegueDatabaseException | ContentManagerException e) {
            event = null;
        }
        if (null == event) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "No event found with this ID.").toResponse();
        }
        if (!EventBookingManager.eventAllowsGroupBookings(event)) {
            return new SegueErrorResponse(Status.FORBIDDEN, "This event does not accept group bookings.").toResponse();
        }

        Map<Long, RegisteredUserDTO> bookableUsers;
        try {
            currentUser = userManager.getCurrentRegisteredUser(request);
            if (!Arrays.asList(Role.TEACHER, Role.EVENT_LEADER, Role.EVENT_MANAGER, Role.ADMIN).contains(currentUser.getRole())) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            bookableUsers = new HashMap<>();
            List<Long> unbookableIds = new ArrayList<>();
            for (Long userId : userIds) {
                // Do not add a reservation if a user is already booked or reserved.
                RegisteredUserDTO u = userManager.getUserDTOById(userId);
                if (!userAssociationManager.hasPermission(currentUser, u)) {
                    return new SegueErrorResponse(Status.FORBIDDEN,
                            "You do not have permission to book or reserve some of these users onto this event.")
                            .toResponse();
                }
                if (bookingManager.isUserBooked(eventId, userId) || bookingManager.isUserReserved(eventId, userId)) {
                    unbookableIds.add(userId);
                } else {
                    bookableUsers.put(userId, u);
                }
            }
            if (unbookableIds.size() > 0) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        "Some of the users you requested are already booked or reserved on this event.")
                        .toResponse();
            }

            // This would be neater with streams and lambdas, but handling exceptions in lambdas is ugly.
            List<RegisteredUserDTO> usersToBook = new ArrayList<>();
            for (Long bookableId : bookableUsers.keySet()) {
                usersToBook.add(bookableUsers.get(bookableId));
            }
            List<EventBookingDTO> bookings = bookingManager.requestReservations(event, usersToBook, currentUser);
            this.getLogManager().logEvent(currentUser, request,
                    SegueLogType.EVENT_RESERVATIONS_CREATED,
                    ImmutableMap.of(
                            EVENT_ID_FKEY_FIELDNAME, event.getId(),
                            USER_ID_FKEY_FIELDNAME, currentUser.getId(),
                            USER_ID_LIST_FKEY_FIELDNAME, bookableUsers.keySet().toArray(),
                            BOOKING_STATUS_FIELDNAME, BookingStatus.RESERVED.toString()
                    ));
            return Response.ok(bookings).build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error occurred while trying to reserve space for a user onto an event.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (EventIsFullException e) {
            return new SegueErrorResponse(Status.CONFLICT,
                    "There are not enough spaces available for this event. Please try again with fewer users.")
                    .toResponse();
        } catch (EventGroupReservationLimitException e) {
            return new SegueErrorResponse(Status.CONFLICT,
                    String.format("You can only request a maximum of %d student reservations for this event.",
                            event.getGroupReservationLimit())).toResponse();
        } catch (EventDeadlineException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "The booking deadline for this event has passed. No more bookings or reservations are being accepted.")
                    .toResponse();
        } catch (DuplicateBookingException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "One of the users requested is already booked or reserved on this event. Unable to create a duplicate booking.")
                    .toResponse();
        } catch (NoUserException e) {
            return SegueErrorResponse.getResourceNotFoundResponse("Unable to locate one of the users specified.");
        } catch (EmailMustBeVerifiedException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "All users must have a verified email address before they can be reserved on this event.")
                    .toResponse();
        }
    }

    /**
     * This function allows cancellation of the reservations for the given users
     *
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - event id
     * @param userIds
     *            - user ids
     */
    @POST
    @Path("{event_id}/reservations/cancel")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Cancel a reservations on an event for a set of users.")
    public final Response cancelReservations(@Context final HttpServletRequest request,
                                        @PathParam("event_id") final String eventId,
                                        final List<Long> userIds) {
        try {
            IsaacEventPageDTO event = getRawEventDTOById(eventId);
            if (null == event) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "No event found with this ID.").toResponse();
            }
            RegisteredUserDTO userLoggedIn = this.userManager.getCurrentRegisteredUser(request);

            if (event.getDate() != null && new Date().after(event.getDate())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You cannot cancel a reservation on an event that has already started.")
                        .toResponse();
            }

            boolean userIsAbleToManageEvent = bookingManager.isUserAbleToManageEvent(userLoggedIn, event);

            List<RegisteredUserDTO> validUsers = new ArrayList<>();
            for (Long userId : userIds) {
                RegisteredUserDTO userOwningBooking = userManager.getUserDTOById(userId);
                if (userIsAbleToManageEvent || (bookingManager.isReservationMadeByRequestingUser(userLoggedIn, userOwningBooking, event) && userAssociationManager.hasPermission(userLoggedIn, userOwningBooking))) {
                    if (bookingManager.hasBookingWithAnyOfStatuses(eventId, userId, new HashSet<>(Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.WAITING_LIST, BookingStatus.RESERVED)))) {
                        validUsers.add(userOwningBooking);
                    } else {
                        // Maybe silently carry on instead?
                        return new SegueErrorResponse(Status.BAD_REQUEST,
                                "Some of the reservations cannot be cancelled. Please reload the page and try again.")
                                .toResponse();
                    }
                } else {
                    return new SegueErrorResponse(Status.FORBIDDEN,
                            "You are not authorized to cancel some of the reservations specified.")
                            .toResponse();
                }
            }

            for (RegisteredUserDTO user : validUsers) {
                bookingManager.cancelBooking(event, user);
            }

            this.getLogManager().logEvent(userLoggedIn, request,
                    SegueLogType.EVENT_RESERVATIONS_CANCELLED,
                    ImmutableMap.of(
                            EVENT_ID_FKEY_FIELDNAME, event.getId(),
                            USER_ID_FKEY_FIELDNAME, userLoggedIn.getId(),
                            USER_ID_LIST_FKEY_FIELDNAME, validUsers.stream().map(RegisteredUserDTO::getId).toArray(),
                            BOOKING_STATUS_FIELDNAME, BookingStatus.CANCELLED.toString()
                    ));
            return Response.noContent().build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (ContentManagerException e) {
            log.error("Error during event request", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the content you requested.")
                    .toResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error occurred while trying to delete an event booking.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (NoUserException e) {
            return SegueErrorResponse.getResourceNotFoundResponse("Unable to locate user specified.");
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
    @ApiOperation(value = "Create an event booking for the current user.")
    public final Response createBookingForMe(@Context final HttpServletRequest request,
                                             @PathParam("event_id") final String eventId,
                                             final Map<String, String> additionalInformation) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
            IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);

            if (EventStatus.CLOSED.equals(event.getEventStatus())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "Sorry booking for this event is closed. Please try again later.")
                    .toResponse();
            }

            if (EventStatus.WAITING_LIST_ONLY.equals(event.getEventStatus())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "Sorry booking for this event is restricted. You can only be added to a waiting list.")
                        .toResponse();
            }

            if (bookingManager.isUserBooked(eventId, user.getId())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You are already booked on this event.")
                    .toResponse();
            }

            // reservedBy is null. If there is a reservation for me, it will be updated to CONFIRMED.
            EventBookingDTO eventBookingDTO = bookingManager.requestBooking(event, user, additionalInformation);

            this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                    SegueLogType.EVENT_BOOKING, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, event.getId()));

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
    @ApiOperation(value = "Add the current user to an event waiting list.")
    public final Response addMeToWaitingList(@Context final HttpServletRequest request,
                                             @PathParam("event_id") final String eventId,
                                             final Map<String, String> additionalInformation) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);

            // Fail if the user is already booked or reserved for this event, so there's no need to add to a waiting list.
            if (bookingManager.isUserBooked(eventId, user.getId()) || bookingManager.isUserReserved(eventId, user.getId())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You are already booked or reserved on this event.")
                    .toResponse();
            }

            EventBookingDTO eventBookingDTO = bookingManager.requestWaitingListBooking(event, user, additionalInformation);
            this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                    SegueLogType.EVENT_WAITING_LIST_BOOKING, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, event.getId()));

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
        } catch (EventIsFullException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                "There are no spaces in this event. No more bookings are accepted.")
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
    @ApiOperation(value = "Cancel the current user's booking on an event.")
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
    @ApiOperation(value = "Cancel a user's booking on an event.")
    public final Response cancelBooking(@Context final HttpServletRequest request,
                                        @PathParam("event_id") final String eventId,
                                        @PathParam("user_id") final Long userId) {
        try {
            IsaacEventPageDTO event = getRawEventDTOById(eventId);
            if (null == event) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "No event found with this ID.").toResponse();
            }

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
            if (userId != null) {
                if (!(bookingManager.isUserAbleToManageEvent(userLoggedIn, event) ||
                      bookingManager.isReservationMadeByRequestingUser(userLoggedIn, userOwningBooking, event))) {
                    return SegueErrorResponse.getIncorrectRoleResponse();
                }
            }

            Set<BookingStatus> cancelableStatuses =
                    new HashSet<>(Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.WAITING_LIST, BookingStatus.RESERVED));
            if (!bookingManager.hasBookingWithAnyOfStatuses(eventId, userOwningBooking.getId(), cancelableStatuses)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "User is not booked on this event.").toResponse();
            }

            bookingManager.cancelBooking(event, userOwningBooking);

            if (!userOwningBooking.equals(userLoggedIn)) {
                this.getLogManager().logEvent(userLoggedIn, request,
                        SegueLogType.ADMIN_EVENT_BOOKING_CANCELLED, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, event.getId(), USER_ID_FKEY_FIELDNAME, userOwningBooking.getId()));
            } else {
                this.getLogManager().logEvent(userLoggedIn, request,
                        SegueLogType.EVENT_BOOKING_CANCELLED, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, event.getId()));
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
    @ApiOperation(value = "Resend an event booking confirmation to a user.")
    public final Response resendEventEmail(@Context final HttpServletRequest request,
                                           @PathParam("event_id") final String eventId,
                                           @PathParam("user_id") final Long userId) {
        try {
            IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);
            RegisteredUserDTO bookedUser = this.userManager.getUserDTOById(userId);
            RegisteredUserDTO currentUser = this.userManager.getCurrentRegisteredUser(request);

            if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            this.bookingManager.resendEventEmail(event, bookedUser);

            log.info(String.format("User (%s) has just resent an event email to user id (%s)",
                    currentUser.getEmail(), bookedUser.getId()));

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
    @ApiOperation(value = "Erase a user's booking on an event.",
                  notes = "This method removes the booking entirely, rather than recording the booking as cancelled.")
    public final Response deleteBooking(@Context final HttpServletRequest request,
                                        @PathParam("event_id") final String eventId,
                                        @PathParam("user_id") final Long userId) {
        try {
            if (!isUserAnAdmin(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an Admin user to access this endpoint.")
                        .toResponse();
            }

            Set<BookingStatus> allValidBookingStatuses = new HashSet<>(Arrays.asList(BookingStatus.values()));
            if (!bookingManager.hasBookingWithAnyOfStatuses(eventId, userId, allValidBookingStatuses)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "User is not booked on this event.").toResponse();
            }

            IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);
            RegisteredUserDTO user = this.userManager.getUserDTOById(userId);

            bookingManager.deleteBooking(event, user);

            this.getLogManager().logEvent(userManager.getCurrentUser(request), request,
                    SegueLogType.ADMIN_EVENT_BOOKING_DELETED, ImmutableMap.of(EVENT_ID_FKEY_FIELDNAME, eventId, USER_ID_FKEY_FIELDNAME, userId));

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
     * Allow a staff user to record event attendance.
     *
     * @param request
     *            - so we can determine if the user is logged in
     * @param eventId
     *            - event booking containing updates, must contain primary id.
     * @param userId
     *            - the user to be promoted.
     * @param attended
     *            - boolean value representing whether the user was present, true, or absent, false.
     * @return the updated booking.
     */
    @POST
    @Path("{event_id}/bookings/{user_id}/record_attendance")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Update the attendance status of a user for an event.")
    public final Response recordEventAttendance(@Context final HttpServletRequest request,
                                                @PathParam("event_id") final String eventId,
                                                @PathParam("user_id") final Long userId,
                                                @QueryParam("attended") final Boolean attended) {
        try {
            RegisteredUserDTO currentUser = this.userManager.getCurrentRegisteredUser(request);
            RegisteredUserDTO userOfInterest = this.userManager.getUserDTOById(userId);
            IsaacEventPageDTO event = this.getAugmentedEventDTOById(request, eventId);

            if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            EventBookingDTO eventBookingDTO = this.bookingManager.recordAttendance(event, userOfInterest, attended);
            this.getLogManager().logEvent(currentUser, request,
                    SegueLogType.ADMIN_EVENT_ATTENDANCE_RECORDED,
                    ImmutableMap.of(
                        EVENT_ID_FKEY_FIELDNAME, event.getId(),
                        USER_ID_FKEY_FIELDNAME, userId,
                        ATTENDED_FIELDNAME, attended,
                        EVENT_DATE_FIELDNAME, event.getDate(),
                        EVENT_TAGS_FIELDNAME, event.getTags()
                    ));

            if (event.getTags().contains("teacher")) {
                this.userBadgeManager.updateBadge(userOfInterest,
                        UserBadgeManager.Badge.TEACHER_CPD_EVENTS_ATTENDED, eventId);
            }

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
     * REST end point to provide a list of events.
     *
     * @param request
     *            - this allows us to check to see if a user is currently loggedin.
     * @param startIndex
     *            - the initial index for the first result.
     * @param limit
     *            - the maximums number of results to return
     * @param filter
     *            - in which way should the results be filtered from a choice defined in the EventFilterOption enum.
     * @return a Response containing a list of events objects or containing a SegueErrorResponse.
     */
    @GET
    @Path("/overview")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "List summary information of events matching the provided criteria.")
    public final Response getEventOverviews(@Context final HttpServletRequest request,
                                            @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index") final Integer startIndex,
                                            @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit") final Integer limit,
                                            @QueryParam("filter") final String filter) {
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
        sortInstructions.put(DATE_FIELDNAME, SortOrder.DESC);

        fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));

        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
            if (!Arrays.asList(Role.EVENT_LEADER, Role.EVENT_MANAGER, Role.ADMIN).contains(currentUser.getRole())) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            Map<String, AbstractFilterInstruction> filterInstructions = null;
            if (filter != null) {
                EventFilterOption filterOption = EventFilterOption.valueOf(filter);
                filterInstructions = Maps.newHashMap();
                if (filterOption.equals(EventFilterOption.FUTURE)) {
                    DateRangeFilterInstruction anyEventsFromNow = new DateRangeFilterInstruction(new Date(), null);
                    filterInstructions.put(ENDDATE_FIELDNAME, anyEventsFromNow);
                    sortInstructions.put(DATE_FIELDNAME, SortOrder.ASC);
                } else if (filterOption.equals(EventFilterOption.RECENT)) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.add(Calendar.MONTH, -1);
                    DateRangeFilterInstruction eventsOverPreviousMonth =
                            new DateRangeFilterInstruction(calendar.getTime(), new Date());
                    filterInstructions.put(ENDDATE_FIELDNAME, eventsOverPreviousMonth);
                } else if (filterOption.equals(EventFilterOption.PAST)) {
                    DateRangeFilterInstruction anyEventsToNow = new DateRangeFilterInstruction(null, new Date());
                    filterInstructions.put(ENDDATE_FIELDNAME, anyEventsToNow);
                }
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
                IsaacEventPageDTO event = (IsaacEventPageDTO) c;

                if (!bookingManager.isUserAbleToManageEvent(currentUser, event)) {
                    continue;
                }

                ImmutableMap.Builder<String, Object> eventOverviewBuilder = new ImmutableMap.Builder<>();
                eventOverviewBuilder.put("id", event.getId());
                eventOverviewBuilder.put("title", event.getTitle());
                eventOverviewBuilder.put("subtitle", event.getSubtitle());
                eventOverviewBuilder.put("date", event.getDate());
                eventOverviewBuilder.put("bookingDeadline",
                        event.getBookingDeadline() == null ? event.getDate() : event.getBookingDeadline());
                eventOverviewBuilder.put("eventStatus", event.getEventStatus());

                if (null != event.getLocation()) {
                    eventOverviewBuilder.put("location", event.getLocation());
                }

                eventOverviewBuilder.put("numberOfConfirmedBookings",
                        this.bookingManager.countNumberOfBookingsWithStatus(event.getId(), BookingStatus.CONFIRMED));
                eventOverviewBuilder.put("numberOfWaitingListBookings",
                        this.bookingManager.countNumberOfBookingsWithStatus(event.getId(), BookingStatus.WAITING_LIST));
                eventOverviewBuilder.put("numberAttended",
                        this.bookingManager.countNumberOfBookingsWithStatus(event.getId(), BookingStatus.ATTENDED));
                eventOverviewBuilder.put("numberAbsent",
                        this.bookingManager.countNumberOfBookingsWithStatus(event.getId(), BookingStatus.ABSENT));

                if (null != event.getNumberOfPlaces()) {
                    eventOverviewBuilder.put("numberOfPlaces", event.getNumberOfPlaces());
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
        } catch (IllegalArgumentException e) {
            log.error("Error occurred during event overview look up", e);
            return new SegueErrorResponse(Status.BAD_REQUEST, "Invalid request format.").toResponse();
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
    @ApiOperation(value = "List summary details suitable for mapping for events matching the provided criteria.")
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
        sortInstructions.put(DATE_FIELDNAME, SortOrder.DESC);

        fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));

        Map<String, AbstractFilterInstruction> filterInstructions = null;
        if (null == showActiveOnly || showActiveOnly) {
            filterInstructions = Maps.newHashMap();
            DateRangeFilterInstruction anyEventsFromNow = new DateRangeFilterInstruction(new Date(), null);
            filterInstructions.put(ENDDATE_FIELDNAME, anyEventsFromNow);
            sortInstructions.put(DATE_FIELDNAME, SortOrder.ASC);
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
     * A helper method for retrieving an event object without augmented information
     *
     * @param eventId the id of the event of interest
     * @return the fully populated event dto with user context information.
     * @throws ContentManagerException - if there is a problem finding the event information
     * @throws SegueDatabaseException if there is a database error.
     */
    private IsaacEventPageDTO getRawEventDTOById(final String eventId)
            throws ContentManagerException, SegueDatabaseException {

        ContentDTO possibleEvent = this.contentManager.getContentById(this.contentManager.getCurrentContentSHA(), eventId);

        if (null == possibleEvent) {
            throw new ResourceNotFoundException(String.format("Unable to locate the event with id; %s", eventId));
        }

        if (possibleEvent instanceof IsaacEventPageDTO) {
            return (IsaacEventPageDTO) possibleEvent;
        }
        return null;
    }

    /**
     * A helper method for retrieving an event and the number of places available and if the user is booked or not.
     *
     *
     * @param request so we can determine if the user is logged in
     * @param eventId the id of the event of interest
     * @return the fully populated event dto with user context information.
     * @throws ContentManagerException - if there is a problem finding the event information
     * @throws SegueDatabaseException if there is a database error.
	 */
    private IsaacEventPageDTO getAugmentedEventDTOById(final HttpServletRequest request, final String eventId)
            throws ContentManagerException, SegueDatabaseException {
        IsaacEventPageDTO event = getRawEventDTOById(eventId);
        return augmentEventWithBookingInformation(request, event);
    }

	/**
     * Augment a single event with booking information before we send it out.
     *
     * @param request - for user look up
     * @param possibleEvent - a ContentDTO that should hopefully be an IsaacEventPageDTO.
     * @return an augmented IsaacEventPageDTO.
     * @throws SegueDatabaseException
     */
    private IsaacEventPageDTO augmentEventWithBookingInformation(final HttpServletRequest request,
                                                                 final ContentDTO possibleEvent)
            throws SegueDatabaseException {
        if (possibleEvent instanceof IsaacEventPageDTO) {
            IsaacEventPageDTO page = (IsaacEventPageDTO) possibleEvent;

            try {
                RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
                page.setUserBooked(this.bookingManager.isUserBooked(page.getId(), user.getId()));
                page.setUserOnWaitList(this.bookingManager.hasBookingWithStatus(page.getId(), user.getId(), BookingStatus.WAITING_LIST));
                // TODO: Are either of the above attributes necessary with this new booking status attribute?
                page.setUserBookingStatus(this.bookingManager.getBookingStatus(page.getId(), user.getId()));
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