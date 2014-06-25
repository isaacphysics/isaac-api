package uk.ac.cam.cl.dtg.segue.dto.users;

import java.util.Date;
import org.mongojack.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
	protected String _id;
	protected String givenName;
	protected String familyName;
	protected String email;
	protected String role;
	protected String school;
	protected String year;
	protected Boolean feedbackAgreement;
	protected Date registrationTime;

	@JsonCreator
	public User(@JsonProperty("_id") String _id,
			@JsonProperty("givenName") String givenName,
			@JsonProperty("familyName") String familyName,
			@JsonProperty("email") String email,
			@JsonProperty("role") String role,
			@JsonProperty("school") String school,
			@JsonProperty("year") String year,
			@JsonProperty("feedbackAgreement") Boolean feedbackAgreement,
			@JsonProperty("registrationTime") Date registrationTime) {
		this._id = _id;
		this.familyName = familyName;
		this.givenName = givenName;
		this.email = email;
		this.role = role;
		this.school = school;
		this.year = year;
		this.feedbackAgreement = feedbackAgreement;
		this.registrationTime = registrationTime;
	}

	/**
	 * Default constructor required for Jackson
	 */
	public User() {

	}

	@JsonProperty("_id")
	@ObjectId
	public String getDbId() {
		return _id;
	}

	@JsonProperty("_id")
	@ObjectId
	public void setDbId(String _id) {
		this._id = _id;
	}

	public String getFamilyName() {
		return familyName;
	}

	public String getGivenName() {
		return givenName;
	}

	public String getEmail() {
		return email;
	}

	public String getRole() {
		return role;
	}

	public String getSchool() {
		return school;
	}

	public String getYear() {
		return year;
	}

	public Boolean getFeedbackAgreement() {
		return feedbackAgreement;
	}

	public Date getRegistrationTime() {
		return registrationTime;
	}
}
