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

import uk.ac.cam.cl.dtg.segue.comm.EmailType;


/**
 * An email preference implemented with postgres.
 *
 * @author Alistair Stead
 *
 */
public class PgEmailPreference implements IEmailPreference {

	private final long userId;
	private final EmailType emailType;
	private boolean emailPreferenceStatus;
	
	
	/**
	 * An email preference implemented with postgres.
	 * @param userId - the id of the user in the database
	 * @param emailType - the preference type
	 * @param emailPreferenceStatus - the status of the preference
	 */
	public PgEmailPreference(final long userId, 
					final EmailType emailType, final boolean emailPreferenceStatus) {
		this.userId = userId;
		this.emailType = emailType;
		this.emailPreferenceStatus = emailPreferenceStatus;
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
	public long getUserId() {
		return userId;
	}


	/**
	 * @return the emailPreference
	 */
	@Override
	public EmailType getEmailType() {
		return emailType;
	}

	@Override
	public boolean getEmailPreferenceStatus() {
		return emailPreferenceStatus;
	}

	
}
