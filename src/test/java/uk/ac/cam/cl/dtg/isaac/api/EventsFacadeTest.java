package uk.ac.cam.cl.dtg.isaac.api;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.CustomAssertions.assertDeepEquals;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.MainObjectMapper;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
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
            this.userBadgeManager, this.userAssociationManager, this.groupManager,
            this.userAccountManager, this.schoolListReader, this.mapper);
  }

  public static Stream<Arguments> getEvent_returns_event_with_expected_meeting_url() {
    return Stream.of(
        Arguments.of("http://www.example.com", BookingStatus.CONFIRMED, Instant.now()),
        Arguments.of(null, BookingStatus.CONFIRMED, Instant.now().plus(2, ChronoUnit.DAYS)),
        Arguments.of(null, BookingStatus.CANCELLED, Instant.now().plus(2, ChronoUnit.DAYS)),
        Arguments.of(null, BookingStatus.CANCELLED, Instant.now()),
        Arguments.of(null, null, Instant.now()));
  }

  @ParameterizedTest (name = "[{index}] Meeting URL is {0} if user booking status is {1} and event date is {2}")
  @MethodSource
  void getEvent_returns_event_with_expected_meeting_url(String expectedMeetingUrl, BookingStatus bookingStatus,
                                                        Instant eventDate)
      throws ContentManagerException, NoUserLoggedInException, SegueDatabaseException {

    // Arrange
    HttpServletRequest mockRequest = createMock(HttpServletRequest.class);
    String eventId = "example_event";
    IsaacEventPageDTO page = prepareMockEventPage(eventId, eventDate);
    RegisteredUserDTO mockUser = new RegisteredUserDTO();
    mockUser.setId(1234L);

    expect(contentManager.getContentById(eventId)).andReturn(page);
    expect(mapper.copy(page)).andReturn(page);
    expect(userManager.getCurrentRegisteredUser(mockRequest)).andReturn(mockUser);
    expect(bookingManager.getBookingStatus(page.getId(), mockUser.getId())).andReturn(bookingStatus);
    expect(bookingManager.getPlacesAvailable(page)).andReturn(12);

    replay(properties, logManager, bookingManager, userManager, contentManager, userBadgeManager,
        userAssociationManager, groupManager, userAccountManager, schoolListReader, mapper);

    // Act
    Response response = eventsFacade.getEvent(mockRequest, eventId);

    // Assert
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    assertEquals(expectedMeetingUrl, response.readEntity(IsaacEventPageDTO.class).getMeetingUrl());
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