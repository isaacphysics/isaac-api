package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.isaac.IsaacE2ETest;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.DetailedEventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

//@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@Ignore
public class EventsFacadeTest extends IsaacE2ETest {

    private EventsFacade eventsFacade;

    @Before
    public void setUp() {
        // Get an instance of the facade to test
        eventsFacade = new EventsFacade(properties, logManager, eventBookingManager, userAccountManager, contentManager, "latest", userBadgeManager, userAssociationManager, groupManager, userAccountManager, schoolListReader, mapperFacade);
    }

    @Test
    public void getEventsTest() throws InterruptedException {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        expect(request.getCookies()).andReturn(new Cookie[]{}).anyTimes();
        replay(request);

        Response response = eventsFacade.getEvents(request, null, 0, 10, null, null, null, null, null, null);
        int status = response.getStatus();
        assertEquals(status, Response.Status.OK.getStatusCode());
        Object entityObject = response.getEntity();
        assertNotNull(entityObject);
        @SuppressWarnings("unchecked") ResultsWrapper<IsaacEventPageDTO> entity = (ResultsWrapper<IsaacEventPageDTO>) entityObject;
        assertNotNull(entity);
        List<IsaacEventPageDTO> results = entity.getResults();
        assertNotNull(entity);
        assertEquals(results.size(), 5);
    }

    @Test
    public void getBookingByIdTest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException, AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException, MFARequiredButNotConfiguredException, ContentManagerException {
        // --- Login as a student
        LoginResult studentLogin = loginAs(httpSession, properties.getProperty("TEST_STUDENT_EMAIL"), properties.getProperty("TEST_STUDENT_PASSWORD"));
        // --- Login as an event manager
        LoginResult eventManagerLogin = loginAs(httpSession, properties.getProperty("TEST_EVENTMANAGER_EMAIL"), properties.getProperty("TEST_EVENTMANAGER_PASSWORD"));

        // --- Create a booking as a logged in student
        HttpServletRequest createBookingRequest = createNiceMock(HttpServletRequest.class);
        expect(createBookingRequest.getCookies()).andReturn(new Cookie[] { studentLogin.cookie }).atLeastOnce();
        replay(createBookingRequest);
        Response createBookingResponse = eventsFacade.createBookingForMe(createBookingRequest, "_regular_test_event", null);

        // Check that the booking was created successfully
        assertEquals(createBookingResponse.getStatus(), Response.Status.OK.getStatusCode());
        EventBookingDTO eventBookingDTO = null;
        if (null != createBookingResponse.getEntity() && createBookingResponse.getEntity() instanceof EventBookingDTO) {
            eventBookingDTO = (EventBookingDTO) createBookingResponse.getEntity();
            // Check that the returned entity is an EventBookingDTO and the ID of the user who created the booking matches
            assertEquals(studentLogin.user.getId(), ((EventBookingDTO) createBookingResponse.getEntity()).getUserBooked().getId());
        }
        assertNotNull(eventBookingDTO);

        // --- Check whether what we get as event managers
        HttpServletRequest getEventBookingsByIdRequest = createNiceMock(HttpServletRequest.class);
        expect(getEventBookingsByIdRequest.getCookies()).andReturn(new Cookie[] { eventManagerLogin.cookie }).atLeastOnce();
        replay(getEventBookingsByIdRequest);

        Response getEventBookingsByIdResponse = eventsFacade.getEventBookingsById(getEventBookingsByIdRequest, eventBookingDTO.getBookingId().toString());
        if (null != getEventBookingsByIdResponse.getEntity() && getEventBookingsByIdResponse.getEntity() instanceof EventBookingDTO) {
            assertNotNull(((EventBookingDTO) getEventBookingsByIdResponse.getEntity()).getUserBooked());
            assertEquals(((EventBookingDTO) getEventBookingsByIdResponse.getEntity()).getUserBooked().getClass(), UserSummaryWithEmailAddressDTO.class);
        }
    }

    // events/{event_id}/bookings
    @Test
    public void getEventBookingsByEventIdTest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException, AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
        // Get event bookings by event id as an anonymous user (should fail)
        HttpServletRequest getEventBookingsAsAnonymous_Request = createNiceMock(HttpServletRequest.class);
        replay(getEventBookingsAsAnonymous_Request);
        Response getEventBookingsAsAnonymous_Response = eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsAnonymous_Request, "_regular_test_event");
        assertNotEquals(getEventBookingsAsAnonymous_Response.getStatus(), Response.Status.OK.getStatusCode());

        // Get event bookings by event id as a student (should fail)
        LoginResult studentLogin = loginAs(httpSession, properties.getProperty("TEST_STUDENT_EMAIL"), properties.getProperty("TEST_STUDENT_PASSWORD"));
        HttpServletRequest getEventBookingsAsStudent_Request = createNiceMock(HttpServletRequest.class);
        expect(getEventBookingsAsStudent_Request.getCookies()).andReturn(new Cookie[] { studentLogin.cookie }).atLeastOnce();
        replay(getEventBookingsAsStudent_Request);
        Response getEventBookingsAsStudent_Response = eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsStudent_Request, "_regular_test_event");
        assertNotEquals(getEventBookingsAsStudent_Response.getStatus(), Response.Status.OK.getStatusCode());

        // Get event bookings by event id as a teacher (should fail)
        LoginResult teacherLogin = loginAs(httpSession, properties.getProperty("TEST_TEACHER_EMAIL"), properties.getProperty("TEST_TEACHER_PASSWORD"));
        HttpServletRequest getEventBookingsAsTeacher_Request = createNiceMock(HttpServletRequest.class);
        expect(getEventBookingsAsTeacher_Request.getCookies()).andReturn(new Cookie[] { teacherLogin.cookie }).atLeastOnce();
        replay(getEventBookingsAsTeacher_Request);
        Response getEventBookingsAsTeacher_Response = eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsTeacher_Request, "_regular_test_event");
        assertNotEquals(getEventBookingsAsTeacher_Response.getStatus(), Response.Status.OK.getStatusCode());

        // Get event bookings by event id as a teacher (should fail)
        LoginResult editorLogin = loginAs(httpSession, properties.getProperty("TEST_EDITOR_EMAIL"), properties.getProperty("TEST_EDITOR_PASSWORD"));
        HttpServletRequest getEventBookingsAsEditor_Request = createNiceMock(HttpServletRequest.class);
        expect(getEventBookingsAsEditor_Request.getCookies()).andReturn(new Cookie[] { editorLogin.cookie }).atLeastOnce();
        replay(getEventBookingsAsEditor_Request);
        Response getEventBookingsAsEditor_Response = eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsEditor_Request, "_regular_test_event");
        assertNotEquals(getEventBookingsAsEditor_Response.getStatus(), Response.Status.OK.getStatusCode());

        // Get event bookings by event id as an event manager (should succeed)
        LoginResult eventManagerLogin = loginAs(httpSession, properties.getProperty("TEST_EVENTMANAGER_EMAIL"), properties.getProperty("TEST_EVENTMANAGER_PASSWORD"));
        HttpServletRequest getEventBookingsAsEventManager_Request = createNiceMock(HttpServletRequest.class);
        expect(getEventBookingsAsEventManager_Request.getCookies()).andReturn(new Cookie[] { eventManagerLogin.cookie }).atLeastOnce();
        replay(getEventBookingsAsEventManager_Request);
        Response getEventBookingsAsEventManager_Response = eventsFacade.adminGetEventBookingByEventId(getEventBookingsAsEventManager_Request, "_regular_test_event");
        assertEquals(getEventBookingsAsEventManager_Response.getStatus(), Response.Status.OK.getStatusCode());
        if (null != getEventBookingsAsEventManager_Response.getEntity() && getEventBookingsAsAnonymous_Response.getEntity() instanceof List) {
            List<?> entity = (List<?>) getEventBookingsAsEventManager_Response.getEntity();
            assertEquals(entity.size(), 2);
            for (Object o : entity) {
                assert(o instanceof DetailedEventBookingDTO);
            }
        }

        // Get event bookings by event id as an admin (should succeed)
        // NOTE: I was going to test as an admin too (same code as for Event Managers) but logging in with MFA is a
        // nightmare I'm not prepared to face yet. Plus, if we have people who obtained the ADMIN role, we have
        // bigger problems anyway.
    }
}
