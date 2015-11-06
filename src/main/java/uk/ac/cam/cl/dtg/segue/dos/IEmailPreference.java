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
 * Interface class for a user email preference.
 * @author Alistair Stead
 *
 */
public interface IEmailPreference {
	
	/**
	 * @return the user's id
	 */
	long getUserId();
	
	/**
	 * @return the type of email preference
	 */
	EmailType getEmailType();
	
	/**
	 * @return the status of the email preference
	 */
	boolean getEmailPreferenceStatus();

}
