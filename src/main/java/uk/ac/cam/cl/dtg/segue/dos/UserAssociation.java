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
package uk.ac.cam.cl.dtg.segue.dos;

import java.util.Date;

import javax.annotation.Nullable;

import org.mongojack.ObjectId;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A UserAssociation represents the fact that a user has granted permissions to view their users information to another
 * user.
 * 
 */
public class UserAssociation {
    private String id;
    private String userIdGrantingPermission;
    private String userIdReceivingPermission;
    private Date created;

    /**
     * UserAssociation.
     */
    public UserAssociation() {

    }

    /**
     * UserAssociation.
     * 
     * @param id
     *            - unique identifier for the database use only.
     * @param userIdGrantingPermission
     *            - The user who is granting permission (the owner of the data)
     * @param userIdReceivingPermission
     *            - The user who is receiving access.
     * @param created
     *            - date
     */
    public UserAssociation(@Nullable final String id, final String userIdGrantingPermission,
            final String userIdReceivingPermission, final Date created) {
        super();
        this.id = id;
        this.userIdGrantingPermission = userIdGrantingPermission;
        this.userIdReceivingPermission = userIdReceivingPermission;
        this.created = created;
    }

    /**
     * Gets the id.
     * 
     * @return the id
     */
    @JsonProperty("_id")
    @ObjectId
    public String getId() {
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
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Gets the userIdGrantingPermission.
     * 
     * @return the userIdGrantingPermission
     */
    public String getUserIdGrantingPermission() {
        return userIdGrantingPermission;
    }

    /**
     * Sets the userIdGrantingPermission.
     * 
     * @param userIdGrantingPermission
     *            the userIdGrantingPermission to set
     */
    public void setUserIdGrantingPermission(final String userIdGrantingPermission) {
        this.userIdGrantingPermission = userIdGrantingPermission;
    }

    /**
     * Gets the userIdReceivingPermission.
     * 
     * @return the userIdReceivingPermission
     */
    public String getUserIdReceivingPermission() {
        return userIdReceivingPermission;
    }

    /**
     * Sets the userIdReceivingPermission.
     * 
     * @param userIdReceivingPermission
     *            the userIdReceivingPermission to set
     */
    public void setUserIdReceivingPermission(final String userIdReceivingPermission) {
        this.userIdReceivingPermission = userIdReceivingPermission;
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
