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
 * Domain Object to store relationships between users and gameboards such that
 * they can be shared etc.
 * 
 * This is a light weight DO that can be used to create a relationship between a particular gameboard and a user.
 * 
 */
public class UserGameboardsDO {
	@JsonProperty("_id")
	private String id;
	private String userId;
	private String gameboardId;
	private Date created;
	private Date lastVisited;

	/**
	 * Default constructor.
	 */
	public UserGameboardsDO() {
		
	}
	
	/**
	 * Create a new gameboard.
	 * @param id
	 * @param userId
	 * @param gameboardId
	 * @param created
	 * @param lastVisited
	 */
	public UserGameboardsDO(final String id, final String userId, final String gameboardId,
			final Date created, final Date lastVisited) {
		this.id = id;
		this.userId = userId;
		this.gameboardId = gameboardId;
		this.created = created;
		this.lastVisited = lastVisited;
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
	 * Gets the created.
	 * @return the created
	 */
	public Date getCreated() {
		return created;
	}
	
	/**
	 * Sets the created.
	 * @param created the created to set
	 */
	public void setCreated(final Date created) {
		this.created = created;
	}
	
	/**
	 * Gets the lastVisited.
	 * @return the lastVisited
	 */
	public Date getLastVisited() {
		return lastVisited;
	}
	
	/**
	 * Sets the lastVisited.
	 * @param lastVisited the lastVisited to set
	 */
	public void setLastVisited(final Date lastVisited) {
		this.lastVisited = lastVisited;
	}
}
