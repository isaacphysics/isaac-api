/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
