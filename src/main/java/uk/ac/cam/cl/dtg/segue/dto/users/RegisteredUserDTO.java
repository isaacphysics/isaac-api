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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.ExamBoard;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.UserContext;

import java.util.Date;
import java.util.List;

/**
 * Data Transfer Object to represent a user of the system. This object will be persisted in the database.
 * 
 */
public class RegisteredUserDTO extends AbstractSegueUserDTO {
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
    private ExamBoard examBoard;
    private List<UserContext> registeredContexts;
    private Date registeredContextsLastConfirmed;

    private boolean firstLogin = false;
    private Date lastUpdated;
    private Date lastSeen;
    private EmailVerificationStatus emailVerificationStatus;

    /**
     * Full constructor for the User object.
     * 
     * @param givenName
     *            - Equivalent to firstname
     * @param familyName
     *            - Equivalent to second name
     * @param email
     *            - primary e-mail address
     * @param emailVerificationStatus
     *            - verification status of email address
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
    public RegisteredUserDTO(
            @JsonProperty("givenName") final String givenName, @JsonProperty("familyName") final String familyName,
            @JsonProperty("email") final String email, 
            @JsonProperty("verificationStatus") final EmailVerificationStatus emailVerificationStatus,
            @JsonProperty("dateOfBirth") final Date dateOfBirth,
            @JsonProperty("gender") final Gender gender, @JsonProperty("registrationDate") final Date registrationDate,
            @JsonProperty("schoolId") final String schoolId) {
        this.familyName = familyName;
        this.givenName = givenName;
        this.email = email;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.registrationDate = registrationDate;
        this.schoolId = schoolId;
        this.emailVerificationStatus = emailVerificationStatus;
    }


    /**
     * Default constructor required for Jackson.
     */
    public RegisteredUserDTO() {

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
     * Gets the id.
     * @return the id
     * @deprecated - TODO need to remove _id from frontend
     */
    @JsonProperty("_id")
    @Deprecated
    public Long getLegacyId() {
        return this.getId();
    }


    /**
     * Sets the id.
     * @param id the id to set
     * @deprecated - TODO need to remove _id from frontend
     */
    @JsonProperty("_id")
    @Deprecated
    public void setLegacyId(final Long id) {
        this.setId(id);
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
     * @return email verification status
     */
    public EmailVerificationStatus getEmailVerificationStatus() {
        return this.emailVerificationStatus;
    }
    /**
     * @param emailVerificationStatus the new verification status
     */
    public void setEmailVerificationStatus(final EmailVerificationStatus emailVerificationStatus) {
        this.emailVerificationStatus = emailVerificationStatus;
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
     * Gets the examBoard.
     *
     * @return the examBoard
     */
    public ExamBoard getExamBoard() {
        return examBoard;
    }

    /**
     * Sets the examBoard.
     *
     * @param examBoard
     *            the examBoard to set
     */
    public void setExamBoard(final ExamBoard examBoard) {
        this.examBoard = examBoard;
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

    public List<UserContext> getRegisteredContexts() {
        return registeredContexts;
    }

    public void setRegisteredContexts(List<UserContext> registeredContexts) {
        this.registeredContexts = registeredContexts;
    }

    public Date getRegisteredContextsLastConfirmed() {
        return registeredContextsLastConfirmed;
    }

    public void setRegisteredContextsLastConfirmed(Date registeredContextsLastConfirmed) {
        this.registeredContextsLastConfirmed = registeredContextsLastConfirmed;
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
        if (!(obj instanceof RegisteredUserDTO)) {
            return false;
        }
        RegisteredUserDTO other = (RegisteredUserDTO) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
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
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (dateOfBirth == null) {
            if (other.dateOfBirth != null) {
                return false;
            }
        } else if (!dateOfBirth.equals(other.dateOfBirth)) {
            return false;
        }
        if (email == null) {
            if (other.email != null) {
                return false;
            }
        } else if (!email.equals(other.email)) {
            return false;
        }
        if (emailVerificationStatus == null) {
            if (other.emailVerificationStatus != null) {
                return false;
            }
        } else if (emailVerificationStatus.compareTo(other.emailVerificationStatus) != 0) {
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
        if (lastUpdated == null) {
            if (other.lastUpdated != null) {
                return false;
            }
        } else if (!lastUpdated.equals(other.lastUpdated)) {
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
        return "RegisteredUserDTO [id=" + id + ", givenName=" + givenName + ", familyName="
                + familyName + ", email=" + email + ", role=" + role + ", dateOfBirth=" + dateOfBirth + ", gender="
                + gender + ", registrationDate=" + registrationDate + ", schoolId=" + schoolId + ", schoolOther="
                + schoolOther +  ", emailVerificationStatus="
                + emailVerificationStatus + ", firstLogin=" + firstLogin + ", lastUpdated=" + lastUpdated + "]";
    }
}
