package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Gender;
import uk.ac.cam.cl.dtg.isaac.dto.LocalAuthDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginByEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginByIPMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import static org.easymock.EasyMock.*;
import static org.eclipse.jetty.http.HttpCookie.SAME_SITE_LAX_COMMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class AuthenticationFacadeIT extends IsaacIntegrationTest {
    private AuthenticationFacade authenticationFacade;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;

    @BeforeAll
    public static void beforeAll() {
        misuseMonitor.registerHandler(SegueLoginByEmailMisuseHandler.class.getSimpleName(),
                new SegueLoginByEmailMisuseHandler(1, 2, NUMBER_SECONDS_IN_MINUTE));
        misuseMonitor.registerHandler(SegueLoginByIPMisuseHandler.class.getSimpleName(),
                new SegueLoginByIPMisuseHandler(1, 2, NUMBER_SECONDS_IN_MINUTE));
    }

    @BeforeEach
    public void beforeEach() {
        misuseMonitor.resetMisuseCount("test-student@test.com", SegueLoginByEmailMisuseHandler.class.getSimpleName());
        misuseMonitor.resetMisuseCount("0.0.0.0", SegueLoginByIPMisuseHandler.class.getSimpleName());
        this.authenticationFacade = new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor);
        mockRequest = createMockRequestObject();
        mockResponse = niceMock(HttpServletResponse.class);
    }

    private static HttpServletRequest createMockRequestObject() {
        HttpSession mockSession = createNiceMock(HttpSession.class);
        expect(mockSession.getAttribute(ANONYMOUS_USER)).andReturn(null).anyTimes();
        expect(mockSession.getId()).andReturn("sessionId").anyTimes();
        replay(mockSession);
        HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
        expect(mockRequest.getHeader("X-Forwarded-For")).andReturn("0.0.0.0").anyTimes();
        expect(mockRequest.getSession()).andReturn(mockSession).anyTimes();
        replay(mockRequest);
        return mockRequest;
    }

    @Test
    public void login_cookie_samesite() throws Exception {
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        assertEquals(SAME_SITE_LAX_COMMENT, teacherLogin.cookie.getComment());
    }

    // See E2E for logout test - response object is mocked in IT tests, preventing retrieval of transformed logout cookie
    // Usage of actual object may be possible in future but the complexity is currently prohibitive

    @Test
    public void resetPassword_emailRateLimits() throws InvalidKeySpecException, NoSuchAlgorithmException {
        LocalAuthDTO targetUser = new LocalAuthDTO();
        targetUser.setEmail("test-student@test.com");
        targetUser.setPassword("123");

        Response firstResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), firstResetResponse.getStatus());

        Response secondResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), secondResetResponse.getStatus());

        Response thirdResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
    }

    @Test
    public void resetPassword_ipRateLimits() throws InvalidKeySpecException, NoSuchAlgorithmException {
        LocalAuthDTO targetUser = new LocalAuthDTO();
        targetUser.setPassword("123");

        targetUser.setEmail("test-student@test.com");
        Response firstResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), firstResetResponse.getStatus());

        targetUser.setEmail("test-student2@test.com");
        Response secondResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), secondResetResponse.getStatus());

        targetUser.setEmail("test-student3@test.com");
        Response thirdResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, AuthenticationProvider.SEGUE.toString(), targetUser);
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
    }

    @Test
    public void authenticateWithCredentials_local_success() throws InvalidKeySpecException, NoSuchAlgorithmException {
        RegisteredUserDTO expectedUser = new RegisteredUserDTO("Test Student", "Student", "test-student@test.com", EmailVerificationStatus.VERIFIED, null, Gender.MALE, Date.from(LocalDateTime.parse("2019-08-01T12:51:39.981").toInstant(ZoneOffset.UTC)), "110158");
        expectedUser.setId(6L);
        LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
        testLocalAuthDTO.setEmail("test-student@test.com");
        testLocalAuthDTO.setPassword("test1234");

        Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE", testLocalAuthDTO);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(expectedUser, response.readEntity(RegisteredUserDTO.class));
    }

    @Test
    public void authenticateWithCredentials_local_nullAuthDTO() throws InvalidKeySpecException, NoSuchAlgorithmException {
        Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE", null);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_MISSING_CREDENTIALS_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidAuthDTO")
    public void authenticateWithCredentials_local_invalidAuthDTO(String email, String password) throws InvalidKeySpecException, NoSuchAlgorithmException {
        LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
        testLocalAuthDTO.setEmail(email);
        testLocalAuthDTO.setPassword(password);

        Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE", testLocalAuthDTO);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_MISSING_CREDENTIALS_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    private static Stream<Arguments> invalidAuthDTO() {
        return Stream.of(
                Arguments.of(null, "test1234"),
                Arguments.of("", "test1234"),
                Arguments.of("test-student@test.com", null),
                Arguments.of("test-student@test.com", "")
        );
    }

    @Test
    public void authenticateWithCredentials_local_incorrectCredentials() throws InvalidKeySpecException, NoSuchAlgorithmException {
        LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
        testLocalAuthDTO.setEmail("test-student@test.com");
        testLocalAuthDTO.setPassword("wrongPassword");

        Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE", testLocalAuthDTO);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_INCORRECT_CREDENTIALS_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    @Test
    public void authenticateWithCredentials_local_unknownUser() throws InvalidKeySpecException, NoSuchAlgorithmException {
        LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
        testLocalAuthDTO.setEmail("test-notastudent@test.com");
        testLocalAuthDTO.setPassword("test1234");

        Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE", testLocalAuthDTO);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_INCORRECT_CREDENTIALS_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    @Test
    public void authenticateWithCredentials_local_emptyProvider() throws InvalidKeySpecException, NoSuchAlgorithmException {
        LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
        testLocalAuthDTO.setEmail("test-student@test.com");
        testLocalAuthDTO.setPassword("wrongPassword");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "", testLocalAuthDTO));
        assertEquals("Provider name must not be empty or null if we are going to map it to an implementation.", exception.getMessage());
    }

    @Test
    public void authenticateWithCredentials_local_unknownProvider() throws InvalidKeySpecException, NoSuchAlgorithmException {
        LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
        testLocalAuthDTO.setEmail("test-student@test.com");
        testLocalAuthDTO.setPassword("wrongPassword");

        Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "OtherProvider", testLocalAuthDTO);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_UNKNOWN_PROVIDER_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    @Test
    public void authenticateWithCredentials_local_unconfiguredRequiredMFA() throws InvalidKeySpecException, NoSuchAlgorithmException {
        LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
        testLocalAuthDTO.setEmail("test-admin@test.com");
        testLocalAuthDTO.setPassword("test1234");

        Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE", testLocalAuthDTO);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_2FA_REQUIRED_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    @Test
    public void authenticateWithCredentials_local_incompleteMFA() throws InvalidKeySpecException, NoSuchAlgorithmException, SegueDatabaseException {
        reset(secondFactorManager);
        expect(secondFactorManager.has2FAConfigured(anyObject(RegisteredUserDTO.class))).andReturn(true).atLeastOnce();
        replay(secondFactorManager);
        LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
        testLocalAuthDTO.setEmail("test-student@test.com");
        testLocalAuthDTO.setPassword("test1234");

        Response response;
        try {
            response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE", testLocalAuthDTO);
        } finally {
            // Make sure to restore the mfa mock afterwards!
            reset(secondFactorManager);
            expect(secondFactorManager.has2FAConfigured(anyObject())).andReturn(false).atLeastOnce();
            replay(secondFactorManager);
        }

        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        assertEquals(Map.of("2FA_REQUIRED", true), response.getEntity());
    }

    @Test
    public void authenticateWithCredentials_local_rateThrottleByIP() throws InvalidKeySpecException, NoSuchAlgorithmException, SegueResourceMisuseException {
        LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
        testLocalAuthDTO.setEmail("test-student1@test.com");
        testLocalAuthDTO.setPassword("test1234");

        misuseMonitor.notifyEvent("0.0.0.0", SegueLoginByIPMisuseHandler.class.getSimpleName());
        misuseMonitor.notifyEvent("0.0.0.0", SegueLoginByIPMisuseHandler.class.getSimpleName());
        Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE", testLocalAuthDTO);
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_RATE_THROTTLE_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
    }

    @Test
    public void authenticateWithCredentials_local_rateThrottleByEmail() throws InvalidKeySpecException, NoSuchAlgorithmException, SegueResourceMisuseException {
        LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
        testLocalAuthDTO.setEmail("test-student@test.com");
        testLocalAuthDTO.setPassword("test1234");

        misuseMonitor.notifyEvent("test-student@test.com", SegueLoginByEmailMisuseHandler.class.getSimpleName());
        misuseMonitor.notifyEvent("test-student@test.com", SegueLoginByEmailMisuseHandler.class.getSimpleName());
        Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE", testLocalAuthDTO);
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_RATE_THROTTLE_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
    }
}
