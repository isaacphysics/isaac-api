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

import java.util.Date;
import java.util.List;

import org.mongojack.ObjectId;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object to represent a user of the system. This object will be persisted in the database.
 * 
 */
public class RegisteredUserDTO extends AbstractSegueUserDTO {
    @JsonProperty("_id")
    private String databaseId;
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

    private List<AuthenticationProvider> linkedAccounts;
    private boolean hasSegueAccount;

    private boolean firstLogin = false;
    private Date lastUpdated;
    private Date lastSeen;

    private String emailVerificationToken;
    private Date emailVerificationExpiryDate;
    private Boolean emailVerified;

    /**
     * Full constructor for the User object.
     * 
     * @param databaseId
     *            - Our database Unique ID
     * @param givenName
     *            - Equivalent to firstname
     * @param familyName
     *            - Equivalent to second name
     * @param email
     *            - primary e-mail address
     * @param dateOfBirth
     *            - date of birth to help with monitoring
     * @param gender
     *            - gender of the user
     * @param registrationDate
     *            - date of registration
     * @param schoolId
     *            - the list of linked authentication provider accounts.
     */
    @JsonCreator
    public RegisteredUserDTO(@JsonProperty("_id") final String databaseId,
            @JsonProperty("givenName") final String givenName, @JsonProperty("familyName") final String familyName,
            @JsonProperty("email") final String email, @JsonProperty("dateOfBirth") final Date dateOfBirth,
            @JsonProperty("gender") final Gender gender, @JsonProperty("registrationDate") final Date registrationDate,
            @JsonProperty("schoolId") final String schoolId) {
        this.databaseId = databaseId;
        this.familyName = familyName;
        this.givenName = givenName;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.registrationDate = registrationDate;
        this.schoolId = schoolId;
    }

    /**
     * @return the emailVerificationToken
     */
    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    /**
     * @param emailVerificationToken
     *            the emailVerificationToken to set
     */
    public void setEmailVerificationToken(final String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    /**
     * @return the emailVerificationExpiryDate
     */
    public Date getEmailVerificationExpiryDate() {
        return emailVerificationExpiryDate;
    }

    /**
     * @param emailVerificationExpiryDate
     *            the emailVerificationExpiryDate to set
     */
    public void setEmailVerificationExpiryDate(final Date emailVerificationExpiryDate) {
        this.emailVerificationExpiryDate = emailVerificationExpiryDate;
    }

    /**
     * @return the emailVerified
     */
    public Boolean isEmailVerified() {
        return emailVerified;
    }

    /**
     * @param emailVerified
     *            the emailVerified to set
     */
    public void setEmailVerified(final Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    /**
     * Default constructor required for Jackson.
     */
    public RegisteredUserDTO() {

    }

    /**
     * Gets the database id for the user object.
     * 
     * @return database id as a string.
     */
    @JsonProperty("_id")
    @ObjectId
    public String getDbId() {
        return databaseId;
    }

    /**
     * Sets the database id for the user object.
     * 
     * @param id
     *            the db id for the user.
     */
    @JsonProperty("_id")
    @ObjectId
    public void setDbId(final String id) {
        this.databaseId = id;
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
     * Gets the dateOfBirth.
     * 
     * @return the dateOfBirth
     */
    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Sets the dateOfBirth.
     * 
     * @param dateOfBirth
     *            the dateOfBirth to set
     */
    public void setDateOfBirth(final Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Gets the gender.
     * 
     * @return the gender
     */
    public Gender getGender() {
        return gender;
    }

    /**
     * Sets the gender.
     * 
     * @param gender
     *            the gender to set
     */
    public void setGender(final Gender gender) {
        this.gender = gender;
    }

    /**
     * Gets the registrationDate.
     * 
     * @return the registrationDate
     */
    public Date getRegistrationDate() {
        return registrationDate;
    }

    /**
     * Sets the registrationDate.
     * 
     * @param registrationDate
     *            the registrationDate to set
     */
    public void setRegistrationDate(final Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    /**
     * Gets the schoolId.
     * 
     * @return the schoolId
     */
    public String getSchoolId() {
        return schoolId;
    }

    /**
     * Sets the schoolId.
     * 
     * @param schoolId
     *            the schoolId to set
     */
    public void setSchoolId(final String schoolId) {
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
     * Gets the linkedAccounts.
     * 
     * @return the linkedAccounts
     */
    public List<AuthenticationProvider> getLinkedAccounts() {
        return linkedAccounts;
    }

    /**
     * Sets the linkedAccounts.
     * 
     * @param linkedAccounts
     *            the linkedAccounts to set
     */
    public void setLinkedAccounts(final List<AuthenticationProvider> linkedAccounts) {
        this.linkedAccounts = linkedAccounts;
    }

    /**
     * Gets the hasSegueAccount.
     * 
     * @return the hasSegueAccount
     */
    public boolean getHasSegueAccount() {
        return hasSegueAccount;
    }

    /**
     * Sets the hasSegueAccount.
     * 
     * @param hasSegueAccount
     *            the hasSegueAccount to set
     */
    public void setHasSegueAccount(final boolean hasSegueAccount) {
        this.hasSegueAccount = hasSegueAccount;
    }

    /**
     * Gets the firstLogin.
     * 
     * @return the firstLogin
     */
    public boolean isFirstLogin() {
        return firstLogin;
    }

    /**
     * Sets the firstLogin.
     * 
     * @param firstLogin
     *            the firstLogin to set
     */
    public void setFirstLogin(final boolean firstLogin) {
        this.firstLogin = firstLogin;
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
        result = prime * result + ((databaseId == null) ? 0 : databaseId.hashCode());
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
        if (!(obj instanceof RegisteredUserDTO)) {
            return false;
        }
        RegisteredUserDTO other = (RegisteredUserDTO) obj;
        if (databaseId == null) {
            if (other.databaseId != null) {
                return false;
            }
        } else if (!databaseId.equals(other.databaseId)) {
            return false;
        }
        return true;
    }

    /**
     * A method that tests if each field in the object is equal to each in the other.
     * 
     * @param obj
     *            - to check
     * @return true if the same false if not.
     */
    public boolean strictEquals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof RegisteredUserDTO)) {
            return false;
        }
        RegisteredUserDTO other = (RegisteredUserDTO) obj;
        if (databaseId == null) {
            if (other.databaseId != null) {
                return false;
            }
        } else if (!databaseId.equals(other.databaseId)) {
            return false;
        }
        if (dateOfBirth == null) {
            if (other.dateOfBirth != null) {
                return false;
            }
        } else if (!dateOfBirth.equals(other.dateOfBirth)) {
            return false;
        }
        if (defaultLevel == null) {
            if (other.defaultLevel != null) {
                return false;
            }
        } else if (!defaultLevel.equals(other.defaultLevel)) {
            return false;
        }
        if (email == null) {
            if (other.email != null) {
                return false;
            }
        } else if (!email.equals(other.email)) {
            return false;
        }
        if (familyName == null) {
            if (other.familyName != null) {
                return false;
            }
        } else if (!familyName.equals(other.familyName)) {
            return false;
        }
        if (firstLogin != other.firstLogin) {
            return false;
        }
        if (gender != other.gender) {
            return false;
        }
        if (givenName == null) {
            if (other.givenName != null) {
                return false;
            }
        } else if (!givenName.equals(other.givenName)) {
            return false;
        }
        if (hasSegueAccount != other.hasSegueAccount) {
            return false;
        }
        if (lastUpdated == null) {
            if (other.lastUpdated != null) {
                return false;
            }
        } else if (!lastUpdated.equals(other.lastUpdated)) {
            return false;
        }
        if (linkedAccounts == null) {
            if (other.linkedAccounts != null) {
                return false;
            }
        } else if (!linkedAccounts.equals(other.linkedAccounts)) {
            return false;
        }
        if (registrationDate == null) {
            if (other.registrationDate != null) {
                return false;
            }
        } else if (!registrationDate.equals(other.registrationDate)) {
            return false;
        }
        if (role != other.role) {
            return false;
        }
        if (schoolId == null) {
            if (other.schoolId != null) {
                return false;
            }
        } else if (!schoolId.equals(other.schoolId)) {
            return false;
        }
        if (schoolOther == null) {
            if (other.schoolOther != null) {
                return false;
            }
        } else if (!schoolOther.equals(other.schoolOther)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "RegisteredUserDTO [databaseId=" + databaseId + ", givenName=" + givenName + ", familyName="
                + familyName + ", email=" + email + ", role=" + role + ", dateOfBirth=" + dateOfBirth + ", gender="
                + gender + ", registrationDate=" + registrationDate + ", schoolId=" + schoolId + ", schoolOther="
                + schoolOther + ", defaultLevel=" + defaultLevel + ", linkedAccounts=" + linkedAccounts
                + ", hasSegueAccount=" + hasSegueAccount + ", firstLogin=" + firstLogin + ", lastUpdated="
                + lastUpdated + "]";
    }
}
