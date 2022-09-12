package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.assertEquals;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EventsFacadeIT extends IsaacIntegrationTest {

    private EventsFacade eventsFacade;

    @Before
    public void setUp() {
        // Get an instance of the facade to test
        eventsFacade = new EventsFacade(properties, logManager, eventBookingManager, userAccountManager, contentManager, "latest", userBadgeManager, userAssociationManager, groupManager, userAccountManager, schoolListReader, mapperFacade);
    }

    @Test
    // GET /events -> EventFacade::getEvents(request, tags, startIndex, limit, sortOrder, showActiveOnly, showInactiveOnly, showMyBookingsOnly, showReservationsOnly, showStageOnly)
    public void getEventsTest() {
        // Create an anonymous request (this is a mocked object)
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{});
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
        @SuppressWarnings("unchecked") ResultsWrapper<IsaacEventPageDTO> entity = (ResultsWrapper<IsaacEventPageDTO>) entityObject;
        // Check this again just in case the cast fails or something
        assertNotNull(entity);
        List<IsaacEventPageDTO> results = entity.getResults();
        // Check that we retrieved the expected amount of results
        assertEquals(8, results.size());
        // NOTE: We may end up having more events in the dataset than the limit specified in the call.
        //       In this case, we need to check for the limit up here and then check if the response object tells us
        //       that there are more, and how many there are.
    }

    @Test
    // GET    /events/bookings/{booking_id}      -> EventsFacade::getEventBookingsById(request, bookingId)
    // POST   /events/{event_id}/booking         -> EventsFacade::createBookingForMe(request, eventId, additionalInformation)
    // DELETE /events/{event_id}/bookings/cancel -> EventsFacade::cancelBooking(request, eventId)
    public void getBookingByIdTest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException, AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException, MFARequiredButNotConfiguredException, SQLException {
        // --- Login as a student
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
        // --- Login as an event manager
        LoginResult eventManagerLogin = loginAs(httpSession, ITConstants.TEST_EVENTMANAGER_EMAIL, ITConstants.TEST_EVENTMANAGER_PASSWORD);

        // --- Create a booking as a logged in student
        HttpServletRequest createBookingRequest = createRequestWithCookies(new Cookie[] { studentLogin.cookie });
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
        HttpServletRequest getEventBookingsByIdRequest = createRequestWithCookies(new Cookie[] { eventManagerLogin.cookie });
        replay(getEventBookingsByIdRequest);

        Response getEventBookingsByIdResponse = eventsFacade.getEventBookingsById(getEventBookingsByIdRequest, eventBookingDTO.getBookingId().toString());
        assertNotNull(getEventBookingsByIdResponse.getEntity());
        assertEquals(DetailedEventBookingDTO.class.getCanonicalName(), getEventBookingsByIdResponse.getEntity().getClass().getCanonicalName());
        assertNotNull(((DetailedEventBookingDTO) getEventBookingsByIdResponse.getEntity()).getUserBooked());
        assertEquals(UserSummaryWithEmailAddressDTO.class.getCanonicalName(), ((EventBookingDTO) getEventBookingsByIdResponse.getEntity()).getUserBooked().getClass().getCanonicalName());

        // --- Delete the booking created above otherwise the other tests may be affected.
        HttpServletRequest cancelBookingRequest = createRequestWithCookies(new Cookie[] { studentLogin.cookie });
        replay(cancelBookingRequest);
        Response cancelBookingResponse = eventsFacade.cancelBooking(cancelBookingRequest, "_regular_test_event");
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cancelBookingResponse.getStatus());

        // --- Tear down
        // BEWARE: Because we don't actually remove the cancelled reservation records from the database, this would
        //         leave lingering state that may lead to unexpected behaviour in other test cases (e.g., wrong counts).
        PreparedStatement pst = postgresSqlDb.getDatabaseConnection().prepareStatement("DELETE FROM event_bookings WHERE id = ?;");
        pst.setLong(1, ((EventBookingDTO) createBookingResponse.getEntity()).getBookingId());
        pst.executeUpdate();
    }

    // events/{event_id}/bookings
    @Test
    // GET /events/{event_id}/bookings -> EventsFacade::adminGetEventBookingByEventId(request, eventId)
    public void getEventBookingsByEventIdTest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException, AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
        // Get event bookings by event id as an anonymous user (should fail)
        HttpServletRequest getEventBookingsAsAnonymous_Request = createRequestWithCookies(new Cookie[]{});
        Response getEventBookingsAsAnonymous_Response = eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsAnonymous_Request, "_regular_test_event");
        assertNotEquals(Response.Status.OK.getStatusCode(), getEventBookingsAsAnonymous_Response.getStatus());

        // Get event bookings by event id as a student (should fail)
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest getEventBookingsAsStudent_Request = createRequestWithCookies(new Cookie[] { studentLogin.cookie });
        replay(getEventBookingsAsStudent_Request);
        Response getEventBookingsAsStudent_Response = eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsStudent_Request, "_regular_test_event");
        assertNotEquals(Response.Status.OK.getStatusCode(), getEventBookingsAsStudent_Response.getStatus());

        // Get event bookings by event id as a teacher (should fail)
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL, ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest getEventBookingsAsTeacher_Request = createRequestWithCookies(new Cookie[] { teacherLogin.cookie });
        replay(getEventBookingsAsTeacher_Request);
        Response getEventBookingsAsTeacher_Response = eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsTeacher_Request, "_regular_test_event");
        assertNotEquals(Response.Status.OK.getStatusCode(), getEventBookingsAsTeacher_Response.getStatus());

        // Get event bookings by event id as a teacher (should fail)
        LoginResult editorLogin = loginAs(httpSession, ITConstants.TEST_EDITOR_EMAIL, ITConstants.TEST_EDITOR_PASSWORD);
        HttpServletRequest getEventBookingsAsEditor_Request = createRequestWithCookies(new Cookie[] { editorLogin.cookie });
        replay(getEventBookingsAsEditor_Request);
        Response getEventBookingsAsEditor_Response = eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsEditor_Request, "_regular_test_event");
        assertNotEquals(Response.Status.OK.getStatusCode(), getEventBookingsAsEditor_Response.getStatus());

        // Get event bookings by event id as an event manager (should succeed)
        LoginResult eventManagerLogin = loginAs(httpSession, ITConstants.TEST_EVENTMANAGER_EMAIL, ITConstants.TEST_EVENTMANAGER_PASSWORD);
        HttpServletRequest getEventBookingsAsEventManager_Request = createRequestWithCookies(new Cookie[] { eventManagerLogin.cookie });
        replay(getEventBookingsAsEventManager_Request);
        Response getEventBookingsAsEventManager_Response = eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsEventManager_Request, "_regular_test_event");
        assertEquals(Response.Status.OK.getStatusCode(), getEventBookingsAsEventManager_Response.getStatus());
        assertNotNull(getEventBookingsAsEventManager_Response.getEntity());
        assertTrue(getEventBookingsAsEventManager_Response.getEntity() instanceof List);
        List<?> entity = (List<?>) getEventBookingsAsEventManager_Response.getEntity();
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
    public void getEventBookingForAllGroupsTest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException, AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
        // Get event bookings by event id as an anonymous user (should fail)
        HttpServletRequest anonymous_Request = createRequestWithCookies(new Cookie[] {});
        replay(anonymous_Request);
        Response anonymous_Response = eventsFacade.getEventBookingForAllGroups(anonymous_Request, "_regular_test_event");
        assertNotEquals(Response.Status.OK.getStatusCode(), anonymous_Response.getStatus());

        // Get event bookings by event id as a student (should fail)
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest student_Request = createRequestWithCookies(new Cookie[] { studentLogin.cookie });
        replay(student_Request);
        Response student_Response = eventsFacade.getEventBookingForAllGroups(student_Request, "_regular_test_event");
        assertNotEquals(Response.Status.OK.getStatusCode(), student_Response.getStatus());

        // Get event bookings by event id as a teacher (should succeed)
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL, ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest teacher_Request = createRequestWithCookies(new Cookie[] { teacherLogin.cookie });
        replay(teacher_Request);
        Response teacher_Response = eventsFacade.getEventBookingForAllGroups(teacher_Request, "_regular_test_event");
        assertEquals(Response.Status.OK.getStatusCode(), teacher_Response.getStatus());
        assertNotNull(teacher_Response.getEntity());

        // Make sure the EventBookingDTOs contain UserSummaryDTOs, thus not leaking information
        assertTrue(teacher_Response.getEntity() instanceof List);
        List<?> teacherEntity = (List<?>) teacher_Response.getEntity();
        for (Object o : teacherEntity) {
            assertEquals(EventBookingDTO.class.getCanonicalName(), o.getClass().getCanonicalName());
            assertEquals(UserSummaryDTO.class.getCanonicalName(), ((EventBookingDTO) o).getUserBooked().getClass().getCanonicalName());
        }
        Optional<UserSummaryDTO> teacherAlice = (Optional<UserSummaryDTO>) teacherEntity.stream().filter(e -> ((EventBookingDTO) e).getUserBooked().getId() == 7).findFirst();
        // Alice is associated with Teacher and is booked for this event => Alice should be present
        assertTrue(teacherAlice.isPresent());
        Optional<UserSummaryDTO> teacherCharlie = (Optional<UserSummaryDTO>) teacherEntity.stream().filter(e -> ((EventBookingDTO) e).getUserBooked().getId() == 9).findFirst();
        // Charlie is not associated with Teacher and is not booked for this event => Charlie should not be present
        assertFalse(teacherCharlie.isPresent());

        // Try logging in with another teacher account and see if we are sending the wrong information to the wrong teachers
        LoginResult daveLogin = loginAs(httpSession, "dave-teacher@test.com", ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest dave_Request = createRequestWithCookies(new Cookie[] { daveLogin.cookie });
        replay(dave_Request);
        Response dave_Response = eventsFacade.getEventBookingForAllGroups(dave_Request, "_regular_test_event");
        assertEquals(Response.Status.OK.getStatusCode(), dave_Response.getStatus());
        assertNotNull(dave_Response.getEntity());
        // Make sure the EventBookingDTOs contain UserSummaryDTOs, thus not leaking information
        assertTrue(dave_Response.getEntity() instanceof List); // instanceof is OK here because we just need to know this is a subclass of a List
        List<?> daveEntity = (List<?>) dave_Response.getEntity();
        for (Object o : daveEntity) {
            assertEquals(EventBookingDTO.class.getCanonicalName(), o.getClass().getCanonicalName());
            assertEquals(UserSummaryDTO.class.getCanonicalName(), ((EventBookingDTO) o).getUserBooked().getClass().getCanonicalName());
        }
        Optional<UserSummaryDTO> daveAlice = (Optional<UserSummaryDTO>) daveEntity.stream().filter(e -> ((EventBookingDTO) e).getUserBooked().getId() == 7).findFirst();
        // Alice is not associated with Dave but is booked for this event => Alice should not be present
        assertFalse(daveAlice.isPresent());
        Optional<UserSummaryDTO> daveCharlie = (Optional<UserSummaryDTO>) daveEntity.stream().filter(e -> ((EventBookingDTO) e).getUserBooked().getId() == 9).findFirst();
        // Charlie is associated with Dave and is not booked for this event => Charlie should not be present
        assertFalse(daveCharlie.isPresent());
    }

    @Test
    // GET /events/{event_id}/bookings/for_group/{group_id} -> EventFacade::getEventBookingForGivenGroup(request, eventId, groupId)
    public void getEventBookingsByEventIdForGroup() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException, AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
        // Anonymous users MUST NOT be able to get event bookings by event id and group id
        HttpServletRequest anonymous_Request = createNiceMock(HttpServletRequest.class);
        replay(anonymous_Request);
        Response anonymous_Response = eventsFacade.getEventBookingForGivenGroup(anonymous_Request, "_regular_test_event", "1");
        assertNotEquals(Response.Status.OK.getStatusCode(), anonymous_Response.getStatus());

        // Teachers MUST be able to get event bookings by event id and group id if and only if they own the given group
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL, ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest teacher_Request = createRequestWithCookies(new Cookie[] { teacherLogin.cookie });
        replay(teacher_Request);
        Response teacher_Response = eventsFacade.getEventBookingForGivenGroup(teacher_Request, "_regular_test_event", "1");
        assertEquals(Response.Status.OK.getStatusCode(), teacher_Response.getStatus());
        List<?> teacherEntity = (List<?>) teacher_Response.getEntity();
        List<Long> bookedUserIds = teacherEntity.stream().map(booking -> ((EventBookingDTO)booking).getUserBooked().getId()).collect(Collectors.toList());
        assertTrue(bookedUserIds.containsAll(Arrays.asList(7L, 8L)));
        assertFalse(bookedUserIds.contains(9L)); // User 9 is booked but is not in Teacher's group.

        // Students MUST NOT be able to get event bookings by event id and group id
        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest student_Request = createRequestWithCookies(new Cookie[] { studentLogin.cookie });
        replay(student_Request);
        Response student_Response = eventsFacade.getEventBookingForGivenGroup(student_Request, "_regular_test_event", "2");
        // The student does not own the group so this should not succeed
        assertNotEquals(Response.Status.OK.getStatusCode(), student_Response.getStatus());

        // A student MUST NOT be able to get event bookings by event id and group id EVEN IF they belong in the group
        // Alice is part of group id 1
        LoginResult aliceLogin = loginAs(httpSession, "alice-student@test.com", ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest alice_Request = createRequestWithCookies(new Cookie[] { aliceLogin.cookie });
        replay(alice_Request);
        Response alice_Response = eventsFacade.getEventBookingForGivenGroup(alice_Request, "_regular_test_event", "1");
        // The student does not own the group so this should not succeed
        assertNotEquals(Response.Status.OK.getStatusCode(), alice_Response.getStatus());

        LoginResult eventManagerLogin = loginAs(httpSession, ITConstants.TEST_EVENTMANAGER_EMAIL, ITConstants.TEST_EVENTMANAGER_PASSWORD);
        HttpServletRequest eventManager_Request = createRequestWithCookies(new Cookie[] { eventManagerLogin.cookie });
        replay(eventManager_Request);
        Response eventManager_Response = eventsFacade.getEventBookingForGivenGroup(eventManager_Request, "_regular_test_event", "2");
        // The event manager does not own the group so this should not succeed
        assertNotEquals(Response.Status.OK.getStatusCode(), eventManager_Response.getStatus());
    }
}
