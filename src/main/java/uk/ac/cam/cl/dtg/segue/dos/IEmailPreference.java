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
 * Interface class for a user email preference.
 * @author Alistair Stead
 *
 */
public interface IEmailPreference {
	
	/**
	 * Enumerator for email preferences.
	 */
	public enum EmailPreference {
		NEWS_AND_UPDATES,
		EVENTS,
		ASSIGNMENTS;

		/**
		 * @param emailPreference - the representation of the email preference type
		 * @return EmailPreference - the  corresponding preference
		 */
		public static EmailPreference mapIntToPreference(final int emailPreference) {
			switch (emailPreference) {
				case 0:
					return NEWS_AND_UPDATES;
				case 1:
					return EVENTS;
				case 2:
					return ASSIGNMENTS;
				default:
					return null;
			}
		}

		/**
		 * @return the corresponding int representation of the email preference
		 */
		public int mapPreferenceToInt() {
			switch (this) {
				case NEWS_AND_UPDATES:
					return 0;
				case EVENTS:
					return 1;
				case ASSIGNMENTS:
					return 2;
				default:
					return -1;
			}
		}
	}
	
	/**
	 * @return the user's id
	 */
	String getUserId();
	
	/**
	 * @return the type of email preference
	 */
	EmailPreference getEmailPreference();
	
	/**
	 * @return the status of the email preference
	 */
	boolean getEmailPreferenceStatus();

}
