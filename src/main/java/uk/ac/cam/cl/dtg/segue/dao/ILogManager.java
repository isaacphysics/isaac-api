package uk.ac.cam.cl.dtg.segue.dao;

import javax.servlet.http.HttpServletRequest;

import uk.ac.cam.cl.dtg.segue.dto.users.UserDTO;

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
	void logEvent(UserDTO user, HttpServletRequest httpRequest, String eventType, Object eventDetails);
}
