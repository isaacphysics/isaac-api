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
package uk.ac.cam.cl.dtg.segue.dto.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;

/**
 * Minimal view of a User object.
 */
public class UserSummaryDTO extends AbstractSegueUserDTO {
    private Long id;
    private String givenName;
    private String familyName;
    private Role role;
    private boolean authorisedFullAccess;
    private EmailVerificationStatus emailVerificationStatus;
    private String examBoard;

    /**
     * UserSummaryDTO.
     */
    public UserSummaryDTO() {

    }

    /**
     * Gets the id.
     * @return the id
     */
    @JsonProperty("id")
    public Long getId() {
        return id;
    }


    /**
     * Sets the id.
     * @param id the id to set
     */
    @JsonProperty("id")
    public void setId(final Long id) {
        this.id = id;
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
     * Gets the role.
     *
     * @return the role
     */
    public Role getRole() {
        return role;
    }

    /**
     * Sets the role.
     *
     * @param role
     *            the role to set
     */
    public void setRole(final Role role) {
        this.role = role;
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

    /**
     * Gets the emailVerificationStatus.
     *
     * @return the emailVerificationStatus
     */
    public EmailVerificationStatus getEmailVerificationStatus() {
        return emailVerificationStatus;
    }

    /**
     * Sets the emailVerificationStatus.
     *
     * @param emailVerificationStatus
     *            the emailVerificationStatus to set
     */
    public void setEmailVerificationStatus(final EmailVerificationStatus emailVerificationStatus) {
        this.emailVerificationStatus = emailVerificationStatus;
    }

    /**
     * Gets the examBoard.
     *
     * @return the examBoard
     */
    public String getExamBoard() {
        return examBoard;
    }

    /**
     * Sets the examBoard.
     *
     * @param examBoard
     *            the examBoard to set
     */
    public void setExamBoard(final String examBoard) {
        this.examBoard = examBoard;
    }
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("UserSummaryDTO [databaseId=");
        builder.append(id);
        builder.append(", givenName=");
        builder.append(givenName);
        builder.append(", familyName=");
        builder.append(familyName);
        builder.append(", emailVerificationStatus=");
        builder.append(emailVerificationStatus.name());
        builder.append(", authorisedFullAccess=");
        builder.append(authorisedFullAccess);
        builder.append(", examBoard=");
        builder.append(examBoard);
        builder.append("]");
        return builder.toString();
    }
}
