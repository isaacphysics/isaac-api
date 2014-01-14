package uk.ac.cam.cl.dtg.segue.dto;

import java.util.Date;

import org.mongojack.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {
	private String _id;
	protected String name;
	protected String email;
	protected String role;
	protected String school;
	protected String year;
	protected Boolean feedbackAgreement;
	protected Date registrationTime;
	
	@JsonCreator
	public User(@JsonProperty("_id") String _id,
				@JsonProperty("name") String name,
				@JsonProperty("email") String email,
				@JsonProperty("role") String role,
				@JsonProperty("school") String school,
				@JsonProperty("year") String year,
				@JsonProperty("feedbackAgreement") Boolean feedbackAgreement,
				@JsonProperty("registrationTime") Date registrationTime
				) {
		this._id = _id;
		this.name = name;
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

	public String getName() { return name; }
	
	public String getEmail() { return email; }
	
	public String getRole() { return role; }
	
	public String getSchool() { return school; }
	
	public String getYear() { return year; }
	
	public Boolean getFeedbackAgreement() { return feedbackAgreement; }

	public Date getRegistrationTime() { return registrationTime; }
}
