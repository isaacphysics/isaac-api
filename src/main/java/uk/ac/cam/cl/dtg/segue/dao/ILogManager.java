/**
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.dao;

import javax.servlet.http.HttpServletRequest;

import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * Interface for logging components.
 * 
 */
public interface ILogManager {
	
	/**
	 * Log an event with the persistence logging framework by looking up the user from the httpRequest.
	 * 
	 * @param httpRequest
	 *            - so we can figure out the user who triggered the event.
	 * @param eventType
	 *            - Type of event that we are interested in.
	 * @param eventDetails
	 *            - Additional information associated with the event - this is
	 *            expected to be a json deserializable object
	 */
	void logEvent(HttpServletRequest httpRequest, String eventType, Object eventDetails);
	
	/**
	 * Log an event with the persistence logging framework without looking up the user from the database.
	 * 
	 * @param user - user to log must not be null.
	 * @param httpRequest
	 *            - so we can figure out request specific information e.g. ip address.
	 * @param eventType
	 *            - Type of event that we are interested in.
	 * @param eventDetails
	 *            - Additional information associated with the event - this is
	 *            expected to be a json deserializable object
	 */
	void logEvent(RegisteredUserDTO user, HttpServletRequest httpRequest, String eventType, Object eventDetails);
}
