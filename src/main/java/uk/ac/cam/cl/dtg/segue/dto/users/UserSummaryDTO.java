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
package uk.ac.cam.cl.dtg.segue.dto.users;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User Summary object.
 */
public class UserSummaryDTO extends AbstractSegueUserDTO {
    private String databaseId;
    private String givenName;
    private String familyName;
    private String email;
    private boolean authorisedFullAccess;

    /**
     * UserSummaryDTO.
     */
    public UserSummaryDTO() {

    }

    /**
     * Gets the databaseId.
     * 
     * @return the databaseId
     */
    @JsonProperty("_id")
    public String getDbId() {
        return databaseId;
    }

    /**
     * Sets the databaseId.
     * 
     * @param databaseId
     *            the databaseId to set
     */
    @JsonProperty("_id")
    public void setDbId(final String databaseId) {
        this.databaseId = databaseId;
    }

    /**
     * Gets the givenName.
     * 
     * @return the givenName
     */
    public String getGivenName() {
        return givenName;
    }

    /**
     * Sets the givenName.
     * 
     * @param givenName
     *            the givenName to set
     */
    public void setGivenName(final String givenName) {
        this.givenName = givenName;
    }

    /**
     * Gets the familyName.
     * 
     * @return the familyName
     */
    public String getFamilyName() {
        return familyName;
    }

    /**
     * Sets the familyName.
     * 
     * @param familyName
     *            the familyName to set
     */
    public void setFamilyName(final String familyName) {
        this.familyName = familyName;
    }

    /**
     * Gets the email.
     * 
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email.
     * 
     * @param email
     *            the email to set
     */
    public void setEmail(final String email) {
        this.email = email;
    }

    /**
     * Gets the authorisedFullAccess.
     * 
     * @return the authorisedFullAccess
     */
    public boolean isAuthorisedFullAccess() {
        return authorisedFullAccess;
    }

    /**
     * Sets the authorisedFullAccess.
     * 
     * @param authorisedFullAccess
     *            the authorisedFullAccess to set
     */
    public void setAuthorisedFullAccess(final boolean authorisedFullAccess) {
        this.authorisedFullAccess = authorisedFullAccess;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserSummaryDTO [databaseId=");
        builder.append(databaseId);
        builder.append(", givenName=");
        builder.append(givenName);
        builder.append(", familyName=");
        builder.append(familyName);
        builder.append(", email=");
        builder.append(email);
        builder.append(", authorisedFullAccess=");
        builder.append(authorisedFullAccess);
        builder.append("]");
        return builder.toString();
    }
}
