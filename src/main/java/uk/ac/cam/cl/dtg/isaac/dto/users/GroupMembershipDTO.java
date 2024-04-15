/**
 * Copyright 2017 Stephen Cummins
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

package uk.ac.cam.cl.dtg.isaac.dto.users;

import java.time.Instant;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembershipStatus;


/**
 * GroupMembership DTO - this object represents a users membership to a group.
 */
public class GroupMembershipDTO {
  private Long groupId;
  private Long userId;
  private GroupMembershipStatus status;
  private Instant updated;
  private Instant created;

  /**
   * Default Constructor.
   */
  public GroupMembershipDTO() {

  }

  /**
   * @param groupId - the group id
   * @param userId  - the user id
   * @param status  - the group membership status
   * @param created - the date the membership object was created
   * @param updated - the date the membership object was last updated
   */
  public GroupMembershipDTO(final Long groupId, final Long userId, final GroupMembershipStatus status,
                            final Instant created, final Instant updated) {
    this.groupId = groupId;
    this.userId = userId;
    this.status = status;
    this.created = created;
    this.updated = updated;
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
   * @param groupId the groupId to set
   */
  public void setGroupId(final Long groupId) {
    this.groupId = groupId;
  }

  /**
   * Gets the userId.
   *
   * @return the userId
   */
  public Long getUserId() {
    return userId;
  }

  /**
   * Sets the userId.
   *
   * @param userId the userId to set
   */
  public void setUserId(final Long userId) {
    this.userId = userId;
  }

  /**
   * Get the status of the group membership.
   *
   * @return the group status
   */
  public GroupMembershipStatus getStatus() {
    return status;
  }

  /**
   * set the status of the group membership.
   *
   * @param status to set
   */
  public void setStatus(final GroupMembershipStatus status) {
    this.status = status;
  }

  /**
   * Gets the updated date.
   *
   * @return the updated date
   */
  public Instant getUpdated() {
    return updated;
  }

  /**
   * Sets the updated date.
   *
   * @param updated the updated date to set
   */
  public void setUpdated(final Instant updated) {
    this.updated = updated;
  }

  /**
   * Gets the created.
   *
   * @return the created
   */
  public Instant getCreated() {
    return created;
  }

  /**
   * Sets the created.
   *
   * @param created the created to set
   */
  public void setCreated(final Instant created) {
    this.created = created;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("GroupMembershipDTO [groupId=");
    builder.append(groupId.toString());
    builder.append(", userId=");
    builder.append(userId.toString());
    builder.append(", status=");
    builder.append(status);
    builder.append("]");
    return builder.toString();
  }
}
