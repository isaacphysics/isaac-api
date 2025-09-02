package uk.ac.cam.cl.dtg.isaac.api.managers;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_EVENT_FEEDBACK_DAYS_AGO;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_EVENT_REMINDER_DAYS_AHEAD;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_MAX_WINDOW_SIZE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;

import com.google.api.client.util.Maps;
import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
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

  public static final Integer EMAIL_EVENT_SECOND_FEEDBACK_HOURS = 96;

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
                                                     final List<BookingStatus> bookingStatuses) {

    if (event == null || templateId == null || templateId.trim().isEmpty()) {
      throw new IllegalArgumentException("Event and templateId cannot be null or empty");
    }

    final List<DetailedEventBookingDTO> eventBookings;
    try {
      eventBookings = bookingManager.adminGetBookingsByEventId(event.getId());
    } catch (SegueDatabaseException e) {
      log.error("Failed to retrieve bookings for event ID {}: ", event.getId(), e);
      return;
    }

    if (eventBookings.isEmpty()) {
      log.info("No bookings found for event ID {}", event.getId());
      return;
    }

    final Map<String, Object> emailContext = Map.of(
        "event.emailEventDetails", Objects.toString(event.getEmailEventDetails(), ""),
        "event", event
    );

    final List<Long> userIds = eventBookings.stream()
        .filter(booking -> booking != null && booking.getUserBooked() != null)
        .filter(booking -> bookingStatuses == null || bookingStatuses.contains(booking.getBookingStatus()))
        .map(DetailedEventBookingDTO::getUserBooked)
        .map(UserSummaryDTO::getId)
        .filter(Objects::nonNull)
        .toList();

    if (userIds.isEmpty()) {
      log.error("No users match the specified booking statuses for event ID {}", event.getId());
      return;
    }

    final EmailTemplateDTO emailTemplate;
    try {
      emailTemplate = emailManager.getEmailTemplateDTO(templateId);
    } catch (ContentManagerException | ResourceNotFoundException e) {
      log.error("Failed to retrieve email template with ID {}: ", templateId, e);
      return;
    }

    final AtomicInteger successCount = new AtomicInteger(0);
    final AtomicInteger failureCount = new AtomicInteger(0);
    final List<Long> failedUserIds = Collections.synchronizedList(new ArrayList<>());

    userIds.forEach(userId -> processUser(userId, emailTemplate, emailContext,
        successCount, failureCount, failedUserIds));

    final int totalProcessed = successCount.get() + failureCount.get();
    log.info("Email processing completed for event ID {}: {} successes, {} failures out of {} users",
        event.getId(), successCount.get(), failureCount.get(), totalProcessed);

    if (!failedUserIds.isEmpty()) {
      log.warn("Failed to send emails to user IDs: {}", failedUserIds);
    }
  }

  private void processUser(final Long userId, final EmailTemplateDTO emailTemplate,
                           final Map<String, Object> emailContext,
                           final AtomicInteger successCount, final AtomicInteger failureCount,
                           final List<Long> failedUserIds) {
    try {
      final RegisteredUserDTO user = userAccountManager.getUserDTOById(userId);

      emailManager.sendTemplatedEmailToUser(user, emailTemplate, emailContext, EmailType.SYSTEM);

      successCount.incrementAndGet();
      log.debug("Sent email to user: {} {}, at: {}",
          user.getGivenName(), user.getFamilyName(), user.getEmail());

    } catch (NoUserException e) {
      log.error("No user found with ID {}: ", userId, e);
      failureCount.incrementAndGet();
      failedUserIds.add(userId);
    } catch (ContentManagerException e) {
      log.error("Failed to send email to user ID {}: ", userId, e);
      failureCount.incrementAndGet();
      failedUserIds.add(userId);
    } catch (Exception e) {
      log.error("Unexpected error processing email for user ID {}: ", userId, e);
      failureCount.incrementAndGet();
      failedUserIds.add(userId);
    }
  }

  private void commitAndSendReminderEmail(IsaacEventPageDTO event, String emailKeyPostfix, String templateId)
      throws SegueDatabaseException {
    String emailKey = String.format("%s@%s", event.getId(), emailKeyPostfix);
    /*
    Confirmed and Attended statuses are both included for pre-event emails.
    Pre-event emails include the attended status in case the events team have pre-emptively marked someone as attended.
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
    ZonedDateTime eventReminderThresholdDate = now.plusDays(EMAIL_EVENT_REMINDER_DAYS_AHEAD);
    Instant endOfToday = now.with(LocalTime.MAX).toInstant();
    DateRangeFilterInstruction eventsWithinReminderDateRange =
        new DateRangeFilterInstruction(Instant.now(), eventReminderThresholdDate.toInstant());
    filterInstructions.put(DATE_FIELDNAME, eventsWithinReminderDateRange);

    try {
      ResultsWrapper<ContentDTO> findByFieldNames = this.contentManager.findByFieldNames(
          ContentService.generateDefaultFieldToMatch(fieldsToMatch), startIndex, DEFAULT_MAX_WINDOW_SIZE,
          sortInstructions,
          filterInstructions);
      for (ContentDTO contentResult : findByFieldNames.getResults()) {
        if (contentResult instanceof IsaacEventPageDTO event) {
          // Skip sending emails for cancelled events
          if (EventStatus.CANCELLED.equals(event.getEventStatus())) {
            continue;
          }
          if (event.getDate().isAfter(endOfToday)) {
            commitAndSendReminderEmail(event, "pre", "event_reminder");
          } else {
            commitAndSendReminderEmail(event, "presameday", "event_reminder_same_day");
          }
        }
      }
    } catch (ContentManagerException | SegueDatabaseException e) {
      log.error("Failed to send scheduled event reminder emails: ", e);
    }
  }

  private void commitAndSendFeedbackEmail(IsaacEventPageDTO event, String emailKeyPostfix, String templateId)
      throws SegueDatabaseException {
    String emailKey = String.format("%s@%s", event.getId(), emailKeyPostfix);
    /*
    Attended statuses included for post-event emails.
     */
    List<BookingStatus> bookingStatuses = List.of(BookingStatus.ATTENDED);
    if (pgScheduledEmailManager.commitToSchedulingEmail(emailKey)) {
      this.sendBookingStatusFilteredEmailForEvent(event, templateId, bookingStatuses);
    }
  }

  public void sendFeedbackEmails() {
    log.info("Starting feedback email processing...");

    var failedEvents = new ArrayList<>();
    var successfulEvents = new ArrayList<>();

    try {
      Instant thresholdDate = Instant.now().minus(EMAIL_EVENT_FEEDBACK_DAYS_AGO, ChronoUnit.DAYS);
      Map<String, List<String>> fieldsToMatch = Map.of(TYPE_FIELDNAME, List.of(EVENT_TYPE));
      Map<String, Constants.SortOrder> sortInstructions = Map.of(DATE_FIELDNAME, Constants.SortOrder.DESC);
      Map<String, AbstractFilterInstruction> filterInstructions = Map.of(
          DATE_FIELDNAME, new DateRangeFilterInstruction(thresholdDate, Instant.now())
      );

      ResultsWrapper<ContentDTO> results = contentManager.findByFieldNames(
          ContentService.generateDefaultFieldToMatch(fieldsToMatch),
          0, DEFAULT_MAX_WINDOW_SIZE, sortInstructions, filterInstructions);

      Instant currentTime = Instant.now();

      results.getResults().stream()
          .filter(IsaacEventPageDTO.class::isInstance)
          .map(IsaacEventPageDTO.class::cast)
          .filter(event -> !EventStatus.CANCELLED.equals(event.getEventStatus()))
          .filter(this::hasRequiredResourcesOrSurvey)
          .forEach(event -> {
            try {
              processEvent(event, currentTime);
              successfulEvents.add(event);
            } catch (SegueDatabaseException e) {
              failedEvents.add(event);
              log.error("Failed to process event {}: {}", event.getId(), e.getMessage(), e);
            }
          });
    } catch (ContentManagerException e) {
      log.warn("The following feedback emails processing failed for the following events: {}", failedEvents);
    }
    log.info("Completed feedback email processing: {} successes, {} failures",
        successfulEvents.size(), failedEvents.size());
  }

  private boolean hasRequiredResourcesOrSurvey(IsaacEventPageDTO event) {
    boolean hasResources = Optional.ofNullable(event.getPostResources())
        .map(resources -> !resources.isEmpty())
        .orElse(false);

    boolean hasSurvey = Optional.ofNullable(event.getEventSurvey())
        .filter(url -> !url.trim().isEmpty())
        .isPresent();

    return hasResources || hasSurvey;
  }

  private void processEvent(IsaacEventPageDTO event, Instant currentTime)
      throws SegueDatabaseException {
    Instant eventEndTime = Optional.ofNullable(event.getEndDate()).orElse(event.getDate());
    if (eventEndTime == null) {
      return;
    }

    Duration timeSinceEvent = Duration.between(eventEndTime, currentTime);

    // 96+ hours after event
    if (timeSinceEvent.compareTo(Duration.ofHours(EMAIL_EVENT_SECOND_FEEDBACK_HOURS)) >= 0) {
      log.info("Sending 96-hour survey email for event: {}", event.getId());
      commitAndSendFeedbackEmail(event, "survey96", "event_survey");
      log.info("Sent 96-hour survey email for event: {}", event.getId());
      // 24+ hours after event (but less than 96 hours)
    } else if (timeSinceEvent.compareTo(Duration.ofDays(1)) >= 0) {
      log.info("Sending 24-hour feedback email for event: {}", event.getId());
      commitAndSendFeedbackEmail(event, "post", "event_feedback");
      log.info("Sent 24-hour feedback email for event: {}", event.getId());
    }
  }
}
