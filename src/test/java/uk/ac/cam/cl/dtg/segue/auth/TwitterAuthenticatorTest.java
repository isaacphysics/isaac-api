package uk.ac.cam.cl.dtg.segue.auth;

import org.junit.Before;

public class TwitterAuthenticatorTest extends IOAuth1AuthenticatorTest {
	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		this.oauth1Authenticator =
				new TwitterAuthenticator(clientId, clientSecret, callbackUri);
		this.authenticator = this.oauth1Authenticator;
	}
}
