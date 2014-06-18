package uk.ac.cam.cl.dtg.segue.dto.users;

import org.mongojack.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import uk.ac.cam.cl.dtg.segue.api.UserManager.AuthenticationProvider;

public class LinkedAccount {
	
	private String _id;
	private String localUserId;
	private AuthenticationProvider provider;
	private String providerId;

	public LinkedAccount(){
		
	}
	
	@JsonCreator
	public LinkedAccount(@JsonProperty("_id")String _id,
			@JsonProperty("localUserId")String localUserId, 
			@JsonProperty("provider") AuthenticationProvider provider, 
			@JsonProperty("providerId") String providerId){
		this._id = _id;
		this.localUserId = localUserId;
		this.provider = provider;
		this.providerId = providerId;
	}

	@JsonProperty("_id")
	@ObjectId
	public String get_id() {
		return _id;
	}

	@JsonProperty("_id")
	@ObjectId
	public void set_id(String _id) {
		this._id = _id;
	}
	
	public AuthenticationProvider getProvider() {
		return provider;
	}

	public void setProvider(AuthenticationProvider provider) {
		this.provider = provider;
	}


	public String getProviderId() {
		return providerId;
	}


	public void setProviderID(String providerId) {
		this.providerId = providerId;
	}


	public String getLocalUserId() {
		return localUserId;
	}

	public void setLocalUserId(String localUserId) {
		this.localUserId = localUserId;
	}	
}
