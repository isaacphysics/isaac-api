/**
 * Copyright 2014 Nick Rogers
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
