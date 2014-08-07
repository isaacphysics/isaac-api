package uk.ac.cam.cl.dtg.segue.dto.users;

import java.util.Date;
import org.mongojack.ObjectId;

import uk.ac.cam.cl.dtg.segue.dos.users.Gender;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Object to represent a user of the system. This object will be persisted
 * in the database.
 * 
 */
public class UserDTO {
	@JsonProperty("_id")
	private String databaseId;
	private String givenName;
	private String familyName;
	private String email;
	private Role role;
	private String school;
	private Date dateOfBirth;
	private Gender gender;
	private Date registrationDate;
	
	private String schoolId;
	private String schoolOther;
	
	private Integer defaultLevel;
	
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
	 * @param role
	 *            - role description
	 * @param school
	 *            - unique school identifier.
	 * @param dateOfBirth
	 *            - date of birth to help with monitoring
	 * @param gender
	 *            - gender of the user
	 * @param registrationTime
	 *            - date of registration
	 * @param schoolId
	 *            - the list of linked authentication provider accounts.         
	 */
	@JsonCreator
	public UserDTO(
			@JsonProperty("_id") final String databaseId,
			@JsonProperty("givenName") final String givenName,
			@JsonProperty("familyName") final String familyName,
			@JsonProperty("email") final String email,
			@JsonProperty("role") final Role role,
			@JsonProperty("school") final String school,
			@JsonProperty("dateOfBirth") final Date dateOfBirth,
			@JsonProperty("gender") final Gender gender,
			@JsonProperty("registrationTime") final Date registrationTime,
			@JsonProperty("schoolId") final String schoolId) {
		this.databaseId = databaseId;
		this.familyName = familyName;
		this.givenName = givenName;
		this.email = email;
		this.role = role;
		this.school = school;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.registrationDate = registrationTime;
		this.schoolId = schoolId;
	}

	/**
	 * Default constructor required for Jackson.
	 */
	public UserDTO() {
		
	}

	/**
	 * Gets the database id for the user object.
	 * 
	 * @return database id as a string.
	 */
	@JsonProperty("_id")
	@ObjectId
	public final String getDbId() {
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
	public final void setDbId(final String id) {
		this.databaseId = id;
	}

	/**
	 * Gets the givenName.
	 * @return the givenName
	 */
	public final String getGivenName() {
		return givenName;
	}

	/**
	 * Sets the givenName.
	 * @param givenName the givenName to set
	 */
	public final void setGivenName(final String givenName) {
		this.givenName = givenName;
	}

	/**
	 * Gets the familyName.
	 * @return the familyName
	 */
	public final String getFamilyName() {
		return familyName;
	}

	/**
	 * Sets the familyName.
	 * @param familyName the familyName to set
	 */
	public final void setFamilyName(final String familyName) {
		this.familyName = familyName;
	}

	/**
	 * Gets the email.
	 * @return the email
	 */
	public final String getEmail() {
		return email;
	}

	/**
	 * Sets the email.
	 * @param email the email to set
	 */
	public final void setEmail(final String email) {
		this.email = email;
	}

	/**
	 * Gets the role.
	 * @return the role
	 */
	public final Role getRole() {
		return role;
	}

	/**
	 * Sets the role.
	 * @param role the role to set
	 */
	public final void setRole(final Role role) {
		this.role = role;
	}

	/**
	 * Gets the school.
	 * @return the school
	 */
	public final String getSchool() {
		return school;
	}

	/**
	 * Sets the school.
	 * @param school the school to set
	 */
	public final void setSchool(final String school) {
		this.school = school;
	}

	/**
	 * Gets the dateOfBirth.
	 * @return the dateOfBirth
	 */
	public final Date getDateOfBirth() {
		return dateOfBirth;
	}

	/**
	 * Sets the dateOfBirth.
	 * @param dateOfBirth the dateOfBirth to set
	 */
	public final void setDateOfBirth(final Date dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	/**
	 * Gets the gender.
	 * @return the gender
	 */
	public final Gender getGender() {
		return gender;
	}

	/**
	 * Sets the gender.
	 * @param gender the gender to set
	 */
	public final void setGender(final Gender gender) {
		this.gender = gender;
	}

	/**
	 * Gets the registrationDate.
	 * @return the registrationDate
	 */
	public final Date getRegistrationDate() {
		return registrationDate;
	}

	/**
	 * Sets the registrationDate.
	 * @param registrationDate the registrationDate to set
	 */
	public final void setRegistrationDate(final Date registrationDate) {
		this.registrationDate = registrationDate;
	}

	/**
	 * Gets the schoolId.
	 * @return the schoolId
	 */
	public final String getSchoolId() {
		return schoolId;
	}

	/**
	 * Sets the schoolId.
	 * @param schoolId the schoolId to set
	 */
	public final void setSchoolId(final String schoolId) {
		this.schoolId = schoolId;
	}

	/**
	 * Gets the databaseId.
	 * @return the databaseId
	 */
	public String getDatabaseId() {
		return databaseId;
	}

	/**
	 * Sets the databaseId.
	 * @param databaseId the databaseId to set
	 */
	public void setDatabaseId(final String databaseId) {
		this.databaseId = databaseId;
	}

	/**
	 * Gets the schoolOther.
	 * @return the schoolOther
	 */
	public String getSchoolOther() {
		return schoolOther;
	}

	/**
	 * Sets the schoolOther.
	 * @param schoolOther the schoolOther to set
	 */
	public void setSchoolOther(final String schoolOther) {
		this.schoolOther = schoolOther;
	}

	/**
	 * Gets the defaultLevel.
	 * @return the defaultLevel
	 */
	public Integer getDefaultLevel() {
		return defaultLevel;
	}

	/**
	 * Sets the defaultLevel.
	 * @param defaultLevel the defaultLevel to set
	 */
	public void setDefaultLevel(final Integer defaultLevel) {
		this.defaultLevel = defaultLevel;
	}
}
