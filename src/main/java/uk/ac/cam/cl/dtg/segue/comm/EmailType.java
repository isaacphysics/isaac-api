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
package uk.ac.cam.cl.dtg.segue.comm;

/**
 * The types of email we can send. 
 *
 * @author Alistair Stead
 *
 */
public enum EmailType {
	ADMIN,
	SYSTEM,
	ASSIGNMENTS,
	NEWS_AND_UPDATES,
	EVENTS;
	
	/**
	 * @return integer representation of priority
	 */
	public int getPriority() {
		switch (this) {
			case ADMIN:
				return 0;
			case SYSTEM:
				return 1;
			case ASSIGNMENTS:
				return 2;
			case NEWS_AND_UPDATES:
				return 3;
			case EVENTS:
				return 4;
			default:
				return Integer.MAX_VALUE;
		}
	}
	
	/**
	 * @return boolean giving the validity of email type as email preference
	 */
	public boolean isValidEmailPreference() {
		switch (this) {
			case ADMIN:
				return false;
			case SYSTEM:
				return false;
			case ASSIGNMENTS:
				return true;
			case NEWS_AND_UPDATES:
				return true;
			case EVENTS:
				return true;
			default:
				return false;
		}
	}

	
}
