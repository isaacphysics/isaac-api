package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.scheduler.jobs.EventReminderEmailJob;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.DateRangeFilterInstruction;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.ENDDATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;

public class ScheduledEmailManager {
    private static final Logger log = LoggerFactory.getLogger(EventReminderEmailJob.class);

    private final PropertiesLoader properties;
    private final PostgresSqlDb database;
    private final IContentManager contentManager;
    private final EventBookingManager bookingManager;
    private final UserAccountManager userAccountManager;
    private final EmailManager emailManager;

    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    @Inject
    public ScheduledEmailManager(final PropertiesLoader properties,
                                 final PostgresSqlDb database,
                                 final IContentManager contentManager,
                                 final EventBookingManager bookingManager,
                                 final UserAccountManager userAccountManager,
                                 final EmailManager emailManager) {
        this.properties = properties;
        this.contentManager = contentManager;
        this.bookingManager = bookingManager;
        this.database = database;
        this.userAccountManager = userAccountManager;
        this.emailManager = emailManager;
    }

    public boolean scheduledEmailNotAlreadySent(IsaacEventPageDTO event, String appendId) {
        ZonedDateTime now = ZonedDateTime.now();
        String query = "INSERT INTO scheduled_emails(email_id, sent) VALUES (?, ?)"
            + " ON CONFLICT (email_id) DO NOTHING";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, event.getId() + appendId);
            pst.setTimestamp(2, Timestamp.valueOf(now.toLocalDateTime()));

            int notAlreadySent = pst.executeUpdate();
            return notAlreadySent > 0;
        } catch (SQLException e) {
            log.error("Failed to add the scheduled email sent time: ", e);
        }
        return false;
    };

    public void sendEmailForEvent(IsaacEventPageDTO event, String templateId, String appendId) throws SegueDatabaseException {
        if (this.scheduledEmailNotAlreadySent(event, appendId)) {
            List<Long> ids = Lists.newArrayList();
            List<DetailedEventBookingDTO> eventBookings = bookingManager.adminGetBookingsByEventId(event.getId());
            eventBookings.stream().map(DetailedEventBookingDTO::getUserBooked).map(UserSummaryDTO::getId)
                .forEach(ids::add);
            for (Long id : ids) {
                try {
                    RegisteredUserDTO user = userAccountManager.getUserDTOById(id);
                    emailManager.sendTemplatedEmailToUser(user,
                        emailManager.getEmailTemplateDTO(templateId),
                        new ImmutableMap.Builder<String, Object>()
                            .put("event.emailEventDetails", event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                            .put("event", event)
                            .build(),
                        EmailType.SYSTEM);
                    log.info(String.format("Sent email to user: %s %s, at: %s", user.getGivenName(), user.getFamilyName(), user.getEmail()));
                } catch (NoUserException e) {
                    log.error(String.format("No user found with ID: %s", id));
                } catch (ContentManagerException e) {
                    log.error("Failed to add the scheduled email sent time: ", e);
                }

            }
        }
        log.info(" ");
    }

    public void sendReminderEmails() {
        // Magic number
        Integer limit = 10000;
        Integer startIndex = 0;
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
        Map<String, AbstractFilterInstruction> filterInstructions = Maps.newHashMap();
        fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));
        sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.DESC);
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime threeDaysAhead = now.plusDays(3);
        DateRangeFilterInstruction
            eventsWithinThreeDays = new DateRangeFilterInstruction(new Date(), Date.from(threeDaysAhead.toInstant()));
        filterInstructions.put(ENDDATE_FIELDNAME, eventsWithinThreeDays);

        try {
            ResultsWrapper<ContentDTO> findByFieldNames = this.contentManager.findByFieldNames(
                properties.getProperty(CONTENT_INDEX), ContentService.generateDefaultFieldToMatch(fieldsToMatch),
                startIndex, limit, sortInstructions, filterInstructions);
            for (ContentDTO contentResult : findByFieldNames.getResults()) {
                if (contentResult instanceof IsaacEventPageDTO) {
                    IsaacEventPageDTO event = (IsaacEventPageDTO) contentResult;
                    this.sendEmailForEvent(event, "event_reminder", "@pre");
                }
            }
        } catch (ContentManagerException | SegueDatabaseException e) {
            log.error("Failed to send scheduled event reminder emails: ", e);
        }
    }

    public void sendFeedbackEmails() {
        // Magic number
        Integer limit = 10000;
        Integer startIndex = 0;
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
        Map<String, AbstractFilterInstruction> filterInstructions = Maps.newHashMap();
        fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));
        sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.DESC);
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime sixtyDaysAgo = now.plusDays(-60);

        DateRangeFilterInstruction
            eventsInLastSixtyDays = new DateRangeFilterInstruction(Date.from(sixtyDaysAgo.toInstant()), new Date());
        filterInstructions.put(ENDDATE_FIELDNAME, eventsInLastSixtyDays);

        try {
            ResultsWrapper<ContentDTO> findByFieldNames = this.contentManager.findByFieldNames(
                properties.getProperty(CONTENT_INDEX), ContentService.generateDefaultFieldToMatch(fieldsToMatch),
                startIndex, limit, sortInstructions, filterInstructions);
            for (ContentDTO contentResult : findByFieldNames.getResults()) {
                if (contentResult instanceof IsaacEventPageDTO) {
                    IsaacEventPageDTO event = (IsaacEventPageDTO) contentResult;
                    // Event end date (if present) is today or before, else event date is today or before
                    boolean endDateToday = event.getEndDate() != null && event.getEndDate().toInstant().isBefore(new Date().toInstant());
                    boolean noEndDateAndStartDateToday = event.getEndDate() == null && event.getDate().toInstant().isBefore(new Date().toInstant());
                    if (endDateToday || noEndDateAndStartDateToday) {
                        this.sendEmailForEvent(event, "event_feedback", "@post");
                    }
                }
            }
        } catch (ContentManagerException | SegueDatabaseException e) {
            log.error("Failed to send scheduled event feedback emails: ", e);
        }
    }

}
