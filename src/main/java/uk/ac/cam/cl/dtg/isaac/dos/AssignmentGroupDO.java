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
package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AssignmentUserDO.
 *
 */
public class AssignmentGroupDO {
	@JsonProperty("_id")
	private String id;
	private String assignmentId;
	private String groupId;

	private Date creationDate;
	
	/**
	 * Complete AssignmentDO constructor with all dependencies.
	 * 
	 * @param id
	 *            - unique id for the gameboard
	 * @param assignmentId
	 *            - The gameboard to assign as homework.
	 * @param groupId
	 *            - User id who should be assigned the game board.
	 * @param creationDate
	 *            - The date the assignment was created.
	 */
	public AssignmentGroupDO(final String id, final String assignmentId, final String groupId,
			final Date creationDate) {
		this.id = id;
		this.groupId = groupId;
		this.creationDate = creationDate;
	}
	
	/**
	 * Default constructor required for AutoMapping.
	 */
	public AssignmentGroupDO() {

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
	 * Gets the assignmentId.
	 * @return the assignmentId
	 */
	public String getAssignmentId() {
		return assignmentId;
	}

	/**
	 * Sets the assignmentId.
	 * @param assignmentId the assignmentId to set
	 */
	public void setAssignmentId(final String assignmentId) {
		this.assignmentId = assignmentId;
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
