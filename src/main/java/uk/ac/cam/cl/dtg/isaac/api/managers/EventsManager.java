package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentSubclassMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.search.BooleanInstruction;
import uk.ac.cam.cl.dtg.segue.search.ISearchProvider;
import uk.ac.cam.cl.dtg.segue.search.IsaacSearchInstructionBuilder;
import uk.ac.cam.cl.dtg.segue.search.SearchInField;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;
import uk.ac.cam.cl.dtg.util.mappers.MainMapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.STAGE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TAGS_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TITLE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX;

/**
 * EventsManager.
 */
public class EventsManager {
    private static final Logger log = LoggerFactory.getLogger(EventsManager.class);

    private static final String CONTENT_TYPE = "content";

    private final String contentIndex;

    private final ISearchProvider searchProvider;
    private final EventBookingManager bookingManager;
    private final GitContentManager contentManager;

    private final MainMapper mapper;
    private final ContentSubclassMapper contentSubclassMapper;

    /**
     * EventsManager.
     *
     * @param properties            - global properties map
     * @param searchProvider        - search provider
     * @param bookingManager        - event booking manager
     * @param contentManager        - git content manager
     * @param mapper                - mapper for mapping between DOs and DTOs
     * @param contentSubclassMapper - content subclass mapper
     */
    @Inject
    public EventsManager(final AbstractConfigLoader properties, final ISearchProvider searchProvider,
                         final EventBookingManager bookingManager, final GitContentManager contentManager,
                         final MainMapper mapper, final ContentSubclassMapper contentSubclassMapper) {
        this.searchProvider = searchProvider;
        this.bookingManager = bookingManager;
        this.contentManager = contentManager;
        this.mapper = mapper;
        this.contentSubclassMapper = contentSubclassMapper;

        this.contentIndex = properties.getProperty(CONTENT_INDEX);
    }

    /**
     * Logic for the /events endpoint to provide a list of events.
     *
     * @param tags                 - a comma separated list of tags to include in the search.
     * @param startIndex           - the initial index for the first result.
     * @param limit                - the maximums number of results to return
     * @param sortOrder            - flag to indicate preferred sort order.
     * @param showActiveOnly       - true will impose filtering on the results. False will not. Defaults to false.
     * @param showStageOnly        - if present, only events with an audience matching this string will be shown.
     * @param includeHiddenContent - if true, include hidden (nofilter) events.
     * @return a ResultsWrapper containing a list of filtered events as ContentDTOs.
     */
    public ResultsWrapper<ContentDTO> getEvents(final String tags, final Integer startIndex, final Integer limit,
                                                final String sortOrder, final Boolean showActiveOnly,
                                                final String showStageOnly, final Boolean includeHiddenContent)
            throws ContentManagerException, SegueDatabaseException {

        IsaacSearchInstructionBuilder searchInstructionBuilder = this.contentManager.getBaseSearchInstructionBuilder()
                .includeContentTypes(Collections.singleton(EVENT_TYPE));

        if (tags != null) {
            searchInstructionBuilder.searchFor(new SearchInField(TAGS_FIELDNAME,
                    Arrays.stream(tags.split(",")).collect(Collectors.toSet())));
        }

        if (showStageOnly != null) {
            searchInstructionBuilder.searchFor(new SearchInField(STAGE_FIELDNAME,
                    Arrays.stream(showStageOnly.split(",")).collect(Collectors.toSet())));
        }

        if (null != includeHiddenContent && includeHiddenContent) {
            searchInstructionBuilder.includeHiddenContent(true);
        }

        final Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
        if (sortOrder != null && sortOrder.equals("title")) {
            sortInstructions.put(TITLE_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX,
                    Constants.SortOrder.ASC);
        } else {
            sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.DESC);
        }

        if (null != showActiveOnly && showActiveOnly) {
            // Should default to future events only, but set this explicitly anyway
            searchInstructionBuilder.setEventFilterOption(Constants.EventFilterOption.FUTURE);
            sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.ASC);
        } else {
            searchInstructionBuilder.setEventFilterOption(Constants.EventFilterOption.ALL);
        }

        ResultsWrapper<ContentDTO> findByFieldNames = null;

        BooleanInstruction instruction = searchInstructionBuilder.build();
        ResultsWrapper<String> searchHits = this.searchProvider.nestedMatchSearch(contentIndex, CONTENT_TYPE,
                startIndex, limit, instruction, null, sortInstructions);

        List<Content> searchResults = this.contentSubclassMapper
                .mapFromStringListToContentList(searchHits.getResults());
        List<ContentDTO> dtoResults = this.contentSubclassMapper.getDTOByDOList(searchResults);
        findByFieldNames = new ResultsWrapper<>(dtoResults, searchHits.getTotalResults());

        return findByFieldNames;
    }

    /**
     * Logic for the /events/overview endpoint to provide a list of events with summary information.
     *
     * @param startIndex  - the initial index for the first result.
     * @param limit       - the maximums number of results to return
     * @param filter      - in which way should the results be filtered from a choice defined in the EventFilterOption enum.
     * @param currentUser - the currently logged-in user (must be event leader or above).
     * @return a ResultsWrapper containing a list of filtered events with summary information.
     */
    public ResultsWrapper<Map<String, Object>> getEventOverviews(final Integer startIndex, final Integer limit,
                                                                 final String filter, final RegisteredUserDTO currentUser)
            throws ContentManagerException, SegueDatabaseException {

        IsaacSearchInstructionBuilder searchInstructionBuilder = this.contentManager.getBaseSearchInstructionBuilder()
                .includeContentTypes(Collections.singleton(EVENT_TYPE));

        final Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
        sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.DESC);

        if (filter != null) {
            Constants.EventFilterOption filterOption = Constants.EventFilterOption.valueOf(filter);
            searchInstructionBuilder.setEventFilterOption(filterOption);
            if (filterOption.equals(Constants.EventFilterOption.FUTURE)) {
                sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.ASC);
            }
        }

        BooleanInstruction instruction = searchInstructionBuilder.build();
        ResultsWrapper<String> searchHits = this.searchProvider.nestedMatchSearch(contentIndex, CONTENT_TYPE,
                startIndex, limit, instruction, null, sortInstructions);

        List<Content> searchResults = this.contentSubclassMapper.mapFromStringListToContentList(searchHits.getResults());
        List<ContentDTO> dtoResults = this.contentSubclassMapper.getDTOByDOList(searchResults);
        ResultsWrapper<ContentDTO> findByFieldNames = new ResultsWrapper<>(dtoResults, searchHits.getTotalResults());

        List<Map<String, Object>> resultList = Lists.newArrayList();

        for (ContentDTO c : findByFieldNames.getResults()) {
            if (!(c instanceof IsaacEventPageDTO)) {
                continue;
            }
            IsaacEventPageDTO event = (IsaacEventPageDTO) c;

            if (null == currentUser || !bookingManager.isUserAbleToManageEvent(currentUser, event)) {
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

            Map<BookingStatus, Long> bookingCounts =  this.bookingManager.getBookingStatusCountsByEventId(event.getId());
            eventOverviewBuilder.put("numberOfConfirmedBookings",
                    bookingCounts.getOrDefault(BookingStatus.CONFIRMED, 0L));
            eventOverviewBuilder.put("numberOfWaitingListBookings",
                    bookingCounts.getOrDefault(BookingStatus.WAITING_LIST, 0L));
            eventOverviewBuilder.put("numberAttended",
                    bookingCounts.getOrDefault(BookingStatus.ATTENDED, 0L));
            eventOverviewBuilder.put("numberAbsent",
                    bookingCounts.getOrDefault(BookingStatus.ABSENT, 0L));

            if (null != event.getNumberOfPlaces()) {
                eventOverviewBuilder.put("numberOfPlaces", event.getNumberOfPlaces());
            }

            resultList.add(eventOverviewBuilder.build());
        }
        return new ResultsWrapper<>(resultList, findByFieldNames.getTotalResults());
    }

    /**
     * Logic for the /events/map_data endpoint to provide a list of events with summary information.
     *
     * @param startIndex     - the initial index for the first result.
     * @param limit          - the maximum number of results to return.
     * @param showActiveOnly - true will impose filtering on the results. False will not. Defaults to false.
     * @param showStageOnly  - if present, only events with an audience matching this string will be shown.
     * @return a ResultsWrapper containing a list of event map summaries.
     */
    public ResultsWrapper<Map<String, Object>> getEventMapData(final String tags, final Integer startIndex,
                                                               final Integer limit, final Boolean showActiveOnly,
                                                               final String showStageOnly)
            throws ContentManagerException {

        IsaacSearchInstructionBuilder searchInstructionBuilder = this.contentManager.getBaseSearchInstructionBuilder()
                .includeContentTypes(Collections.singleton(EVENT_TYPE));

        if (tags != null) {
            searchInstructionBuilder.searchFor(new SearchInField(TAGS_FIELDNAME,
                    Arrays.stream(tags.split(",")).collect(Collectors.toSet())));
        }

        if (showStageOnly != null) {
            searchInstructionBuilder.searchFor(new SearchInField(STAGE_FIELDNAME,
                    Arrays.stream(showStageOnly.split(",")).collect(Collectors.toSet())));
        }

        final Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
        sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.DESC);

        if (null == showActiveOnly || showActiveOnly) {
            // Should default to future events only, but set this explicitly anyway
            searchInstructionBuilder.setEventFilterOption(Constants.EventFilterOption.FUTURE);
            sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.ASC);
        } else {
            searchInstructionBuilder.setEventFilterOption(Constants.EventFilterOption.ALL);
        }

        BooleanInstruction instruction = searchInstructionBuilder.build();
        ResultsWrapper<String> searchHits = this.searchProvider.nestedMatchSearch(contentIndex, CONTENT_TYPE,
                startIndex, limit, instruction, null, sortInstructions);

        List<Content> searchResults = this.contentSubclassMapper.mapFromStringListToContentList(searchHits.getResults());
        List<ContentDTO> dtoResults = this.contentSubclassMapper.getDTOByDOList(searchResults);
        ResultsWrapper<ContentDTO> findByFieldNames = new ResultsWrapper<>(dtoResults, searchHits.getTotalResults());

        List<Map<String, Object>> resultList = Lists.newArrayList();

        for (ContentDTO c : findByFieldNames.getResults()) {
            if (!(c instanceof IsaacEventPageDTO)) {
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

        return new ResultsWrapper<>(resultList, findByFieldNames.getTotalResults());
    }

    /**
     * Get Events Booked by user.
     *
     * @param tags        - the tags we want to filter on
     * @param currentUser - the currently logged on user.
     * @return a list of event pages that the user has been booked
     * @throws SegueDatabaseException
     * @throws ContentManagerException
     */
    public ResultsWrapper<ContentDTO> getEventsBookedByUser(final List<String> tags,
                                                             final RegisteredUserDTO currentUser)
            throws SegueDatabaseException, ContentManagerException {
        List<ContentDTO> filteredResults = Lists.newArrayList();

        Map<String, BookingStatus> userBookingMap = this.bookingManager.getAllEventStatesForUser(currentUser.getId());

        for (String eventId : userBookingMap.keySet()) {
            if (BookingStatus.CANCELLED.equals(userBookingMap.get(eventId))) {
                continue;
            }

            final IsaacEventPageDTO eventDTOById = this.getAugmentedEventDTOById(currentUser, eventId);

            if (tags != null) {
                Set<String> tagsList = Sets.newHashSet(tags);
                tagsList.retainAll(eventDTOById.getTags()); // get intersection
                if (tagsList.isEmpty()) {
                    // if the intersection is empty then we can continue
                    continue;
                }
            }

            filteredResults.add(eventDTOById);
        }
        return new ResultsWrapper<>(filteredResults, (long) filteredResults.size());
    }

    /**
     * Get Events Reserved by user.
     *
     * @param currentUser - the currently logged on user.
     * @return a list of event pages that the user has been booked
     * @throws SegueDatabaseException
     * @throws ContentManagerException
     */
    public ResultsWrapper<ContentDTO> getEventsReservedByUser(final RegisteredUserDTO currentUser)
            throws SegueDatabaseException, ContentManagerException {
        List<ContentDTO> filteredResults = Lists.newArrayList();

        List<EventBookingDTO> userReservationList = this.mapper.mapToListOfEventBookingDTO(bookingManager.getAllEventReservationsForUser(currentUser.getId()));

        for (EventBookingDTO booking : userReservationList) {

            final IsaacEventPageDTO eventDTOById = this.getAugmentedEventDTOById(currentUser, booking.getEventId());

            filteredResults.add(eventDTOById);
        }
        return new ResultsWrapper<>(filteredResults, (long) filteredResults.size());
    }

    /**
     * A helper method for retrieving an event and the number of places available and if the user is booked or not.
     *
     * @param eventId the id of the event of interest
     * @return the fully populated event dto with user context information.
     * @throws ContentManagerException - if there is a problem finding the event information
     * @throws SegueDatabaseException  if there is a database error.
     */
    public IsaacEventPageDTO getAugmentedEventDTOById(final RegisteredUserDTO currentUser, final String eventId)
            throws ContentManagerException, SegueDatabaseException {
        IsaacEventPageDTO event = getRawEventDTOById(eventId);
        return augmentEventWithBookingInformation(currentUser, event);
    }

    /**
     * Augment a single event with booking information before we send it out.
     *
     * @param possibleEvent - a ContentDTO that should hopefully be an IsaacEventPageDTO.
     * @return an augmented IsaacEventPageDTO.
     * @throws SegueDatabaseException
     */
    public IsaacEventPageDTO augmentEventWithBookingInformation(final RegisteredUserDTO currentUser,
                                                                 final ContentDTO possibleEvent)
            throws SegueDatabaseException {
        if (possibleEvent instanceof IsaacEventPageDTO) {
            IsaacEventPageDTO page = (IsaacEventPageDTO) possibleEvent;

            if (null != currentUser) {
                page.setUserBookingStatus(this.bookingManager.getBookingStatus(page.getId(), currentUser.getId()));
            } else {
                page.setUserBookingStatus(null);
            }

            page.setPlacesAvailable(this.bookingManager.getPlacesAvailable(page));
            return page;
        } else {
            throw new ClassCastException("The object provided was not an event.");
        }
    }

    /**
     * A helper method for retrieving an event object without augmented information
     *
     * @param eventId the id of the event of interest
     * @return the fully populated event dto with user context information.
     * @throws ContentManagerException - if there is a problem finding the event information
     * @throws SegueDatabaseException  if there is a database error.
     */
    public IsaacEventPageDTO getRawEventDTOById(final String eventId)
            throws ContentManagerException, SegueDatabaseException {

        ContentDTO possibleEvent = this.contentManager.getContentById(eventId);

        if (null == possibleEvent) {
            throw new ResourceNotFoundException(String.format("Unable to locate the event with id; %s", eventId));
        }

        if (possibleEvent instanceof IsaacEventPageDTO) {
            // The Events Facade *mutates* the EventDTO returned by this method; we must return a copy of
            // the original object else we will poison the contentManager's cache!
            // TODO: might it be better to get the DO from the cache and map it to DTO here to reduce overhead?
            IsaacEventPageDTO eventPageDTO = (IsaacEventPageDTO) possibleEvent;
            return mapper.copy(eventPageDTO);
        }
        return null;
    }
}
