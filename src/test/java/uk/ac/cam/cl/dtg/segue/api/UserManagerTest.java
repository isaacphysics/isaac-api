package uk.ac.cam.cl.dtg.segue.api;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import uk.ac.cam.cl.dtg.segue.api.UserManager.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.dao.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dto.User;

public class UserManagerTest {

	private IUserDataManager dummyDatabase;
	private String dummyHMACSalt;
	private Map<AuthenticationProvider, IFederatedAuthenticator> dummyProvidersMap;
	
	@Before
	public void setUp() throws Exception {
		this.dummyDatabase = EasyMock.createMock(IUserDataManager.class);
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
		GoogleAuthenticator dummyGoogleAuth = EasyMock.createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		// Setup object under test
		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = EasyMock.createMock(HttpSession.class);
		HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
		EasyMock.expect(request.getSession()).andReturn(dummySession);
		EasyMock.expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn(null);
		
		EasyMock.replay(dummySession);
		EasyMock.replay(request);

		// test method returns null when we can't find a session variable set for the user.
		assertTrue(userManager.getCurrentUser(request) == null);
	}
	
	/**
	 * 
	 * NOTE: if this unit test breaks it could be due to the HMAC SALT being changed on the local settings.
	 * 
	 */
	@Test
	public void testGetCurrentUserIsAuthenticatedValidHMAC() {
		// Object Setup		
		GoogleAuthenticator dummyGoogleAuth = EasyMock.createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		// Setup object under test
		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = EasyMock.createMock(HttpSession.class);
		HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
		
		EasyMock.expect(request.getSession()).andReturn(dummySession).times(5);
		EasyMock.expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn("533ee66842f639e95ce35e29").times(2);
		
		EasyMock.expect(dummySession.getAttribute(Constants.DATE_SIGNED)).andReturn("Mon, 7 Apr 2014 11:21:13 BST");

		EasyMock.expect(dummySession.getAttribute(Constants.SESSION_ID)).andReturn("5AC7F3523043FB791DFF97DA81350D22");
		EasyMock.expect(dummySession.getAttribute(Constants.HMAC)).andReturn("UEwiXcJvKskSf3jyuQCnNPrXwBU=");
		
		EasyMock.replay(dummySession);
		EasyMock.replay(request);
		
		User returnUser = new User("533ee66842f639e95ce35e29", "Test", "Test", "", "", "", "", false, new Date());
		
		EasyMock.expect(dummyDatabase.getById("533ee66842f639e95ce35e29")).andReturn(returnUser);
		EasyMock.replay(dummyDatabase);

		User resultingUser = null;
		resultingUser = userManager.getCurrentUser(request);
		
		assertTrue(null != resultingUser);
	}
	
	// Not logged in
	@Test
	public void testAuthenticate() {
//		// TODO
		
	}

	@Test
	public void testAuthenticateCallback() {
		// TODO
	}

	@Test
	public void testGetUserFromLinkedAccount() {
		assertTrue(true);
	}

	@Test
	public void testValidateUsersSessionSuccess() {
		// Object Setup		
		GoogleAuthenticator dummyGoogleAuth = EasyMock.createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		// Setup object under test
		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = EasyMock.createMock(HttpSession.class);
		HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
		
		EasyMock.expect(request.getSession()).andReturn(dummySession).times(5);
		EasyMock.expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn("533ee66842f639e95ce35e29").times(2);
		
		EasyMock.expect(dummySession.getAttribute(Constants.DATE_SIGNED)).andReturn("Mon, 7 Apr 2014 11:21:13 BST");

		EasyMock.expect(dummySession.getAttribute(Constants.SESSION_ID)).andReturn("5AC7F3523043FB791DFF97DA81350D22");
		EasyMock.expect(dummySession.getAttribute(Constants.HMAC)).andReturn("UEwiXcJvKskSf3jyuQCnNPrXwBU=");
		
		EasyMock.replay(dummySession);
		EasyMock.replay(request);
		
		User returnUser = new User("533ee66842f639e95ce35e29", "Test", "Test", "", "", "", "", false, new Date());
		
		EasyMock.expect(dummyDatabase.getById("533ee66842f639e95ce35e29")).andReturn(returnUser);
		EasyMock.replay(dummyDatabase);

		boolean valid = userManager.validateUsersSession(request);

		// this should be a valid hmac
		assertTrue(valid);
	}
	
	@Test
	public void testValidateBadUsersSessionFail() {
		// Object Setup		
		GoogleAuthenticator dummyGoogleAuth = EasyMock.createMock(GoogleAuthenticator.class);
		HashMap<AuthenticationProvider, IFederatedAuthenticator> providerMap = new HashMap<AuthenticationProvider, IFederatedAuthenticator>();
		providerMap.put(AuthenticationProvider.GOOGLE, dummyGoogleAuth);

		// Setup object under test
		UserManager userManager = new UserManager(this.dummyDatabase, this.dummyHMACSalt, providerMap);

		// method param setup for method under test
		HttpSession dummySession = EasyMock.createMock(HttpSession.class);
		HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
		
		EasyMock.expect(request.getSession()).andReturn(dummySession).times(5);
		EasyMock.expect(dummySession.getAttribute(Constants.SESSION_USER_ID)).andReturn("533ee66842f639e95ce35e29").times(2);
		
		EasyMock.expect(dummySession.getAttribute(Constants.DATE_SIGNED)).andReturn("Mon, 7 Apr 2014 11:21:13 BST");

		EasyMock.expect(dummySession.getAttribute(Constants.SESSION_ID)).andReturn("5AC7F3523043FB791DFF97DA81350D22");
		EasyMock.expect(dummySession.getAttribute(Constants.HMAC)).andReturn("BAD HMAC");
		
		EasyMock.replay(dummySession);
		EasyMock.replay(request);
		
		User returnUser = new User("533ee66842f639e95ce35e29", "Test", "Test", "", "", "", "", false, new Date());
		
		EasyMock.expect(dummyDatabase.getById("533ee66842f639e95ce35e29")).andReturn(returnUser);
		EasyMock.replay(dummyDatabase);

		// test
		boolean valid = userManager.validateUsersSession(request);

		// this should be a bad hmac. 
		assertTrue(!valid);
	}

}
