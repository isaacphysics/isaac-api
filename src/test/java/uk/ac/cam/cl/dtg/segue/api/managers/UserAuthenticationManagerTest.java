package uk.ac.cam.cl.dtg.segue.api.managers;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DATE_EXPIRES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_DATE_FORMAT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC_SALT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.PARTIAL_LOGIN_FLAG;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_EXPIRY_SECONDS_DEFAULT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_TOKEN;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_USER_ID;
import static uk.ac.cam.cl.dtg.util.ServletTestUtils.createMockServletRequest;
import static uk.ac.cam.cl.dtg.util.ServletTestUtils.createMockSession;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.easymock.Capture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

public class UserAuthenticationManagerTest {
  private UserAuthenticationManager userAuthenticationManager;
  private IUserDataManager dummyDatabase;
  private PropertiesLoader dummyPropertiesLoader;
  private Map<AuthenticationProvider, IAuthenticator> dummyProvidersMap;
  private EmailManager dummyQueue;
  private String dummyHMACSalt;
  private String dummyHostName;

  @BeforeEach
  public void BeforeEach() {
    this.dummyDatabase = createMock(IUserDataManager.class);
    this.dummyPropertiesLoader = createMock(PropertiesLoader.class);
    this.dummyProvidersMap = new HashMap<>();

    this.dummyHMACSalt = "BOB";
    this.dummyHostName = "bob";

    expect(this.dummyPropertiesLoader.getProperty(HMAC_SALT)).andReturn(dummyHMACSalt).anyTimes();
    expect(this.dummyPropertiesLoader.getProperty(HOST_NAME)).andReturn(dummyHostName).anyTimes();
    expect(this.dummyPropertiesLoader.getProperty(SESSION_EXPIRY_SECONDS_DEFAULT)).andReturn("60")
        .anyTimes();
    expect(this.dummyPropertiesLoader.getProperty(Constants.SEGUE_APP_ENVIRONMENT)).andReturn("DEV").anyTimes();
    replay(this.dummyPropertiesLoader);

    userAuthenticationManager =
        new UserAuthenticationManager(dummyDatabase, dummyPropertiesLoader, dummyProvidersMap, dummyQueue);
  }

  @Test
  public void isSessionValid_valid() throws JsonProcessingException, SegueDatabaseException {
    Date cookieExpiryDate = Date.from(Instant.now().plus(300, SECONDS));
    String cookieExpiryDateString = new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(cookieExpiryDate);
    Map<String, String> sessionInformation = new HashMap<>(4);
    sessionInformation.put(SESSION_USER_ID, "1");
    sessionInformation.put(SESSION_TOKEN, "1");
    sessionInformation.put(DATE_EXPIRES, cookieExpiryDateString);
    sessionInformation.put(HMAC, userAuthenticationManager.calculateUpdatedHMAC(sessionInformation));
    Cookie segueAuthCookie = userAuthenticationManager.createAuthCookie(sessionInformation, 300);

    HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
    expect(mockRequest.getCookies()).andReturn(new Cookie[] {segueAuthCookie}).anyTimes();
    replay(mockRequest);

    RegisteredUser mockUser = createNiceMock(RegisteredUser.class);
    expect(mockUser.getSessionToken()).andReturn(1).times(2);
    replay(mockUser);

    expect(dummyDatabase.getById(1L)).andReturn(mockUser);
    replay(dummyDatabase);

    assertTrue(userAuthenticationManager.isSessionValid(mockRequest));
  }

  @Test
  public void isSessionValid_noCookies() throws SegueDatabaseException {
    HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
    expect(mockRequest.getCookies()).andReturn(null).anyTimes();
    replay(mockRequest);

    RegisteredUser mockUser = createNiceMock(RegisteredUser.class);
    expect(mockUser.getSessionToken()).andReturn(1);
    replay(mockUser);

    expect(dummyDatabase.getById(1L)).andReturn(mockUser);
    replay(dummyDatabase);

    assertFalse(userAuthenticationManager.isSessionValid(mockRequest));
  }

  @Test
  public void isSessionValid_noAuthCookie() throws SegueDatabaseException {
    Cookie notAuthCookie = new Cookie("NOT_AUTH_COOKIE", "");
    HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
    expect(mockRequest.getCookies()).andReturn(new Cookie[] {notAuthCookie}).anyTimes();
    replay(mockRequest);

    RegisteredUser mockUser = createNiceMock(RegisteredUser.class);
    expect(mockUser.getSessionToken()).andReturn(1);
    replay(mockUser);

    expect(dummyDatabase.getById(1L)).andReturn(mockUser);
    replay(dummyDatabase);

    assertFalse(userAuthenticationManager.isSessionValid(mockRequest));
  }

  @Test
  public void calculateUpdatedHMAC_noPartialLogin() {
    String expectedHMAC = "dwHtgxiiU7r7xH/BNet7bZb4PQMK0CrOfSVnn+ctWXQ=";
    Date cookieExpiryDate = Date.from(LocalDateTime.of(2020, 1, 1, 0, 0, 0).toInstant(UTC));
    String cookieExpiryDateString = new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(cookieExpiryDate);
    Map<String, String> sessionInformation = Map.of(
        SESSION_USER_ID, "1",
        SESSION_TOKEN, "1",
        DATE_EXPIRES, cookieExpiryDateString
    );

    assertEquals(expectedHMAC, userAuthenticationManager.calculateUpdatedHMAC(sessionInformation));
  }

  @Test
  public void calculateUpdatedHMAC_PartialLogin() {
    String expectedHMAC = "/reauAoeghGgfvoMxC+zpQTVlOytSKncUpOrgzwjomw=";
    Date cookieExpiryDate = Date.from(LocalDateTime.of(2020, 1, 1, 0, 0, 0).toInstant(UTC));
    String cookieExpiryDateString = new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(cookieExpiryDate);
    Map<String, String> sessionInformation = Map.of(
        SESSION_USER_ID, "1",
        SESSION_TOKEN, "1",
        DATE_EXPIRES, cookieExpiryDateString,
        PARTIAL_LOGIN_FLAG, "true"
    );

    assertEquals(expectedHMAC, userAuthenticationManager.calculateUpdatedHMAC(sessionInformation));
  }

  @Test
  public void destroyUserSession() throws JsonProcessingException, SegueDatabaseException, NoUserLoggedInException {
    RegisteredUser mockUser = createNiceMock(RegisteredUser.class);
    expect(mockUser.getSessionToken()).andReturn(1).times(2);
    replay(mockUser);

    expect(dummyDatabase.getById(1L)).andReturn(mockUser);
    dummyDatabase.invalidateSessionToken(mockUser);
    expectLastCall();
    replay(dummyDatabase);

    Map<String, String> sessionInformation = new HashMap(Map.of(
        SESSION_USER_ID, "1",
        SESSION_TOKEN, "1",
        DATE_EXPIRES, new SimpleDateFormat(DEFAULT_DATE_FORMAT).format(Date.from(Instant.now().plus(3600, SECONDS)))
    ));
    sessionInformation.put(HMAC, userAuthenticationManager.calculateUpdatedHMAC(sessionInformation));

    Cookie authCookie = userAuthenticationManager.createAuthCookie(sessionInformation, 3600);
    HttpSession logoutSession = createMockSession();
    replay(logoutSession);
    HttpServletRequest logoutRequest = createMockServletRequest(logoutSession);
    expect(logoutRequest.getCookies()).andReturn(new Cookie[] {authCookie}).anyTimes();
    replay(logoutRequest);

    Capture<Cookie> logoutResponseCookie = newCapture();
    HttpServletResponse logoutResponse = createMock(HttpServletResponse.class);
    logoutResponse.addCookie(capture(logoutResponseCookie));
    replay(logoutResponse);

    userAuthenticationManager.destroyUserSession(logoutRequest, logoutResponse);
    assertEquals("", logoutResponseCookie.getValue().getValue());
    verify(dummyDatabase);
  }
}
