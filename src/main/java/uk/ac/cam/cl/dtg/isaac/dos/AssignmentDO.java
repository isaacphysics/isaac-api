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
package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.Date;

import org.mongojack.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is the Domain Object used to store Assignments in the isaac CMS.
 */
public class AssignmentDO {
	@ObjectId
	@JsonProperty("_id")
	private String id;
	private String gameboardId;
	private String groupId;
	private String ownerUserId;
	private Date creationDate;
	
	/**
	 * Complete AssignmentDO constructor with all dependencies.
	 * 
	 * @param id
	 *            - unique id for the gameboard
	 * @param gameboardId
	 *            - The gameboard to assign as homework.
	 * @param ownerUserId
	 *            - User id of the owner of the gameboard.
	 * @param groupId
	 *            - Group id who should be assigned the game board.
	 * @param creationDate
	 *            - the date the assignment was created.
	 */
	public AssignmentDO(final String id, final String gameboardId, final String ownerUserId,
			final String groupId, final Date creationDate) {
		this.id = id;
		this.gameboardId = gameboardId;
		this.ownerUserId = ownerUserId;
		this.groupId = groupId;
		this.creationDate = creationDate;
	}
	
	/**
	 * Default constructor required for AutoMapping.
	 */
	public AssignmentDO() {

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
	 * Gets the gameboardId.
	 * @return the gameboardId
	 */
	public String getGameboardId() {
		return gameboardId;
	}

	/**
	 * Sets the gameboardId.
	 * @param gameboardId the gameboardId to set
	 */
	public void setGameboardId(final String gameboardId) {
		this.gameboardId = gameboardId;
	}

	/**
	 * Gets the groupId.
	 * @return the groupId
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Sets the groupId.
	 * @param groupId the groupId to set
	 */
	public void setGroupId(final String groupId) {
		this.groupId = groupId;
	}

	/**
	 * Gets the ownerUserId.
	 * @return the ownerUserId
	 */
	public String getOwnerUserId() {
		return ownerUserId;
	}

	/**
	 * Sets the ownerUserId.
	 * @param ownerUserId the ownerUserId to set
	 */
	public void setOwnerUserId(final String ownerUserId) {
		this.ownerUserId = ownerUserId;
	}

	/**
	 * Gets the creationDate.
	 * @return the creationDate
	 */
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * Sets the creationDate.
	 * @param creationDate the creationDate to set
	 */
	public void setCreationDate(final Date creationDate) {
		this.creationDate = creationDate;
	}
}
