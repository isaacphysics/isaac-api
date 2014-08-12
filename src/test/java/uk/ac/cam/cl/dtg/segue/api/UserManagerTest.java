package uk.ac.cam.cl.dtg.segue.api;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import static org.easymock.EasyMock.*;
import ma.glasnost.orika.MapperFacade;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.FacebookAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuth2Authenticator;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.comm.ICommunicator;
import uk.ac.cam.cl.dtg.segue.dao.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.User;
import uk.ac.cam.cl.dtg.segue.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.dto.users.UserDTO;

/**
 * Test class for the user manager class.
 * 
 */
public class UserManagerTest {

	private IUserDataManager dummyDatabase;
	private String dummyHMACSalt;
	private Map<AuthenticationProvider, IAuthenticator> dummyProvidersMap;
	private String dummyHostName;
	private static final String CSRF_TEST_VALUE = "CSRFTESTVALUE";

	private MapperFacade dummyMapper;
	private ICommunicator dummyCommunicator;
	
	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		this.dummyDatabase = createMock(IUserDataManager.class);
		this.dummyHMACSalt = "BOB";
		this.dummyProvidersMap = new HashMap<AuthenticationProvider, IAuthenticator>();
		this.dummyHostName = "bob";
		this.dummyMapper = createMock(MapperFacade.class);
		this.dummyCommunicator = createMock(ICommunicator.class);
	}

	/**
	 * Verify that the constructor responds correctly to bad input.
	 */
	@Test
	public final void userManager_checkConstructorForBadInput_exceptionsShouldBeThrown() {
		try {
			new UserManager(null, this.dummyHMACSalt, this.dummyProvidersMap, this.dummyMapper,
					this.dummyHostName, this.dummyCommunicator);
			fail("Expected a null pointer exception immediately");
		} catch (NullPointerException e) {
			// fine
		}
		try {
			new UserManager(this.dummyDatabase, null, this.dummyProvidersMap, this.dummyMapper,
					this.dummyHostName, this.dummyCommunicator);
			fail("Expected a null pointer exception immediately");
		} catch (NullPointerException e) {
			// fine
		}
		try {
			new UserManager(this.dummyDatabase, this.dummyHMACSalt, null, this.dummyMapper,
					this.dummyHostName, this.dummyCommunicator);
			fail("Expected a null pointer exception immediately");
		} catch (NullPointerException e) {
			// fine
		}
	}

	/**
	 * Test that the get current user method behaves correctly when not logged
	 * in.
	 */
	@Test
	public final void getCurrentUser_isNotLoggedIn_noUserObjectReturned() {
		UserManager userManager = buildTestUserManager();

		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		expect(request.getSession()).andReturn(dummySession);
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				null);

		replay(dummySession);
		replay(request);
		replay(dummyDatabase);

		// Act
		UserDTO u = userManager.getCurrentUser(request);

		// Assert
		assertTrue(null == u);
		verify(dummyDatabase, dummySession, request);
	}

	/**
	 * Test that get current user with valid HMAC works correctly.
	 */
	@Test
	public final void getCurrentUser_IsAuthenticatedWithValidHMAC_userIsReturned() {
		UserManager userManager = buildTestUserManager();

		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);

		String validUserId = "533ee66842f639e95ce35e29";
		String validDateString = "Mon, 7 Apr 2014 11:21:13 BST";
		String validSessionId = "5AC7F3523043FB791DFF97DA81350D22";
		String validHMAC = "UEwiXcJvKskSf3jyuQCnNPrXwBU=";
		User returnUser = new User(validUserId, "TestFirstName",
				"TestLastName", "", Role.STUDENT, new Date(), Gender.MALE,
				new Date(), null, null, null, null);

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
		expect(dummyDatabase.getAuthenticationProvidersByUser(returnUser)).andReturn(
				Arrays.asList(AuthenticationProvider.GOOGLE));
		replay(dummyDatabase);
		
		expect(dummyMapper.map(returnUser, UserDTO.class)).andReturn(new UserDTO()).atLeastOnce();
		replay(dummyMapper);
		// Act
		userManager.getCurrentUser(request);

		// Assert
		verify(dummyDatabase, dummySession, request, dummyMapper);
	}

	/**
	 * Test that requesting authentication with a bad provider behaves as
	 * expected.
	 */
	@Test
	public final void authenticate_badProviderGiven_givesServerErrorResponse() {
		UserManager userManager = buildTestUserManager();

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
	 * Test that a valid OAuth provider (Facebook) provides a redirect response.
	 * 
	 * @throws IOException
	 *             - test exception
	 */
	@Test
	public final void authenticate_selectedValidOAuthProvider_providesRedirectResponseForAuthorization()
			throws IOException {
		// Arrange
		IOAuth2Authenticator dummyAuth = createMock(IOAuth2Authenticator.class);
		UserManager userManager = buildTestUserManager(
				AuthenticationProvider.TEST, dummyAuth);

		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		String exampleRedirectUrl = "https://accounts.google.com/o/oauth2/auth?client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms";
		String someValidProviderString = "test";
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

		String someAntiForgeryToken = "someAntiForgeryToken";
		expect(dummyAuth.getAntiForgeryStateToken()).andReturn(someAntiForgeryToken).once();
		expect(dummyAuth.getAuthorizationUrl(someAntiForgeryToken)).andReturn(exampleRedirectUrl).once();
		replay(dummyAuth);

		// Act
		Response r = userManager.authenticate(request, someValidProviderString,
				"/");

		// Assert
		verify(dummyDatabase, dummySession, request);
		assertTrue(r.getEntity().toString().equals(exampleRedirectUrl));
		assertTrue(r.getStatus() == expectedResponseCode.getStatusCode());
	}

	/**
	 * Check that a new (unseen) user is registered.
	 * 
	 * @throws IOException
	 *             - test exceptions
	 * @throws CodeExchangeException
	 *             - test exceptions
	 * @throws NoUserException
	 *             - test exceptions
	 * @throws AuthenticatorSecurityException
	 *             - test exceptions
	 */
	@Test
	public final void authenticateCallback_checkNewUserIsAuthenticated_registerUserWithSegue()
			throws IOException, CodeExchangeException, NoUserException,
			AuthenticatorSecurityException {
		IOAuth2Authenticator dummyAuth = createMock(FacebookAuthenticator.class);
		UserManager userManager = buildTestUserManager(
				AuthenticationProvider.TEST, dummyAuth);

		// method param setup for method under test
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);

		// TODO: What do these strings actually need to be?
		String someDomain = "http://www.somedomain.com/";
		String someClientId = "someClientId";
		String someAuthCode = "someAuthCode";
		String someState = "someState";

		StringBuffer sb = new StringBuffer(someDomain + "?state=" + someState
				+ "&code=" + someAuthCode);
		String validQueryStringFromProvider = "client_id=" + someClientId
				+ "&redirect_uri=" + someDomain;
		String fullResponseUrlFromProvider = someDomain + "?state=" + someState
				+ "&code=" + someAuthCode + "?client_id=" + someClientId
				+ "&redirect_uri=" + someDomain;
		String someProviderGeneratedLookupValue = "MYPROVIDERREF";
		String someProviderUniqueUserId = "USER-1";
		String someSegueUserId = "533ee66842f639e95ce35e29";
		String validOAuthProvider = "test";

		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				null).atLeastOnce();
		expect(dummySession.getAttribute("auth_redirect")).andReturn("/")
				.atLeastOnce();

		// Mock CSRF checks
		expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME))
				.andReturn(CSRF_TEST_VALUE).atLeastOnce();
		expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(
				CSRF_TEST_VALUE).atLeastOnce();

		// Mock URL params extract stuff
		expect(request.getQueryString())
				.andReturn(validQueryStringFromProvider).atLeastOnce();

		expect(request.getRequestURL()).andReturn(sb);

		// Mock extract auth code call
		expect(dummyAuth.extractAuthCode(fullResponseUrlFromProvider))
				.andReturn(someAuthCode);

		// Mock exchange code for token call
		expect(dummyAuth.exchangeCode(someAuthCode)).andReturn(
				someProviderGeneratedLookupValue);

		expect(
				((IFederatedAuthenticator) dummyAuth)
						.getAuthenticationProvider()).andReturn(
				AuthenticationProvider.TEST).atLeastOnce();

		// User object back from provider
		UserFromAuthProvider providerUser = new UserFromAuthProvider(someProviderUniqueUserId, "TestFirstName",
				"TestLastName", "", Role.STUDENT, new Date(), Gender.MALE);


		// Mock get User Information from provider call
		expect(
				((IFederatedAuthenticator) dummyAuth)
						.getUserInfo(someProviderGeneratedLookupValue))
				.andReturn(providerUser);

		// Expect this to be a new user and to register them (i.e. return null
		// from database)
		expect(
				dummyDatabase.getByLinkedAccount(AuthenticationProvider.TEST,
						someProviderUniqueUserId)).andReturn(null);
		
		User mappedUser = new User(null, "TestFirstName", "testLastName", "",
				Role.STUDENT, new Date(), Gender.MALE, new Date(),
				null, null, null, null);
		
		expect(dummyMapper.map(providerUser, User.class)).andReturn(mappedUser).atLeastOnce();
		replay(dummyMapper);
		
		// A main part of the test is to check the below call happens
		expect(
				dummyDatabase.registerNewUserWithProvider(mappedUser,
						AuthenticationProvider.TEST, someProviderUniqueUserId))
				.andReturn(someSegueUserId).atLeastOnce();
		
		mappedUser.setDatabaseId(someSegueUserId);
		
		expect(dummyDatabase.getById(someSegueUserId)).andReturn(mappedUser);

		// Expect a session to be created
		dummySession.setAttribute(EasyMock.<String> anyObject(),
				EasyMock.<String> anyObject());
		expectLastCall().atLeastOnce();
		expect(dummySession.getId()).andReturn("sessionid").atLeastOnce();
		dummySession.removeAttribute(EasyMock.<String> anyObject());

		replay(dummySession, request, dummyAuth, dummyDatabase);

		// Act
		Response r = userManager.authenticateCallback(request,
				validOAuthProvider);

		// Assert
		verify(dummySession, request, dummyAuth, dummyDatabase);
		assertTrue(r.getStatusInfo().equals(Status.TEMPORARY_REDIRECT));
	}

	/**
	 * Verify that a bad CSRF response from the authentication provider causes
	 * an error response.
	 * 
	 * @throws IOException
	 *             - test exceptions
	 * @throws CodeExchangeException
	 *             - test exceptions
	 * @throws NoUserException
	 *             - test exceptions
	 */
	@Test
	public final void authenticateCallback_checkInvalidCSRF_returnsUnauthorizedResponse()
			throws IOException, CodeExchangeException, NoUserException {
		UserManager userManager = buildTestUserManager();

		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		String someInvalidCSRFValue = "FRAUDHASHAPPENED";
		String validOAuthProvider = "test";
		Status expectedResponseCode = Status.UNAUTHORIZED;

		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				null).atLeastOnce();

		// Mock URL params extract stuff
		// Return any non-null string
		String queryString = Constants.STATE_PARAM_NAME + "="
				+ someInvalidCSRFValue;
		expect(request.getQueryString()).andReturn(queryString).once();

		// Mock CSRF checks
		expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME))
				.andReturn(CSRF_TEST_VALUE).atLeastOnce();
		expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(
				someInvalidCSRFValue).atLeastOnce();

		replay(dummySession, request, dummyDatabase);

		// Act
		Response r = userManager.authenticateCallback(request,
				validOAuthProvider);

		// Assert
		verify(dummyDatabase, dummySession, request);
		assertTrue(r.getStatus() == expectedResponseCode.getStatusCode());
	}

	/**
	 * Verify that a bad (null) CSRF response from the authentication provider
	 * causes an error response.
	 * 
	 * @throws IOException
	 *             - test exceptions
	 * @throws CodeExchangeException
	 *             - test exceptions
	 * @throws NoUserException
	 *             - test exceptions
	 */
	@Test
	public final void authenticateCallback_checkWhenNoCSRFProvided_respondWithUnauthorized()
			throws IOException, CodeExchangeException, NoUserException {
		UserManager userManager = buildTestUserManager();

		// method param setup for method under test
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		String queryStringFromProviderWithCSRFToken = "state="
				+ CSRF_TEST_VALUE;
		String validOAuthProvider = "test";
		Status expectedResponseCode = Status.UNAUTHORIZED;

		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(
				null).atLeastOnce();

		// Mock URL params extract stuff
		expect(request.getQueryString()).andReturn("").atLeastOnce();

		// Mock CSRF checks
		expect(dummySession.getAttribute(Constants.STATE_PARAM_NAME))
				.andReturn(null).atLeastOnce();
		expect(request.getParameter(Constants.STATE_PARAM_NAME)).andReturn(
				CSRF_TEST_VALUE).atLeastOnce();

		replay(dummySession);
		replay(request);
		replay(dummyDatabase);

		// Act
		Response r = userManager.authenticateCallback(request,
				validOAuthProvider);

		// Assert
		verify(dummyDatabase, dummySession, request);
		assertTrue(r.getStatus() == expectedResponseCode.getStatusCode());
	}

	/**
	 * Verify that a correct HMAC response works correctly.
	 */
	@Test
	public final void validateUsersSession_checkForValidHMAC_shouldReturnAsCorrect() {
		UserManager userManager = buildTestUserManager();

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
		UserManager userManager = buildTestUserManager();

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

	/**
	 * Helper method to construct a UserManager with the default TEST provider.
	 * 
	 * @return A new UserManager instance
	 */
	private UserManager buildTestUserManager() {
		return buildTestUserManager(AuthenticationProvider.TEST,
				createMock(IOAuth2Authenticator.class));
	}

	/**
	 * Helper method to construct a UserManager with the specified providers.
	 * 
	 * @param provider
	 *            - The provider to register
	 * @param authenticator
	 *            - The associated authenticating engine
	 * @return A new UserManager instance
	 */
	private UserManager buildTestUserManager(final AuthenticationProvider provider,
			final IFederatedAuthenticator authenticator) {
		HashMap<AuthenticationProvider, IAuthenticator> providerMap = new HashMap<AuthenticationProvider, IAuthenticator>();
		providerMap.put(provider, authenticator);
		return new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap, this.dummyMapper,
				this.dummyHostName, this.dummyCommunicator);
	}
}
