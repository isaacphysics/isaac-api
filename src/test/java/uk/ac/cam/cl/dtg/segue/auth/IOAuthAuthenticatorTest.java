package uk.ac.cam.cl.dtg.segue.auth;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

/**
 * Test class for the facebook authenticator class.
 * 
 */
public abstract class IOAuthAuthenticatorTest {
	protected final String clientId = "someClientId";
	protected final String clientSecret = "someClientSecret";
	protected final String callbackUri = "someCallbackUri";
	protected final String requestedScopes = "requestedScopes";
	protected final String someDomain = "http://www.somedomain.com/";
	protected final String someAuthCode = "someAuthCode";
	
	protected IOAuthAuthenticator authenticator;
	
	
	/**
	 * Verify that the authenticator correctly identifies itself.
	 */
	@Test
	public final void getAuthenticationProvider_returnsNonNullProvider() {
		assertTrue(authenticator.getAuthenticationProvider() != null);
	}


	/**
	 * Verify that the extractAuthCode method returns the correct value.
	 * @throws NoUserIdException 
	 * @throws IOException 
	 */
	@Test
	public final void exchangeCode_invalidToken_throwsException() throws IOException, NoUserIdException {
		try {
			authenticator.exchangeCode(someAuthCode);
			fail("Exception should have been thrown.");
		} catch (CodeExchangeException e) {
			// fine
		}
	}
	
	/**
	 * Verify that the getAntiForgeryStateToken returns some non-null non-empty string.
	 * @throws IOException 
	 * @throws NoUserIdException 
	 * @throws AuthenticatorSecurityException 
	 */
	@Test
	public final void getUserInfo_nullReference_throwsException() throws NoUserIdException, IOException, AuthenticatorSecurityException {
		try {
			authenticator.getUserInfo(null);
			fail("Exception should have been thrown.");
		} catch (NullPointerException e) {
			// fine
		}
	}

}
