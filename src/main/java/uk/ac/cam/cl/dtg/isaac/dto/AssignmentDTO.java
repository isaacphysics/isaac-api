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

import org.mongojack.ObjectId;

import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is the Data Transfer Object used to store Assignments in the isaac CMS.
 */
public class AssignmentDTO {
    @ObjectId
    @JsonProperty("_id")
    private Long id;
    private String gameboardId;
    private GameboardDTO gameboard;
    private Long groupId;
    private Long ownerUserId;
    private UserSummaryDTO assignerSummary;
    private Date creationDate;
    private Date dueDate;

    /**
     * Complete AssignmentDTO constructor with all dependencies.
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
     * @param dueDate
     *            - the date the assignment should be completed by
     */
    public AssignmentDTO(final Long id, final String gameboardId, final Long ownerUserId, final Long groupId,
            final Date creationDate, final Date dueDate) {
        this.id = id;
        this.gameboardId = gameboardId;
        this.ownerUserId = ownerUserId;
        this.groupId = groupId;
        this.creationDate = creationDate;
        this.dueDate = dueDate;
    }

    /**
     * Default constructor required for AutoMapping.
     */
    public AssignmentDTO() {

    }

    /**
     * Gets the id.
     * 
     * @return the id
     */
    @JsonProperty("_id")
    @ObjectId
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     * 
     * @param id
     *            the id to set
     */
    @JsonProperty("_id")
    @ObjectId
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Gets the gameboardId.
     * 
     * @return the gameboardId
     */
    public String getGameboardId() {
        return gameboardId;
    }

    /**
     * Sets the gameboardId.
     * 
     * @param gameboardId
     *            the gameboardId to set
     */
    public void setGameboardId(final String gameboardId) {
        this.gameboardId = gameboardId;
    }

    /**
     * Gets the gameboardDTO.
     * 
     * @return the gameboardDTO
     */
    public GameboardDTO getGameboard() {
        return gameboard;
    }

    /**
     * Sets the gameboardDTO.
     * 
     * @param gameboardDTO
     *            the gameboardDTO to set
     */
    public void setGameboard(final GameboardDTO gameboardDTO) {
        this.gameboard = gameboardDTO;
    }

    /**
     * Gets the groupId.
     * 
     * @return the groupId
     */
    public Long getGroupId() {
        return groupId;
    }

    /**
     * Sets the groupId.
     * 
     * @param groupId
     *            the groupId to set
     */
    public void setGroupId(final Long groupId) {
        this.groupId = groupId;
    }

    /**
     * Gets the ownerUserId.
     * 
     * @return the ownerUserId
     */
    public Long getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * Sets the ownerUserId.
     * 
     * @param ownerUserId
     *            the ownerUserId to set
     */
    public void setOwnerUserId(final Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    /**
     * Gets the assignerSummary.
     * @return the assignerSummary
     */
    public UserSummaryDTO getAssignerSummary() {
        return assignerSummary;
    }

    /**
     * Sets the assignerSummary.
     * @param assignerSummary the assignerSummary to set
     */
    public void setAssignerSummary(final UserSummaryDTO assignerSummary) {
        this.assignerSummary = assignerSummary;
    }

    /**
     * Gets the creationDate.
     * 
     * @return the creationDate
     */
    public Date getCreationDate() {
        return creationDate;
    }

    /**
     * Sets the creationDate.
     * 
     * @param creationDate
     *            the creationDate to set
     */
    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * get the due date of the assignment
     * @return
     */
    public Date getDueDate() {
        return dueDate;
    }

    /**
     * set the due date of an assignment
     * @param dueDate
     */
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }
}
