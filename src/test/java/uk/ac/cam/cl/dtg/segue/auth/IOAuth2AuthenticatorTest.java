package uk.ac.cam.cl.dtg.segue.auth;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import com.google.api.client.http.GenericUrl;

/**
 * Test class for the facebook authenticator class.
 * 
 */
public abstract class IOAuth2AuthenticatorTest extends IOAuthAuthenticatorTest {
	protected IOAuth2Authenticator oauth2Authenticator;
	private final String someAntiForgeryToken = "someAntiForgeryToken";
	
	/**
	 * Verify that the authenticator returns a valid authorization URL.
	 * @throws IOException 
	 */
	@Test
	public final void getAuthorizationUrl_returnsNonNullUrl() throws IOException {
		String urlString = oauth2Authenticator.getAuthorizationUrl(someAntiForgeryToken);
		assertTrue(urlString != null);
		URL url = new URL(urlString);
		assertTrue(url.getAuthority() != null);
	}

	/**
	 * Verify that the getAntiForgeryStateToken returns some non-null non-empty string.
	 */
	@Test
	public final void getAntiForgeryStateToken_returnsNonNullString() {
		String token = oauth2Authenticator.getAntiForgeryStateToken();
		assertTrue(token != null && token.length() > 0);
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
}
