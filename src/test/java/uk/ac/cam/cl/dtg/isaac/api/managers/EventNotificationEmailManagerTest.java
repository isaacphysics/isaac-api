package uk.ac.cam.cl.dtg.isaac.api.managers;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.easymock.EasyMock;
import org.easymock.EasyMockExtension;
import org.easymock.Mock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.content.ExternalReference;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.utils.TestDataFactory;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;

@ExtendWith(EasyMockExtension.class)
class EventNotificationEmailManagerTest {

  @Mock
  private GitContentManager contentManager;

  @Mock
  private EventBookingManager eventBookingManager;

  @Mock
  private UserAccountManager userAccountManager;

  @Mock
  private EmailManager emailManager;

  @Mock
  private PgScheduledEmailManager pgScheduledEmailManager;

  @Mock
  private ResultsWrapper<ContentDTO> resultsWrapper;

  private EventNotificationEmailManager eventNotificationEmailManager;

  private IsaacEventPageDTO testEvent;

  private List<ExternalReference> mockPostResources;

  @BeforeEach
  void setUp() {
    eventNotificationEmailManager = new EventNotificationEmailManager(
        contentManager,
        eventBookingManager,
        userAccountManager,
        emailManager,
        pgScheduledEmailManager
    );

    testEvent = new IsaacEventPageDTO();
    testEvent.setId("test-event-1");
    testEvent.setEventStatus(EventStatus.OPEN);
    testEvent.setEventSurvey("https://survey.example.com");
    testEvent.setEventSurveyTitle("Test Survey");

    mockPostResources = List.of(
        new ExternalReference("Resource 1", "https://resource1.com"),
        new ExternalReference("Resource 2", "https://resource2.com")
    );
    testEvent.setPostResources(mockPostResources);
  }

  private void setupMockContentManager(List<IsaacEventPageDTO> events) throws Exception {
    List<ContentDTO> contentResults = new ArrayList<>(events);
    expect(resultsWrapper.getResults()).andReturn(contentResults).anyTimes();
    expect(contentManager.findByFieldNames(anyObject(), anyInt(), anyInt(), anyObject(), anyObject()))
        .andReturn(resultsWrapper).anyTimes();
  }

  @Nested
  @DisplayName("24 Hour Trigger Tests")
  class TwentyFourHourTriggerTests {

    @Test
    @DisplayName("Should send 24-hour email for event ended 25 hours ago")
    void shouldSend24HourEmailForRecentEvent() throws Exception {
      Instant twentyFiveHoursAgo = Instant.now().minus(25, ChronoUnit.HOURS);
      testEvent.setDate(twentyFiveHoursAgo);
      testEvent.setEndDate(null); // Single day event

      setupMockContentManager(Collections.singletonList(testEvent));

      expect(pgScheduledEmailManager.commitToSchedulingEmail("test-event-1@post"))
          .andReturn(true).atLeastOnce();

      EventNotificationEmailManager partialMock = EasyMock.partialMockBuilder(EventNotificationEmailManager.class)
          .withConstructor(
              GitContentManager.class,
              EventBookingManager.class,
              UserAccountManager.class,
              EmailManager.class,
              PgScheduledEmailManager.class
          )
          .withArgs(contentManager, eventBookingManager, userAccountManager, emailManager, pgScheduledEmailManager)
          .addMockedMethod("sendBookingStatusFilteredEmailForEvent")
          .createMock();

      partialMock.sendBookingStatusFilteredEmailForEvent(
          anyObject(IsaacEventPageDTO.class),
          eq("event_feedback"),
          eq(List.of(BookingStatus.ATTENDED))
      );
      expectLastCall().atLeastOnce();

      replay(contentManager, resultsWrapper, pgScheduledEmailManager, partialMock);

      partialMock.sendFeedbackEmails();

      verify(pgScheduledEmailManager, partialMock);
    }

    @Test
    @DisplayName("Should send 24-hour email for multi-day event ended 25 hours ago")
    void shouldSend24HourEmailForMultiDayEvent() throws Exception {
      Instant startTime = Instant.now().minus(50, ChronoUnit.HOURS);
      Instant endTime = Instant.now().minus(25, ChronoUnit.HOURS);
      testEvent.setDate(startTime);
      testEvent.setEndDate(endTime);

      setupMockContentManager(Collections.singletonList(testEvent));
      expect(pgScheduledEmailManager.commitToSchedulingEmail("test-event-1@post"))
          .andReturn(true).atLeastOnce();

      EventNotificationEmailManager partialMock = EasyMock.partialMockBuilder(EventNotificationEmailManager.class)
          .withConstructor(
              GitContentManager.class,
              EventBookingManager.class,
              UserAccountManager.class,
              EmailManager.class,
              PgScheduledEmailManager.class
          )
          .withArgs(contentManager, eventBookingManager, userAccountManager, emailManager, pgScheduledEmailManager)
          .addMockedMethod("sendBookingStatusFilteredEmailForEvent")
          .createMock();

      partialMock.sendBookingStatusFilteredEmailForEvent(
          anyObject(IsaacEventPageDTO.class),
          eq("event_feedback"),
          eq(List.of(BookingStatus.ATTENDED))
      );
      expectLastCall().atLeastOnce();

      replay(contentManager, resultsWrapper, pgScheduledEmailManager, partialMock);

      partialMock.sendFeedbackEmails();

      verify(pgScheduledEmailManager, partialMock);
    }

    @Test
    @DisplayName("Should NOT send 24-hour email for event ended less than 24 hours ago")
    void shouldNotSend24HourEmailForTooRecentEvent() throws Exception {
      Instant twentyHoursAgo = Instant.now().minus(20, ChronoUnit.HOURS);
      testEvent.setDate(twentyHoursAgo);

      setupMockContentManager(Collections.singletonList(testEvent));

      replay(contentManager, resultsWrapper, pgScheduledEmailManager);

      eventNotificationEmailManager.sendFeedbackEmails();

      verify(contentManager, resultsWrapper, pgScheduledEmailManager);
    }
  }

  @Nested
  @DisplayName("96 Hour Trigger Tests")
  class NinetySixHourTriggerTests {

    @Test
    @DisplayName("Should send 96-hour email for event ended 100 hours ago")
    void shouldSend96HourEmailForOldEvent() throws Exception {
      Instant hundredHoursAgo = Instant.now().minus(100, ChronoUnit.HOURS);
      testEvent.setDate(hundredHoursAgo);
      testEvent.setEndDate(null);

      setupMockContentManager(Collections.singletonList(testEvent));
      expect(pgScheduledEmailManager.commitToSchedulingEmail("test-event-1@survey96"))
          .andReturn(true).atLeastOnce();

      EventNotificationEmailManager partialMock = EasyMock.partialMockBuilder(EventNotificationEmailManager.class)
          .withConstructor(
              GitContentManager.class,
              EventBookingManager.class,
              UserAccountManager.class,
              EmailManager.class,
              PgScheduledEmailManager.class
          )
          .withArgs(contentManager, eventBookingManager, userAccountManager, emailManager, pgScheduledEmailManager)
          .addMockedMethod("sendBookingStatusFilteredEmailForEvent")
          .createMock();

      partialMock.sendBookingStatusFilteredEmailForEvent(
          anyObject(IsaacEventPageDTO.class),
          eq("event_survey"),
          eq(List.of(BookingStatus.ATTENDED))
      );
      expectLastCall().atLeastOnce();

      replay(contentManager, resultsWrapper, pgScheduledEmailManager, partialMock);

      partialMock.sendFeedbackEmails();

      verify(pgScheduledEmailManager, partialMock);
    }

    @Test
    @DisplayName("Should send 96-hour email for multi-day event ended 100 hours ago")
    void shouldSend96HourEmailForOldMultiDayEvent() throws Exception {
      Instant startTime = Instant.now().minus(120, ChronoUnit.HOURS);
      Instant endTime = Instant.now().minus(100, ChronoUnit.HOURS);
      testEvent.setDate(startTime);
      testEvent.setEndDate(endTime);

      setupMockContentManager(Collections.singletonList(testEvent));
      expect(pgScheduledEmailManager.commitToSchedulingEmail("test-event-1@survey96"))
          .andReturn(true).atLeastOnce();

      EventNotificationEmailManager partialMock = EasyMock.partialMockBuilder(EventNotificationEmailManager.class)
          .withConstructor(
              GitContentManager.class,
              EventBookingManager.class,
              UserAccountManager.class,
              EmailManager.class,
              PgScheduledEmailManager.class
          )
          .withArgs(contentManager, eventBookingManager, userAccountManager, emailManager, pgScheduledEmailManager)
          .addMockedMethod("sendBookingStatusFilteredEmailForEvent")
          .createMock();

      partialMock.sendBookingStatusFilteredEmailForEvent(
          anyObject(IsaacEventPageDTO.class),
          eq("event_survey"),
          eq(List.of(BookingStatus.ATTENDED))
      );
      expectLastCall().atLeastOnce();

      replay(contentManager, resultsWrapper, pgScheduledEmailManager, partialMock);

      partialMock.sendFeedbackEmails();

      verify(pgScheduledEmailManager, partialMock);
    }

    @Nested
    @DisplayName("Edge Case Tests")
    class EdgeCaseTests {

      @Test
      @DisplayName("Should skip cancelled events")
      void shouldSkipCancelledEvents() throws Exception {
        testEvent.setEventStatus(EventStatus.CANCELLED);
        testEvent.setDate(Instant.now().minus(25, ChronoUnit.HOURS));

        setupMockContentManager(Collections.singletonList(testEvent));

        replay(contentManager, resultsWrapper, pgScheduledEmailManager);

        eventNotificationEmailManager.sendFeedbackEmails();

        verify(contentManager, resultsWrapper, pgScheduledEmailManager);
      }

      @Test
      @DisplayName("Should NOT send email when no post resources and no survey")
      void shouldNotSendEmailWithoutResourcesOrSurvey() throws Exception {
        testEvent.setPostResources(null);
        testEvent.setEventSurvey(null);
        testEvent.setEventSurveyTitle(null);
        testEvent.setDate(Instant.now().minus(25, ChronoUnit.HOURS));

        setupMockContentManager(Collections.singletonList(testEvent));

        replay(contentManager, resultsWrapper, pgScheduledEmailManager);

        eventNotificationEmailManager.sendFeedbackEmails();

        verify(contentManager, resultsWrapper, pgScheduledEmailManager);
      }

      @Test
      @DisplayName("Should handle multiple events with different timings correctly")
      void shouldHandleMultipleEventsCorrectly() throws Exception {
        IsaacEventPageDTO event24h = TestDataFactory.createTestEvent("event-24h", 25, mockPostResources);
        IsaacEventPageDTO event96h = TestDataFactory.createTestEvent("event-96h", 100, mockPostResources);
        IsaacEventPageDTO eventTooRecent = TestDataFactory.createTestEvent("event-recent", 20, mockPostResources);

        setupMockContentManager(Arrays.asList(event24h, event96h, eventTooRecent));

        expect(pgScheduledEmailManager.commitToSchedulingEmail("event-24h@post"))
            .andReturn(true).atLeastOnce();
        expect(pgScheduledEmailManager.commitToSchedulingEmail("event-96h@survey96"))
            .andReturn(true).atLeastOnce();

        EventNotificationEmailManager partialMock = EasyMock.partialMockBuilder(EventNotificationEmailManager.class)
            .withConstructor(
                GitContentManager.class,
                EventBookingManager.class,
                UserAccountManager.class,
                EmailManager.class,
                PgScheduledEmailManager.class
            )
            .withArgs(contentManager, eventBookingManager, userAccountManager, emailManager, pgScheduledEmailManager)
            .addMockedMethod("sendBookingStatusFilteredEmailForEvent")
            .createMock();

        partialMock.sendBookingStatusFilteredEmailForEvent(
            anyObject(IsaacEventPageDTO.class),
            eq("event_feedback"),
            eq(List.of(BookingStatus.ATTENDED))
        );
        expectLastCall().atLeastOnce();

        partialMock.sendBookingStatusFilteredEmailForEvent(
            anyObject(IsaacEventPageDTO.class),
            eq("event_survey"),
            eq(List.of(BookingStatus.ATTENDED))
        );
        expectLastCall().atLeastOnce();

        replay(contentManager, resultsWrapper, pgScheduledEmailManager, partialMock);

        partialMock.sendFeedbackEmails();

        verify(pgScheduledEmailManager, partialMock);
      }
    }
  }
}