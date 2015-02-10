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
package uk.ac.cam.cl.dtg.segue.dto;

import java.util.Date;

import javax.annotation.Nullable;

import org.mongojack.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * UserGroupDTO - this object represents a group or label assigned to
 * users who have been placed into a group.
 * 
 * This allows users to be organised by class / project and for teachers (or
 * those granted permission) to view progress.
 */
public class UserGroupDTO {
	private String id;
	private String groupName;
	private String ownerId;
	private Date created;
	private String token;

	/**
	 * Default Constructor.
	 */
	public UserGroupDTO() {

	}

	/**
	 * Fully injected constructor.
	 * 
	 * @param id
	 *            - database id
	 * @param groupName
	 *            - name of the group
	 * @param ownerId
	 *            - owner of the group
	 * @param created
	 *            - date created.
	 */
	public UserGroupDTO(@Nullable final String id, final String groupName, final String ownerId,
			final Date created) {
		this.id = id;
		this.groupName = groupName;
		this.ownerId = ownerId;
		this.created = created;
	}

	/**
	 * Gets the _id.
	 * 
	 * @return the _id
	 */
	@JsonProperty("_id")
	@ObjectId
	public String getId() {
		return id;
	}

	/**
	 * Sets the _id.
	 * 
	 * @param id
	 *            the _id to set
	 */
	@JsonProperty("_id")
	@ObjectId
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * Gets the groupName.
	 * 
	 * @return the groupName
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * Sets the groupName.
	 * 
	 * @param groupName
	 *            the groupName to set
	 */
	public void setGroupName(final String groupName) {
		this.groupName = groupName;
	}

	/**
	 * Gets the ownerId.
	 * 
	 * @return the ownerId
	 */
	public String getOwnerId() {
		return ownerId;
	}

	/**
	 * Sets the ownerId.
	 * 
	 * @param ownerId
	 *            the ownerId to set
	 */
	public void setOwnerId(final String ownerId) {
		this.ownerId = ownerId;
	}

	/**
	 * Gets the created.
	 * 
	 * @return the created
	 */
	public Date getCreated() {
		return created;
	}

	/**
	 * Sets the created.
	 * 
	 * @param created
	 *            the created to set
	 */
	public void setCreated(final Date created) {
		this.created = created;
	}

	/**
	 * Gets the token.
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Sets the token.
	 * @param token the token to set
	 */
	public void setToken(final String token) {
		this.token = token;
	}
}
