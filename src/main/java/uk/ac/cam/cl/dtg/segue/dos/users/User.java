package uk.ac.cam.cl.dtg.segue.dos.users;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.mongojack.ObjectId;

import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Maps;

/**
 * Data Object to represent a user of the system. This object will be persisted
 * in the database.
 * 
 */
public class User {
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
	
	// Map of questionPage id -> map of question id -> questionAttempt information
	private Map<String, Map<String, QuestionAttempt>> questionAttempts;
	// TODO: move out of segue DTO into isaac one.
	private List<GameboardDTO> gameBoards;

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
	 * @param questionAttempts
	 *            - the list of question attempts made by the user.
	 */
	@JsonCreator
	public User(
			@JsonProperty("_id") final String databaseId,
			@JsonProperty("givenName") final String givenName,
			@JsonProperty("familyName") final String familyName,
			@JsonProperty("email") final String email,
			@JsonProperty("role") final Role role,
			@JsonProperty("school") final String school,
			@JsonProperty("dateOfBirth") final Date dateOfBirth,
			@JsonProperty("gender") final Gender gender,
			@JsonProperty("registrationTime") final Date registrationTime,
			@JsonProperty("schoolId") final String schoolId,
			@JsonProperty("questionAttempts") 
			final Map<String, Map<String, QuestionAttempt>> questionAttempts) {
		this.databaseId = databaseId;
		this.familyName = familyName;
		this.givenName = givenName;
		this.email = email;
		this.role = role;
		this.school = school;
		this.dateOfBirth = dateOfBirth;
		this.registrationDate = registrationTime;
		this.schoolId = schoolId;
		this.questionAttempts = questionAttempts;
	}

	/**
	 * Default constructor required for Jackson.
	 */
	public User() {
		this.questionAttempts = Maps.newHashMap();
		this.gameBoards = new ArrayList<GameboardDTO>();
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
	public final Role getRole() {
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
	 * Get Gender for this user.
	 * 
	 * @return the gender
	 */
	public final Gender getGender() {
		return gender;
	}

	/**
	 * Gets the date of when the user first registered with us.
	 * 
	 * @return the date in which the user registered.
	 */
	public final Date getRegistrationTime() {
		return registrationDate;
	}

	/**
	 * Gets the list of SchoolId for this user.
	 * 
	 * @return the schoolId that the user is a member of.
	 */
	public final String getSchoolId() {
		return schoolId;
	}

	/**
	 * Get Question attempts for this user.
	 * 
	 * @return list of attempts
	 */
	public final Map<String, Map<String, QuestionAttempt>> getQuestionAttempts() {
		return questionAttempts;
	}

	/**
	 * Gets the gameBoards.
	 * 
	 * @return the gameBoards
	 */
	public final List<GameboardDTO> getGameBoards() {
		return gameBoards;
	}

	/**
	 * Sets the gameBoards.
	 * 
	 * @param gameBoards
	 *            the gameBoards to set
	 */
	public final void setGameBoards(final List<GameboardDTO> gameBoards) {
		this.gameBoards = gameBoards;
	}

}
