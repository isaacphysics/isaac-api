/**
 * Copyright 2017 James Sharkey
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

/**
 * Teacher User Summary object, which contains additional information (email).
 */
public class DetailedUserSummaryDTO extends UserSummaryDTO {
    private String email;

    /**
     * UserSummaryDTO.
     */
    public DetailedUserSummaryDTO() {

    }

    /**
     * Gets the email.
     * @return the email
     */
    public String getEmail() {
        return email;
    }


    /**
     * Sets the email.
     * @param email the id to set
     */
    public void setEmail(final String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserSummaryDTO [databaseId=");
        builder.append(this.getLegacyDbId());
        builder.append(", givenName=");
        builder.append(this.getGivenName());
        builder.append(", familyName=");
        builder.append(this.getFamilyName());
        builder.append(", email=");
        builder.append(email);
        builder.append(", authorisedFullAccess=");
        builder.append(this.isAuthorisedFullAccess());
        builder.append("]");
        return builder.toString();
    }
}
