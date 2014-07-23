package uk.ac.cam.cl.dtg.segue.auth;

import org.junit.Before;

public class FacebookAuthenticatorTest extends IOAuth2AuthenticatorTest {
	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		this.oauth2Authenticator =
				new FacebookAuthenticator(clientId, clientSecret, callbackUri, requestedScopes);
		this.authenticator = this.oauth2Authenticator;
	}

}
