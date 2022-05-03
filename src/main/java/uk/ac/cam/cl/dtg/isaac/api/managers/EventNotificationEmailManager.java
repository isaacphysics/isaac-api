package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.DateRangeFilterInstruction;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class EventNotificationEmailManager {
    private static final Logger log = LoggerFactory.getLogger(EventNotificationEmailManager.class);

    private final PropertiesLoader properties;
    private final IContentManager contentManager;
    private final EventBookingManager bookingManager;
    private final UserAccountManager userAccountManager;
    private final EmailManager emailManager;
    private final PgScheduledEmailManager pgScheduledEmailManager;


    /**
     * This class is required by quartz and must be executable by any instance of the segue api relying only on the
     * jobdata context provided.
     */
    @Inject
    public EventNotificationEmailManager(final PropertiesLoader properties,
                                         final IContentManager contentManager,
                                         final EventBookingManager bookingManager,
                                         final UserAccountManager userAccountManager,
                                         final EmailManager emailManager,
                                         final PgScheduledEmailManager pgScheduledEmailManager) {
        this.properties = properties;
        this.contentManager = contentManager;
        this.bookingManager = bookingManager;
        this.userAccountManager = userAccountManager;
        this.emailManager = emailManager;
        this.pgScheduledEmailManager = pgScheduledEmailManager;
    }

    public void sendBookingStatusFilteredEmailForEvent(IsaacEventPageDTO event, String templateId, List<BookingStatus> bookingStatuses) throws SegueDatabaseException {
        List<DetailedEventBookingDTO> eventBookings = bookingManager.adminGetBookingsByEventId(event.getId());
        List<Long> ids = eventBookings.stream()
                            .filter(DetailedEventBookingDTO -> bookingStatuses == null || bookingStatuses.contains(DetailedEventBookingDTO.getBookingStatus()))
                            .map(DetailedEventBookingDTO::getUserBooked)
                            .map(UserSummaryDTO::getId)
                            .distinct().collect(Collectors.toList());
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
                log.debug(String.format("Sent email to user: %s %s, at: %s", user.getGivenName(), user.getFamilyName(), user.getEmail()));
            } catch (NoUserException e) {
                log.error(String.format("No user found with ID: %s", id));
            } catch (ContentManagerException e) {
                log.error("Failed to add the scheduled email sent time: ", e);
            }
        }
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
        filterInstructions.put(DATE_FIELDNAME, eventsWithinThreeDays);

        try {
            ResultsWrapper<ContentDTO> findByFieldNames = this.contentManager.findByFieldNames(
                properties.getProperty(CONTENT_INDEX), ContentService.generateDefaultFieldToMatch(fieldsToMatch),
                startIndex, limit, sortInstructions, filterInstructions);
            for (ContentDTO contentResult : findByFieldNames.getResults()) {
                if (contentResult instanceof IsaacEventPageDTO) {
                    IsaacEventPageDTO event = (IsaacEventPageDTO) contentResult;
                    String emailKey = String.format("%s@pre", event.getId());
                    // Includes the attended status in case the events team have pre-emptively marked someone as attended.
                    List<BookingStatus> bookingStatuses = Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.ATTENDED);
                    if (pgScheduledEmailManager.saveScheduledEmailSent(emailKey)) {
                        this.sendBookingStatusFilteredEmailForEvent(event, "event_reminder", bookingStatuses);
                    }
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
        filterInstructions.put(DATE_FIELDNAME, eventsInLastSixtyDays);

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
                        String emailKey = String.format("%s@post", event.getId());
                        // Includes the confirmed status in case the events team don't update the status to attended in time.
                        List<BookingStatus> bookingStatuses = Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.ATTENDED);
                        if (pgScheduledEmailManager.saveScheduledEmailSent(emailKey)) {
                            this.sendBookingStatusFilteredEmailForEvent(event, "event_feedback", bookingStatuses);
                        }
                    }
                }
            }
        } catch (ContentManagerException | SegueDatabaseException e) {
            log.error("Failed to send scheduled event feedback emails: ", e);
        }
    }

}
