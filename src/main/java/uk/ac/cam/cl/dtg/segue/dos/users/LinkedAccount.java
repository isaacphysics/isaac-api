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
package uk.ac.cam.cl.dtg.segue.dos.users;

import org.mongojack.ObjectId;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Object to represent a user has linked their local account to an external 
 * authenticators account.
 *
 */
public class LinkedAccount {

	@JsonProperty("_id")
	private String id;
	private String localUserId;
	private AuthenticationProvider provider;
	private String providerUserId;
	
	/**
	 * Default constructor for linkedAccount. 
	 */
	public LinkedAccount() {

	}
	
	/**
	 * Linked Account Constructor.
	 * @param id - Database Id.
	 * @param localUserId - Local user id.
	 * @param provider - provider for authentication
	 * @param providerUserId - provider's user Id.
	 */
	@JsonCreator
	public LinkedAccount(@JsonProperty("_id") final String id,
			@JsonProperty("localUserId") final String localUserId,
			@JsonProperty("provider") final AuthenticationProvider provider,
			@JsonProperty("providerId") final String providerUserId) {
		this.id = id;
		this.localUserId = localUserId;
		this.provider = provider;
		this.providerUserId = providerUserId;
	}

	/**
	 * Get database Id.
	 * @return database id
	 */
	@JsonProperty("_id")
	@ObjectId
	public final String getId() {
		return id;
	}

	/**
	 * Set id method.
	 * @param id - database id
	 */
	@JsonProperty("_id")
	@ObjectId
	public final void setId(final String id) {
		this.id = id;
	}

	/**
	 * Gets the localUserId.
	 * @return the localUserId
	 */
	public final String getLocalUserId() {
		return localUserId;
	}

	/**
	 * Sets the localUserId.
	 * @param localUserId the localUserId to set
	 */
	public final void setLocalUserId(final String localUserId) {
		this.localUserId = localUserId;
	}

	/**
	 * Gets the provider.
	 * @return the provider
	 */
	public final AuthenticationProvider getProvider() {
		return provider;
	}

	/**
	 * Sets the provider.
	 * @param provider the provider to set
	 */
	public final void setProvider(final AuthenticationProvider provider) {
		this.provider = provider;
	}

	/**
	 * Gets the providerUserId.
	 * @return the providerUserId
	 */
	public final String getProviderUserId() {
		return providerUserId;
	}

	/**
	 * Sets the providerUserId.
	 * @param providerUserId the providerUserId to set
	 */
	public final void setProviderUserId(final String providerUserId) {
		this.providerUserId = providerUserId;
	}

}
