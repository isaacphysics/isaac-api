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
 * UserGroupDTO - this object represents a group or label assigned to users who have been placed into a group.
 * 
 * This allows users to be organised by class / project and for teachers (or those granted permission) to view progress.
 */
public class UserGroupDTO {
    private Long id;
    private String groupName;
    private Long ownerId;
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
    public UserGroupDTO(@Nullable final Long id, final String groupName, final Long ownerId, final Date created) {
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
    public Long getId() {
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
     * 
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * Sets the token.
     * 
     * @param token
     *            the token to set
     */
    public void setToken(final String token) {
        this.token = token;
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
        if (!(obj instanceof UserGroupDTO)) {
            return false;
        }
        UserGroupDTO other = (UserGroupDTO) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return String.format("UserGroupDTO [id=%s owner_id=%s name=%s]", id, ownerId, groupName);
    }
}
