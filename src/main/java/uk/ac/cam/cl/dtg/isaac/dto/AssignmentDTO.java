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
package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is the Data Transfer Object used to store Assignments in the segue CMS.
 */
public class AssignmentDTO {
	@JsonProperty("_id")
	private String id;
	private String gameboardId;
	private String ownerUserId;

	private Date creationDate;
	
	/**
	 * Complete AssignmentDTO constructor with all dependencies.
	 * 
	 * @param id
	 *            - unique id for the gameboard
	 * @param gameboardId
	 *            - The gameboard to assign as homework.
	 * @param ownerUserId
	 *            - User id of the owner of the gameboard.
	 * @param creationDate
	 *            - the date the assignment was created.
	 */
	public AssignmentDTO(final String id, final String gameboardId, final String ownerUserId, final Date creationDate) {
		this.id = id;
		this.gameboardId = gameboardId;
		this.ownerUserId = ownerUserId;
		this.creationDate = creationDate;
	}
	
	/**
	 * Default constructor required for AutoMapping.
	 */
	public AssignmentDTO() {

	}

	/**
	 * Gets the id.
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 * @param id the id to set
	 */
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
