package uk.ac.cam.cl.dtg.isaac.api.managers;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_EVENT_FEEDBACK_DAYS_AGO;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EVENT_TYPE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_MAX_WINDOW_SIZE;

import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.search.AbstractFilterInstruction;
import uk.ac.cam.cl.dtg.segue.search.DateRangeFilterInstruction;

/**
 * Unit tests for EventNotificationEmailManager.
 */
class EventNotificationEmailManagerTest {

  private GitContentManager mockContentManager;
  private EventBookingManager mockBookingManager;
  private UserAccountManager mockUserAccountManager;
  private EmailManager mockEmailManager;
  private PgScheduledEmailManager mockPgScheduledEmailManager;
  private EventNotificationEmailManager eventNotificationEmailManager;
  private Object[] mockedObjects;

  @BeforeEach
  void setUp() {
    mockContentManager = createMock(GitContentManager.class);
    mockBookingManager = createMock(EventBookingManager.class);
    mockUserAccountManager = createMock(UserAccountManager.class);
    mockEmailManager = createMock(EmailManager.class);
    mockPgScheduledEmailManager = createMock(PgScheduledEmailManager.class);

    mockedObjects = new Object[] {
        mockContentManager, mockBookingManager, mockUserAccountManager,
        mockEmailManager, mockPgScheduledEmailManager
    };

    eventNotificationEmailManager = new EventNotificationEmailManager(
        mockContentManager, mockBookingManager, mockUserAccountManager,
        mockEmailManager, mockPgScheduledEmailManager
    );
  }

  @Nested
  class SendFeedbackEmails {

    @Test
    void sendFeedbackEmails_EventAt24HoursPostResources_SendsFeedbackEmail() throws Exception {
      // Arrange
      Instant eventDate = Instant.now().minus(24, ChronoUnit.HOURS); // 24 hours ago
      IsaacEventPageDTO event = createTestEvent("event1", eventDate, null, EventStatus.FULLY_BOOKED);
      event.setPostResources(ImmutableList.of(createExternalReference("resource1")));

      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 1);

      setupContentManagerExpectations(Collections.singletonList(event));
      setupBookingManagerExpectations(event.getId(), bookings, 1);
      setupEmailSendingExpectations(event, "post", "event_feedback", 1, 1);

      replay(mockedObjects);

      // Act
      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());

      // Assert
      verify(mockedObjects);
    }

    @Test
    void sendFeedbackEmails_EventAt96HoursWithPostResources_SendsFeedbackEmail() throws Exception {
      // Arrange
      Instant eventDate = Instant.now().minus(96, ChronoUnit.HOURS); // 96 hours ago
      IsaacEventPageDTO event = createTestEvent("event96", eventDate, null, EventStatus.FULLY_BOOKED);
      event.setPostResources(ImmutableList.of(createExternalReference("resource96")));

      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 1);

      setupContentManagerExpectations(Collections.singletonList(event));
      setupBookingManagerExpectations(event.getId(), bookings, 2);
      setupEmailSendingExpectations(event, "post", "event_feedback", 1, 2);

      replay(mockedObjects);

      // Act
      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());

      // Assert
      verify(mockedObjects);
    }

    @Test
    void sendFeedbackEmails_EventAt22Hours_DoesNotSendFeedbackEmail() throws Exception {
      // Arrange
      Instant eventDate = Instant.now().minus(22, ChronoUnit.HOURS); // 22 hours ago
      IsaacEventPageDTO event = createTestEvent("event22", eventDate, null, EventStatus.FULLY_BOOKED);
      event.setPostResources(ImmutableList.of(createExternalReference("resource22")));

      List<DetailedEventBookingDTO> bookings = createAttendedBookings(event.getId(), 1);

      setupContentManagerExpectations(Collections.singletonList(event));
      setupBookingManagerExpectations(event.getId(), bookings, 0);

      // Ensure no email is sent
      expect(mockPgScheduledEmailManager.commitToSchedulingEmail(anyObject())).andThrow(new AssertionFailedError())
          .anyTimes();

      replay(mockedObjects);

      // Act
      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());

      // Assert
      verify(mockedObjects);
    }

    @Test
    void sendFeedbackEmails_CancelledEvent_DoesNotSendFeedbackEmail() throws Exception {
      // Arrange
      Instant eventDate = Instant.now().minus(24, ChronoUnit.HOURS); // 24 hours ago
      IsaacEventPageDTO event = createTestEvent("eventCancelled", eventDate, null, EventStatus.CANCELLED);
      event.setPostResources(ImmutableList.of(createExternalReference("resourceCancelled")));

      setupContentManagerExpectations(Collections.singletonList(event));

      // Ensure no email is sent
      expect(mockPgScheduledEmailManager.commitToSchedulingEmail(anyObject())).andThrow(new AssertionFailedError())
          .anyTimes();

      replay(mockedObjects);

      // Act
      assertDoesNotThrow(() -> eventNotificationEmailManager.sendFeedbackEmails());

      // Assert
      verify(mockedObjects);
    }
  }

  // Helper methods

  private IsaacEventPageDTO createTestEvent(String id, Instant date, Instant endDate, EventStatus status) {
    IsaacEventPageDTO event = new IsaacEventPageDTO();
    event.setId(id);
    event.setDate(date);
    event.setEndDate(endDate);
    event.setEventStatus(status);
    event.setType(EVENT_TYPE);
    return event;
  }

  private ExternalReference createExternalReference(String title) {
    ExternalReference ref = new ExternalReference();
    ref.setTitle(title);
    ref.setUrl("https://example.com/" + title);
    return ref;
  }

  private List<DetailedEventBookingDTO> createAttendedBookings(String eventId, int count) {
    ImmutableList.Builder<DetailedEventBookingDTO> builder = ImmutableList.builder();
    for (int i = 1; i <= count; i++) {
      UserSummaryWithEmailAddressDTO user = new UserSummaryWithEmailAddressDTO();
      user.setId((long) i);
      user.setGivenName("User" + i);
      user.setFamilyName("Test");
      user.setEmail("user" + i + "@test.com");

      DetailedEventBookingDTO booking = new DetailedEventBookingDTO();
      booking.setUserBooked(user);
      booking.setBookingStatus(BookingStatus.ATTENDED);
      booking.setEventId(eventId);

      builder.add(booking);
    }
    return builder.build();
  }

  private void setupContentManagerExpectations(List<IsaacEventPageDTO> events) throws ContentManagerException {
    Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
    fieldsToMatch.put(TYPE_FIELDNAME, Collections.singletonList(EVENT_TYPE));

    Map<String, Constants.SortOrder> sortInstructions = Maps.newHashMap();
    sortInstructions.put(DATE_FIELDNAME, Constants.SortOrder.DESC);

    Map<String, AbstractFilterInstruction> filterInstructions = Maps.newHashMap();
    ZonedDateTime now = ZonedDateTime.now();
    ZonedDateTime eventFeedbackThresholdDate = now.minusDays(EMAIL_EVENT_FEEDBACK_DAYS_AGO);
    DateRangeFilterInstruction eventsWithinFeedbackDateRange = new DateRangeFilterInstruction(
        eventFeedbackThresholdDate.toInstant(), Instant.now());
    filterInstructions.put(DATE_FIELDNAME, eventsWithinFeedbackDateRange);

    ResultsWrapper<ContentDTO> resultsWrapper =
        new ResultsWrapper<>((List<ContentDTO>) (List<?>) events, (long) events.size());

    expect(mockContentManager.findByFieldNames(
        anyObject(),
        eq(0),
        eq(DEFAULT_MAX_WINDOW_SIZE),
        anyObject(),
        anyObject()
    )).andReturn(resultsWrapper);
  }

  private void setupBookingManagerExpectations(String eventId, List<DetailedEventBookingDTO> bookings, int callsCount)
      throws SegueDatabaseException {
    for (int i = 0; i < callsCount; i++) {
      expect(mockBookingManager.adminGetBookingsByEventId(eventId)).andReturn(bookings);
    }
  }

  private void setupEmailSendingExpectations(IsaacEventPageDTO event, String emailKeyPostfix,
                                             String templateId, int userCount, int numberOfEmails) throws Exception {
    String emailKey = event.getId() + "@" + emailKeyPostfix;

    for (int i = 0; i < userCount; i++) {
      RegisteredUserDTO user = new RegisteredUserDTO();
      user.setId((long) (i + 1));
      user.setGivenName("User" + (i + 1));
      user.setFamilyName("Test");
      user.setEmail("user" + (i + 1) + "@test.com");

      //Multiple emails maybe sent for 96 hours in test (ONLY) but are stopped by infrastructure in production.
      for (int j = 0; j < numberOfEmails; j++) {
        expect(mockEmailManager.getEmailTemplateDTO(templateId)).andReturn(createEmailTemplate());
        expect(mockUserAccountManager.getUserDTOById((long) (i + 1))).andReturn(user);
        expect(mockPgScheduledEmailManager.commitToSchedulingEmail(emailKey)).andReturn(true);
        mockEmailManager.sendTemplatedEmailToUser(eq(user), anyObject(), anyObject(), eq(EmailType.SYSTEM));
      }
      expectLastCall();
    }
  }

  private uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO createEmailTemplate() {
    uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO template =
        new uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO();
    template.setId("test-template");
    template.setSubject("Test Subject");
    template.setHtmlContent("<p>Test content</p>");
    template.setPlainTextContent("Test content");
    return template;
  }
}
