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

/**
 * AssociationToken. This allows one user to request permission to view other users details.
 * 
 * This token will be used to make new associations between users.
 */
public class AssociationToken {
    private String token;
    private Long ownerUserId;
    private Long groupId;

    /**
	 * 
	 */
    public AssociationToken() {

    }

    /**
     * AssociationToken - Default Constructor.
     * 
     * @param token
     *            - unique id and token string.
     * @param ownerUserId
     *            - id of user who should be granted permission
     * @param groupId
     *            - group / label that users who use this token should be put in / labelled.
     */
    public AssociationToken(final String token, final Long ownerUserId, final Long groupId) {
        this.token = token;
        this.ownerUserId = ownerUserId;
        this.groupId = groupId;
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
}
