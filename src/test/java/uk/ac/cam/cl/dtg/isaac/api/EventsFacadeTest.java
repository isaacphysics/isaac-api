package uk.ac.cam.cl.dtg.isaac.api;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ATTENDED_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_DATE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_ID_FKEY_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EVENT_TAGS_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SegueServerLogType;
import static uk.ac.cam.cl.dtg.segue.api.Constants.USER_ID_FKEY_FIELDNAME;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingUpdateException;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventIsCancelledException;
import uk.ac.cam.cl.dtg.isaac.dos.EventStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.MainObjectMapper;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

class EventsFacadeTest {

  private EventsFacade eventsFacade;
  private PropertiesLoader properties;
  private ILogManager logManager;
  private EventBookingManager bookingManager;
  private UserAccountManager userManager;
  private GitContentManager contentManager;
  private UserBadgeManager userBadgeManager;
  private UserAssociationManager userAssociationManager;
  private GroupManager groupManager;
  private UserAccountManager userAccountManager;
  private SchoolListReader schoolListReader;
  private MainObjectMapper mapper;
  private HttpServletRequest mockRequest;
  private String eventId;
  private RegisteredUserDTO mockUser;

  @BeforeEach
  void beforeEach() {
    this.properties = createMock(PropertiesLoader.class);
    this.logManager = createMock(ILogManager.class);
    this.bookingManager = createMock(EventBookingManager.class);
    this.userManager = createMock(UserAccountManager.class);
    this.contentManager = createMock(GitContentManager.class);
    this.userBadgeManager = createMock(UserBadgeManager.class);
    this.userAssociationManager = createMock(UserAssociationManager.class);
    this.groupManager = createMock(GroupManager.class);
    this.userAccountManager = createMock(UserAccountManager.class);
    this.schoolListReader = createMock(SchoolListReader.class);
    this.mapper = createMock(MainObjectMapper.class);
    this.eventsFacade =
        new EventsFacade(this.properties, this.logManager, this.bookingManager, this.userManager, this.contentManager,
            this.userBadgeManager, this.userAssociationManager, this.groupManager, this.schoolListReader, this.mapper);
    this.mockRequest = createMock(HttpServletRequest.class);
    this.eventId = "example_event";
    this.mockUser = new RegisteredUserDTO();
    this.mockUser.setId(1234L);
  }

  private void setupExpectations(HttpServletRequest mockRequest, String eventId, IsaacEventPageDTO page,
                                       RegisteredUserDTO mockUser, BookingStatus bookingStatus)
      throws ContentManagerException, NoUserLoggedInException, SegueDatabaseException {
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockUser).times(2);
    expect(contentManager.getContentById(eventId)).andReturn(page);
    expect(mapper.copy(page)).andReturn(page);
    expect(bookingManager.getBookingStatus(page.getId(), mockUser.getId())).andReturn(bookingStatus);
    expect(bookingManager.getPlacesAvailable(page)).andReturn(12);
  }

  public static Stream<Arguments> getEvent_returns_event_with_expected_meeting_url() {
    return Stream.of(
        Arguments.of("http://www.example.com", BookingStatus.CONFIRMED, Instant.now()),
        Arguments.of(null, BookingStatus.CONFIRMED, Instant.now().plus(2, ChronoUnit.DAYS)),
        Arguments.of(null, BookingStatus.CANCELLED, Instant.now().plus(2, ChronoUnit.DAYS)),
        Arguments.of(null, BookingStatus.CANCELLED, Instant.now()),
        Arguments.of(null, null, Instant.now()));
  }

  @ParameterizedTest(name = "[{index}] Meeting URL is {0} if user booking status is {1} and event date is {2}")
  @MethodSource
  void getEvent_returns_event_with_expected_meeting_url(String expectedMeetingUrl, BookingStatus bookingStatus,
                                                        Instant eventDate)
      throws ContentManagerException, NoUserLoggedInException, SegueDatabaseException {

    // Arrange
    IsaacEventPageDTO page = prepareMockEventPage(eventId, eventDate);

    setupExpectations(mockRequest, eventId, page, mockUser, bookingStatus);

    replay(properties, logManager, bookingManager, userManager, contentManager, userBadgeManager,
        userAssociationManager, groupManager, userAccountManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.getEvent(mockRequest, eventId);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals(expectedMeetingUrl, response.readEntity(IsaacEventPageDTO.class).getMeetingUrl());
  }

  @Test
  void recordEventAttendance_noUserIds_ReturnsError() {
    // Arrange
    List<Long> userIds = new ArrayList<>();

    replay(properties, logManager, bookingManager, userManager, contentManager, userBadgeManager,
        userAssociationManager, groupManager, userAccountManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.recordEventAttendance(mockRequest, eventId, userIds, true);

    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
  }

  @Test
  void recordEventAttendance_updatesAttendanceAndReturnsOk() throws NoUserLoggedInException, ContentManagerException,
      SegueDatabaseException, NoUserException, EventIsCancelledException, EventBookingUpdateException {
    // Arrange
    IsaacEventPageDTO page = prepareMockEventPage(eventId, Instant.now().minus(2, ChronoUnit.DAYS));
    List<Long> userIds = List.of(155L, 156L, 157L);
    DetailedEventBookingDTO booking = new DetailedEventBookingDTO();
    BookingStatus bookingStatus = BookingStatus.CONFIRMED;
    booking.setBookingStatus(bookingStatus);

    setupExpectations(mockRequest, eventId, page, mockUser, bookingStatus);
    expect(bookingManager.isUserAbleToManageEvent(mockUser, page)).andReturn(true);

    for (Long id : userIds) {
      RegisteredUserDTO userOfInterest = new RegisteredUserDTO();
      userOfInterest.setId(id);
      expect(userManager.getUserDTOById(id)).andReturn(userOfInterest);
      expect(bookingManager.recordAttendance(page, userOfInterest, true)).andReturn(booking);
      logManager.logEvent(mockUser, mockRequest,
          SegueServerLogType.ADMIN_EVENT_ATTENDANCE_RECORDED, Map.of(
              EVENT_ID_FKEY_FIELDNAME, eventId,
              USER_ID_FKEY_FIELDNAME, id,
              ATTENDED_FIELDNAME, true,
              EVENT_DATE_FIELDNAME, page.getDate(),
              EVENT_TAGS_FIELDNAME, page.getTags()
          ));
      expectLastCall();
    }

    replay(properties, logManager, bookingManager, userManager, contentManager, userBadgeManager,
        userAssociationManager, groupManager, userAccountManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.recordEventAttendance(mockRequest, eventId, userIds, true);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

  }

  @Test
  void recordEventAttendance_returnsIncorrectRoleIfCantManageEvents() throws NoUserLoggedInException,
      ContentManagerException, SegueDatabaseException {
    // Arrange
    IsaacEventPageDTO page = prepareMockEventPage(eventId, Instant.now().minus(2, ChronoUnit.DAYS));
    List<Long> userIds = List.of(155L, 156L, 157L);
    DetailedEventBookingDTO booking = new DetailedEventBookingDTO();
    BookingStatus bookingStatus = BookingStatus.CONFIRMED;
    booking.setBookingStatus(bookingStatus);

    setupExpectations(mockRequest, eventId, page, mockUser, bookingStatus);
    expect(bookingManager.isUserAbleToManageEvent(mockUser, page)).andReturn(false);


    replay(properties, logManager, bookingManager, userManager, contentManager, userBadgeManager,
        userAssociationManager, groupManager, userAccountManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.recordEventAttendance(mockRequest, eventId, userIds, true);

    // Assert
    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

  }

  @Test
  void recordEventAttendance_returnsBadRequestIfUsersCannotBeFound()
      throws NoUserLoggedInException, ContentManagerException,
      SegueDatabaseException, NoUserException, EventIsCancelledException, EventBookingUpdateException {
    // Arrange
    IsaacEventPageDTO page = prepareMockEventPage(eventId, Instant.now().minus(2, ChronoUnit.DAYS));
    List<Long> userIds = List.of(155L, 156L, 157L);
    DetailedEventBookingDTO booking = new DetailedEventBookingDTO();
    BookingStatus bookingStatus = BookingStatus.CONFIRMED;
    booking.setBookingStatus(bookingStatus);

    setupExpectations(mockRequest, eventId, page, mockUser, bookingStatus);
    expect(bookingManager.isUserAbleToManageEvent(mockUser, page)).andReturn(true);

    for (Long id : userIds) {
      RegisteredUserDTO userOfInterest = new RegisteredUserDTO();
      userOfInterest.setId(id);
      expect(userManager.getUserDTOById(id)).andThrow(new NoUserException("No user found"));
    }

    replay(properties, logManager, bookingManager, userManager, contentManager, userBadgeManager,
        userAssociationManager, groupManager, userAccountManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.recordEventAttendance(mockRequest, eventId, userIds, true);

    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals("One or more bookings could not be updated: [155, 156, 157]", response.readEntity(
        SegueErrorResponse.class).getErrorMessage());

  }

  @Test
  void recordEventAttendance_doesNotUpdateBookingsIfEventCancelled()
      throws NoUserLoggedInException, ContentManagerException,
      SegueDatabaseException, NoUserException, EventIsCancelledException, EventBookingUpdateException {
    // Arrange
    IsaacEventPageDTO page = prepareMockEventPage(eventId, Instant.now().minus(2, ChronoUnit.DAYS));
    page.setEventStatus(EventStatus.CANCELLED);
    List<Long> userIds = List.of(155L, 156L, 157L);
    DetailedEventBookingDTO booking = new DetailedEventBookingDTO();
    BookingStatus bookingStatus = BookingStatus.CONFIRMED;
    booking.setBookingStatus(bookingStatus);

    setupExpectations(mockRequest, eventId, page, mockUser, bookingStatus);
    expect(bookingManager.isUserAbleToManageEvent(mockUser, page)).andReturn(true);


    for (Long id : userIds) {
      RegisteredUserDTO userOfInterest = new RegisteredUserDTO();
      userOfInterest.setId(id);
      expect(userManager.getUserDTOById(id)).andReturn(userOfInterest);
      expect(bookingManager.recordAttendance(page, userOfInterest, true)).andThrow(
          new EventIsCancelledException(String.format("Unable to record user (%s) attendance for event (%s); the "
          + "event is cancelled.", id, eventId)));
    }

    replay(properties, logManager, bookingManager, userManager, contentManager, userBadgeManager,
        userAssociationManager, groupManager, userAccountManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.recordEventAttendance(mockRequest, eventId, userIds, true);

    // Assert
    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals("The event is cancelled: event attendance cannot be recorded.", response.readEntity(
        SegueErrorResponse.class).getErrorMessage());

  }

  private static @NotNull IsaacEventPageDTO prepareMockEventPage(String eventId, Instant date) {
    IsaacEventPageDTO page = new IsaacEventPageDTO();
    page.setDate(date);
    page.setTitle("Example Event");
    page.setMeetingUrl("http://www.example.com");
    page.setId(eventId);
    page.setType("isaacEventPage");
    return page;
  }
}