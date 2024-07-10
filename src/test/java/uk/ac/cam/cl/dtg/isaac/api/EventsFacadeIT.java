package uk.ac.cam.cl.dtg.isaac.api;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus.CANCELLED;
import static uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus.CONFIRMED;
import static uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus.WAITING_LIST;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.BookingStatus;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBooking;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.EventBookings;
import uk.ac.cam.cl.dtg.isaac.dos.eventbookings.PgEventBookings;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressAndGenderDTO;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

class EventsFacadeIT extends IsaacIntegrationTest {

  private static final String BOOKING_CANCELLATION_TEST_EVENT_ID = "dc8686cf-be3b-4c0d-8761-1e5504146867";

  private EventsFacade eventsFacade;
  private EventBookings bookingDatabase;

  @BeforeEach
  public void setUp() {
    // Get an instance of the facade to test
    eventsFacade =
        new EventsFacade(properties, logManager, eventBookingManager, userAccountManager, contentManager,
            userBadgeManager, userAssociationManager, groupManager, schoolListReader,
            mainObjectMapper);
    bookingDatabase = new PgEventBookings(postgresSqlDb, new ObjectMapper());
  }

  @Test
  // GET /events -> EventFacade::getEvents(request, tags, startIndex, limit, sortOrder, showActiveOnly, showInactiveOnly, showMyBookingsOnly, showReservationsOnly, showStageOnly)
  void getEventsTest() {
    // Create an anonymous request (this is a mocked object)
    HttpServletRequest request = createRequestWithCookies(new Cookie[] {});
    replay(request);

    // Execute the method (endpoint) to be tested
    Response response = eventsFacade.getEvents(request, null, 0, 10, null, null, null, null, null, null);
    // Check that the request succeeded
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    // Fetch the entity object. This can be anything, so we declare it first as Object
    Object entityObject = response.getEntity();
    // Check that the entity object is not null.
    assertNotNull(entityObject);
    // We could/should also check its type, but we know what type it should be, so we can just (un)safely cast it
    @SuppressWarnings("unchecked") ResultsWrapper<IsaacEventPageDTO> entity =
        (ResultsWrapper<IsaacEventPageDTO>) entityObject;
    // Check this again just in case the cast fails or something
    assertNotNull(entity);
    List<IsaacEventPageDTO> results = entity.getResults();
    // Check that we retrieved the expected amount of results
    assertEquals(8, results.size());
    // NOTE: We may end up having more events in the dataset than the limit specified in the call.
    //       In this case, we need to check for the limit up here and then check if the response object tells us
    //       that there are more, and how many there are.
    Long numberOfPublicEventsInResults = results.stream().filter(event -> event.isPrivateEvent() == null || !event.isPrivateEvent()).count();
    Long numberOfPrivateEventsInResults = results.stream().filter(event -> event.isPrivateEvent() != null && event.isPrivateEvent()).count();
    assertEquals(8, numberOfPublicEventsInResults);
    assertEquals(0, numberOfPrivateEventsInResults);
  }

  @Test
  // GET    /events/bookings/{booking_id}      -> EventsFacade::getEventBookingsById(request, bookingId)
  // POST   /events/{event_id}/booking         -> EventsFacade::createBookingForMe(request, eventId, additionalInformation)
  // DELETE /events/{event_id}/bookings/cancel -> EventsFacade::cancelBooking(request, eventId)
  void getBookingByIdTest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException, SQLException {
    // --- Login as a student
    LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
    // --- Login as an event manager
    LoginResult eventManagerLogin =
        loginAs(httpSession, ITConstants.TEST_EVENTMANAGER_EMAIL, ITConstants.TEST_EVENTMANAGER_PASSWORD);

    // --- Create a booking as a logged in student
    HttpServletRequest createBookingRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
    replay(createBookingRequest);
    Response createBookingResponse = eventsFacade.createBookingForMe(createBookingRequest, "_regular_test_event", null);
    // Check that the booking was created successfully
    assertEquals(Response.Status.OK.getStatusCode(), createBookingResponse.getStatus());
    EventBookingDTO eventBookingDTO = null;
    assertNotNull(createBookingResponse.getEntity());
    if (createBookingResponse.getEntity() instanceof EventBookingDTO) {
      eventBookingDTO = (EventBookingDTO) createBookingResponse.getEntity();
      // Check that the returned entity is an EventBookingDTO and the ID of the user who created the booking matches
      assertEquals(studentLogin.user.getId(), eventBookingDTO.getUserBooked().getId());
    } else {
      fail("The returned entity is not an instance of EventBookingDTO.");
    }
    // This is probably redundant at this point, but we use this object later so might as well.
    assertNotNull(eventBookingDTO);

    // --- Check whether what we get as event managers contains the right amount of information
    HttpServletRequest getEventBookingsByIdRequest = createRequestWithCookies(new Cookie[] {eventManagerLogin.cookie});
    replay(getEventBookingsByIdRequest);

    Response getEventBookingsByIdResponse =
        eventsFacade.getEventBookingsById(getEventBookingsByIdRequest, eventBookingDTO.getBookingId().toString());
    assertNotNull(getEventBookingsByIdResponse.getEntity());
    assertEquals(DetailedEventBookingDTO.class.getCanonicalName(),
        getEventBookingsByIdResponse.getEntity().getClass().getCanonicalName());
    assertNotNull(((DetailedEventBookingDTO) getEventBookingsByIdResponse.getEntity()).getUserBooked());
    assertEquals(UserSummaryWithEmailAddressAndGenderDTO.class.getCanonicalName(),
        ((EventBookingDTO) getEventBookingsByIdResponse.getEntity()).getUserBooked().getClass().getCanonicalName());

    // --- Delete the booking created above otherwise the other tests may be affected.
    HttpServletRequest cancelBookingRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
    replay(cancelBookingRequest);
    Response cancelBookingResponse = eventsFacade.cancelBooking(cancelBookingRequest, "_regular_test_event");
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cancelBookingResponse.getStatus());

    // --- Tear down
    // BEWARE: Because we don't actually remove the cancelled reservation records from the database, this would
    //         leave lingering state that may lead to unexpected behaviour in other test cases (e.g., wrong counts).
    try (PreparedStatement pst = postgresSqlDb.getDatabaseConnection().prepareStatement(
        "DELETE FROM event_bookings WHERE id = ?;")) {
      pst.setLong(1, ((EventBookingDTO) createBookingResponse.getEntity()).getBookingId());
      pst.executeUpdate();
    }
  }

  // events/{event_id}/bookings
  @Test
  // GET /events/{event_id}/bookings -> EventsFacade::adminGetEventBookingByEventId(request, eventId)
  void getEventBookingsByEventIdTest()
      throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException {
    // Get event bookings by event id as an anonymous user (should fail)
    HttpServletRequest getEventBookingsAsAnonymousRequest = createRequestWithCookies(new Cookie[] {});
    Response getEventBookingsAsAnonymousResponse =
        eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsAnonymousRequest, "_regular_test_event");
    assertNotEquals(Response.Status.OK.getStatusCode(), getEventBookingsAsAnonymousResponse.getStatus());

    // Get event bookings by event id as a student (should fail)
    LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
    HttpServletRequest getEventBookingsAsStudentRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
    replay(getEventBookingsAsStudentRequest);
    Response getEventBookingsAsStudentResponse =
        eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsStudentRequest, "_regular_test_event");
    assertNotEquals(Response.Status.OK.getStatusCode(), getEventBookingsAsStudentResponse.getStatus());

    // Get event bookings by event id as a teacher (should fail)
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL, ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest getEventBookingsAsTeacherRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(getEventBookingsAsTeacherRequest);
    Response getEventBookingsAsTeacherResponse =
        eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsTeacherRequest, "_regular_test_event");
    assertNotEquals(Response.Status.OK.getStatusCode(), getEventBookingsAsTeacherResponse.getStatus());

    // Get event bookings by event id as a teacher (should fail)
    LoginResult editorLogin = loginAs(httpSession, ITConstants.TEST_EDITOR_EMAIL, ITConstants.TEST_EDITOR_PASSWORD);
    HttpServletRequest getEventBookingsAsEditorRequest = createRequestWithCookies(new Cookie[] {editorLogin.cookie});
    replay(getEventBookingsAsEditorRequest);
    Response getEventBookingsAsEditorResponse =
        eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsEditorRequest, "_regular_test_event");
    assertNotEquals(Response.Status.OK.getStatusCode(), getEventBookingsAsEditorResponse.getStatus());

    // Get event bookings by event id as an event manager (should succeed)
    LoginResult eventManagerLogin =
        loginAs(httpSession, ITConstants.TEST_EVENTMANAGER_EMAIL, ITConstants.TEST_EVENTMANAGER_PASSWORD);
    HttpServletRequest getEventBookingsAsEventManagerRequest =
        createRequestWithCookies(new Cookie[] {eventManagerLogin.cookie});
    replay(getEventBookingsAsEventManagerRequest);
    Response getEventBookingsAsEventManagerResponse =
        eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsEventManagerRequest, "_regular_test_event");
    assertEquals(Response.Status.OK.getStatusCode(), getEventBookingsAsEventManagerResponse.getStatus());
    assertNotNull(getEventBookingsAsEventManagerResponse.getEntity());
    assertTrue(getEventBookingsAsEventManagerResponse.getEntity() instanceof List);
    List<?> entity = (List<?>) getEventBookingsAsEventManagerResponse.getEntity();
    assertEquals(3, entity.size());
    for (Object o : entity) {
      assertEquals(DetailedEventBookingDTO.class.getCanonicalName(), o.getClass().getCanonicalName());
    }

    // Get event bookings by event id as an admin (should succeed)
    // NOTE: I was going to test as an admin too (same code as for Event Managers) but logging in with MFA is a
    //       nightmare I'm not prepared to face yet. Plus, if we have people who obtained the ADMIN role, we have
    //       bigger problems anyway.
  }

  @Test
  // GET /events/{event_id}/groups_bookings
  void getEventBookingForAllGroupsTest()
      throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException {
    // Get event bookings by event id as an anonymous user (should fail)
    HttpServletRequest anonymousRequest = createRequestWithCookies(new Cookie[] {});
    replay(anonymousRequest);
    Response anonymousResponse = eventsFacade.getEventBookingForAllGroups(anonymousRequest, "_regular_test_event");
    assertNotEquals(Response.Status.OK.getStatusCode(), anonymousResponse.getStatus());

    // Get event bookings by event id as a student (should fail)
    LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
    HttpServletRequest studentRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
    replay(studentRequest);
    Response studentResponse = eventsFacade.getEventBookingForAllGroups(studentRequest, "_regular_test_event");
    assertNotEquals(Response.Status.OK.getStatusCode(), studentResponse.getStatus());

    // Get event bookings by event id as a teacher (should succeed)
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL, ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest teacherRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(teacherRequest);
    Response teacherResponse = eventsFacade.getEventBookingForAllGroups(teacherRequest, "_regular_test_event");
    assertEquals(Response.Status.OK.getStatusCode(), teacherResponse.getStatus());
    assertNotNull(teacherResponse.getEntity());

    // Make sure the EventBookingDTOs contain UserSummaryDTOs, thus not leaking information
    assertTrue(teacherResponse.getEntity() instanceof List);
    List<?> teacherEntity = (List<?>) teacherResponse.getEntity();
    for (Object o : teacherEntity) {
      assertEquals(EventBookingDTO.class.getCanonicalName(), o.getClass().getCanonicalName());
      assertEquals(UserSummaryDTO.class.getCanonicalName(),
          ((EventBookingDTO) o).getUserBooked().getClass().getCanonicalName());
    }
    Optional<UserSummaryDTO> teacherAlice = (Optional<UserSummaryDTO>) teacherEntity.stream()
        .filter(e -> ((EventBookingDTO) e).getUserBooked().getId() == 7).findFirst();
    // Alice is associated with Teacher and is booked for this event => Alice should be present
    assertTrue(teacherAlice.isPresent());
    Optional<UserSummaryDTO> teacherCharlie = (Optional<UserSummaryDTO>) teacherEntity.stream()
        .filter(e -> ((EventBookingDTO) e).getUserBooked().getId() == 9).findFirst();
    // Charlie is not associated with Teacher and is not booked for this event => Charlie should not be present
    assertFalse(teacherCharlie.isPresent());

    // Try logging in with another teacher account and see if we are sending the wrong information to the wrong teachers
    LoginResult daveLogin = loginAs(httpSession, "dave-teacher@test.com", ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest daveRequest = createRequestWithCookies(new Cookie[] {daveLogin.cookie});
    replay(daveRequest);
    Response daveResponse = eventsFacade.getEventBookingForAllGroups(daveRequest, "_regular_test_event");
    assertEquals(Response.Status.OK.getStatusCode(), daveResponse.getStatus());
    assertNotNull(daveResponse.getEntity());
    // Make sure the EventBookingDTOs contain UserSummaryDTOs, thus not leaking information
    assertTrue(
        daveResponse.getEntity() instanceof List); // instanceof is OK here because we just need to know this is a subclass of a List
    List<?> daveEntity = (List<?>) daveResponse.getEntity();
    for (Object o : daveEntity) {
      assertEquals(EventBookingDTO.class.getCanonicalName(), o.getClass().getCanonicalName());
      assertEquals(UserSummaryDTO.class.getCanonicalName(),
          ((EventBookingDTO) o).getUserBooked().getClass().getCanonicalName());
    }
    Optional<UserSummaryDTO> daveAlice =
        (Optional<UserSummaryDTO>) daveEntity.stream().filter(e -> ((EventBookingDTO) e).getUserBooked().getId() == 7)
            .findFirst();
    // Alice is not associated with Dave but is booked for this event => Alice should not be present
    assertFalse(daveAlice.isPresent());
    Optional<UserSummaryDTO> daveCharlie =
        (Optional<UserSummaryDTO>) daveEntity.stream().filter(e -> ((EventBookingDTO) e).getUserBooked().getId() == 9)
            .findFirst();
    // Charlie is associated with Dave and is not booked for this event => Charlie should not be present
    assertFalse(daveCharlie.isPresent());
  }

  @Test
  // GET /events/{event_id}/bookings/for_group/{group_id} -> EventFacade::getEventBookingForGivenGroup(request, eventId, groupId)
  void getEventBookingsByEventIdForGroup()
      throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException {
    // Anonymous users MUST NOT be able to get event bookings by event id and group id
    HttpServletRequest anonymousRequest = createNiceMock(HttpServletRequest.class);
    replay(anonymousRequest);
    Response anonymousResponse =
        eventsFacade.getEventBookingForGivenGroup(anonymousRequest, "_regular_test_event", "1");
    assertNotEquals(Response.Status.OK.getStatusCode(), anonymousResponse.getStatus());

    // Teachers MUST be able to get event bookings by event id and group id if and only if they own the given group
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL, ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest teacherRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(teacherRequest);
    Response teacherResponse = eventsFacade.getEventBookingForGivenGroup(teacherRequest, "_regular_test_event", "1");
    assertEquals(Response.Status.OK.getStatusCode(), teacherResponse.getStatus());
    List<?> teacherEntity = (List<?>) teacherResponse.getEntity();
    List<Long> bookedUserIds =
        teacherEntity.stream().map(booking -> ((EventBookingDTO) booking).getUserBooked().getId())
            .collect(Collectors.toList());
    assertTrue(bookedUserIds.containsAll(Arrays.asList(7L, 8L)));
    assertFalse(bookedUserIds.contains(9L)); // User 9 is booked but is not in Teacher's group.

    // Students MUST NOT be able to get event bookings by event id and group id
    LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
    HttpServletRequest studentRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
    replay(studentRequest);
    Response studentResponse = eventsFacade.getEventBookingForGivenGroup(studentRequest, "_regular_test_event", "2");
    // The student does not own the group so this should not succeed
    assertNotEquals(Response.Status.OK.getStatusCode(), studentResponse.getStatus());

    // A student MUST NOT be able to get event bookings by event id and group id EVEN IF they belong in the group
    // Alice is part of group id 1
    LoginResult aliceLogin = loginAs(httpSession, "alice-student@test.com", ITConstants.TEST_STUDENT_PASSWORD);
    HttpServletRequest aliceRequest = createRequestWithCookies(new Cookie[] {aliceLogin.cookie});
    replay(aliceRequest);
    Response aliceResponse = eventsFacade.getEventBookingForGivenGroup(aliceRequest, "_regular_test_event", "1");
    // The student does not own the group so this should not succeed
    assertNotEquals(Response.Status.OK.getStatusCode(), aliceResponse.getStatus());

    LoginResult eventManagerLogin =
        loginAs(httpSession, ITConstants.TEST_EVENTMANAGER_EMAIL, ITConstants.TEST_EVENTMANAGER_PASSWORD);
    HttpServletRequest eventManagerRequest = createRequestWithCookies(new Cookie[] {eventManagerLogin.cookie});
    replay(eventManagerRequest);
    Response eventManagerResponse =
        eventsFacade.getEventBookingForGivenGroup(eventManagerRequest, "_regular_test_event", "2");
    // The event manager does not own the group so this should not succeed
    assertNotEquals(Response.Status.OK.getStatusCode(), eventManagerResponse.getStatus());
  }

  @Test
  void getEventOverviewsTest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException {
    LoginResult eventManagerLogin = loginAs(httpSession, ITConstants.TEST_EVENTMANAGER_EMAIL, ITConstants.TEST_EVENTMANAGER_PASSWORD);

    HttpServletRequest mockRequest = createRequestWithCookies(new Cookie[] {eventManagerLogin.cookie});
    replay(mockRequest);

    Response response = eventsFacade.getEventOverviews(mockRequest, 0, 10, null);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    Object entityObject = response.getEntity();
    assertNotNull(entityObject);
    @SuppressWarnings("unchecked") ResultsWrapper<ImmutableMap<String, Object>> entity = (ResultsWrapper<ImmutableMap<String, Object>>) entityObject;
    assertNotNull(entity);
    List<ImmutableMap<String, Object>> results = entity.getResults();
    assertEquals(9, results.size());
    Long numberOfPublicEventsInResults = results.stream().filter(overview -> !(Boolean) overview.get("privateEvent")).count();
    Long numberOfPrivateEventsInResults = results.stream().filter(overview -> (Boolean) overview.get("privateEvent")).count();
    assertEquals(8, numberOfPublicEventsInResults);
    assertEquals(1, numberOfPrivateEventsInResults);
  }

  @Nested
  class CancelBooking {
    @Test
    void cancelBookingAndPromoteWaitingListTest()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException, SQLException {
      LoginResult studentLogin =
          loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
      HttpServletRequest cancelBookingRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(cancelBookingRequest);
      try (Response cancelBookingResponse = eventsFacade.cancelBooking(cancelBookingRequest,
          BOOKING_CANCELLATION_TEST_EVENT_ID)) {
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cancelBookingResponse.getStatus());
      }

      assertUpdatedBookingsMatchExpectations();

      resetCancellationTestDatabaseEntries();
    }

    @Test
    void cancelOtherUserBookingAsManager()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException, SQLException {
      LoginResult managerLogin =
          loginAs(httpSession, ITConstants.TEST_EVENTMANAGER_EMAIL, ITConstants.TEST_EVENTMANAGER_PASSWORD);
      HttpServletRequest cancelBookingRequest = createRequestWithCookies(new Cookie[] {managerLogin.cookie});
      replay(cancelBookingRequest);
      try (Response cancelBookingResponse = eventsFacade.cancelBooking(cancelBookingRequest,
          BOOKING_CANCELLATION_TEST_EVENT_ID, 6L)) {
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cancelBookingResponse.getStatus());
      }

      assertUpdatedBookingsMatchExpectations();

      resetCancellationTestDatabaseEntries();
    }

    @Test
    void cancelBookingWithoutLoginReturnsError() {
      HttpServletRequest cancelBookingRequest = createRequestWithCookies(new Cookie[] {});
      try (Response cancelBookingResponse = eventsFacade.cancelBooking(cancelBookingRequest,
          BOOKING_CANCELLATION_TEST_EVENT_ID, 6L)) {
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cancelBookingResponse.getStatus());
        assertEquals("You must be logged in to access this resource.",
            cancelBookingResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Test
    void cancelOtherUsersBookingWithoutPermissionsReturnsError()
        throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
        AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
        AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
        MFARequiredButNotConfiguredException {
      LoginResult studentLogin =
          loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
      HttpServletRequest cancelBookingRequest = createRequestWithCookies(new Cookie[] {studentLogin.cookie});
      replay(cancelBookingRequest);
      try (Response cancelBookingResponse = eventsFacade.cancelBooking(cancelBookingRequest,
          BOOKING_CANCELLATION_TEST_EVENT_ID, 7L)) {
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelBookingResponse.getStatus());
        assertEquals("You do not have the permissions to complete this action",
            cancelBookingResponse.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    private void assertUpdatedBookingsMatchExpectations() throws SegueDatabaseException {
      List<EventBooking> bookingsFromDatabase =
          (List<EventBooking>) bookingDatabase.findAllByEventIdAndStatus(BOOKING_CANCELLATION_TEST_EVENT_ID, null);
      Map<Long, BookingStatus> bookingMap = bookingsFromDatabase.stream()
          .collect(Collectors.toMap(EventBooking::getUserId, EventBooking::getBookingStatus));
      assertEquals(4, bookingMap.size());
      assertEquals(CANCELLED, bookingMap.get(6L));
      assertEquals(CONFIRMED, bookingMap.get(7L));
      assertEquals(WAITING_LIST, bookingMap.get(8L));
      assertEquals(WAITING_LIST, bookingMap.get(11L));
    }

    private void resetCancellationTestDatabaseEntries() throws SQLException {
      try (PreparedStatement pst = postgresSqlDb.getDatabaseConnection().prepareStatement(
          "UPDATE event_bookings SET status = ? WHERE event_id = ? AND user_id = ?;")) {
        pst.setString(1, CONFIRMED.name());
        pst.setString(2, BOOKING_CANCELLATION_TEST_EVENT_ID);
        pst.setLong(3, 6L);
        pst.executeUpdate();
      }

      try (PreparedStatement pst = postgresSqlDb.getDatabaseConnection().prepareStatement(
          "UPDATE event_bookings SET status = ? WHERE event_id = ? AND user_id = ?;")) {
        pst.setString(1, WAITING_LIST.name());
        pst.setString(2, BOOKING_CANCELLATION_TEST_EVENT_ID);
        pst.setLong(3, 7L);
        pst.executeUpdate();
      }
    }
  }
}
