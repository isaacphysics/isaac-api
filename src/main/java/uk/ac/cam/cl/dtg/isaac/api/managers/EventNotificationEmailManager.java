package uk.ac.cam.cl.dtg.isaac.api.managers;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_EVENT_FEEDBACK_DAYS_AGO;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_EVENT_REMINDER_DAYS_AHEAD;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_MAX_WINDOW_SIZE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;
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
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.DateRangeFilterInstruction;

public class EventNotificationEmailManager {
  private static final Logger log = LoggerFactory.getLogger(EventNotificationEmailManager.class);

  private final GitContentManager contentManager;
  private final EventBookingManager bookingManager;
  private final UserAccountManager userAccountManager;
  private final EmailManager emailManager;
  private final PgScheduledEmailManager pgScheduledEmailManager;


  /**
   * This class is required by quartz and must be executable by any instance of the segue api relying only on the
   * jobdata context provided.
   *
   * @param contentManager          - for retrieving content
   * @param bookingManager          - Instance of Booking Manager
   * @param userAccountManager      - Instance of User Account Manager, for retrieving users
   * @param emailManager            - for constructing and sending emails
   * @param pgScheduledEmailManager - for scheduling the sending of emails
   */
  @Inject
  public EventNotificationEmailManager(
      final GitContentManager contentManager,
      final EventBookingManager bookingManager,
      final UserAccountManager userAccountManager,
      final EmailManager emailManager,
      final PgScheduledEmailManager pgScheduledEmailManager
  ) {
    this.contentManager = contentManager;
    this.bookingManager = bookingManager;
    this.userAccountManager = userAccountManager;
    this.emailManager = emailManager;
    this.pgScheduledEmailManager = pgScheduledEmailManager;
  }

  public void sendBookingStatusFilteredEmailForEvent(final IsaacEventPageDTO event, final String templateId,
                                                     final List<BookingStatus> bookingStatuses)
      throws SegueDatabaseException {
    List<DetailedEventBookingDTO> eventBookings = bookingManager.adminGetBookingsByEventId(event.getId());
    List<Long> ids = eventBookings.stream()
        .filter(detailedEventBookingDTO -> bookingStatuses == null || bookingStatuses.contains(
            detailedEventBookingDTO.getBookingStatus()))
        .map(DetailedEventBookingDTO::getUserBooked)
        .map(UserSummaryDTO::getId)
        .distinct().collect(Collectors.toList());
    for (Long id : ids) {
      try {
        RegisteredUserDTO user = userAccountManager.getUserDTOById(id);
        emailManager.sendTemplatedEmailToUser(user,
            emailManager.getEmailTemplateDTO(templateId),
            new ImmutableMap.Builder<String, Object>()
                .put("event.emailEventDetails",
                    event.getEmailEventDetails() == null ? "" : event.getEmailEventDetails())
                .put("event", event)
                .build(),
            EmailType.SYSTEM);
        log.debug("Sent email to user: {} {}, at: {}", user.getGivenName(), user.getFamilyName(), user.getEmail());
      } catch (NoUserException e) {
        log.error(String.format("No user found with ID: %s", id));
      } catch (ContentManagerException e) {
        log.error("Failed to add the scheduled email sent time: ", e);
      }
    }
  }

  private void commitAndSendStatusFilteredEmail(IsaacEventPageDTO event, String emailKeyPostfix, String templateId)
      throws SegueDatabaseException {
    String emailKey = String.format("%s@%s", event.getId(), emailKeyPostfix);
    /*
    Confirmed and Attended statuses are both included for both pre- and post-event emails.
    Pre-event emails include the attended status in case the events team have pre-emptively marked someone as attended.
    Post-event emails include the confirmed status in case the events team don't update the status to attended in time.
     */
    List<BookingStatus> bookingStatuses = Arrays.asList(BookingStatus.CONFIRMED, BookingStatus.ATTENDED);
    if (pgScheduledEmailManager.commitToSchedulingEmail(emailKey)) {
      this.sendBookingStatusFilteredEmailForEvent(event, templateId, bookingStatuses);
    }
  }

  public void sendReminderEmails() {
    // Magic number
    Integer startIndex = 0;
    Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
    Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
    Map<String, AbstractFilterInstruction> filterInstructions = Maps.newHashMap();
    fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));
    sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.DESC);
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime threeDaysAhead = now.plusDays(EMAIL_EVENT_REMINDER_DAYS_AHEAD);
    Date endOfToday = Date.from(now.with(LocalTime.MAX).toInstant());
    DateRangeFilterInstruction
        eventsWithinThreeDays = new DateRangeFilterInstruction(new Date(), Date.from(threeDaysAhead.toInstant()));
    filterInstructions.put(DATE_FIELDNAME, eventsWithinThreeDays);

    try {
      ResultsWrapper<ContentDTO> findByFieldNames = this.contentManager.findByFieldNames(
          ContentService.generateDefaultFieldToMatch(fieldsToMatch), startIndex, DEFAULT_MAX_WINDOW_SIZE,
          sortInstructions,
          filterInstructions);
      for (ContentDTO contentResult : findByFieldNames.getResults()) {
        if (contentResult instanceof IsaacEventPageDTO) {
          IsaacEventPageDTO event = (IsaacEventPageDTO) contentResult;
          // Skip sending emails for cancelled events
          if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
            continue;
          }
          if (event.getDate().after(endOfToday)) {
            commitAndSendStatusFilteredEmail(event, "pre", "event_reminder");
          } else {
            commitAndSendStatusFilteredEmail(event, "presameday", "event_reminder_same_day");
          }
        }
      }
    } catch (ContentManagerException | SegueDatabaseException e) {
      log.error("Failed to send scheduled event reminder emails: ", e);
    }
  }

  public void sendFeedbackEmails() {
    // Magic number
    Integer startIndex = 0;
    Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
    Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
    Map<String, AbstractFilterInstruction> filterInstructions = Maps.newHashMap();
    fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));
    sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.DESC);
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime sixtyDaysAgo = now.plusDays(EMAIL_EVENT_FEEDBACK_DAYS_AGO);

    DateRangeFilterInstruction eventsInLastSixtyDays = new DateRangeFilterInstruction(
        Date.from(sixtyDaysAgo.toInstant()), new Date());
    filterInstructions.put(DATE_FIELDNAME, eventsInLastSixtyDays);

    try {
      ResultsWrapper<ContentDTO> findByFieldNames = this.contentManager.findByFieldNames(
          ContentService.generateDefaultFieldToMatch(fieldsToMatch), startIndex, DEFAULT_MAX_WINDOW_SIZE,
          sortInstructions,
          filterInstructions);
      for (ContentDTO contentResult : findByFieldNames.getResults()) {
        if (contentResult instanceof IsaacEventPageDTO) {
          IsaacEventPageDTO event = (IsaacEventPageDTO) contentResult;
          // Skip sending emails for cancelled events
          if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
            continue;
          }
          // Event end date (if present) is today or before, else event date is today or before
          boolean endDateToday =
              event.getEndDate() != null && event.getEndDate().toInstant().isBefore(new Date().toInstant());
          boolean noEndDateAndStartDateToday =
              event.getEndDate() == null && event.getDate().toInstant().isBefore(new Date().toInstant());
          if (endDateToday || noEndDateAndStartDateToday) {
            List<ExternalReference> postResources = event.getPostResources();
            boolean postResourcesPresent =
                postResources != null && !postResources.isEmpty() && !postResources.contains(null);
            if (postResourcesPresent) {
              commitAndSendStatusFilteredEmail(event, "post", "event_feedback");
            }
          }
        }
      }
    } catch (ContentManagerException | SegueDatabaseException e) {
      log.error("Failed to send scheduled event feedback emails: ", e);
    }
  }
}
