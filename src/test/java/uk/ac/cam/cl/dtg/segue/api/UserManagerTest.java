package uk.ac.cam.cl.dtg.segue.api;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.easymock.EasyMock.*;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuth2Authenticator;
import uk.ac.cam.cl.dtg.segue.auth.NoUserIdException;
import uk.ac.cam.cl.dtg.segue.dao.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dto.users.Gender;
import uk.ac.cam.cl.dtg.segue.dto.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.users.User;
/**
 * Test class for the user manager class.
 * 
 */
public class UserManagerTest {

	private IUserDataManager dummyDatabase;
	private String dummyHMACSalt;
	private Map<AuthenticationProvider, IFederatedAuthenticator> dummyProvidersMap;
	private static final String CSRF_Test_VALUE = "googleomrdd07hbe6vc1efim5rnsgvms";

	/**
	 * Initial configuration of tests.
	 * @throws Exception - test exception 
	 */
	@Before
	public final void setUp() throws Exception {
		this.dummyDatabase = createMock(IUserDataManager.class);
		this.dummyHMACSalt = "BOB";
		this.dummyProvidersMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
	}
	
	/**
	 * Verify that the constructor responds correctly to bad input.
	 */
	@Test
	public final void userManager_checkConstructorForBadInput_exceptionsShouldBeThrown() {
		try {
			new UserManager(null, this.dummyHMACSalt, this.dummyProvidersMap);
			fail("Expected a null pointer exception immediately");
		} catch (NullPointerException e) {
			// fine
		}
		try {
			new UserManager(this.dummyDatabase, null, this.dummyProvidersMap);
			fail("Expected a null pointer exception immediately");
		} catch (NullPointerException e) {
			// fine
		}
		try {
			new UserManager(this.dummyDatabase, this.dummyHMACSalt, null);
			fail("Expected a null pointer exception immediately");
		} catch (NullPointerException e) {
			// fine
		}
	}

	/**
	 * Test that the get current user method behaves correctly when not logged in.
	 */
	@Test
	public final void getCurrentUser_isNotLoggedIn_noUserObjectReturned() {
		// Arrange
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		UserManager userManager = new UserManager(this.dummyDatabase,
				this.dummyHMACSalt, providerMap);

		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		expect(request.getSession()).andReturn(dummySession);
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				null);

		replay(dummySession);
		replay(request);
		replay(dummyDatabase);

		// Act
		User u = userManager.getCurrentUser(request);

		// Assert
		assertTrue(null == u);
		verify(dummyDatabase, dummySession, request);
	}

	/**
	 * Test that get current user with valid HMAC works correctly.
	 */
	@Test
	public final void getCurrentUser_IsAuthenticatedWithValidHMAC_userIsReturned() {
		// Arrange
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		UserManager userManager = new UserManager(this.dummyDatabase,
				this.dummyHMACSalt, providerMap);

		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);

		String validUserId = "533ee66842f639e95ce35e29";
		String validDateString = "Mon, 7 Apr 2014 11:21:13 BST";
		String validSessionId = "5AC7F3523043FB791DFF97DA81350D22";
		String validHMAC = "UEwiXcJvKskSf3jyuQCnNPrXwBU=";
		User returnUser = new User(validUserId, "TestFirstName",
				"TestLastName", "", Role.STUDENT, "", new Date(), Gender.MALE,
				new Date(), null, null);

		expect(request.getSession()).andReturn(dummySession).times(5);
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				validUserId).atLeastOnce();
		expect(dummySession.getAttribute(Constants.DATE_SIGNED)).andReturn(
				validDateString).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_ID)).andReturn(
				validSessionId).atLeastOnce();
		expect(dummySession.getAttribute(Constants.HMAC)).andReturn(validHMAC)
				.atLeastOnce();

		replay(dummySession);
		replay(request);

		expect(dummyDatabase.getById("533ee66842f639e95ce35e29")).andReturn(
				returnUser);
		replay(dummyDatabase);

		// Act
		User returnedUser = null;
		returnedUser = userManager.getCurrentUser(request);

		// Assert
		verify(dummyDatabase, dummySession, request);
		assertTrue(null != returnedUser && returnedUser instanceof User);
	}

	/**
	 * Test that requesting authentication with a bad provider behaves as expected.
	 */
	@Test
	public final void authenticate_badProviderGiven_givesServerErrorResponse() {
		// Arrange
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);
		UserManager userManager = new UserManager(this.dummyDatabase,
				this.dummyHMACSalt, providerMap);
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		String someInvalidProvider = "BAD_PROVIDER!!";
		Status expectedResponseCode = Status.BAD_REQUEST;

		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				null).atLeastOnce();
		dummySession.setAttribute(EasyMock.<String> anyObject(),
				EasyMock.<String> anyObject());
		expectLastCall().atLeastOnce();

		replay(dummySession);
		replay(request);
		replay(dummyDatabase);

		// Act
		Response r = userManager
				.authenticate(request, someInvalidProvider, "/");

		// Assert
		assertTrue(r.getStatus() == expectedResponseCode.getStatusCode());
		verify(dummyDatabase, dummySession, request);
	}

	/**
	 * Test that a valid OAuth provider (Google) provides a redirect response.
	 * 
	 * @throws IOException - test exception
	 */
	@Test
	public final void 
	authenticate_selectedValidOAuthProvider_providesRedirectResponseForAuthorization()
		throws IOException {
		// Arrange
		IOAuth2Authenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE,
				(IFederatedAuthenticator) dummyGoogleAuth);
		UserManager userManager = new UserManager(this.dummyDatabase,
				this.dummyHMACSalt, providerMap);

		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		String exampleRedirectUrl = "https://accounts.google.com/o/oauth2/auth?client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms";
		String someValidProviderString = "google";
		Status expectedResponseCode = Status.TEMPORARY_REDIRECT;

		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				null).atLeastOnce();

		dummySession.setAttribute(EasyMock.<String> anyObject(),
				EasyMock.<String> anyObject());
		expectLastCall().atLeastOnce();

		replay(dummySession);
		replay(request);
		replay(dummyDatabase);

		expect(dummyGoogleAuth.getAuthorizationUrl()).andReturn(
				exampleRedirectUrl);
		replay(dummyGoogleAuth);

		// Act
		Response r = userManager.authenticate(request, someValidProviderString,
				"/");

		// Assert
		verify(dummyDatabase, dummySession, request);
		assertTrue(r.getStatus() == expectedResponseCode.getStatusCode());
	}

	/**
	 * Check that a new (unseen) user is registered.
	 * 
	 * @throws IOException - test exceptions
	 * @throws CodeExchangeException - test exceptions
	 * @throws NoUserIdException - test exceptions
	 * @throws AuthenticatorSecurityException - test exceptions
	 */
	@Test
	public final void authenticateCallback_checkNewUserIsAuthenticated_registerUserWithSegue()
		throws IOException, CodeExchangeException, NoUserIdException,
			AuthenticatorSecurityException {
		// Arrange
		IOAuth2Authenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE,
				(IFederatedAuthenticator) dummyGoogleAuth);
		UserManager userManager = new UserManager(this.dummyDatabase,
				this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		HttpServletResponse response = createMock(HttpServletResponse.class);
		StringBuffer sb = new StringBuffer(
				"http://localhost:8080/rutherford-server/segue/api/auth/google/callback?state=googleh0317vhdvo5375tf55r8fqeit0&code=4/IuHuyvm3zNYMuqy5JS_pS4hiCsfv.YpQGR8XEqzIeYKs_1NgQtmVFQjZ5igI");
		String validQueryStringFromProvider = "client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms";
		String fullResponseUrlFromProvider = "http://localhost:8080/rutherford-server/segue/api/auth/google/callback?state=googleh0317vhdvo5375tf55r8fqeit0&code=4/IuHuyvm3zNYMuqy5JS_pS4hiCsfv.YpQGR8XEqzIeYKs_1NgQtmVFQjZ5igI?client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms";
		String authorizationCodeFromProviderUrl = "4/IuHuyvm3zNYMuqy5JS_pS4hiCsfv.YpQGR8XEqzIeYKs_1NgQtmVFQjZ5igI";
		String someProviderGeneratedLookupValue = "MYPROVIDERREF";
		String someProviderUniqueUserId = "GOOGLEUSER-1";
		String someSegueUserId = "533ee66842f639e95ce35e29";
		String validOAuthProvider = "google";

		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				null).atLeastOnce();
		expect(dummySession.getAttribute("auth_redirect")).andReturn("/")
				.atLeastOnce();

		// Mock CSRF checks
		expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME))
				.andReturn(CSRF_Test_VALUE).atLeastOnce();
		expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(
				CSRF_Test_VALUE).atLeastOnce();

		// Mock URL params extract stuff
		expect(request.getQueryString())
				.andReturn(validQueryStringFromProvider).atLeastOnce();

		expect(request.getRequestURL()).andReturn(sb);

		// Mock extract auth code call
		expect(dummyGoogleAuth.extractAuthCode(fullResponseUrlFromProvider))
				.andReturn(authorizationCodeFromProviderUrl);

		// Mock exchange code for token call
		expect(dummyGoogleAuth.exchangeCode(authorizationCodeFromProviderUrl))
				.andReturn(someProviderGeneratedLookupValue);

		expect(
				((IFederatedAuthenticator) dummyGoogleAuth)
						.getAuthenticationProvider()).andReturn(
				AuthenticationProvider.GOOGLE).atLeastOnce();

		// User object back from provider
		User providerUser = new User(someProviderUniqueUserId, "TestFirstName",
				"testLastName", "", Role.STUDENT, "", new Date(), Gender.MALE,
				new Date(), null, null);

		// Mock get User Information from provider call
		expect(
				((IFederatedAuthenticator) dummyGoogleAuth)
						.getUserInfo(someProviderGeneratedLookupValue))
				.andReturn(providerUser);

		// Expect this to be a new user and to register them (i.e. return null
		// from database)
		expect(
				dummyDatabase.getByLinkedAccount(AuthenticationProvider.GOOGLE,
						someProviderUniqueUserId)).andReturn(null);

		// A main part of the test is to check the below call happens
		expect(
				dummyDatabase
						.register(providerUser, AuthenticationProvider.GOOGLE,
								someProviderUniqueUserId)).andReturn(
				someSegueUserId).atLeastOnce();
		expect(dummyDatabase.getById(someSegueUserId)).andReturn(
				new User(someSegueUserId, "TestFirstName", "testLastName", "",
						Role.STUDENT, "", new Date(), Gender.MALE, new Date(),
						null, null));

		// Expect a session to be created
		dummySession.setAttribute(EasyMock.<String> anyObject(),
				EasyMock.<String> anyObject());
		expectLastCall().atLeastOnce();
		expect(dummySession.getId()).andReturn("sessionid").atLeastOnce();
		dummySession.removeAttribute(EasyMock.<String> anyObject());

		replay(dummySession);
		replay(request);
		replay(dummyGoogleAuth);
		replay(dummyDatabase);

		// Act
		Response r = userManager.authenticateCallback(request, response,
				validOAuthProvider);

		// Assert
		verify(dummyDatabase, dummySession, request, dummyGoogleAuth);
		assertTrue(r.getStatusInfo().equals(Status.TEMPORARY_REDIRECT));
	}

	/**
	 * Verify that a bad CSRF response from the authentication provider causes an error response.
	 * @throws IOException - test exceptions
	 * @throws CodeExchangeException - test exceptions
	 * @throws NoUserIdException - test exceptions
	 */
	@Test
	public final void authenticateCallback_checkInvalidCSRF_returnsUnauthorizedResponse()
		throws IOException, CodeExchangeException, NoUserIdException {
		// Arrange
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);

		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);
		UserManager userManager = new UserManager(this.dummyDatabase,
				this.dummyHMACSalt, providerMap);

		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		HttpServletResponse response = createMock(HttpServletResponse.class);
		String validQueryStringFromProvider = "client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms";
		String someInvalidCSRFValue = "FRAUDHASHAPPENED";
		String validOAuthProvider = "google";
		Status expectedResponseCode = Status.UNAUTHORIZED;

		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				null).atLeastOnce();

		// Mock URL params extract stuff
		expect(request.getQueryString())
				.andReturn(validQueryStringFromProvider).atLeastOnce();

		// Mock CSRF checks
		expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME))
				.andReturn(CSRF_Test_VALUE).atLeastOnce();
		expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(
				someInvalidCSRFValue).atLeastOnce();

		replay(dummySession);
		replay(request);
		replay(dummyGoogleAuth);
		replay(dummyDatabase);

		// Act
		Response r = userManager.authenticateCallback(request, response,
				validOAuthProvider);

		// Assert
		verify(dummyDatabase, dummySession, request, dummyGoogleAuth);
		assertTrue(r.getStatus() == expectedResponseCode.getStatusCode());
	}

	
	/**
	 * Verify that a bad (null) CSRF response from the authentication provider causes 
	 * an error response.
	 * @throws IOException - test exceptions
	 * @throws CodeExchangeException - test exceptions
	 * @throws NoUserIdException - test exceptions
	 */
	@Test
	public final void authenticateCallback_checkWhenNoCSRFProvided_respondWithUnauthorized()
		throws IOException, CodeExchangeException, NoUserIdException {
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);

		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap 
			= new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		// Setup object under test
		UserManager userManager = new UserManager(this.dummyDatabase,
				this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		HttpServletResponse response = createMock(HttpServletResponse.class);
		String queryStringFromProviderWithCSRFToken 
			= "client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com"
					+ "&redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&response_type=code"
					+ "&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email"
					+ "&state=googleomrdd07hbe6vc1efim5rnsgvms";
		String validOAuthProvider = "google";
		Status expectedResponseCode = Status.UNAUTHORIZED;

		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				null).atLeastOnce();

		// Mock URL params extract stuff
		expect(request.getQueryString()).andReturn(
				queryStringFromProviderWithCSRFToken).atLeastOnce();

		// Mock CSRF checks
		expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME))
				.andReturn(null).atLeastOnce();
		expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(
				CSRF_Test_VALUE).atLeastOnce();

		replay(dummySession);
		replay(request);
		replay(dummyGoogleAuth);
		replay(dummyDatabase);

		// Act
		Response r = userManager.authenticateCallback(request, response,
				validOAuthProvider);

		// Assert
		verify(dummyDatabase, dummySession, request, dummyGoogleAuth);
		assertTrue(r.getStatus() == expectedResponseCode.getStatusCode());
	}

	/**
	 * Verify that a correct HMAC response works correctly.
	 */
	@Test
	public final void validateUsersSession_checkForValidHMAC_shouldReturnAsCorrect() {
		// Arrange
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);
		UserManager userManager = new UserManager(this.dummyDatabase,
				this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);

		String validUserId = "533ee66842f639e95ce35e29";
		String validDateString = "Mon, 7 Apr 2014 11:21:13 BST";
		String validSessionId = "5AC7F3523043FB791DFF97DA81350D22";
		String validHMAC = "UEwiXcJvKskSf3jyuQCnNPrXwBU=";

		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				validUserId).atLeastOnce();
		expect(dummySession.getAttribute(Constants.DATE_SIGNED)).andReturn(
				validDateString).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_ID)).andReturn(
				validSessionId).atLeastOnce();
		expect(dummySession.getAttribute(Constants.HMAC)).andReturn(validHMAC)
				.atLeastOnce();

		replay(dummySession);
		replay(request);
		replay(dummyDatabase);

		// Act
		boolean valid = userManager.validateUsersSession(request);

		// Assert
		verify(dummyDatabase, dummySession, request);
		assertTrue(valid);
	}
	
	/**
	 * Verify that a bad user session is detected as invalid.
	 */
	@Test
	public final void validateUsersSession_badUsersSession_shouldReturnAsIncorrect() {
		// Arrange
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);
		UserManager userManager = new UserManager(this.dummyDatabase,
				this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);

		String validUserId = "533ee66842f639e95ce35e29";
		String validDateString = "Mon, 7 Apr 2014 11:21:13 BST";
		String validSessionId = "5AC7F3523043FB791DFF97DA81350D22";
		String someInvalidHMAC = "BAD HMAC";

		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				validUserId).atLeastOnce();
		expect(dummySession.getAttribute(Constants.DATE_SIGNED)).andReturn(
				validDateString).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_ID)).andReturn(
				validSessionId).atLeastOnce();
		expect(dummySession.getAttribute(Constants.HMAC)).andReturn(
				someInvalidHMAC).atLeastOnce();

		replay(dummySession);
		replay(request);
		replay(dummyDatabase);

		// Act
		boolean valid = userManager.validateUsersSession(request);

		// Assert
		verify(dummyDatabase, dummySession, request);
		assertTrue(!valid);
	}
}
