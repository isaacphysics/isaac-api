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
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;

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
	 * @throws NoUserException 
	 * @throws IOException 
	 */
	@Test
	public final void exchangeCode_invalidToken_throwsException() throws IOException, NoUserException {
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
	 * @throws NoUserException 
	 * @throws AuthenticatorSecurityException 
	 */
	@Test
	public final void getUserInfo_nullReference_throwsException() throws NoUserException, IOException, AuthenticatorSecurityException {
		try {
			authenticator.getUserInfo(null);
			fail("Exception should have been thrown.");
		} catch (NullPointerException e) {
			// fine
		}
	}

}
