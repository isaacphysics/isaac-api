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
package uk.ac.cam.cl.dtg.segue.dos.users;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * FacebookTokenData.
 *
 */
public class FacebookTokenData {
	private String appId;
	private boolean isValid;

	/**
	 * 
	 * @param appId - application id
	 * @param isValid - 
	 */
	@JsonCreator
	public FacebookTokenData(@JsonProperty("app_id") final String appId,
			@JsonProperty("is_valid") final boolean isValid) {
		this.appId = appId;
		this.isValid = isValid;
	}

	/**
	 * @return the appId
	 */
	public String getAppId() {
		return appId;
	}

	/**
	 * @param appId the appId to set
	 */
	public void setAppId(final String appId) {
		this.appId = appId;
	}

	/**
	 * @return the isValid
	 */
	public boolean isValid() {
		return isValid;
	}

	/**
	 * @param isValid the isValid to set
	 */
	public void setValid(final boolean isValid) {
		this.isValid = isValid;
	}
}
