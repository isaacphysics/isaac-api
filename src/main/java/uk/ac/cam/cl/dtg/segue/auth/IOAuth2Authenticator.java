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

/**
 * This interface defines the required methods for an oauth 2.0 provider.
 * 
 * @author sac92
 */
public interface IOAuth2Authenticator extends IOAuthAuthenticator {
	/**
	 * Step 1 of OAUTH2 - Request authorisation URL. Request an authorisation
	 * url which will allow the user to be logged in with their oauth provider.
	 * 
	 * @param antiForgeryStateToken
	 *            - A unique token to protect against CSRF attacks
	 * @return String - A url which should be fully formed and ready for the
	 *         user to login with - this should result in a callback to a
	 *         prearranged api endpoint if successful.
	 */
	String getAuthorizationUrl(final String antiForgeryStateToken);

	/**
	 * This method generates an anti CSRF token to be included in the
	 * authorisation step (step 1).
	 * 
	 * @return SecureRandom token.
	 */
	String getAntiForgeryStateToken();
}
