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

import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;

/**
 * Interface for logging components.
 * 
 * This is for logging user interaction events for the purpose of research and /
 * or technology improvement not for logging system errors / events.
 * 
 */
public interface ILogManager {
	/**
	 * Log an event with the persistence logging framework without looking up
	 * the user from the database.
	 * 
	 * @param user
	 *            - user to log must not be null.
	 * @param httpRequest
	 *            - so we can figure out request specific information e.g. ip
	 *            address.
	 * @param eventType
	 *            - Type of event that we are interested in.
	 * @param eventDetails
	 *            - Additional information associated with the event - this is
	 *            expected to be a json deserializable object
	 */
	void logEvent(AbstractSegueUserDTO user, HttpServletRequest httpRequest, String eventType,
			Object eventDetails);

	/**
	 * Log an event with the persistence logging framework without looking up
	 * the user from the database.
	 * 
	 * @param user
	 *            - user to log must not be null.
	 * @param eventType
	 *            - Type of event that we are interested in.
	 * @param eventDetails
	 *            - Additional information associated with the event - this is
	 *            expected to be a json deserializable object
	 */
	void logInternalEvent(AbstractSegueUserDTO user, String eventType, Object eventDetails);

	/**
	 * This method will endeavour to find all log events for a given anonymous
	 * user and reassign ownership to a registered user.
	 * 
	 * This will also result in an event being created.
	 * 
	 * @param oldUserId
	 *            - the id of the old anonymous user
	 * @param newUserId
	 *            - the user object of the newly registered user.
	 */
	void transferLogEventsToNewRegisteredUser(final String oldUserId, final String newUserId);

	/**
	 * To enable some simple analytics we provide a way to query logs by event type.
	 * @param type - string representing the type of event to find.
	 * @return all events of the type requested or null if none available. The map should be of type String, Object
	 */
	List<HashMap> getLogsByType(String type);
}
