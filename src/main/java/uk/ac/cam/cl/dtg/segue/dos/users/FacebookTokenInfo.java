package uk.ac.cam.cl.dtg.segue.dos.users;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FacebookTokenInfo {
	private FacebookTokenData data;

	@JsonCreator
	public FacebookTokenInfo(@JsonProperty("data") final FacebookTokenData data) {
		this.data = data;
	}
	
	/**
	 * @return the data
	 */
	public FacebookTokenData getData() {
		return data;
	}

	/**
	 * @param data the data to set
	 */
	public void setData(FacebookTokenData data) {
		this.data = data;
	}

}
