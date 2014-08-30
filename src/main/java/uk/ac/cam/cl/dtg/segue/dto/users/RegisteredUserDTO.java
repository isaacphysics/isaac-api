package uk.ac.cam.cl.dtg.segue.dto.users;

import java.util.Date;
import java.util.List;

import org.mongojack.ObjectId;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.dos.users.Gender;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Object to represent a user of the system. This object will be persisted
 * in the database.
 * 
 */
public class RegisteredUserDTO extends AbstractSegueUserDTO {
	@JsonProperty("_id")
	private String databaseId;
	private String givenName;
	private String familyName;
	private String email;

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
	 * @param registrationTime
	 *            - date of registration
	 * @param schoolId
	 *            - the list of linked authentication provider accounts.         
	 */
	@JsonCreator
	public RegisteredUserDTO(
			@JsonProperty("_id") final String databaseId,
			@JsonProperty("givenName") final String givenName,
			@JsonProperty("familyName") final String familyName,
			@JsonProperty("email") final String email,
			@JsonProperty("dateOfBirth") final Date dateOfBirth,
			@JsonProperty("gender") final Gender gender,
			@JsonProperty("registrationTime") final Date registrationTime,
			@JsonProperty("schoolId") final String schoolId) {
		this.databaseId = databaseId;
		this.familyName = familyName;
		this.givenName = givenName;
		this.email = email;
		this.dateOfBirth = dateOfBirth;
		this.gender = gender;
		this.registrationDate = registrationTime;
		this.schoolId = schoolId;
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

	/**
	 * Gets the linkedAccounts.
	 * @return the linkedAccounts
	 */
	public List<AuthenticationProvider> getLinkedAccounts() {
		return linkedAccounts;
	}

	/**
	 * Sets the linkedAccounts.
	 * @param linkedAccounts the linkedAccounts to set
	 */
	public void setLinkedAccounts(final List<AuthenticationProvider> linkedAccounts) {
		this.linkedAccounts = linkedAccounts;
	}

	/**
	 * Gets the hasSegueAccount.
	 * @return the hasSegueAccount
	 */
	public boolean getHasSegueAccount() {
		return hasSegueAccount;
	}

	/**
	 * Sets the hasSegueAccount.
	 * @param hasSegueAccount the hasSegueAccount to set
	 */
	public void setHasSegueAccount(final boolean hasSegueAccount) {
		this.hasSegueAccount = hasSegueAccount;
	}

	/**
	 * Gets the firstLogin.
	 * @return the firstLogin
	 */
	public boolean isFirstLogin() {
		return firstLogin;
	}

	/**
	 * Sets the firstLogin.
	 * @param firstLogin the firstLogin to set
	 */
	public void setFirstLogin(final boolean firstLogin) {
		this.firstLogin = firstLogin;
	}

	/**
	 * Gets the lastUpdated.
	 * @return the lastUpdated
	 */
	public Date getLastUpdated() {
		return lastUpdated;
	}

	/**
	 * Sets the lastUpdated.
	 * @param lastUpdated the lastUpdated to set
	 */
	public void setLastUpdated(final Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((databaseId == null) ? 0 : databaseId.hashCode());
		result = prime * result + ((dateOfBirth == null) ? 0 : dateOfBirth.hashCode());
		result = prime * result + ((defaultLevel == null) ? 0 : defaultLevel.hashCode());
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result + ((familyName == null) ? 0 : familyName.hashCode());
		result = prime * result + ((gender == null) ? 0 : gender.hashCode());
		result = prime * result + ((givenName == null) ? 0 : givenName.hashCode());
		result = prime * result + ((linkedAccounts == null) ? 0 : linkedAccounts.hashCode());
		result = prime * result + ((registrationDate == null) ? 0 : registrationDate.hashCode());
		result = prime * result + ((schoolId == null) ? 0 : schoolId.hashCode());
		result = prime * result + ((schoolOther == null) ? 0 : schoolOther.hashCode());
		result = prime * result + ((lastUpdated == null) ? 0 : lastUpdated.hashCode());
		return result;
	}	
}
