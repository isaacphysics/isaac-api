package uk.ac.cam.cl.dtg.segue.api;

import static org.junit.Assert.*;
import java.util.HashMap;
import java.util.Map;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import uk.ac.cam.cl.dtg.segue.api.UserManager.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.dao.IUserDataManager;

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
			new UserManager(null,this.dummyHMACSalt,this.dummyProvidersMap);
			fail("Expected a null pointer exception immediately");
		}
		catch(NullPointerException e){
			// fine
		}
		try{
			new UserManager(this.dummyDatabase,null,this.dummyProvidersMap);
			fail("Expected a null pointer exception immediately");
		}
		catch(NullPointerException e){
			// fine
		}
		try{
			new UserManager(this.dummyDatabase,this.dummyHMACSalt,null);
			fail("Expected a null pointer exception immediately");
		}
		catch(NullPointerException e){
			// fine
		}
	}
	
	@Test
	public void testAuthenticate() {
		// TODO
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
	public void testGetCurrentUser() {
		// TODO
	}

	@Test
	public void testValidateUsersSession() {
		// TODO
	}

}
