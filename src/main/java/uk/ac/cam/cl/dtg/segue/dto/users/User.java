package uk.ac.cam.cl.dtg.segue.dto.users;

import java.util.Date;
import java.util.List;

import org.mongojack.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Object to represent a user of the system. This object will be persisted in the database.
 *
 */
public class User {
	@JsonProperty("_id")
	private String databaseId;
	private String givenName;
	private String familyName;
	private String email;
	private String role;
	private String school;
	private Date dateOfBirth;
	private Date registrationDate;
	
	private List<LinkedAccount> linkedAccounts;

	/**
	 * Full constructor for the User object.
	 * 
	 * @param databaseId - Our database Unique ID
	 * @param givenName - Equivalent to firstname
	 * @param familyName - Equivalent to second name
	 * @param email - primary e-mail address
	 * @param role - role description
	 * @param school - unique school identifier.
	 * @param dateOfBirth - date of birth to help with monitoring
	 * @param registrationTime - date of registration
	 * @param linkedAccounts - the list of linked authentication provider accounts.
	 */
	@JsonCreator
	public User(@JsonProperty("_id") final String databaseId,
			@JsonProperty("givenName") final String givenName,
			@JsonProperty("familyName") final String familyName,
			@JsonProperty("email") final String email,
			@JsonProperty("role") final String role,
			@JsonProperty("school") final String school,
			@JsonProperty("dateOfBirth") final Date dateOfBirth,
			@JsonProperty("registrationTime") final Date registrationTime,
			@JsonProperty("linkedAccounts") final List<LinkedAccount> linkedAccounts) {
		this.databaseId = databaseId;
		this.familyName = familyName;
		this.givenName = givenName;
		this.email = email;
		this.role = role;
		this.school = school;
		this.dateOfBirth = dateOfBirth;
		this.registrationDate = registrationTime;
		this.linkedAccounts = linkedAccounts;
	}

	/**
	 * Default constructor required for Jackson.
	 */
	public User() {

	}

	/**
	 * Gets the database id for the user object.
	 * @return database id as a string.
	 */
	@JsonProperty("_id")
	@ObjectId
	public final String getDbId() {
		return databaseId;
	}

	/**
	 * Sets the database id for the user object.
	 * @param id the db id for the user.
	 */
	@JsonProperty("_id")
	@ObjectId
	public final void setDbId(final String id) {
		this.databaseId = id;
	}

	/**
	 * Gets the family name for the user.
	 * 
	 * @return familyName.
	 */
	public final String getFamilyName() {
		return familyName;
	}

	
	/**
	 * Gets the given name / firstname of the user.
	 * 
	 * @return users given name.
	 */
	public final String getGivenName() {
		return givenName;
	}

	/**
	 * Gets the users e-mail address.
	 * 
	 * @return users email address
	 */
	public final String getEmail() {
		return email;
	}

	/**
	 * Gets the users role information.
	 * 
	 * @return the role of the user.
	 */
	public final String getRole() {
		return role;
	}

	/**
	 * Gets the unique id of the school the user should belong to.
	 * 
	 * @return school id.
	 */
	public final String getSchool() {
		return school;
	}

	/**
	 * Gets the date of birth for the given user.
	 * 
	 * @return date of birth
	 */
	public final Date getDateOfBirth() {
		return dateOfBirth;
	}

	/**
	 * Gets the date of when the user first registered with us.
	 * @return the date in which the user registered.
	 */
	public final Date getRegistrationTime() {
		return registrationDate;
	}
	
	/**
	 * Gets the list of linked accounts for this user.
	 * @return list of linked accounts.
	 */
	public final List<LinkedAccount> getLinkedAccounts() {
		return linkedAccounts;
	}
}
