/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;


/**
 * This class is the Data Transfer Object used to store Assignments in the isaac CMS.
 */
public class AssignmentDTO implements IAssignmentLike {
  private Long id;
  private String gameboardId;
  private GameboardDTO gameboard;
  private Long groupId;
  private String groupName;
  private Long ownerUserId;
  private String notes;
  private UserSummaryDTO assignerSummary;
  private Instant creationDate;

  // dueDate is not read correctly as an epoch by the jackson converter, this forces conversion
  @JsonDeserialize(converter = LongToInstantConverter.class)
  private Instant dueDate;
  private Instant scheduledStartDate;

  /**
   * Complete AssignmentDTO constructor with all dependencies.
   *
   * @param id                 unique id for the gameboard
   * @param gameboardId        The gameboard to assign as homework.
   * @param ownerUserId        User id of the owner of the gameboard.
   * @param groupId            Group id who should be assigned the game board.
   * @param groupName          name string for the group to be assigned the game board.
   * @param notes              any additional information added to the assignment.
   * @param creationDate       the date the assignment was created.
   * @param dueDate            the date the assignment is due (should be completed by)
   * @param scheduledStartDate the date the assignment should be shown to users/groups that it is set to
   */
  public AssignmentDTO(final Long id, final String gameboardId, final Long ownerUserId, final Long groupId,
                       final String groupName, final String notes, final Instant creationDate, final Instant dueDate,
                       final Instant scheduledStartDate) {
    this.id = id;
    this.gameboardId = gameboardId;
    this.ownerUserId = ownerUserId;
    this.groupId = groupId;
    this.groupName = groupName;
    this.notes = notes;
    this.creationDate = creationDate;
    this.dueDate = dueDate;
    this.scheduledStartDate = scheduledStartDate;
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
   * @deprecated use getId  - TODO need to remove _id from frontend
   */
  @JsonProperty("_id")
  @Deprecated
  public Long getLegacyId() {
    return getId();
  }

  /**
   * Gets the id.
   *
   * @return the id
   */
  @Override
  public Long getId() {
    return id;
  }

  /**
   * Sets the id.
   *
   * @param id the id to set.
   * @deprecated use setId  - TODO need to remove _id from frontend
   */
  @JsonProperty("_id")
  @Deprecated
  public void setLegacyId(final Long id) {
    this.setId(id);
  }

  /**
   * Sets the id.
   *
   * @param id the id to set
   */
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
   * @param gameboardId the gameboardId to set
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
   * @param gameboardDTO the gameboardDTO to set
   */
  public void setGameboard(final GameboardDTO gameboardDTO) {
    this.gameboard = gameboardDTO;
  }

  /**
   * Gets the groupId.
   *
   * @return the groupId
   */
  @Override
  public Long getGroupId() {
    return groupId;
  }

  /**
   * Sets the groupId.
   *
   * @param groupId the groupId to set
   */
  public void setGroupId(final Long groupId) {
    this.groupId = groupId;
  }

  /**
   * get the group's name.
   *
   * @return groupName -- the group's name
   */
  public String getGroupName() {
    return groupName;
  }

  /**
   * set the group's name.
   *
   * @param groupName -- the group's name
   */
  public void setGroupName(final String groupName) {
    this.groupName = groupName;
  }

  /**
   * Gets the ownerUserId.
   *
   * @return the ownerUserId
   */
  @Override
  public Long getOwnerUserId() {
    return ownerUserId;
  }

  /**
   * Sets the ownerUserId.
   *
   * @param ownerUserId the ownerUserId to set
   */
  public void setOwnerUserId(final Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  /**
   * get notes to an assignment.
   *
   * @return notes - the notes
   */
  public String getNotes() {
    return notes;
  }

  /**
   * set notes to an assignment.
   *
   * @param notes - the notes
   */
  public void setNotes(final String notes) {
    this.notes = notes;
  }

  /**
   * Gets the assignerSummary.
   *
   * @return the assignerSummary
   */
  public UserSummaryDTO getAssignerSummary() {
    return assignerSummary;
  }

  /**
   * Sets the assignerSummary.
   *
   * @param assignerSummary the assignerSummary to set
   */
  @Override
  public void setAssignerSummary(final UserSummaryDTO assignerSummary) {
    this.assignerSummary = assignerSummary;
  }

  /**
   * Gets the creationDate.
   *
   * @return the creationDate
   */
  @Override
  public Instant getCreationDate() {
    return creationDate;
  }

  /**
   * Sets the creationDate.
   *
   * @param creationDate the creationDate to set
   */
  public void setCreationDate(final Instant creationDate) {
    this.creationDate = creationDate;
  }

  /**
   * get the due date of the assignment.
   *
   * @return dueDate
   */
  @Override
  public Instant getDueDate() {
    return dueDate;
  }

  /**
   * set the due date of an assignment.
   *
   * @param dueDate - date due
   */
  public void setDueDate(final Instant dueDate) {
    this.dueDate = dueDate;
  }

  /**
   * get the date of when the assignment should be displayed to users.
   *
   * @return scheduledStartDate
   */
  public Instant getScheduledStartDate() {
    return scheduledStartDate;
  }

  /**
   * set the date of when the assignment should be displayed to users.
   *
   * @param scheduledStartDate - the scheduled start date
   */
  public void setScheduledStartDate(final Instant scheduledStartDate) {
    this.scheduledStartDate = scheduledStartDate;
  }

}
