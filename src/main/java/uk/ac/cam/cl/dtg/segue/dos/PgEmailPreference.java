/**
 * Copyright 2015 Alistair Stead
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
package uk.ac.cam.cl.dtg.segue.dos;


/**
 * An email preference implemented with postgres.
 *
 * @author Alistair Stead
 *
 */
public class PgEmailPreference implements IEmailPreference {

	private final String userId;
	private final EmailPreference emailPreference;
	private boolean emailPreferenceStatus;
	
	
	/**
	 * An email preference implemented with postgres.
	 * @param userId - the id of the user in the database
	 * @param emailPreference - the preference type
	 * @param emailPreferenceStatus - the status of the preference
	 */
	public PgEmailPreference(final String userId, 
					final EmailPreference emailPreference, final boolean emailPreferenceStatus) {
		this.userId = userId;
		this.emailPreference = emailPreference;
		this.emailPreferenceStatus = emailPreferenceStatus;
	}
	
	/**
	 * @return the emailPreferenceStatus
	 */
	public boolean isEmailPreferenceStatus() {
		return emailPreferenceStatus;
	}


	/**
	 * @param emailPreferenceStatus the emailPreferenceStatus to set
	 */
	public void setEmailPreferenceStatus(final boolean emailPreferenceStatus) {
		this.emailPreferenceStatus = emailPreferenceStatus;
	}


	/**
	 * @return the userId
	 */
	@Override
	public String getUserId() {
		return userId;
	}


	/**
	 * @return the emailPreference
	 */
	@Override
	public EmailPreference getEmailPreference() {
		return emailPreference;
	}

	@Override
	public boolean getEmailPreferenceStatus() {
		return emailPreferenceStatus;
	}

	
}
