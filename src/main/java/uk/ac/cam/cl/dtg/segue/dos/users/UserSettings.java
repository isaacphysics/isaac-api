/*
 * Copyright 2015 Alistair Stead, 2017 James Sharkey
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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A collection of user settings objects.
 *
 * @author Alistair Stead
 * @author James Sharkey
 *
 */
public class UserSettings {
	
	private RegisteredUser registeredUser;
	private String passwordCurrent;
	private Map<String, Map<String, Boolean>> userPreferences;

    /**
     * A collection of user settings objects.
     * @param registeredUser - the user data
     * @param userPreferences - a list of email preferences
     */
    @JsonCreator
    public UserSettings(@JsonProperty("registeredUser") final RegisteredUser registeredUser,
						@JsonProperty("userPreferences") final Map<String, Map<String, Boolean>> userPreferences) {
        this.registeredUser = registeredUser;
		this.userPreferences = userPreferences;
    }

	/**
	 * @return the passwordCurrent
	 */
	public String getPasswordCurrent() {
		return passwordCurrent;
	}

	/**
	 * @param passwordCurrent the passwordCurrent to set
	 */
	public void setPasswordCurrent(final String passwordCurrent) {
		this.passwordCurrent = passwordCurrent;
	}

	/**
     * @return a registered user object
     */
    @JsonProperty("registeredUser")
    public RegisteredUser getRegisteredUser() {
        return this.registeredUser;
    }
    /**
	 * @param registeredUser the registeredUser to set
	 */
	@JsonProperty("registeredUser")
	public void setRegisteredUser(final RegisteredUser registeredUser) {
		this.registeredUser = registeredUser;
	}

	/**
	 * @return a list of user preferences
	 */
	@JsonProperty("userPreferences")
	public Map<String, Map<String, Boolean>> getUserPreferences() {
		return this.userPreferences;
	}

	/**
	 * @param userPreferences the userPreferences to set
	 */
	@JsonProperty("userPreferences")
	public void setUserPreferences(final Map<String, Map<String, Boolean>> userPreferences) {
		this.userPreferences = userPreferences;
	}


}
