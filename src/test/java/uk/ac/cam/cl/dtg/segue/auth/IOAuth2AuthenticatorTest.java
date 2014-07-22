package uk.ac.cam.cl.dtg.segue.auth;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import com.google.api.client.http.GenericUrl;

/**
 * Test class for the facebook authenticator class.
 * 
 */
public abstract class IOAuth2AuthenticatorTest {
	protected final String clientId = "someClientId";
	protected final String clientSecret = "someClientSecret";
	protected final String callbackUri = "someCallbackUri";
	protected final String requestedScopes = "requestedScopes";
	
	protected IOAuth2Authenticator authenticator;
	
	private final String someDomain = "http://www.somedomain.com/";
	private final String someAuthCode = "someAuthCode";
	
	/**
	 * Verify that the authenticator correctly identifies itself.
	 */
	@Test
	public final void getAuthenticationProvider_returnsNonNullProvider() {
		assertTrue(authenticator.getAuthenticationProvider() != null);
	}
	
	/**
	 * Verify that the authenticator returns a valid authorization URL.
	 * @throws IOException 
	 */
	@Test
	public final void getAuthorizationUrl_returnsNonNullUrl() throws IOException {
		String urlString = authenticator.getAuthorizationUrl();
		assertTrue(urlString != null);
		URL url = new URL(urlString);
		assertTrue(url.getAuthority() != null);
	}

	/**
	 * Verify that the extractAuthCode method returns the correct value.
	 * @throws IOException 
	 */
	@Test
	public final void extractAuthCode_givenValidUrl_returnsCorrectCode() throws IOException {
		GenericUrl url = new GenericUrl(someDomain);
		url.set("code", someAuthCode);
		String code = authenticator.extractAuthCode(url.build());
		assertTrue(code != null && code.equals(someAuthCode));
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
	 */
	@Test
	public final void getAntiForgeryStateToken_returnsNonNullString() {
		String token = authenticator.getAntiForgeryStateToken();
		assertTrue(token != null && token.length() > 0);
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
