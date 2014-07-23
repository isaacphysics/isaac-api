package uk.ac.cam.cl.dtg.segue.auth;

import org.junit.Before;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;


public class GoogleAuthenticatorTest extends IOAuth2AuthenticatorTest {
	/**
	 * Initial configuration of tests.
	 * 
	 * @throws Exception
	 *             - test exception
	 */
	@Before
	public final void setUp() throws Exception {
		GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
		Details details = new Details();
		clientSecrets.setWeb(details);
		
		details.setClientId(clientId);
		details.setClientSecret(clientSecret);
        
		this.oauth2Authenticator =
				new GoogleAuthenticator(clientSecrets, callbackUri, requestedScopes);
		this.authenticator = this.oauth2Authenticator;
	}
}
