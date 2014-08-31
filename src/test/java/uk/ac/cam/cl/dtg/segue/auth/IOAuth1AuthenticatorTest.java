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
