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
	 * @param emailType the integer representation of email type.
	 * @return the type of email corresponding to the integer 
	 */
	public static EmailType mapIntToPreference(final int emailType) {
		switch (emailType) {
			case 0:
				return ADMIN;
			case 1:
				return SYSTEM;
			case 2:
				return ASSIGNMENTS;
			case 3:
				return NEWS_AND_UPDATES;
			case 4:
				return EVENTS;
			default:
				return null;
		}
	}
	
	/**
	 * @return the integer representation of email type.
	 */
	public int mapEmailTypeToInt() {
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
				return -1;
		}
	}
	
	/**
	 * @return integer representation of priority
	 */
	public int getPriority(){
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
	
}
