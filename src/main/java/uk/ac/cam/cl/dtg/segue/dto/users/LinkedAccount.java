package uk.ac.cam.cl.dtg.segue.dto.users;

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

	private String _id;
	private String localUserId;
	private AuthenticationProvider provider;
	private String providerUserId;

	public LinkedAccount() {

	}

	@JsonCreator
	public LinkedAccount(@JsonProperty("_id") String _id,
			@JsonProperty("localUserId") String localUserId,
			@JsonProperty("provider") AuthenticationProvider provider,
			@JsonProperty("providerId") String providerUserId) {
		this._id = _id;
		this.localUserId = localUserId;
		this.provider = provider;
		this.providerUserId = providerUserId;
	}

	@JsonProperty("_id")
	@ObjectId
	public String getId() {
		return _id;
	}

	@JsonProperty("_id")
	@ObjectId
	public void setId(String _id) {
		this._id = _id;
	}

	public AuthenticationProvider getProvider() {
		return provider;
	}

	public void setProvider(AuthenticationProvider provider) {
		this.provider = provider;
	}

	public String getProviderUserId() {
		return providerUserId;
	}

	public void setProviderUserId(String providerUserId) {
		this.providerUserId = providerUserId;
	}

	public String getLocalUserId() {
		return localUserId;
	}

	public void setLocalUserId(String localUserId) {
		this.localUserId = localUserId;
	}
}
