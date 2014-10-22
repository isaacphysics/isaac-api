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

import java.io.IOException;

import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dos.users.UserFromAuthProvider;

/**
 * Classes that implement this interface must provide some way (via another
 * interface) of allowing a user to be authenticated. The result of this
 * authentication should be some unique reference that when provided to this
 * class will allow us to extract user information from the external provider.
 * 
 * The exact specification of the unique reference number is up to the
 * implementing class as long as it can be used to gain information about a
 * specific user.
 * 
 */
public interface IFederatedAuthenticator extends IAuthenticator {

	/**
	 * Send a request to the Provider's API to retrieve the user's information.
	 * 
	 * @param internalProviderReference
	 *            reference code that allows the authenticator to identify the
	 *            user (e.g. OAuth implementations will map this code to
	 *            external tokens to allow api calls to be made).
	 * @return User's information contained in a User DTO.
	 * @throws NoUserException 
	 * @throws IOException 
	 * @throws AuthenticatorSecurityException 
	 */
	UserFromAuthProvider getUserInfo(String internalProviderReference) throws NoUserException, IOException,
			AuthenticatorSecurityException;
}