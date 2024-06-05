package uk.ac.cam.cl.dtg.isaac.api;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.niceMock;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_ADMIN_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_ADMIN_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_IP_ADDRESS;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_UNKNOWN_USER_ONE_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_UNKNOWN_USER_THREE_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_UNKNOWN_USER_TWO_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_WRONG_PASSWORD;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGIN_2FA_REQUIRED_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGIN_INCORRECT_CREDENTIALS_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGIN_MISSING_CREDENTIALS_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGIN_RATE_THROTTLE_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGIN_UNKNOWN_PROVIDER_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGOUT_NO_ACTIVE_SESSION_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_MINUTE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_TOKEN;
import static uk.ac.cam.cl.dtg.util.ServletTestUtils.createMockServletRequest;
import static uk.ac.cam.cl.dtg.util.ServletTestUtils.createMockSession;
import static uk.ac.cam.cl.dtg.util.ServletTestUtils.replayMockServletRequest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Stream;
import org.easymock.Capture;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
    misuseMonitor.resetMisuseCount(TEST_STUDENT_EMAIL, SegueLoginByEmailMisuseHandler.class.getSimpleName());
    misuseMonitor.resetMisuseCount(TEST_UNKNOWN_USER_ONE_EMAIL, SegueLoginByEmailMisuseHandler.class.getSimpleName());
    misuseMonitor.resetMisuseCount(TEST_IP_ADDRESS, SegueLoginByIPMisuseHandler.class.getSimpleName());
    this.authenticationFacade = new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor);
    mockRequest = replayMockServletRequest();
    mockResponse = niceMock(HttpServletResponse.class);
  }

  @Test
  void loginCookieSameSite() throws Exception {
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    assertEquals("__SAME_SITE_LAX__", teacherLogin.cookie.getComment());
  }

  // See E2E for logout test - response object is mocked in IT tests, preventing retrieval of transformed logout cookie
  // Usage of actual object may be possible in future but the complexity is currently prohibitive

  @Nested
  class ResetPassword {
    @Test
    void emailRateLimits() throws InvalidKeySpecException, NoSuchAlgorithmException {
      LocalAuthDTO targetUser = new LocalAuthDTO();
      targetUser.setEmail(TEST_STUDENT_EMAIL);
      targetUser.setPassword(TEST_WRONG_PASSWORD);

      try (Response firstResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse,
          AuthenticationProvider.SEGUE.toString(), targetUser)) {
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), firstResetResponse.getStatus());
      }

      try (Response secondResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse,
          AuthenticationProvider.SEGUE.toString(), targetUser)) {
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), secondResetResponse.getStatus());
      }

      try (Response thirdResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse,
          AuthenticationProvider.SEGUE.toString(), targetUser)) {
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
      }
    }

    @Test
    void ipRateLimits() throws InvalidKeySpecException, NoSuchAlgorithmException {
      LocalAuthDTO targetUser = new LocalAuthDTO();
      targetUser.setPassword(TEST_WRONG_PASSWORD);

      targetUser.setEmail(TEST_UNKNOWN_USER_ONE_EMAIL);
      try (Response firstResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse,
          AuthenticationProvider.SEGUE.toString(), targetUser)) {
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), firstResetResponse.getStatus());
      }

      targetUser.setEmail(TEST_UNKNOWN_USER_TWO_EMAIL);
      try (Response secondResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse,
          AuthenticationProvider.SEGUE.toString(), targetUser)) {
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), secondResetResponse.getStatus());
      }

      targetUser.setEmail(TEST_UNKNOWN_USER_THREE_EMAIL);
      try (Response thirdResetResponse = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse,
          AuthenticationProvider.SEGUE.toString(), targetUser)) {
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), thirdResetResponse.getStatus());
      }
    }
  }

  @Nested
  class AuthenticateWithCredentialsLocalProvider {
    @Test
    void success() throws InvalidKeySpecException, NoSuchAlgorithmException {
      RegisteredUserDTO expectedUser =
          new RegisteredUserDTO("Test Student", "Student", TEST_STUDENT_EMAIL, EmailVerificationStatus.VERIFIED, null,
              Gender.MALE, LocalDateTime.parse("2019-08-01T12:51:39.981").toInstant(ZoneOffset.UTC), "110158", false);
      expectedUser.setId(TEST_STUDENT_ID);
      LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
      testLocalAuthDTO.setEmail(TEST_STUDENT_EMAIL);
      testLocalAuthDTO.setPassword(TEST_STUDENT_PASSWORD);

      try (Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE",
          testLocalAuthDTO)) {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(expectedUser, response.readEntity(RegisteredUserDTO.class));
      }
    }

    @Test
    void nullAuthDTO() throws InvalidKeySpecException, NoSuchAlgorithmException {
      try (Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE",
          null)) {
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_MISSING_CREDENTIALS_MESSAGE,
            response.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Test
    void incorrectCredentials() throws InvalidKeySpecException, NoSuchAlgorithmException {
      LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
      testLocalAuthDTO.setEmail(TEST_STUDENT_EMAIL);
      testLocalAuthDTO.setPassword(TEST_WRONG_PASSWORD);

      try (Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE",
          testLocalAuthDTO)) {
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_INCORRECT_CREDENTIALS_MESSAGE,
            response.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Test
    void unknownUser() throws InvalidKeySpecException, NoSuchAlgorithmException {
      LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
      testLocalAuthDTO.setEmail(TEST_UNKNOWN_USER_ONE_EMAIL);
      testLocalAuthDTO.setPassword(TEST_WRONG_PASSWORD);

      try (Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE",
          testLocalAuthDTO)) {
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_INCORRECT_CREDENTIALS_MESSAGE,
            response.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Test
    void unconfiguredRequiredMFA() throws InvalidKeySpecException, NoSuchAlgorithmException {
      LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
      testLocalAuthDTO.setEmail(TEST_ADMIN_EMAIL);
      testLocalAuthDTO.setPassword(TEST_ADMIN_PASSWORD);

      try (Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE",
          testLocalAuthDTO)) {
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_2FA_REQUIRED_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Test
    void incompleteMFA() throws InvalidKeySpecException, NoSuchAlgorithmException, SegueDatabaseException {
      reset(secondFactorManager);
      expect(secondFactorManager.has2FAConfigured(anyObject(RegisteredUserDTO.class))).andReturn(true).atLeastOnce();
      replay(secondFactorManager);
      LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
      testLocalAuthDTO.setEmail(TEST_STUDENT_EMAIL);
      testLocalAuthDTO.setPassword(TEST_STUDENT_PASSWORD);

      Response response;
      try {
        response =
            authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE", testLocalAuthDTO);
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
    void rateThrottleByIP()
        throws InvalidKeySpecException, NoSuchAlgorithmException, SegueResourceMisuseException {
      LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
      testLocalAuthDTO.setEmail(TEST_UNKNOWN_USER_ONE_EMAIL);
      testLocalAuthDTO.setPassword(TEST_WRONG_PASSWORD);

      misuseMonitor.notifyEvent(TEST_IP_ADDRESS, SegueLoginByIPMisuseHandler.class.getSimpleName());
      misuseMonitor.notifyEvent(TEST_IP_ADDRESS, SegueLoginByIPMisuseHandler.class.getSimpleName());
      try (Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE",
          testLocalAuthDTO)) {
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_RATE_THROTTLE_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @Test
    void rateThrottleByEmail()
        throws InvalidKeySpecException, NoSuchAlgorithmException, SegueResourceMisuseException {
      LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
      testLocalAuthDTO.setEmail(TEST_STUDENT_EMAIL);
      testLocalAuthDTO.setPassword(TEST_STUDENT_PASSWORD);

      misuseMonitor.notifyEvent(TEST_STUDENT_EMAIL, SegueLoginByEmailMisuseHandler.class.getSimpleName());
      misuseMonitor.notifyEvent(TEST_STUDENT_EMAIL, SegueLoginByEmailMisuseHandler.class.getSimpleName());
      try (Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE",
          testLocalAuthDTO)) {
        assertEquals(Response.Status.TOO_MANY_REQUESTS.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_RATE_THROTTLE_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    @ParameterizedTest
    @MethodSource("invalidAuthDTO")
    void authenticateWithCredentialsLocalProviderInvalidAuthDTO(final String email, final String password)
        throws InvalidKeySpecException, NoSuchAlgorithmException {
      LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
      testLocalAuthDTO.setEmail(email);
      testLocalAuthDTO.setPassword(password);

      try (Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "SEGUE",
          testLocalAuthDTO)) {
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals(LOGIN_MISSING_CREDENTIALS_MESSAGE,
            response.readEntity(SegueErrorResponse.class).getErrorMessage());
      }
    }

    private static Stream<Arguments> invalidAuthDTO() {
      return Stream.of(
          Arguments.of(null, TEST_STUDENT_PASSWORD),
          Arguments.of("", TEST_STUDENT_PASSWORD),
          Arguments.of(TEST_STUDENT_EMAIL, null),
          Arguments.of(TEST_STUDENT_EMAIL, "")
      );
    }
  }

  @Test
  void authenticateWithCredentialsEmptyProvider() {
    LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
    testLocalAuthDTO.setEmail(TEST_STUDENT_EMAIL);
    testLocalAuthDTO.setPassword(TEST_WRONG_PASSWORD);

    Exception exception = assertThrows(IllegalArgumentException.class,
        () -> authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "", testLocalAuthDTO));
    assertEquals("Provider name must not be empty or null if we are going to map it to an implementation.",
        exception.getMessage());
  }

  @Test
  void authenticateWithCredentialsUnknownProvider() throws InvalidKeySpecException, NoSuchAlgorithmException {
    LocalAuthDTO testLocalAuthDTO = new LocalAuthDTO();
    testLocalAuthDTO.setEmail(TEST_STUDENT_EMAIL);
    testLocalAuthDTO.setPassword(TEST_WRONG_PASSWORD);

    try (
        Response response = authenticationFacade.authenticateWithCredentials(mockRequest, mockResponse, "OtherProvider",
            testLocalAuthDTO)) {
      assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals(LOGIN_UNKNOWN_PROVIDER_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
    }
  }

  @Nested
  class UserLogout {
    @Test
    void sessionDeauthentication()
        throws InvalidKeySpecException, NoSuchAlgorithmException, IOException {
      LocalAuthDTO targetUser = new LocalAuthDTO();
      targetUser.setEmail(TEST_STUDENT_EMAIL);
      targetUser.setPassword(TEST_STUDENT_PASSWORD);

      Capture<Cookie> firstLoginResponseCookie = newCapture();
      HttpServletResponse firstLoginResponse = createMock(HttpServletResponse.class);
      firstLoginResponse.addCookie(capture(firstLoginResponseCookie));
      replay(firstLoginResponse);

      authenticationFacade.authenticateWithCredentials(mockRequest, firstLoginResponse,
          AuthenticationProvider.SEGUE.toString(), targetUser);
      Cookie firstLoginAuthCookie = firstLoginResponseCookie.getValue();
      Map<String, String> firstLoginSessionInformation = userAuthenticationManager.decodeCookie(firstLoginAuthCookie);
      // Session should be valid
      assertTrue(userAuthenticationManager.isSessionValid(firstLoginSessionInformation));

      Capture<Cookie> logoutResponseCookie = newCapture();
      HttpServletResponse logoutResponse = createMock(HttpServletResponse.class);
      logoutResponse.addCookie(capture(logoutResponseCookie));
      replay(logoutResponse);

      HttpSession logoutSession = createMockSession();
      replay(logoutSession);
      HttpServletRequest logoutRequest = createMockServletRequest(logoutSession);
      expect(logoutRequest.getCookies()).andReturn(new Cookie[] {firstLoginAuthCookie}).anyTimes();
      replay(logoutRequest);

      authenticationFacade.userLogout(logoutRequest, logoutResponse);
      Cookie logoutCookie = logoutResponseCookie.getValue();
      // Should be no session associated with logout
      assertEquals("", logoutCookie.getValue());
      // Session should have been invalidated
      assertFalse(userAuthenticationManager.isSessionValid(firstLoginSessionInformation));

      Capture<Cookie> secondLoginResponseCookie = newCapture();
      HttpServletResponse secondLoginResponse = createMock(HttpServletResponse.class);
      secondLoginResponse.addCookie(capture(secondLoginResponseCookie));
      replay(secondLoginResponse);

      authenticationFacade.authenticateWithCredentials(mockRequest, secondLoginResponse,
          AuthenticationProvider.SEGUE.toString(), targetUser);
      Cookie secondLoginAuthCookie = secondLoginResponseCookie.getValue();
      Map<String, String> secondLoginSessionInformation = userAuthenticationManager.decodeCookie(secondLoginAuthCookie);
      // New session should be valid
      assertTrue(userAuthenticationManager.isSessionValid(secondLoginSessionInformation));
      // Previous session should still be invalid
      assertFalse(userAuthenticationManager.isSessionValid(firstLoginSessionInformation));
      // Sessions should have different tokens
      assertNotEquals(firstLoginSessionInformation.get(SESSION_TOKEN),
          secondLoginSessionInformation.get(SESSION_TOKEN));
    }

    @Test
    void noSession() throws SQLException {
      HttpSession logoutSession = createMockSession();
      replay(logoutSession);
      HttpServletRequest logoutRequest = createMockServletRequest(logoutSession);
      expect(logoutRequest.getCookies()).andReturn(new Cookie[] {}).anyTimes();
      replay(logoutRequest);

      Response response;
      try {
        response = authenticationFacade.userLogout(logoutRequest, mockResponse);
      } finally {
        removeAnonymousUser("sessionId");
      }
      assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      assertEquals(LOGOUT_NO_ACTIVE_SESSION_MESSAGE, response.readEntity(SegueErrorResponse.class).getErrorMessage());
    }
  }

  private void removeAnonymousUser(final String sessionId) throws SQLException {
    try (PreparedStatement pst = postgresSqlDb.getDatabaseConnection()
        .prepareStatement("DELETE FROM temporary_user_store WHERE id = ?")
    ) {
      pst.setString(1, sessionId);
      pst.executeUpdate();
    }
  }
}
