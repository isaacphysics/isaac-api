package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.isaac.IsaacE2ETest;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
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
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;

//@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
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
        assertEquals(results.size(), 7);
    }

    @Test
    public void getBookingsByEventIdTest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException, AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException, MFARequiredButNotConfiguredException, ContentManagerException {
        String someSegueAnonymousUserId = "9284723987anonymous83924923";

        HttpSession httpSession = createNiceMock(HttpSession.class);
        expect(httpSession.getAttribute(Constants.ANONYMOUS_USER)).andReturn(null).atLeastOnce();
        expect(httpSession.getId()).andReturn(someSegueAnonymousUserId).atLeastOnce();
        replay(httpSession);

        // --- Login as a student
        LoginResult studentLogin = loginAs(httpSession, properties.getProperty("TEST_STUDENT_EMAIL"), properties.getProperty("TEST_STUDENT_PASSWORD"));

        // --- Login as an event manager
        LoginResult eventManagerLogin = loginAs(httpSession, properties.getProperty("TEST_EVENTMANAGER_EMAIL"), properties.getProperty("TEST_EVENTMANAGER_PASSWORD"));

        // --- Create a booking as a logged in student
        HttpServletRequest createBookingRequest = createNiceMock(HttpServletRequest.class);
        expect(createBookingRequest.getCookies()).andReturn(new Cookie[] { studentLogin.cookie }).atLeastOnce();
        replay(createBookingRequest);

        Response createBookingResponse = eventsFacade.createBookingForMe(createBookingRequest, "b34eeb0c-7304-4c25-b83b-f28c78b5d078", null);
        // Check that the booking was created successfully
        assertEquals(createBookingResponse.getStatus(), Response.Status.OK.getStatusCode());
        EventBookingDTO eventBookingDTO = null;
        if (null != createBookingResponse.getEntity() && createBookingResponse.getEntity() instanceof EventBookingDTO) {
            eventBookingDTO = (EventBookingDTO) createBookingResponse.getEntity();
            // Check that the returned entity is an EventBookingDTO and the ID of the user who created the booking matches
            assertEquals(studentLogin.user.getId(), ((EventBookingDTO) createBookingResponse.getEntity()).getUserBooked().getId());
        }
        assertNotNull(eventBookingDTO);

        // --- Check whether we are leaking PII to event managers
        HttpServletRequest getEventBookingsByIdRequest = createNiceMock(HttpServletRequest.class);
        expect(getEventBookingsByIdRequest.getCookies()).andReturn(new Cookie[] { eventManagerLogin.cookie }).atLeastOnce();
        replay(getEventBookingsByIdRequest);

        Response getEventBookingsByIdResponse = eventsFacade.getEventBookingsById(getEventBookingsByIdRequest, eventBookingDTO.getBookingId().toString());
        if (null != getEventBookingsByIdResponse.getEntity() && getEventBookingsByIdResponse.getEntity() instanceof EventBookingDTO) {
            assertNotNull(((EventBookingDTO) getEventBookingsByIdResponse.getEntity()).getUserBooked());
            assertEquals(((EventBookingDTO) getEventBookingsByIdResponse.getEntity()).getUserBooked().getClass(), UserSummaryWithEmailAddressDTO.class);
        }
    }
}
