package uk.ac.cam.cl.dtg.segue.api;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.Response;

import static org.easymock.EasyMock.*;
import org.junit.Before;
import org.junit.Test;

import uk.ac.cam.cl.dtg.segue.api.UserManager.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.NoUserIdException;
import uk.ac.cam.cl.dtg.segue.dao.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dto.User;

public class UserManagerTest {

	private IUserDataManager dummyDatabase;
	private String dummyHMACSalt;
	private Map<AuthenticationProvider, IFederatedAuthenticator> dummyProvidersMap;
	private static final String CSRF_Test_VALUE = "googleomrdd07hbe6vc1efim5rnsgvms";
	
	@Before
	public void setUp() throws Exception {
		this.dummyDatabase = createMock(IUserDataManager.class);
		this.dummyHMACSalt = "BOB";
		this.dummyProvidersMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
	}
	
	@Test
	public void testConstructorForBadInput(){
		try{
			new UserManager(null, this.dummyHMACSalt, this.dummyProvidersMap);
			fail("Expected a null pointer exception immediately");
		}
		catch(NullPointerException e){
			// fine
		}
		try{
			new UserManager(this.dummyDatabase, null, this.dummyProvidersMap);
			fail("Expected a null pointer exception immediately");
		}
		catch(NullPointerException e){
			// fine
		}
		try{
			new UserManager(this.dummyDatabase, this.dummyHMACSalt,  null);
			fail("Expected a null pointer exception immediately");
		}
		catch(NullPointerException e){
			// fine
		}
	}
	
	// Not logged in
	@Test
	public void testGetCurrentUserNotLoggedIn() {
		// Object Setup		
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		// Setup object under test
		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		expect(request.getSession()).andReturn(dummySession);
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(null);
		
		replay(dummySession);
		replay(request);
		replay(dummyDatabase);
		
		// test method returns null when we can't find a session variable set for the user.
		assertTrue(userManager.getCurrentUser(request) == null);
		verify(dummyDatabase, dummySession, request);
	}
	
	/**
	 * 
	 * NOTE: if this unit test breaks it could be due to the HMAC SALT being changed on the local settings.
	 * 
	 */
	@Test
	public void testGetCurrentUserIsAuthenticatedValidHMAC() {
		// Object Setup		
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		// Setup object under test
		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		
		expect(request.getSession()).andReturn(dummySession).times(5);
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn("533ee66842f639e95ce35e29").atLeastOnce();
		expect(dummySession.getAttribute(Constants.DATE_SIGNED)).andReturn("Mon, 7 Apr 2014 11:21:13 BST").atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_ID)).andReturn("5AC7F3523043FB791DFF97DA81350D22").atLeastOnce();
		expect(dummySession.getAttribute(Constants.HMAC)).andReturn("UEwiXcJvKskSf3jyuQCnNPrXwBU=").atLeastOnce();
		
		replay(dummySession);
		replay(request);
		
		User returnUser = new User("533ee66842f639e95ce35e29", "Test", "Test", "", "", "", "", false, new Date());
		
		expect(dummyDatabase.getById("533ee66842f639e95ce35e29")).andReturn(returnUser);
		replay(dummyDatabase);

		User returnedUser = null;
		returnedUser = userManager.getCurrentUser(request);
		
		assertTrue(null != returnedUser);
		verify(dummyDatabase, dummySession, request);
	}
	
	// Not logged in
	@Test
	public void testAuthenticateWithNonNullBadProvider() {
//		// Object Setup		
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		// Setup object under test
		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		
		expect(request.getSession()).andReturn(dummySession);
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(null).atLeastOnce();
		
		replay(dummySession);
		replay(request);
		replay(dummyDatabase);
		
		Response r = userManager.authenticate(request, "BAD_PROVIDER!!");
		
		assertTrue(r.getStatus() == 500);
		verify(dummyDatabase, dummySession, request);
	}
	
	// Test things work...
	@Test
	public void testAuthenticateWithOAuthProvider() throws IOException {
//		// Object Setup		
//		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
//		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
//		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);
//
//		// Setup object under test
//		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);
//
//		// method param setup for method under test
//		HttpSession dummySession = createMock(HttpSession.class);
//		HttpServletRequest request = createMock(HttpServletRequest.class);
//		
//		expect(request.getSession()).andReturn(dummySession).times(2);
//		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(null).atLeastOnce();
//		
//		dummySession.setAttribute(<String>anyObject(), <String>anyObject());
//		expectLastCall().once();
//		
//		replay(dummySession);
//		replay(request);
//		replay(dummyDatabase);
//		
//		expect(dummyGoogleAuth.getAuthorizationUrl()).andReturn("https://accounts.google.com/o/oauth2/auth?client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms");
//		replay(dummyGoogleAuth);
//		
//		Response r = userManager.authenticate(request, "google");
//		assertTrue(r.getStatus() == 307);
//		verify(dummyDatabase, dummySession, request);
	}

	@Test
	public void testAuthenticateCallbackRegisterNewUser() throws IOException, CodeExchangeException, NoUserIdException {
//		StringBuffer sb = new StringBuffer("http://localhost:8080/rutherford-server/segue/api/auth/google/callback?state=googleh0317vhdvo5375tf55r8fqeit0&code=4/IuHuyvm3zNYMuqy5JS_pS4hiCsfv.YpQGR8XEqzIeYKs_1NgQtmVFQjZ5igI");
//		// TODO refactor to make it readable
//		
//		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
//		
//		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
//		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);
//
//		// Setup object under test
//		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);
//
//		// method param setup for method under test
//		HttpSession dummySession = createMock(HttpSession.class);
//		HttpServletRequest request = createMock(HttpServletRequest.class);
//		HttpServletResponse response = createMock(HttpServletResponse.class);
//		
//		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
//		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(null).atLeastOnce();
//
//		// Mock CSRF checks
//		expect(dummySession.getAttribute("state")).andReturn(CSRF_Test_VALUE).atLeastOnce();
//		expect(request.getParameter("state")).andReturn(CSRF_Test_VALUE).atLeastOnce();
//
//		// Mock URL params extract stuff
//		expect(request.getQueryString()).andReturn("client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms").atLeastOnce();
//
//		expect(request.getRequestURL()).andReturn(sb);
//		
//		// Mock extract auth code call
//		expect(dummyGoogleAuth.extractAuthCode("http://localhost:8080/rutherford-server/segue/api/auth/google/callback?state=googleh0317vhdvo5375tf55r8fqeit0&code=4/IuHuyvm3zNYMuqy5JS_pS4hiCsfv.YpQGR8XEqzIeYKs_1NgQtmVFQjZ5igI?client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms"))
//		.andReturn("4/IuHuyvm3zNYMuqy5JS_pS4hiCsfv.YpQGR8XEqzIeYKs_1NgQtmVFQjZ5igI");
//
//		// Mock exchange code for token call
//		expect(dummyGoogleAuth.exchangeCode("4/IuHuyvm3zNYMuqy5JS_pS4hiCsfv.YpQGR8XEqzIeYKs_1NgQtmVFQjZ5igI")).andReturn("MYPROVIDERREF");
//
//		// User object back from provider
//		User providerUser = new User("MYPROVIDERREF","Test","test","","","","", false, new Date());
//		
//		// Mock get User Information from provider call
//		expect(dummyGoogleAuth.getUserInfo("MYPROVIDERREF")).andReturn(providerUser);
//		
//		// Expect this to be a new user and to register them
//		expect(dummyDatabase.getByLinkedAccount(AuthenticationProvider.GOOGLE, "MYPROVIDERREF")).andReturn(null);
//
//		// A main part of the test is to check the below call happens
//		expect(dummyDatabase.register(providerUser, AuthenticationProvider.GOOGLE, "MYPROVIDERREF")).andReturn("New User").atLeastOnce();
//		expect(dummyDatabase.getById("New User")).andReturn(new User("LocalRef","Test","test","","","","", false, new Date()));
//		
//		// Expect a session to be created
//		dummySession.setAttribute(<String>anyObject(), <String>anyObject());
//		expectLastCall().atLeastOnce();
//		expect(dummySession.getId()).andReturn("sessionid").atLeastOnce();
//
//		replay(dummySession);
//		replay(request);
//		replay(dummyGoogleAuth);
//		replay(dummyDatabase);
//		
//		Response r = userManager.authenticateCallback(request, response, "google");
//		assertTrue(r.getEntity() instanceof User);
//		verify(dummyDatabase, dummySession, request, dummyGoogleAuth);
	}
	
	@Test
	public void testAuthenticateCallbackBadCSRF() throws IOException, CodeExchangeException, NoUserIdException {
//		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
//		
//		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
//		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);
//
//		// Setup object under test
//		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);
//
//		// method param setup for method under test
//		HttpSession dummySession = createMock(HttpSession.class);
//		HttpServletRequest request = createMock(HttpServletRequest.class);
//		HttpServletResponse response = createMock(HttpServletResponse.class);
//		
//		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
//		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(null).atLeastOnce();
//
//		// Mock URL params extract stuff
//		expect(request.getQueryString()).andReturn("client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms").atLeastOnce();
//
//		// Mock CSRF checks
//		expect(dummySession.getAttribute("state")).andReturn(CSRF_Test_VALUE).atLeastOnce();
//		expect(request.getParameter("state")).andReturn("FRAUDHASHAPPENED").atLeastOnce();
//		
//		replay(dummySession);
//		replay(request);
//		replay(dummyGoogleAuth);
//		replay(dummyDatabase);
//		
//		Response r = userManager.authenticateCallback(request, response, "google");
//		assertTrue(r.getStatus() == 401);
//		verify(dummyDatabase, dummySession, request, dummyGoogleAuth);
	}
	
	@Test
	public void testAuthenticateCallbackNoCSRF() throws IOException, CodeExchangeException, NoUserIdException {
//		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
//		
//		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
//		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);
//
//		// Setup object under test
//		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);
//
//		// method param setup for method under test
//		HttpSession dummySession = createMock(HttpSession.class);
//		HttpServletRequest request = createMock(HttpServletRequest.class);
//		HttpServletResponse response = createMock(HttpServletResponse.class);
//		
//		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
//		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(null).atLeastOnce();
//
//		// Mock URL params extract stuff
//		expect(request.getQueryString()).andReturn("client_id=267566420063-jalcbiffcpmteh42cib5hmgb16upspc0.apps.googleusercontent.com&redirect_uri=http://localhost:8080/rutherford-server/segue/api/auth/google/callback&response_type=code&scope=https://www.googleapis.com/auth/userinfo.profile%20https://www.googleapis.com/auth/userinfo.email&state=googleomrdd07hbe6vc1efim5rnsgvms").atLeastOnce();
//
//		// Mock CSRF checks
//		expect(dummySession.getAttribute("state")).andReturn(null).atLeastOnce();
//		expect(request.getParameter("state")).andReturn(CSRF_Test_VALUE).atLeastOnce();
//		
//		replay(dummySession);
//		replay(request);
//		replay(dummyGoogleAuth);
//		replay(dummyDatabase);
//		
//		Response r = userManager.authenticateCallback(request, response, "google");
//		assertTrue(r.getStatus() == 401);
//		verify(dummyDatabase, dummySession, request, dummyGoogleAuth);
	}

	@Test
	public void testValidateUsersSessionSuccess() {
		// Object Setup		
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		// Setup object under test
		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		
		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn("533ee66842f639e95ce35e29").atLeastOnce();
		expect(dummySession.getAttribute(Constants.DATE_SIGNED)).andReturn("Mon, 7 Apr 2014 11:21:13 BST").atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_ID)).andReturn("5AC7F3523043FB791DFF97DA81350D22").atLeastOnce();
		expect(dummySession.getAttribute(Constants.HMAC)).andReturn("UEwiXcJvKskSf3jyuQCnNPrXwBU=").atLeastOnce();
		
		replay(dummySession);
		replay(request);
		replay(dummyDatabase);

		boolean valid = userManager.validateUsersSession(request);

		// this should be a valid hmac
		assertTrue(valid);
		verify(dummyDatabase, dummySession, request);
	}
	
	@Test
	public void testValidateBadUsersSessionFail() {
		// Object Setup		
		GoogleAuthenticator dummyGoogleAuth = createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		// Setup object under test
		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = createMock(HttpSession.class);
		HttpServletRequest request = createMock(HttpServletRequest.class);
		
		expect(request.getSession()).andReturn(dummySession).atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn("533ee66842f639e95ce35e29").atLeastOnce();
		expect(dummySession.getAttribute(Constants.DATE_SIGNED)).andReturn("Mon, 7 Apr 2014 11:21:13 BST").atLeastOnce();
		expect(dummySession.getAttribute(Constants.SESSION_ID)).andReturn("5AC7F3523043FB791DFF97DA81350D22").atLeastOnce();
		expect(dummySession.getAttribute(Constants.HMAC)).andReturn("BAD HMAC").atLeastOnce();
		
		replay(dummySession);
		replay(request);
		replay(dummyDatabase);

		// test
		boolean valid = userManager.validateUsersSession(request);

		// this should be a bad hmac. 
		assertTrue(!valid);
		verify(dummyDatabase, dummySession, request);
	}
}
