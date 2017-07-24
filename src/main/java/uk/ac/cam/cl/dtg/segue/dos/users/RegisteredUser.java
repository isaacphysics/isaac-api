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
package uk.ac.cam.cl.dtg.segue.dos.users;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Object to represent a user of the system. This object will be persisted in the database.
 * 
 */
public class RegisteredUser extends AbstractSegueUser {
    @JsonProperty("_id")
    private Long id;

    private String givenName;
    private String familyName;
    private String email;
    private Role role;
    private Date dateOfBirth;
    private Gender gender;
    private Date registrationDate;
    private String schoolId;
    private String schoolOther;

    private Integer defaultLevel;

    private String emailVerificationToken;
    private EmailVerificationStatus emailVerificationStatus;

    private Date lastUpdated;
    private Date lastSeen;

    /**
     * Full constructor for the User object.
     * 
     * @param id
     *            - Our database Unique ID
     * @param givenName
     *            - Equivalent to firstname
     * @param familyName
     *            - Equivalent to second name
     * @param email
     *            - primary e-mail address
     * @param role
     *            - role description
     * @param dateOfBirth
     *            - date of birth to help with monitoring
     * @param gender
     *            - gender of the user
     * @param registrationDate
     *            - date of registration
     * @param lastUpdated
     *            - the date this user was last updated.
     * @param emailVerificationToken
     *            - the most recent token generated to verify email addresses
     * @param emailVerificationStatus
     *            - whether the user has verified their email or not
     */
    @JsonCreator
    public RegisteredUser(@JsonProperty("id") final Long id,
            @JsonProperty("givenName") final String givenName, @JsonProperty("familyName") final String familyName,
            @JsonProperty("email") final String email, @JsonProperty("role") final Role role,
            @JsonProperty("dateOfBirth") final Date dateOfBirth, @JsonProperty("gender") final Gender gender,
            @JsonProperty("registrationDate") final Date registrationDate,
            @JsonProperty("lastUpdated") final Date lastUpdated,
            @JsonProperty("emailVerificationToken") final String emailVerificationToken,
            @JsonProperty("emailVerificationStatus") final EmailVerificationStatus emailVerificationStatus) {
        this.id = id;
        this.familyName = familyName;
        this.givenName = givenName;
        this.email = email;
        this.role = role;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.registrationDate = registrationDate;
        this.lastUpdated = lastUpdated;
        this.emailVerificationToken = emailVerificationToken;
        this.emailVerificationStatus = emailVerificationStatus;
    }

    /**
     * Default constructor required for Jackson.
     */
    public RegisteredUser() {

    }

    /**
     * Gets the id (integer form).
     * @return the id
     */
    @JsonProperty("_id")
    public Long getId() {
        return id;
    }
    

    /**
     * Sets the id.
     * @param id the id to set
     */
    @JsonProperty("_id")
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Gets the givenName.
     * 
     * @return the givenName
     */
    public final String getGivenName() {
        return givenName;
    }

    /**
     * Sets the givenName.
     * 
     * @param givenName
     *            the givenName to set
     */
    public final void setGivenName(final String givenName) {
        this.givenName = givenName;
    }

    /**
     * Gets the familyName.
     * 
     * @return the familyName
     */
    public final String getFamilyName() {
        return familyName;
    }

    /**
     * Sets the familyName.
     * 
     * @param familyName
     *            the familyName to set
     */
    public final void setFamilyName(final String familyName) {
        this.familyName = familyName;
    }

    /**
     * Gets the email.
     * 
     * @return the email
     */
    public final String getEmail() {
        return email;
    }

    /**
     * Sets the email.
     * 
     * @param email
     *            the email to set
     */
    public final void setEmail(final String email) {
        if (email != null) {
            this.email = email.trim();
        } else {
            this.email = email;    
        }
    }

    /**
     * Gets the role.
     * 
     * @return the role
     */
    public final Role getRole() {
        return role;
    }

    /**
     * Sets the role.
     * 
     * @param role
     *            the role to set
     */
    public final void setRole(final Role role) {
        this.role = role;
    }

    /**
     * Gets the dateOfBirth.
     * 
     * @return the dateOfBirth
     */
    public final Date getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Sets the dateOfBirth.
     * 
     * @param dateOfBirth
     *            the dateOfBirth to set
     */
    public final void setDateOfBirth(final Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Gets the gender.
     * 
     * @return the gender
     */
    public final Gender getGender() {
        return gender;
    }

    /**
     * Sets the gender.
     * 
     * @param gender
     *            the gender to set
     */
    public final void setGender(final Gender gender) {
        this.gender = gender;
    }

    /**
     * Gets the registrationDate.
     * 
     * @return the registrationDate
     */
    public final Date getRegistrationDate() {
        return registrationDate;
    }

    /**
     * Sets the registrationDate.
     * 
     * @param registrationDate
     *            the registrationDate to set
     */
    public final void setRegistrationDate(final Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    /**
     * Gets the schoolId.
     * 
     * @return the schoolId
     */
    public final String getSchoolId() {
        return schoolId;
    }

    /**
     * Sets the schoolId.
     * 
     * @param schoolId
     *            the schoolId to set
     */
    public final void setSchoolId(final String schoolId) {
        this.schoolId = schoolId;
    }

    /**
     * Gets the schoolOther.
     * 
     * @return the schoolOther
     */
    public String getSchoolOther() {
        return schoolOther;
    }

    /**
     * Sets the schoolOther.
     * 
     * @param schoolOther
     *            the schoolOther to set
     */
    public void setSchoolOther(final String schoolOther) {
        this.schoolOther = schoolOther;
    }

    /**
     * Gets the defaultLevel.
     * 
     * @return the defaultLevel
     */
    public Integer getDefaultLevel() {
        return defaultLevel;
    }

    /**
     * Sets the defaultLevel.
     * 
     * @param defaultLevel
     *            the defaultLevel to set
     */
    public void setDefaultLevel(final Integer defaultLevel) {
        this.defaultLevel = defaultLevel;
    }

    /**
     * Sets the email verification token.
     * 
     * @param verificationToken
     *            token created by authenticator
     */
    public final void setEmailVerificationToken(final String verificationToken) {
        this.emailVerificationToken = verificationToken;
    }

    /**
     * Gets the resetExpiry.
     * 
     * @return the resetExpiry
     */
    public final String getEmailVerificationToken() {
        return this.emailVerificationToken;
    }

    /**
     * @return the emailVerified
     */
    public EmailVerificationStatus getEmailVerificationStatus() {
        return emailVerificationStatus;
    }

    /**
     * @param status
     *            - sets whether email has been verified
     */
    public void setEmailVerificationStatus(final EmailVerificationStatus status) {
        this.emailVerificationStatus = status;
    }    
    
    /**
     * Gets the lastUpdated.
     * 
     * @return the lastUpdated
     */
    public Date getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Sets the lastUpdated.
     * 
     * @param lastUpdated
     *            the lastUpdated to set
     */
    public void setLastUpdated(final Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /**
     * Gets the lastSeen.
     * 
     * @return the lastSeen
     */
    public Date getLastSeen() {
        return lastSeen;
    }

    /**
     * Sets the lastSeen.
     * 
     * @param lastSeen
     *            the lastSeen to set
     */
    public void setLastSeen(final Date lastSeen) {
        this.lastSeen = lastSeen;
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
        if (!(obj instanceof RegisteredUser)) {
            return false;
        }
        RegisteredUser other = (RegisteredUser) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
