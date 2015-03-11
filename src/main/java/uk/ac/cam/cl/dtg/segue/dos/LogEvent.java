/**
 * Copyright 2015 Stephen Cummins
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

import java.util.Date;

import org.mongojack.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An object that represents a log event.
 *
 */
public class LogEvent {
	private String id;
	private String eventType;
	private String eventDetailsType;
	private Object eventDetails;
	private String userId;
	private boolean anonymousUser;
	private String ipAddress;
	private Date timestamp;
	
	/**
	 * Create a log event.
	 */
	public LogEvent() {
		
	}

	/**
	 * Create a log event.
	 * @param eventType - the type of event
	 * @param eventDetailsType - the details type i.e. a class canonical name.
	 * @param eventDetails - the object containing extra information
	 * @param userId - of the user carryingout the operation.
	 * @param anonymousUser - is this an anonymouse user
	 * @param ipAddress - IP address of the user
	 * @param timestamp - when the log happened.
	 */
	public LogEvent(final String eventType, final String eventDetailsType, final Object eventDetails,
			final String userId, final boolean anonymousUser, final String ipAddress, final Date timestamp) {
		this.eventType = eventType;
		this.eventDetailsType = eventDetailsType;
		this.eventDetails = eventDetails;
		this.userId = userId;
		this.anonymousUser = anonymousUser;
		this.ipAddress = ipAddress;
		this.timestamp = timestamp;
	}

	/**
	 * Gets the id.
	 * @return the id
	 */
	@JsonProperty("_id")
	@ObjectId
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 * @param id the id to set
	 */
	@JsonProperty("_id")
	@ObjectId
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * Gets the eventType.
	 * @return the eventType
	 */
	public String getEventType() {
		return eventType;
	}

	/**
	 * Sets the eventType.
	 * @param eventType the eventType to set
	 */
	public void setEventType(final String eventType) {
		this.eventType = eventType;
	}

	/**
	 * Gets the eventDetailsType.
	 * @return the eventDetailsType
	 */
	public String getEventDetailsType() {
		return eventDetailsType;
	}

	/**
	 * Sets the eventDetailsType.
	 * @param eventDetailsType the eventDetailsType to set
	 */
	public void setEventDetailsType(final String eventDetailsType) {
		this.eventDetailsType = eventDetailsType;
	}

	/**
	 * Gets the eventDetails.
	 * @return the eventDetails
	 */
	public Object getEventDetails() {
		return eventDetails;
	}

	/**
	 * Sets the eventDetails.
	 * @param eventDetails the eventDetails to set
	 */
	public void setEventDetails(final Object eventDetails) {
		this.eventDetails = eventDetails;
	}

	/**
	 * Gets the userId.
	 * @return the userId
	 */
	public String getUserId() {
		return userId;
	}

	/**
	 * Sets the userId.
	 * @param userId the userId to set
	 */
	public void setUserId(final String userId) {
		this.userId = userId;
	}

	/**
	 * Gets the anonymousUser.
	 * @return the anonymousUser
	 */
	public boolean isAnonymousUser() {
		return anonymousUser;
	}

	/**
	 * Sets the anonymousUser.
	 * @param anonymousUser the anonymousUser to set
	 */
	public void setAnonymousUser(final boolean anonymousUser) {
		this.anonymousUser = anonymousUser;
	}

	/**
	 * Gets the ipAddress.
	 * @return the ipAddress
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * Sets the ipAddress.
	 * @param ipAddress the ipAddress to set
	 */
	public void setIpAddress(final String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * Gets the timestamp.
	 * @return the timestamp
	 */
	public Date getTimestamp() {
		return timestamp;
	}

	/**
	 * Sets the timestamp.
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(final Date timestamp) {
		this.timestamp = timestamp;
	}
}
