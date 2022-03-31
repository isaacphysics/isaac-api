package uk.ac.cam.cl.dtg.segue.scheduler.jobs;

import com.google.api.client.util.Maps;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.DateRangeFilterInstruction;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.ENDDATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SortOrder;

public class EventScheduledEmailJob implements Job {
    private static final Logger log = LoggerFactory.getLogger(EventScheduledEmailJob.class);

    private final PropertiesLoader properties;
    private final PostgresSqlDb database;
    private final IContentManager contentManager;
    private final EventBookingManager bookingManager;
    private final UserAccountManager userAccountManager;

    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    public EventScheduledEmailJob() {
        Injector injector = SegueGuiceConfigurationModule.getGuiceInjector();
        properties = injector.getInstance(PropertiesLoader.class);
        contentManager = injector.getInstance(IContentManager.class);
        bookingManager = injector.getInstance(EventBookingManager.class);
        database = injector.getInstance(PostgresSqlDb.class);
        userAccountManager = injector.getInstance(UserAccountManager.class);
    }

    @Override
    public void execute(final JobExecutionContext context) throws JobExecutionException {
        // Magic number
        Integer limit = 10000;
        Integer startIndex = 0;
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
        Map<String, AbstractFilterInstruction> filterInstructions = Maps.newHashMap();
        List<String> eventTags = Lists.newArrayList("booster", "teachercpd", "discovery", "masterclass", "voyager", "explorer", "teacher", "workshop", "virtual");
        fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));
        sortInstructions.put(DATE_FIELDNAME, SortOrder.DESC);
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime oneYearAgo = now.plusDays(-365);
        ZonedDateTime oneYearAhead = now.plusDays(365);
        DateRangeFilterInstruction eventsWithinSixtyDays = new DateRangeFilterInstruction(Date.from(oneYearAgo.toInstant()), Date.from(oneYearAhead.toInstant()));
        filterInstructions.put(ENDDATE_FIELDNAME, eventsWithinSixtyDays);

        try {
            ResultsWrapper<ContentDTO> findByFieldNames = this.contentManager.findByFieldNames(
                properties.getProperty(CONTENT_INDEX), ContentService.generateDefaultFieldToMatch(fieldsToMatch),
                startIndex, limit, sortInstructions, filterInstructions);
            for (ContentDTO contentResult : findByFieldNames.getResults()) {
                if (contentResult instanceof IsaacEventPageDTO) {
                    IsaacEventPageDTO page = (IsaacEventPageDTO) contentResult;
                    Set<String> matchingTags = eventTags.stream().distinct().filter(page.getTags()::contains).collect(Collectors.toSet());
                    // Event end date (if present) > 30 days ago, else event date > 30 days ago
                    log.info(page.getId());
                    log.info(matchingTags.toString());
                    log.info(page.getDate().toString());
                    List<Long> ids = Lists.newArrayList();
                    List<DetailedEventBookingDTO> eventBookings = bookingManager.adminGetBookingsByEventId(page.getId());
                    eventBookings.stream().map(DetailedEventBookingDTO::getUserBooked).map(UserSummaryDTO::getId)
                        .forEach(ids::add);
                    for (Long id : ids) {
                        try {
                            RegisteredUserDTO user = userAccountManager.getUserDTOById(id);
                            log.info(String.format("Sent email to user: %s %s, at: %s", user.getGivenName(), user.getFamilyName(), user.getEmail()));
                        } catch (NoUserException e) {
                            log.error(String.format("No user found with ID: %s", id));
                        }
                    }
                    log.info(" ");

//                    boolean endDate30DaysAgo = page.getEndDate() != null && page.getEndDate().toInstant().isBefore(thirtyDaysAgo.toInstant());
//                    boolean noEndDateAndStartDate30DaysAgo = page.getEndDate() == null && page.getDate().toInstant().isBefore(thirtyDaysAgo.toInstant());
//                    if (endDate30DaysAgo || noEndDateAndStartDate30DaysAgo) {
//                        String query = "UPDATE event_bookings SET additional_booking_information=jsonb_set(jsonb_set(jsonb_set(jsonb_set(additional_booking_information,"
//                            + " '{emergencyName}', '\"[REMOVED]\"'::JSONB, FALSE),"
//                            + " '{emergencyNumber}', '\"[REMOVED]\"'::JSONB, FALSE),"
//                            + " '{accessibilityRequirements}', '\"[REMOVED]\"'::JSONB, FALSE),"
//                            + " '{medicalRequirements}', '\"[REMOVED]\"'::JSONB, FALSE),"
//                            + " pii_removed=? "
//                            + " WHERE event_id = ?"
//                            + " AND additional_booking_information ??| array['emergencyName', 'emergencyNumber', 'accessibilityRequirements', 'medicalRequirements']"
//                            + " AND pii_removed IS NULL";
//                        try (Connection conn = database.getDatabaseConnection();
//                             PreparedStatement pst = conn.prepareStatement(query);
//                        ) {
//                            // Check for additional info that needs removing, check if pii has already been removed, if
//                            // so, don't re-remove
//                            pst.setTimestamp(1, Timestamp.valueOf(now.toLocalDateTime()));
//                            pst.setString(2, page.getId());
//
//                            int affectedRows = pst.executeUpdate();
//                            if (affectedRows > 0) {
//                                log.info("Event " + page.getId() + " had " + affectedRows + " bookings which have been scrubbed of PII");
//                            }
//                        }
//                    }
                }
            }
            log.info("Ran EventScheudledEmailJob");
        } catch (ContentManagerException e) { //SQLException |
            log.error("Failed to send scheduled event emails: ", e);
        } catch (SegueDatabaseException e) {
            e.printStackTrace();
        }
    }
}

