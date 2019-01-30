/*
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
package uk.ac.cam.cl.dtg.segue.dos;

import java.util.Date;


/**
 * GroupMembership Object - this object represents a users membership to a group.
 *
 */
public class GroupMembership {
    private Long groupId;
    private Long userId;
    private GroupMembershipStatus status;
    private Date updated;
    private Date created;

    /**
     * Default Constructor.
     */
    public GroupMembership() {

    }

    /**
     * @param groupId - the group id
     * @param userId - the user id
     */
    public GroupMembership(final Long groupId, final Long userId, final GroupMembershipStatus status, final Date created, final Date updated) {
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
     * @param groupId
     *            the groupId to set
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
     * @param userId
     *            the userId to set
     */
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    /**
     * Get the status of the group membership
     * @return the group status
     */
    public GroupMembershipStatus getStatus() {
        return status;
    }

    /**
     * set the status of the group membership
     * @param status to set
     */
    public void setStatus(GroupMembershipStatus status) {
        this.status = status;
    }

    /**
     * Gets the updated date.
     *
     * @return the updated date
     */
    public Date getUpdated() {
        return updated;
    }

    /**
     * Sets the updated date.
     *
     * @param updated
     *            the updated date to set
     */
    public void setUpdated(final Date updated) {
        this.updated = updated;
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
}
