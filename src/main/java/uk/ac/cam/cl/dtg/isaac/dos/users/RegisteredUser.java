/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dos.users;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Data Object to represent a user of the system. This object will be persisted in the database.
 */
public class RegisteredUser extends AbstractSegueUser {
  private Long id;
  private String givenName;
  private String familyName;
  private String email;
  private Role role;
  private Instant dateOfBirth;
  private Gender gender;
  private Instant registrationDate;
  private String schoolId;
  private String schoolOther;
  private List<UserContext> registeredContexts;
  private Instant registeredContextsLastConfirmed;
  private String emailVerificationToken;
  private String emailToVerify;
  private EmailVerificationStatus emailVerificationStatus;
  private Boolean teacherPending;
  private Instant lastUpdated;
  private Instant lastSeen;
  private Instant privacyPolicyAcceptedTime;

  /**
   * Full constructor for the User object.
   */
  @JsonCreator
  public RegisteredUser(
      @JsonProperty("id") final Long id,
      @JsonProperty("givenName") final String givenName,
      @JsonProperty("familyName") final String familyName,
      @JsonProperty("email") final String email,
      @JsonProperty("role") final Role role,
      @JsonProperty("dateOfBirth") final Instant dateOfBirth,
      @JsonProperty("gender") final Gender gender,
      @JsonProperty("registrationDate") final Instant registrationDate,
      @JsonProperty("lastUpdated") final Instant lastUpdated,
      @JsonProperty("privacyPolicyAcceptedTime") final Instant privacyPolicyAcceptedTime,
      @JsonProperty("emailToVerify") final String emailToVerify,
      @JsonProperty("emailVerificationToken") final String emailVerificationToken,
      @JsonProperty("emailVerificationStatus") final EmailVerificationStatus emailVerificationStatus,
      @JsonProperty("teacherPending") final Boolean teacherPending
  ) {
    this.id = id;
    this.familyName = familyName;
    this.givenName = givenName;
    this.role = role;
    this.dateOfBirth = dateOfBirth;
    this.gender = gender;
    this.registrationDate = registrationDate;
    this.lastUpdated = lastUpdated;
    this.privacyPolicyAcceptedTime = privacyPolicyAcceptedTime;
    this.emailVerificationToken = emailVerificationToken;
    this.emailVerificationStatus = emailVerificationStatus;
    this.teacherPending = teacherPending;

    setEmail(email);
    setEmailToVerify(emailToVerify);
  }

  /**
   * Default constructor required for Jackson.
   */
  public RegisteredUser() {
  }

  // ID Management
  @JsonProperty("_id")
  @Deprecated
  public Long getLegacyId() {
    return this.id;
  }

  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  @JsonProperty("_id")
  public void setId(final Long id) {
    this.id = id;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(final String givenName) {
    this.givenName = givenName;
  }

  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(final String familyName) {
    this.familyName = familyName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(final String email) {
    this.email = trimIfNotNull(email);
  }

  public Role getRole() {
    return role;
  }

  public void setRole(final Role role) {
    this.role = role;
  }

  public Instant getDateOfBirth() {
    return dateOfBirth;
  }

  public void setDateOfBirth(final Instant dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public Gender getGender() {
    return gender;
  }

  public void setGender(final Gender gender) {
    this.gender = gender;
  }

  public Instant getRegistrationDate() {
    return registrationDate;
  }

  public void setRegistrationDate(final Instant registrationDate) {
    this.registrationDate = registrationDate;
  }

  public String getSchoolId() {
    return schoolId;
  }

  public void setSchoolId(final String schoolId) {
    this.schoolId = schoolId;
  }

  public String getSchoolOther() {
    return schoolOther;
  }

  public void setSchoolOther(final String schoolOther) {
    this.schoolOther = schoolOther;
  }

  public String getEmailToVerify() {
    return emailToVerify;
  }

  public void setEmailToVerify(final String emailToVerify) {
    this.emailToVerify = trimIfNotNull(emailToVerify);
  }

  public String getEmailVerificationToken() {
    return emailVerificationToken;
  }

  public void setEmailVerificationToken(final String emailVerificationToken) {
    this.emailVerificationToken = emailVerificationToken;
  }

  public EmailVerificationStatus getEmailVerificationStatus() {
    return emailVerificationStatus;
  }

  public void setEmailVerificationStatus(final EmailVerificationStatus emailVerificationStatus) {
    this.emailVerificationStatus = emailVerificationStatus;
  }

  public Instant getLastUpdated() {
    return lastUpdated;
  }

  public void setLastUpdated(final Instant lastUpdated) {
    this.lastUpdated = lastUpdated;
  }

  public Instant getPrivacyPolicyAcceptedTime() {
    return privacyPolicyAcceptedTime;
  }

  public void setPrivacyPolicyAcceptedTime(final Instant privacyPolicyAcceptedTime) {
    this.privacyPolicyAcceptedTime = privacyPolicyAcceptedTime;
  }

  public Instant getLastSeen() {
    return lastSeen;
  }

  public void setLastSeen(final Instant lastSeen) {
    this.lastSeen = lastSeen;
  }

  public Boolean getTeacherPending() {
    return teacherPending;
  }

  public void setTeacherPending(final Boolean teacherPending) {
    this.teacherPending = teacherPending;
  }

  public List<UserContext> getRegisteredContexts() {
    return registeredContexts;
  }

  public void setRegisteredContexts(final List<UserContext> registeredContexts) {
    this.registeredContexts = registeredContexts;
  }

  public Instant getRegisteredContextsLastConfirmed() {
    return registeredContextsLastConfirmed;
  }

  public void setRegisteredContextsLastConfirmed(final Instant registeredContextsLastConfirmed) {
    this.registeredContextsLastConfirmed = registeredContextsLastConfirmed;
  }

  /**
   * Utility method to trim strings consistently, handling null values.
   * Eliminates duplicated trimming logic in email setters.
   */
  private String trimIfNotNull(final String value) {
    return value != null ? value.trim() : null;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof RegisteredUser other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    return String.format(
        "RegisteredUser{id=%d, givenName='%s', familyName='%s', email='%s', role=%s, "
            + "dateOfBirth=%s, gender=%s, registrationDate=%s, schoolId='%s', schoolOther='%s', "
            + "emailVerificationToken='%s', emailToVerify='%s', emailVerificationStatus=%s, "
            + "teacherPending=%s, lastUpdated=%s, privacyPolicyAcceptedTime=%s, lastSeen=%s, "
            + "registeredContexts=%s, registeredContextsLastConfirmed=%s}",
        id, givenName, familyName, email, role, dateOfBirth, gender, registrationDate,
        schoolId, schoolOther, emailVerificationToken, emailToVerify, emailVerificationStatus,
        teacherPending, lastUpdated, privacyPolicyAcceptedTime, lastSeen,
        registeredContexts, registeredContextsLastConfirmed
    );
  }
}