/*
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
 * Parent interface for all authenticators both federated and local in the Segue
 * CMS.
 * 
 */
public interface IAuthenticator {
	
	/**
	 * Returns a string representation of the provider's name, which can be used to uniquely identify the provider.
	 * 
	 * @return AuthenticationProvider
	 */
	AuthenticationProvider getAuthenticationProvider();

	/**
	 * Returns a human-readable string representation of the provider's name, which can be used in user-facing messages.
	 *
	 * @return The provider's friendly name.
	 */
	String getFriendlyName();
}
