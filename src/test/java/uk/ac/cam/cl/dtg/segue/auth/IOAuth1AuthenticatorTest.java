package uk.ac.cam.cl.dtg.segue.auth;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;

/**
 * Test class for the facebook authenticator class.
 * 
 */
public abstract class IOAuth1AuthenticatorTest extends IOAuthAuthenticatorTest {
	protected IOAuth1Authenticator oauth1Authenticator;

	private final String someToken = "someToken";
	private final String someTokenSecret = "someTokenSecret";

	/**
	 * Verify that the authenticator returns a valid authorization URL.
	 * 
	 * @throws IOException
	 */
	@Test
	public final void getAuthorizationUrl_returnsNonNullUrl() throws IOException {
		String urlString = oauth1Authenticator
				.getAuthorizationUrl(new OAuth1Token(someToken, someTokenSecret));
		assertTrue(urlString != null);
		URL url = new URL(urlString);
		assertTrue(url.getAuthority() != null);
	}
}
