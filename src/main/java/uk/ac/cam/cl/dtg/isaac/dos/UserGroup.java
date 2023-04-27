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
package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.Date;
import jakarta.annotation.Nullable;

/**
 * UserGroupDO - this object represents a group or collection of users
 * 
 * This allows users to be organised by class / project and for teachers (or those granted permission) to view progress.
 */
public class UserGroup {
    private Long id;
    private String groupName;
    private Long ownerId;
    private GroupStatus status;
    private Date created;
    private boolean archived;
    private boolean additionalManagerPrivileges;
    private Date lastUpdated;

    /**
     * Default Constructor.
     */
    public UserGroup() {

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
    public UserGroup(@Nullable final Long id, final String groupName, final Long ownerId, final GroupStatus status, final Date created,
                     final boolean archived, final boolean additionalManagerPrivileges, final Date lastUpdated) {
        this.id = id;
        this.groupName = groupName;
        this.ownerId = ownerId;
        this.status = status;
        this.created = created;
        this.archived = archived;
        this.additionalManagerPrivileges = additionalManagerPrivileges;
        this.lastUpdated = lastUpdated;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the id.
     *
     * @param id - the id.
     */
    public void setId(final Long id) {
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
    public Long getOwnerId() {
        return ownerId;
    }

    /**
     * Sets the ownerId.
     * 
     * @param ownerId
     *            the ownerId to set
     */
    public void setOwnerId(final Long ownerId) {
        this.ownerId = ownerId;
    }

    /**
     * Get the status of the group.
     *
     * @return the status of the group - i.e. active or deleted
     */
    public GroupStatus getStatus() {
        return status;
    }

    /**
     * Set the status of the group.
     *
     * @param status e.g. active or deleted.
     */
    public void setStatus(final GroupStatus status) {
        this.status = status;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof UserGroup)) {
            return false;
        }
        UserGroup other = (UserGroup) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(final boolean archived) {
        this.archived = archived;
    }

    /**
     * @return whether additional managers of this group have owner powers or not
     */
    public boolean isAdditionalManagerPrivileges() {
        return this.additionalManagerPrivileges;
    }

    /**
     * @param additionalManagerPrivileges whether additional managers of this group should have owner powers or not
     */
    public void setAdditionalManagerPrivileges(final boolean additionalManagerPrivileges) {
        this.additionalManagerPrivileges = additionalManagerPrivileges;
    }

    /**
     * Gets the last updated date.
     *
     * @return the last updated date
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Sets the last updated date.
     *
     * @param lastUpdated
     *            the last updated date to set
     */
    public void setLastUpdated(final Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
