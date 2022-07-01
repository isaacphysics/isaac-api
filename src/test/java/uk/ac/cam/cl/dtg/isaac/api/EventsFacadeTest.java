package uk.ac.cam.cl.dtg.isaac.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.lang3.SystemUtils;
import org.easymock.Capture;
import org.eclipse.jgit.api.Git;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.reflections.Reflections;
import uk.ac.cam.cl.dtg.isaac.IsaacE2ETest;
import uk.ac.cam.cl.dtg.isaac.IsaacTest;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.isaac.dao.EventBookingPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.PgUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacEventPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.eventbookings.EventBookingDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.PgQuestionAttempts;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.GroupManager;
import uk.ac.cam.cl.dtg.segue.api.managers.PgTransactionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISecondFactorAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISegueHashingAlgorithm;
import uk.ac.cam.cl.dtg.segue.auth.SegueLocalAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.SeguePBKDF2v3;
import uk.ac.cam.cl.dtg.segue.auth.SegueTOTPAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.EmailCommunicator;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.associations.PgAssociationDataManager;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentMapper;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.users.PgAnonymousUsers;
import uk.ac.cam.cl.dtg.segue.dao.users.PgPasswordDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUserGroupPersistenceManager;
import uk.ac.cam.cl.dtg.segue.dao.users.PgUsers;
import uk.ac.cam.cl.dtg.segue.database.GitDb;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;
import uk.ac.cam.cl.dtg.segue.search.ElasticSearchProvider;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertNotNull;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_LINUX_CONFIG_LOCATION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EMAIL_SIGNATURE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

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
        HttpServletRequest request = createMock(HttpServletRequest.class);
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
    public void getBookingsByEventIdTest() throws NoCredentialsAvailableException, NoUserException, SegueDatabaseException, AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException, MFARequiredButNotConfiguredException, ContentManagerException {
        String someSegueAnonymousUserId = "9284723987anonymous83924923";

        HttpSession httpSession = createNiceMock(HttpSession.class);
        expect(httpSession.getAttribute(Constants.ANONYMOUS_USER)).andReturn(null).atLeastOnce();
        expect(httpSession.getId()).andReturn(someSegueAnonymousUserId).atLeastOnce();
        replay(httpSession);

        // --- Login as a student
        Capture<Cookie> capturedStudentCookie = Capture.newInstance(); // new Capture<Cookie>(); seems deprecated

        HttpServletRequest studentLoginRequest = createNiceMock(HttpServletRequest.class);
        expect(studentLoginRequest.getSession()).andReturn(httpSession).atLeastOnce();
        replay(studentLoginRequest);

        HttpServletResponse studentLoginResponse = createNiceMock(HttpServletResponse.class);
        studentLoginResponse.addCookie(and(capture(capturedStudentCookie), isA(Cookie.class)));
        expectLastCall().atLeastOnce(); // This is how you expect void methods, apparently...
        replay(studentLoginResponse);

        RegisteredUserDTO testStudent = userAccountManager.authenticateWithCredentials(studentLoginRequest, studentLoginResponse, AuthenticationProvider.SEGUE.toString(), properties.getProperty("TEST_STUDENT_EMAIL"), properties.getProperty("TEST_STUDENT_PASSWORD"), false);

        // --- Login as an event manager
        Capture<Cookie> capturedEventManagerCookie = Capture.newInstance(); // new Capture<Cookie>(); seems deprecated

        HttpServletRequest eventManagerLoginRequest = createNiceMock(HttpServletRequest.class);
        expect(eventManagerLoginRequest.getSession()).andReturn(httpSession).atLeastOnce();
        replay(eventManagerLoginRequest);

        HttpServletResponse eventManagerLoginResponse = createNiceMock(HttpServletResponse.class);
        eventManagerLoginResponse.addCookie(and(capture(capturedEventManagerCookie), isA(Cookie.class)));
        expectLastCall().atLeastOnce(); // This is how you expect void methods, apparently...
        replay(eventManagerLoginResponse);

        RegisteredUserDTO testEventManager = userAccountManager.authenticateWithCredentials(eventManagerLoginRequest, eventManagerLoginResponse, AuthenticationProvider.SEGUE.toString(), properties.getProperty("TEST_EVENTMANAGER_EMAIL"), properties.getProperty("TEST_EVENTMANAGER_PASSWORD"), false);

        // --- Create a booking as a logged in student
        HttpServletRequest createBookingRequest = createNiceMock(HttpServletRequest.class);
        expect(createBookingRequest.getCookies()).andReturn(new Cookie[] { capturedStudentCookie.getValue() }).atLeastOnce();
        replay(createBookingRequest);

        Response createBookingResponse = eventsFacade.createBookingForMe(createBookingRequest, "b34eeb0c-7304-4c25-b83b-f28c78b5d078", null);
        // Check that the booking was created successfully
        assertEquals(createBookingResponse.getStatus(), Response.Status.OK.getStatusCode());
        EventBookingDTO eventBookingDTO = null;
        if (null != createBookingResponse.getEntity() && createBookingResponse.getEntity() instanceof EventBookingDTO) {
            eventBookingDTO = (EventBookingDTO) createBookingResponse.getEntity();
            // Check that the returned entity is an EventBookingDTO and the ID of the user who created the booking matches
            assertEquals(testStudent.getId(), ((EventBookingDTO) createBookingResponse.getEntity()).getUserBooked().getId());
        }
        assertNotNull(eventBookingDTO);

        // --- Check whether we are leaking PII to event managers
        HttpServletRequest getEventBookingsByIdRequest = createNiceMock(HttpServletRequest.class);
        expect(getEventBookingsByIdRequest.getCookies()).andReturn(new Cookie[] { capturedEventManagerCookie.getValue() }).atLeastOnce();
        replay(getEventBookingsByIdRequest);

        Response getEventBookingsByIdResponse = eventsFacade.getEventBookingsById(getEventBookingsByIdRequest, eventBookingDTO.getBookingId().toString());
        if (null != getEventBookingsByIdResponse.getEntity() && getEventBookingsByIdResponse.getEntity() instanceof EventBookingDTO) {
            assertNotNull(((EventBookingDTO) getEventBookingsByIdResponse.getEntity()).getUserBooked());
            assertEquals(((EventBookingDTO) getEventBookingsByIdResponse.getEntity()).getUserBooked().getClass(), UserSummaryWithEmailAddressDTO.class);
        }
    }
}
